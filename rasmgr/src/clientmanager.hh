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

#ifndef RASMGR_X_SRC_CLIENTMANAGER_HH_
#define RASMGR_X_SRC_CLIENTMANAGER_HH_

#include "clientmanagerconfig.hh"
#include "clientserverrequest.hh"
#include "clientserversession.hh"
#include "common/macros/utildefs.hh"
#include "common/thread/threadutil.hh"

#include <string>
#include <memory>
#include <thread>
#include <mutex>
#include <queue>
#include <atomic>

#include <xenium/harris_michael_hash_map.hpp>
#include <xenium/reclamation/generic_epoch_based.hpp>

namespace rasmgr
{

class Client;
class ClientCredentials;
class ClientServerMatcher;
class PeerManager;
class Server;
class ServerManager;
class UserManager;

/**
  The ClientManager class maintains a registry of clientId -> active client, as
  well as a queue of `Client`s waiting to be assigned to an available server.
  It allows to
  
  - register and deregister clients into its registry on ``connectClient`` and 
    ``disconnectClient`` calls respectively (handled in the 
    `ClientManagementService`). In ``connectClient`` the provided credentials
    are authenticated either via username/password or token mechanism.
  
  - open a db session assigns the client to an available rasserver.
    
    1. The client is added to a queue
  
    2. A separate thread is responsible for taking clients from the queue and
       assigning them as servers become available. This thread is triggered after
       a client
  
       - is added to the queue (1. above)
       - properly closes a DB session
       - is determined to be dead
       - or when none of the above have happened within the last 15 seconds.
  
       When the client queue checking is triggered, the thread
  
       - checks if any client is in the queue
       - if yes, then check if there are available servers to assign
       - if yes, remove the client from the queue and assign to a server
       - otherwise wait until triggered again
  
    An error is thrown if the queue is filled above a fixed capacity of 1000
    clients.
  
  - close a db session deallocates the client on the assigned local or remote
    server.
  
  - extend the "life" of the `Client` on request from the connected client
    (with a KeepAlive call to `ClientManagementService`). When the client
    fails to issue a KeepAlive call within a certain time period, the 
    corresponding `Client` will be considered dead by the cleanup thread and
    removed from the client list.
  
  - runs a cleanup thread at a fixed interval that removes dead clients from its
    registry
  
  - runs a queue check thread at a fixed interval of 15 seconds that tries to
    assign clients in a waiting queue to any available servers
  
  Used by the `ClientManagementService` to handle network requests from clients.
 */
class ClientManager
{
public:
    /**
     * @param config client configuration
     * @param userManager Instance of the user manager that holds information
     * about registered users. It is needed to evaluate the access credentials
     * @param serverManager Instance of the server manager that is used to retrieve
     * servers for clients of each client
     * @param peerManager the peer manager
     * @param csm an object matching clients to servers
     */
    ClientManager(const ClientManagerConfig &config,
                  const std::shared_ptr<UserManager> &userManager,
                  const std::shared_ptr<PeerManager> &peerManager,
                  const std::shared_ptr<ClientServerMatcher> &csm);
    
    DISABLE_COPY_AND_MOVE(ClientManager)

    /**
     * Destruct the ClientManager class object.
     */
    virtual ~ClientManager() = default;

    /**
     * Authenticate and connect the client to rasmgr. If the authentication
     * 
     * - succeeds, the UUID assigned to the client (out_clientUUID) will be returned
     * - fails, an exception is thrown
     * 
     * @param clientCredentials Credentials used to authenticate the client.
     * @param rasmgrHost The rasmgr hostname to which to connect
     * @return The UUID assigned to the connected client
     * @throws InexistentUserException
     * @throws InvalidTokenException
     */
    virtual std::uint32_t connectClient(const ClientCredentials &clientCredentials,
                                        const std::string &rasmgrHost);

    /**
     * Disconnect the client from rasmgr and remove it from its assigned server
     * if any. If the clientId is not found in the list of connected clients,
     * no error is thrown and only a message is logged in the rasmgr log.
     * @param clientId UUID of the client that will be disconnected.
     */
    virtual void disconnectClient(std::uint32_t clientId);

    /**
     * Open a DB session for the client with clientId and return a unique session id.
     * @param clientId UUID identifying the client
     * @param dbName Database the client wants to open
     * @param out_serverSession session ID identifying the client and assigned server.
     * @throws InexistentClientException
     * @throws NoAvailableServerException
     * @throws common::RuntimeException on invalid server hostname
     */
    virtual void openClientDbSession(std::uint32_t clientId,
                                     const std::string &dbName,
                                     ClientServerSession &out_serverSession);

    /**
     * Remove a client session from the client manager and assigned server.
     * @param clientId ID that uniquely identifies a client
     * @param sessionId ID that uniquely identifies a client session
     * @throws InexistentClientException
     */
    virtual void closeClientDbSession(std::uint32_t clientId,
                                      std::uint32_t sessionId);

    /**
     * Extend the liveliness of the client and prevent it from being removed
     * from rasmgr database of active clients.
     * @param clientId UUID of the client
     */
    virtual void keepClientAlive(std::uint32_t clientId);

    /**
     *  Get a copy of the configuration object used by the client manager.
     */
    const ClientManagerConfig &getConfig();

private:
    ClientManagerConfig config;
    std::shared_ptr<UserManager> userManager;
    std::shared_ptr<PeerManager> peerManager;
    std::shared_ptr<ClientServerMatcher> clientServerMatcher;
    
    /// Used to generate unique ids for each client
    std::atomic<std::uint32_t> nextClientId{};

    // -------------------------------------------------------------------------
    // manage all clients
    /// Map of clientId -> active client
    xenium::harris_michael_hash_map<
            std::uint32_t, std::shared_ptr<Client>,
            xenium::policy::reclaimer<xenium::reclamation::generic_epoch_based<>>> clients;
    common::PeriodicTaskExecutor checkAssignedClients;

    /// Evaluate the list of clients assigned to a server and remove the ones that have died.
    void evaluateAssignedClients();
};

} /* namespace rasmgr */

#endif /* CLIENTMANAGER_HH_ */
