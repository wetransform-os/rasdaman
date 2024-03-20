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

#include "globals.hh"
#include "config.h"
#include "version.h"
#include "rasmanager.hh"
#include "constants.hh"
#include "configuration.hh"

#include "common/crypto/crypto.hh"
#include "common/grpc/grpcutils.hh"
#include "common/logging/signalhandler.hh"
#include "common/exceptions/exception.hh"
#include "loggingutils.hh"

#include <sys/prctl.h>
#include <signal.h>
#include <sys/types.h>
#include <unistd.h>

INITIALIZE_EASYLOGGINGPP

void crashHandler(int sig, siginfo_t *info, void *ucontext);
void shutdownHandler(int sig, siginfo_t *info, void *ucontext);

using common::Crypto;
using rasmgr::Configuration;
using rasmgr::RasManager;
using rasmgr::RASMGR_RESULT_FAILED;
using rasmgr::RASMGR_RESULT_OK;

// RasManager object that orchestrates
std::shared_ptr<rasmgr::RasManager> manager;

static std::string logFilePath = "";

// -------------------------------------------------------------------------- //
//                           signal handlers                                  //
// -------------------------------------------------------------------------- //

void shutdownHandler(int /* sig */, siginfo_t *info, void * /* ucontext */)
{
    static bool alreadyExecuting{false};
    if (!alreadyExecuting)
    {
        alreadyExecuting = true;
        common::SignalHandler::printCrashDetailsASSafe(info, logFilePath.c_str());
        // TODO: kill rasservers
        // below is not AS-SAFE
//        if (manager)
//            manager->stop();
        exit(RASMGR_RESULT_OK);
    }
}

void crashHandler(int sig, siginfo_t *info, void * /* ucontext */)
{
    static bool alreadyExecuting{false};
    if (!alreadyExecuting)
    {
        alreadyExecuting = true;
        common::SignalHandler::printCrashDetailsASSafe(info, logFilePath.c_str());
        // TODO: kill rasservers
        // below is not AS-SAFE
//        if (manager)
//            manager->stop();
        exit(sig);
    }
}

void configureLogging(const Configuration &config)
{
    logFilePath = config.getLogFile();
    if (logFilePath.empty())
    {
        logFilePath = std::string(LOGDIR);
        if (logFilePath[logFilePath.length() - 1] != '/')
            logFilePath += "/";
        logFilePath += "rasmgr." + std::to_string(::getpid()) + ".log";
    }

    // setup log config
    common::LogConfiguration logConfig(std::string(CONFDIR), RASMGR_LOG_CONF);
    logConfig.configServerLogging(logFilePath, config.isQuiet());
    common::GrpcUtils::redirectGRPCLogToEasyLogging();

    // should shutdown signals be ignored?
    common::SignalHandler::handleShutdownSignals(shutdownHandler);
}

Configuration parseCmdLine(int argc, char **argv)
{
    // handle abort signals and ignore irrelevant signals
    common::SignalHandler::handleAbortSignals(crashHandler);
    common::SignalHandler::ignoreStandardSignals();

    if (Crypto::isMessageDigestAvailable(DEFAULT_DIGEST) == false)
    {
        std::cerr << "Error: Message Digest MD5 not available." << std::endl;
        exit(rasmgr::RASMGR_RESULT_NO_MD5);
    }

    Configuration config;
    bool result = config.parseCommandLineParameters(argc, argv);
    if (result == false)
    {
        std::cerr << "Error: failed parsing command-line parameters." << std::endl;
        exit(rasmgr::RASMGR_RESULT_ILL_ARGS);
    }
    return config;
}

int main(int argc, char **argv)
{
    auto config = parseCmdLine(argc, argv);
    configureLogging(config);

    LINFO << "rasmgr: rasdaman server manager tool " << RMANVERSION
          << " on base DBMS " << BASEDBSTRING << ".";
    LINFO << "Copyright (c) 2003-2021 Peter Baumann, rasdaman GmbH.\n"
          << "Rasdaman community is free software: you can redistribute it and/or modify "
             "it under the terms of the GNU General Public License as published by "
             "the Free Software Foundation, either version 3 of the License, or "
             "(at your option) any later version.\n"
             "Rasdaman community is distributed in the hope that it will be useful, "
             "but WITHOUT ANY WARRANTY; without even the implied warranty of "
             "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the "
             "GNU General Public License for more details.\n";

    int ret = RASMGR_RESULT_OK;

    manager.reset(new RasManager(config));
    try
    {
        manager->start();
    }
    catch (common::Exception &ex)
    {
        LERROR << "rasmanager failed with exception: " << ex.what();
        std::cerr << "rasmanager failed with exception: " << ex.what() << std::endl;
        ret = RASMGR_RESULT_FAILED;
    }
    catch (std::exception &ex)
    {
        LERROR << "rasmanager failed with exception: " << ex.what();
        std::cerr << "rasmanager failed with exception: " << ex.what() << std::endl;
        ret = RASMGR_RESULT_FAILED;
    }
    catch (...)
    {
        LERROR << "rasmanager failed for an unknown reason.";
        std::cerr << "rasmanager failed for an unknown reason." << std::endl;
        ret = RASMGR_RESULT_FAILED;
    }

    return ret;
}
