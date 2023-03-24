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

#ifndef COMMON_THREADUTIL_HH
#define COMMON_THREADUTIL_HH

#include "common/macros/utildefs.hh"
#include <thread>
#include <mutex>
#include <condition_variable>
#include <atomic>
#include <functional>

namespace common
{

/**
 * Contains the necessary parts for a commonly occuring pattern in the rasmgr
 * code: a thread that does something every X milliseconds.
 */
struct PeriodicTaskExecutor
{
    PeriodicTaskExecutor(const std::function<void(void)> &task,
                         std::chrono::milliseconds interval)
        : repeatInterval{interval},
          executor{&PeriodicTaskExecutor::run, this, task}
    {
    }

    DISABLE_COPY_AND_MOVE(PeriodicTaskExecutor)

    /// execute tasks every X ms
    std::chrono::milliseconds repeatInterval;
    /// thread that will execute the task
    std::thread executor;

    /// used to safely stop the executor when signaling the cv
    std::mutex stopMutex;
    /// the executor thread is waiting on this condition variable, and another
    /// thread does cv.notify_one() when it needs to stop it.
    std::condition_variable cv;
    /// executor thread is running if true; should be set to false when we
    /// want to stop it.
    std::atomic<bool> running{true};
    /// set to true when the executor is awoken outside it's regular repetition
    std::atomic<bool> awoken{false};

    /// stop the executor; this will block until the thread exits.
    void stop()
    {
        {
            std::lock_guard<std::mutex> lock(this->stopMutex);
            this->running = false;
        }
        this->cv.notify_one();
        this->executor.join();
    }
    
    /// wakeup the executor so it can run immediately; the wakeup is done only
    /// if running is true.
    void wakeup()
    {
        if (this->running && !this->awoken)
        {
            {
                std::lock_guard<std::mutex> lock(this->stopMutex);
                this->awoken = true;
            }
            this->cv.notify_one();
        }
    }

private:
    
    /// abstracts away the condition variable handling, the user should just
    /// pass their task logic.
    void run(const std::function<void(void)> &task)
    {
        std::unique_lock<std::mutex> lock(this->stopMutex);
        while (this->running)
        {
            // Wait on the condition variable until notified to stop or timeout
            // after repeatInterval ms have passed
            this->awoken = false;
            auto stopReason = this->cv.wait_for(lock, this->repeatInterval);
            if (stopReason == std::cv_status::timeout || this->awoken)
            {
                // timeout, it's time to execute the task
                task();
            }
        }
    }
};

} /* namespace common */

#endif /* COMMON_THREADUTIL_HH */
