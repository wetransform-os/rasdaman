/*
 * This file is part of rasdaman community.
 *
 * Rasdaman community is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Rasdaman community is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */

#include "clientmanager.hh"
#include "client.hh"
#include "clientcredentials.hh"
#include "clientservermatcher.hh"
#include "user.hh"
#include "usermanager.hh"
#include "servermanager.hh"
#include "peermanager.hh"

#include "exceptions/invalidtokenexception.hh"
#include "exceptions/inexistentuserexception.hh"
#include "exceptions/inexistentclientexception.hh"

#include <logging.hh>

#include <iostream>
#include <stdexcept>
#include <string>
#include <chrono>

namespace rasmgr
{

ClientManager::ClientManager(const ClientManagerConfig &c,
                             const std::shared_ptr<UserManager> &um,
                             const std::shared_ptr<PeerManager> &pm,
                             const std::shared_ptr<ClientServerMatcher> &csm)
    : config(c), userManager(um), peerManager(pm), clientServerMatcher{csm},
      checkAssignedClients{[this]() { this->evaluateAssignedClients(); },
                           std::chrono::milliseconds(config.getCleanupInterval())}
{
}

std::uint32_t ClientManager::connectClient(const ClientCredentials &clientCredentials,
                                           const std::string &rasmgrHost)
{
    /**
     * 1. Check if the user is a token, if yes verify its validity.
     * 2. If not, check if there is a user with the given credentials
     * 3. Generate a unique ID for the client
     * 4. Add the client to the list of managed clients
     */
    LDEBUG << "Connecting client username: " << clientCredentials.getUserName()
           << ", token: " << clientCredentials.getToken();

    std::shared_ptr<User> out_user;
    bool isUserValid = false;
    {
        LDEBUG << "Authenticating username and password client";
        isUserValid = this->userManager->tryGetUser(clientCredentials.getUserName(),
                                                    clientCredentials.getPasswordHash(),
                                                    out_user);
    }

    if (isUserValid)
    {
        LDEBUG << "Successfully authenticated user " << out_user->getName();

        // Generate a UID for the client
        const auto ret = ++nextClientId;
        const auto clientLifeTime = this->config.getClientLifeTime();
        auto client = std::make_shared<Client>(ret, out_user, clientLifeTime, rasmgrHost);

        LDEBUG << "Inserting client object " << ret << " into clients list...";
        this->clients.emplace(ret, client);
        LDEBUG << "Inserted client object " << ret << " into clients list.";
        return ret;
    }
    else
    {
        {
            throw InexistentUserException(clientCredentials.getUserName());
        }
    }
}

void ClientManager::disconnectClient(std::uint32_t clientId)
{
    /*
     * 1. Remove client with the given id from clients map. If not found just log a message.
     * 2. Remove the client from all the servers it might still be in. This ensures a clean exit.
     */
    std::shared_ptr<Client> client;

    // try to remove client from map; client above will be non-null if client found.
    {
        auto it = this->clients.find(clientId);
        if (it != clients.end())
        {
            client = it->second;
            this->clients.erase(it);
        }
    }

    if (client)
    {
        // Remove the client from all the servers where it had opened sessions
        client->removeClientFromServer();
        LDEBUG << "Client " << clientId << " has been removed from the active clients list";
    }
    else
    {
        LDEBUG << "Client " << clientId << " was not found in the active "
               << "clients list while trying to disconnect it, nothing to do.";
    }
}

void ClientManager::openClientDbSession(std::uint32_t clientId,
                                        const std::string &dbName,
                                        ClientServerSession &out_serverSession)
{
    std::shared_ptr<Client> client;
    auto it = this->clients.find(clientId);
    if (it != clients.end())
        client = it->second;
    if (!client)
    {
        throw InexistentClientException(
            clientId, "cannot assign a rasserver to it, possibly the client "
                      "failed to successfully connect first?");
    }

    this->clientServerMatcher->assignServer(client, dbName, out_serverSession);
}

void ClientManager::closeClientDbSession(std::uint32_t clientId, std::uint32_t sessionId)
{
    std::shared_ptr<Client> client;
    {
        auto it = this->clients.find(clientId);
        if (it != clients.end())
            client = it->second;
    }

    if (client)
    {
        client->removeDbSession(sessionId);
    }
    else
    {
        RemoteClientSession clientSession(clientId, sessionId);
        if (this->peerManager->isRemoteClientSession(clientSession))
        {
            this->peerManager->releaseServer(clientSession);
        }
        else
        {
            throw InexistentClientException(
                clientId, "cannot close the client connection to rasserver.");
        }
    }

    // wake up waiting clients thread as one client has finished evaluation
    LDEBUG << "closeClientDbSession - notifyWaitingClientsThread() " << clientId;
    this->clientServerMatcher->releaseServer();
}

void ClientManager::keepClientAlive(std::uint32_t clientId)
{
    std::shared_ptr<Client> client;
    auto it = this->clients.find(clientId);
    if (it != clients.end())
        client = it->second;

    if (client)
    {
        client->resetLiveliness();
    }
    else
    {
        throw InexistentClientException(
            clientId, "cannot reset client's liveliness.");
    }
}

const ClientManagerConfig &ClientManager::getConfig()
{
    return this->config;
}

void ClientManager::evaluateAssignedClients()
{
    LTRACE << "Evaluating assigned clients.";
    auto it = this->clients.begin();
    while (it != this->clients.end())
    {
        auto client = it->second;
        const auto &clientId = client->getClientId();
        try
        {
            if (!client->isAlive())
            {
                LDEBUG << "Removing client from assigned client list as it seems to be dead: " << clientId;
                {
                    it = this->clients.erase(it);
                }
                client->removeClientFromServer();

                // wake up waiting clients thread as a client has been removed,
                // potentially releasing space for a new client to be assigned a server
                LDEBUG << "evaluateAssignedClients() - notifyWaitingClientsThread() " << clientId;
                this->clientServerMatcher->releaseServer();
            }
            else
            {
                ++it;
            }
        }
        catch (common::Exception &ex)
        {
            LERROR << "Failed evaluating status of assigned client " << clientId << ": " << ex.what();
        }
        catch (std::exception &ex)
        {
            LERROR << "Failed evaluating status of assigned client " << clientId << ": " << ex.what();
        }
        catch (...)
        {
            LERROR << "Failed evaluating status of assigned client " << clientId;
        }
    }
}

} /* namespace rasmgr */
