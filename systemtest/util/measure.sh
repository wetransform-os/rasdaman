#!/bin/bash
#
# This file is part of rasdaman community.
#
# Rasdaman community is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Rasdaman community is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
#
# Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Peter Baumann /
# rasdaman GmbH.
#
# For more information please see <http://www.rasdaman.org>
# or contact Peter Baumann via <baumann@rasdaman.com>.
#
# ------------------------------------------------------------------------------
#
# Common functions and variables for instrumenting execution time, 
# CPU utilization, etc.
#


# @global variable: timer_start
start_timer(){ timer_start=$(date +%s%N); }
# @global variable: timer_stop
stop_timer() { timer_stop=$(date +%s%N); }
# get elapsed time in ms
get_time()   { echo "scale=2; ($timer_stop - $timer_start) / 1000000.0" | bc; }
# get elapsed time in s
get_time_s() { echo "scale=2; ($timer_stop - $timer_start) / 1000000000.0" | bc; }

# set a global timestamp when this file is loaded by a test script
# @global variable: total_timer_start
start_timer
total_timer_start="$timer_start"


# Call to start measuring CPU utilization.
# Sets global variables cpu_active_prev and cpu_total_prev.
# It also calls the start_timer function.
# Note: adapted from https://stackoverflow.com/a/26791468
measure_cpu_utilization_start()
{
  # Read /proc/stat file (for first datapoint)
  read cpu user nice system idle iowait irq softirq steal guest < /proc/stat
  # compute active and total utilizations
  cpu_active_prev=$((user+system+nice+softirq+steal))
  cpu_total_prev=$((user+system+nice+softirq+steal+idle+iowait))
  cpus_active_prev=()
  cpus_total_prev=()
  cpu_count=$(nproc)
  for i in $(seq 0 $((cpu_count-1))); do
    read cpu user nice system idle iowait irq softirq steal guest < <(grep "cpu$i " /proc/stat)
    cpus_active_prev+=($((user+system+nice+softirq+steal)))
    cpus_total_prev+=($((user+system+nice+softirq+steal+idle+iowait)))
  done
  start_timer
}

# Call to end measuring CPU utilization; measure_cpu_utilization_start must have
# been called first. It also calls the stop_timer function.
# By default a report is printed with overal and per-CPU utilization as well as
# execution since measure_cpu_utilization_start was called.
# If an argument "noreport" is specified nothing is printed. 
# In both cases a global variable cpu_util is set.
measure_cpu_utilization_end()
{
  stop_timer
  # Read /proc/stat file (for first datapoint)
  read cpu user nice system idle iowait irq softirq steal guest < /proc/stat
  # compute active and total utilizations
  local cpu_active_cur=$((user+system+nice+softirq+steal))
  local cpu_total_cur=$((user+system+nice+softirq+steal+idle+iowait))
  # compute CPU utilization (%)
  cpu_util=$((100*( cpu_active_cur-cpu_active_prev ) / (cpu_total_cur-cpu_total_prev) ))

  # compute individual CPU utilization in %
  declare -a cpus_active_cur
  declare -a cpus_total_cur
  declare -a cpus_util
  for i in $(seq 0 $((cpu_count-1))); do
    read cpu user nice system idle iowait irq softirq steal guest < <(grep "cpu$i " /proc/stat)
    cpus_active_cur+=($((user+system+nice+softirq+steal)))
    cpus_total_cur+=($((user+system+nice+softirq+steal+idle+iowait)))
    cpus_util+=($((100*( cpus_active_cur[i]-cpus_active_prev[i] ) / (cpus_total_cur[i]-cpus_total_prev[i]) )))
  done

  if [ "$1" = report ]; then
    printf "Total execution time: ${c_bold}%s seconds${c_off}\n" $(get_time_s)
    printf "CPU core utilization:\n"
    for i in $(seq 0 $((cpu_count-1))); do
      printf "|%4d " "$i"
    done
    printf "| ${c_bold}Total${c_off} |\n"

    for i in $(seq 0 $((cpu_count-1))); do
      if [ "${cpus_util[$i]}" -gt 80 ]; then
        printf "|${c_red}%3d%%${c_off} " "${cpus_util[$i]}"
      elif [ "${cpus_util[$i]}" -gt 40 ]; then
        printf "|${c_yellow}%3d%%${c_off} " "${cpus_util[$i]}"
      else
        printf "|${c_green}%3d%%${c_off} " "${cpus_util[$i]}"
      fi
    done
    printf "| ${c_bold}%3d%%${c_off}  |\n" "$cpu_util"
  fi
}
