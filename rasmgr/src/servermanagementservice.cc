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

#include <logging.hh>
#include "common/grpc/grpcutils.hh"

#include "servermanager.hh"

#include "servermanagementservice.hh"

namespace rasmgr
{
using grpc::Status;

using std::string;

ServerManagementService::ServerManagementService(std::shared_ptr<ServerManager> m)
{
    this->serverManager = m;
}

grpc::Status ServerManagementService::RegisterServer(__attribute__((unused)) grpc::ServerContext *context,
        const rasnet::service::RegisterServerReq *request,
        __attribute__((unused)) rasnet::service::Void *response)
{
    Status status = Status::OK;

    try
    {
        LDEBUG << "Registering server " << request->serverid();
        this->serverManager->registerServer(request->serverid());
    }
    catch (std::exception &ex)
    {
        status = common::GrpcUtils::convertExceptionToStatus(ex);
    }
    catch (...)
    {
        status = common::GrpcUtils::convertExceptionToStatus("Failed registering server");
    }

    return status;
}
} /* namespace rasmgr */
