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
# Common functions and variables for executing tests in parallel.
#

# Run up to 4 test queries in parallel
PARALLEL_QUERIES=4
RASSERVER_COUNT="$(grep 'define srv ' "$RASMGR_CONF" -c)"
[ $PARALLEL_QUERIES -gt "$RASSERVER_COUNT" ] && PARALLEL_QUERIES=$RASSERVER_COUNT
export RASSERVER_COUNT
export PARALLEL_QUERIES
# if number of background jobs does not decrease within 120 seconds, the test
# will terminate with an error.
export WAIT_TIMEOUT_SEC=120

# if there are more background jobs than $PARALLEL_QUERIES, then this function
# will wait until there are less. It has a timeout of 60 seconds and will exit
# with error if the timeout is reached.
wait_for_background_jobs()
{
  local major=${BASH_VERSINFO[0]}
  local minor=${BASH_VERSINFO[1]}
  if ((major > 4 || (major = 4 && minor >= 3))); then
    # The wait bash command has option -n since Bash 4.3
    if [ "$(jobs -p | wc -l)" -gt "$PARALLEL_QUERIES" ]; then
      #echo waiting, currently "$(jobs -p | wc -l)" jobs running
      wait -n
    fi
  else
    # CentOS 7 has Bash 4.2 without wait -n; we emulate it here with sleep
    elapsed_sec=0
    while ((elapsed_sec < WAIT_TIMEOUT_SEC)); do
      if [ "$(jobs -p | wc -l)" -gt "$PARALLEL_QUERIES" ]; then
        sleep 0.2
        elapsed_sec=$((elapsed_sec + 1))
      else
        break
      fi
    done
    if ((elapsed_sec >= WAIT_TIMEOUT_SEC)); then
      error "background jobs did not terminate after waiting for $elapsed_sec seconds."
    fi
  fi
}
