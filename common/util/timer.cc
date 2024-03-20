/*
* This file is part of rasdaman community.
*
* Rasdaman community is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Rasdaman community is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
*
* Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Peter Baumann /
rasdaman GmbH.
*
* For more information please see <http://www.rasdaman.org>
* or contact Peter Baumann via <baumann@rasdaman.com>.
*/

#include "timer.hh"
#include "common/time/date.h"
#include "logging.hh"
#include <chrono>
#include <ctime>
#include <string>

using namespace std::chrono;

namespace common
{

std::string common::TimerUtil::getCurrentDateTime()
{
    auto currTime = system_clock::to_time_t(system_clock::now());
    char buf[80];
    auto tstruct = *localtime(&currTime);
    strftime(buf, sizeof(buf), "%Y-%m-%d %X", &tstruct);
    return std::string(buf);
}

std::string TimerUtil::getCurrentDateTimeUTC()
{
    /// Uses MIT-licenced lib: https://howardhinnant.github.io/date/date.html
    return date::format("%F %T", system_clock::now());
}

uintmax_t TimerUtil::getSecondsSinceEpoch()
{
    auto result = time(NULL);
    return uintmax_t(result);
}

int TimerUtil::getCurrentMonth()
{
    auto currTime = system_clock::now();
    auto tt = system_clock::to_time_t(currTime);
    auto currCalTime = *std::localtime(&tt);
    return currCalTime.tm_mon;
}

size_t TimerUtil::getMillisecondsUntilNextMonth()
{
    auto currTime = system_clock::now();

    auto tt = system_clock::to_time_t(currTime);

    auto currCalTime = *std::localtime(&tt);

    // yyyy-(mm+1)-01 00:00:10 - we add 10 seconds just in case to account for
    // potential slowness in calculating the final duration in milliseconds
    auto nextCalTime = currCalTime;
    ++nextCalTime.tm_mon;
    if (nextCalTime.tm_mon == 12)
    {
        nextCalTime.tm_mon = 0;
        ++nextCalTime.tm_year;
    }
    nextCalTime.tm_hour = 0;
    nextCalTime.tm_min = 0;
    nextCalTime.tm_sec = 10;
    nextCalTime.tm_mday = 1;

    auto nextCalTimeT = timelocal(&nextCalTime);
    LDEBUG << "calculated start date/time of next month: " << std::ctime(&nextCalTimeT);
    auto nextMonthStart = system_clock::from_time_t(nextCalTimeT);

    auto durationMs = duration_cast<milliseconds>(nextMonthStart - currTime);
    LDEBUG << "milliseconds until start date/time of next month: " << durationMs.count();
    return static_cast<size_t>(durationMs.count());
}

}  // namespace common
