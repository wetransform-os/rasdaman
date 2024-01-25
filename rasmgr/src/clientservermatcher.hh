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

#ifndef RASMGR_X_SRC_CLIENTSERVERMATCHER_HH_
#define RASMGR_X_SRC_CLIENTSERVERMATCHER_HH_

#include "clientmanagerconfig.hh"
#include "clientserversession.hh"
#include "clientserverrequest.hh"
#include "common/macros/utildefs.hh"
#include "common/thread/threadutil.hh"

#include <string>
#include <memory>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <queue>
#include <atomic>

#include <tbb/concurrent_queue.h>
#include <xenium/harris_michael_hash_map.hpp>
#include <xenium/reclamation/generic_epoch_based.hpp>

namespace rasmgr
{

class Client;
class ClientCredentials;
class PeerManager;
class ServerManager;

/**
 * A struct allowing to communicate data (the session assigned to the client)
 * from the `evaluateWaitingClients()` thread back to the thread that created
 * this structure; that thread is waiting on the condition variable cv in the
 * `openClientDbSession(..)` method, and `evaluateWaitingClients()` does
 * `cv.notify()` when the client is assigned a session. The mutex mut is used
 * for synchronizing cv.
 */
struct WaitingClient
{
    enum class WaitingStatus {
        ASSIGNED,
        FAILED,
        WAITING
    };

    WaitingClient(const std::shared_ptr<Client> &c, const std::string &db)
        : client(c), serverSession(), dbName(db) {}

    // it's only every dynamically allocated and stored as a pointer
    DISABLE_COPY_AND_MOVE(WaitingClient)

    ~WaitingClient() = default;
    /// openDB will wait on this to be notified when a server is assigned
    std::condition_variable cv;
    /// mutex for the condition variable
    std::mutex mut;
    /// the client waiting to be assigned a server
    const std::shared_ptr<Client> &client;
    /// the assigne server session
    ClientServerSession serverSession;
    /// database name to open
    const std::string &dbName;
    /// true if a server session was assigned
    WaitingStatus assigned = WaitingStatus::WAITING;
    /// true after the client is done waiting for a server to be assigned
    bool doneWaiting{false};
    std::string errorMessage;
};

class ClientServerMatcher
{
public:
    explicit ClientServerMatcher(const ClientManagerConfig &c,
                                 const std::shared_ptr<ServerManager> &serverManager,
                                 const std::shared_ptr<PeerManager> &peerManager);

    DISABLE_COPY_AND_MOVE(ClientServerMatcher)

    virtual ~ClientServerMatcher();

    /// Assign a server to the given client, with details in out_serverSession.
    /// Called by ClientManager.
    void assignServer(const std::shared_ptr<Client> &client,
                      const std::string &dbName,
                      ClientServerSession &out_serverSession);
    
    /// When a client is done with it's work on the assigned rasserver, this is
    /// called  to release that server.
    void releaseServer();
    
private:
    /// Mutex used to prevent a free server being assigned to two different clients 
    /// when tryGetFreeLocalServer is called
    std::mutex serverManagerMutex;
    std::shared_ptr<ServerManager> serverManager;
    std::shared_ptr<PeerManager> peerManager;

    tbb::concurrent_bounded_queue<WaitingClient *> waitingClients;
    WaitingClient *nextClientToAssign{nullptr};
    common::PeriodicTaskExecutor checkWaitingClients;

    /// Evaluate the list of clients waiting to be assigned to a server.
    void evaluateWaitingClients();

    /**
     * Open a DB session for the client and return a unique session id.
     * @param client the client to be assigned
     * @param dbName Database the client wants to open
     * @param out_serverSession session ID identifying the client and assigned server.
     * @return true if an available server was found, false otherwise.
     * @throws InexistentClientException
     * @throws NoAvailableServerException
     * @throws common::RuntimeException on invalid server hostname
     */
    virtual bool tryGetFreeServer(const std::shared_ptr<Client> &client,
                                  const std::string &dbName,
                                  ClientServerSession &out_serverSession);

    /**
     * 1. Try to acquire a server for the given client.
     * 2. Add the session to the client manager.
     * 3. Fill the response to the client with the server's identity
     */
    bool tryGetFreeLocalServer(std::shared_ptr<Client> client,
                               const std::string &dbName,
                               ClientServerSession &out_serverSession);

    /// Try to get a free remote server for the client from the PeerManager.
    bool tryGetFreeRemoteServer(const ClientServerRequest &request,
                                ClientServerSession &out_serverSession);
};

} /* namespace rasmgr */

#endif /* RASMGR_X_SRC_CLIENTSERVERMATCHER_HH_ */
