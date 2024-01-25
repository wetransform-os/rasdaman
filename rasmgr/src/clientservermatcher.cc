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

#include "clientservermatcher.hh"
#include "client.hh"
#include "rasmgr/src/exceptions/clientevaluationexception.hh"
#include "server.hh"
#include "servermanager.hh"
#include "peermanager.hh"
#include "user.hh"
#include "common/string/stringutil.hh"
#include "exceptions/noavailableserverexception.hh"
#include "exceptions/clientevaluationexception.hh"
#include <logging.hh>
#include <mutex>

namespace rasmgr
{

ClientServerMatcher::ClientServerMatcher(const ClientManagerConfig &c,
                                         const std::shared_ptr<ServerManager> &sm,
                                         const std::shared_ptr<PeerManager> &pm)
    : serverManager{sm}, peerManager{pm},
      checkWaitingClients{
          [this]()
          {
              this->evaluateWaitingClients();
          },
          std::chrono::milliseconds(c.getClientLifeTime() / 2)}
{
    this->waitingClients.set_capacity(c.getMaxClientQueueSize());
}

ClientServerMatcher::~ClientServerMatcher()
{
    this->checkWaitingClients.stop();

    // notify all threads waiting in openClientDbSession
    WaitingClient *wc;
    while (this->waitingClients.try_pop(wc))
    {
        {
            LDEBUG << "~ClientManager() - lock WaitingClient mutex";
            std::lock_guard<std::mutex> lock(wc->mut);
            wc->assigned = WaitingClient::WaitingStatus::ASSIGNED;
            LDEBUG << "~ClientManager() - lock WaitingClient mutex done";
        }
        LDEBUG << "~ClientManager() - notify WaitingClient condition variable";
        wc->cv.notify_one();
    }
}

void ClientServerMatcher::assignServer(const std::shared_ptr<Client> &client,
                                       const std::string &dbName,
                                       ClientServerSession &out_serverSession)
{
    const auto clientId = client->getClientId();
    std::unique_ptr<WaitingClient> wc;
    wc.reset(new WaitingClient(client, dbName));
    {
        LDEBUG << "openClientDbSession - unique_lock on WaitingClient->mut " << clientId;
        std::unique_lock<std::mutex> lock(wc->mut);
        {
            LDEBUG << "openClientDbSession - add client to waitingClients " << clientId;
            bool added = this->waitingClients.try_push(wc.get());
            if (!added)
            {
                // queue capacity reached
                throw NoAvailableServerException();
            }
        }
        // wake up waiting clients thread as there's a new client in the queue
        LDEBUG << "openClientDbSession - notifyWaitingClientsThread() " << clientId;
        this->checkWaitingClients.wakeup();
        LDEBUG << "openClientDbSession - WaitingClient->cv.wait() " << clientId;
        wc->cv.wait(lock, [&wc]
                    {
                        return (wc->assigned != WaitingClient::WaitingStatus::WAITING);
                    });
        wc->doneWaiting = true;
        LDEBUG << "openClientDbSession - assigning client-server session to currentSession for client " << clientId;
        out_serverSession = wc->serverSession;
        LDEBUG << "openClientDbSession - unique_lock on WaitingClient->mut released " << clientId;
    }
    if (wc && wc->assigned == WaitingClient::WaitingStatus::FAILED)
    {
        throw ClientEvaluationException(wc->errorMessage);
    }
}

void ClientServerMatcher::releaseServer()
{
    this->checkWaitingClients.wakeup();
}

void ClientServerMatcher::evaluateWaitingClients()
{
    bool assigned = true;

    // set waiting client's error message and its status to failed, then notify cv
    auto notifyFailed = [](WaitingClient *wcl, std::uint32_t clientId, std::string errorMessage) 
    {
        {
            std::unique_lock<std::mutex> lock(wcl->mut);
            wcl->errorMessage = errorMessage;
            wcl->assigned = WaitingClient::WaitingStatus::FAILED;
        }
        LDEBUG << "evaluateWaitingClients() - WaitingClient->cv.notify_one() " << clientId;
        wcl->cv.notify_one();
        LDEBUG << "evaluateWaitingClients() - WaitingClient->cv.notify_one() done: " << clientId;
    };

    while (assigned)
    {
        // get the next waiting client to be assigned a server
        WaitingClient *wc = nullptr;
        if (nextClientToAssign)
        {
            // a previous run didn't manage to assign the client that was popped
            // from the queue, so retry with it again
            wc = nextClientToAssign;
            nextClientToAssign = nullptr;
        }
        else if (!this->waitingClients.try_pop(wc))
        {
            // queue empty, nothing to do
            return;
        }

        auto client = wc->client;
        const auto &clientId = client->getClientId();
        try
        {
            assigned = false;

            LDEBUG << "evaluateWaitingClients() - check if client is alive " << clientId;
            if (client->isAlive())
            {
                // try to assign a server to the client
                LDEBUG << "evaluateWaitingClients() - client is alive, try to assign a server " << clientId;
                assigned = tryGetFreeServer(client, wc->dbName, wc->serverSession);
#ifdef RASDEBUG
                if (assigned)
                {
                    LDEBUG << "evaluateWaitingClients() - client was assigned a server, will remove it from waiting list " << clientId;
                }
                else
                {
                    LDEBUG << "evaluateWaitingClients() - could not assign a server, keep waiting " << clientId;
                }
#endif
            }
            else
            {
                LDEBUG << "evaluateWaitingClients() - removing client from waiting "
                          "client list as it seems to be dead "
                       << clientId;
                assigned = true;
            }

            if (assigned)
            {
                // notify the original thread that the client has been assigned a server
                {
                    std::lock_guard<std::mutex> lock(wc->mut);
                    wc->assigned = WaitingClient::WaitingStatus::ASSIGNED;
                }
                    LDEBUG << "evaluateWaitingClients() - WaitingClient->cv.notify_one() " << clientId;
                    wc->cv.notify_one();
                    LDEBUG << "evaluateWaitingClients() - WaitingClient->cv.notify_one() done: " << clientId;
            }
            else
            {
                nextClientToAssign = wc;
            }
        }
        catch (common::Exception &ex)
        {
            LERROR << "Failed evaluating status of waiting client " << clientId << ": " << ex.what();
            notifyFailed(wc, clientId, ex.what());
        }
        catch (std::exception &ex)
        {
            LERROR << "Failed evaluating status of waiting client " << clientId << ": " << ex.what();
            notifyFailed(wc, clientId, ex.what());
        }
        catch (...)
        {
            LERROR << "Failed evaluating status of waiting client " << clientId;
            notifyFailed(wc, clientId, "Unexpected error occurred while evaluating client.");
        }
    }
}

bool ClientServerMatcher::tryGetFreeServer(const std::shared_ptr<Client> &client,
                                           const std::string &dbName,
                                           ClientServerSession &out_serverSession)
{
    static const std::string localhost("127.0.0.1");
    static const std::string localhostName = "localhost";

#define NORMALIZE_LOCALHOST(h)                                                    \
    if (h.empty() || common::StringUtil::equalsCaseInsensitive(h, localhostName)) \
        h = localhost;

    auto clientHost = client->getRasmgrHost();
    NORMALIZE_LOCALHOST(clientHost)

    const char *serverType;  // used only for debugging
    if (this->tryGetFreeLocalServer(client, dbName, out_serverSession))
    {
        auto serverHost = out_serverSession.serverHostName;
        NORMALIZE_LOCALHOST(serverHost)
        if (clientHost != localhost && clientHost != serverHost)
        {
            throw common::RuntimeException("No server is configured to listen on host '" +
                                           clientHost + "' in rasmgr.conf, the server -host is '" +
                                           serverHost + "'.");
        }
        serverType = "local";
    }
    else
    {
        // Try to get a remote server for the client.
        ClientServerRequest request(client->getUser()->getName(),
                                    client->getUser()->getPassword(),
                                    dbName);

        if (this->tryGetFreeRemoteServer(request, out_serverSession))
        {
            auto serverHost = out_serverSession.serverHostName;
            NORMALIZE_LOCALHOST(serverHost)
            if (clientHost != localhost && clientHost != serverHost && serverHost == "127.0.0.1")
            {
                throw common::RuntimeException("No server is configured to listen on host '" +
                                               clientHost + "' in rasmgr.conf, the server -host is '" +
                                               serverHost + "'.");
            }
            serverType = "remote";
        }
        else
        {
            return false;
        }
    }

    LDEBUG << "Allocated " << serverType << " server running on "
           << out_serverSession.serverHostName << ":" << out_serverSession.serverPort
           << " to client " << out_serverSession.clientSessionId
           << " connected to rasmgr on " << client->getRasmgrHost();
    return true;
}

bool ClientServerMatcher::tryGetFreeLocalServer(std::shared_ptr<Client> client,
                                                const std::string &dbName,
                                                ClientServerSession &out_serverSession)
{
    std::unique_lock<std::mutex> lock(this->serverManagerMutex);

    bool foundServer = false;

    //Try to get a free server that contains the requested database
    std::shared_ptr<Server> assignedServer;
    if (this->serverManager->tryGetAvailableServer(dbName, assignedServer))
    {
        // A value will be assigned to dbSessionId by addDbSession
        std::uint32_t dbSessionId;
        client->addDbSession(dbName, assignedServer, dbSessionId);

        out_serverSession.clientSessionId = client->getClientId();
        out_serverSession.dbSessionId = dbSessionId;
        out_serverSession.serverHostName = assignedServer->getHostName();
        out_serverSession.serverPort = static_cast<std::uint32_t>(assignedServer->getPort());

        foundServer = true;
    }

    return foundServer;
}

bool ClientServerMatcher::tryGetFreeRemoteServer(const ClientServerRequest &request,
                                                 ClientServerSession &out_serverSession)
{
    return this->peerManager->tryGetRemoteServer(request, out_serverSession);
}

} /* namespace rasmgr */
