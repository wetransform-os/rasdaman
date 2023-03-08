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
# Common functions and variables for logging.
#

# script return codes
RC_OK=0
RC_ERROR=1
RC_SKIP=2

[ -n "$PROG" ] || PROG="test.sh"
LOG_FILE="$SCRIPT_DIR/test.log"
FAILED_LOG_FILE="$SCRIPT_DIR/failed_cases.log"

# test case status
ST_PASS=OK    # test passed
ST_FAIL=FAIL  # test failed
ST_SKIP=SKIP  # test failed, and also marked in known_fails so it was skipped
ST_FIX=FIX    # marked as known_fail but actually passes, i.e. it was fixed
ST_COPY=COPY  # oracle not found = copy output file to the oracle

# OS version
OS_UNKNOWN="unknown"
OS_UBUNTU2004="ubuntu2004"
OS_UBUNTU2204="ubuntu2204"
. /etc/os-release
case "$VERSION_CODENAME" in
  focal) OS_VERSION=$OS_UBUNTU2004;;
  jammy) OS_VERSION=$OS_UBUNTU2204;;
  *)     OS_VERSION=$OS_UNKNOWN;;
esac

# terminal color escape codes
C_OFF="\e[0m"
C_BOLD="\e[1m"
C_RED="$C_BOLD\e[31m"
C_GREEN="$C_BOLD\e[32m"
C_YELLOW="$C_BOLD\e[33m"
# colors set properly depending on whether the test is run in a terminal
# (rather than output redirected for example)
if [[ -t 0 || -t 1 || -t 2 ]]; then
  c_off="$C_OFF"
  c_bold="$C_BOLD"
  c_green="$C_GREEN"
  c_red="$C_RED"
  c_yellow="$C_YELLOW"
fi

# $1: test status
# stdout: $color_on and $color_on if $1 != $ST_PASS
get_status_color()
{
  [ "$1" == $ST_PASS ] && return # pass, no special color
  case "$1" in
    "$ST_FAIL") echo "$c_red";;
    *)          echo "$c_yellow";;
  esac
}

# log as is to stdout, but remove colors from file output
log_colored()
{
  echo -e "$PROG: $*"
  echo -e "$PROG: $*" | sed -r "s/[[:cntrl:]]\[[0-9]{1,3}m//g" >> "$LOG_FILE"
}
log_colored_failed()
{
  echo -e "$PROG: ${c_red}$*${c_off}"
  echo -e "$PROG: $*" >> "$LOG_FILE"
}
loge_colored()
{
  echo -e "$*"
  echo -e "$*" | sed -r "s/[[:cntrl:]]\[[0-9]{1,3}m//g" >> "$LOG_FILE"
}
loge_colored_failed()
{
  echo -e "${c_red}$*${c_off}"
  echo -e "$*" >> "$LOG_FILE"
}

# normal log
log()   { echo -e "$PROG: $*" | tee -a "$LOG_FILE"; }
loge()  { echo -e "$*" | tee -a "$LOG_FILE"; }
logn()  { echo -n -e "$PROG: $*" | tee -a "$LOG_FILE"; }
error() { log_colored_failed "$*"; log_colored_failed "exiting."; exit $RC_ERROR; }
log_failed() { echo "$PROG: $*" >> "$FAILED_LOG_FILE"; }

feedback()   { if [ $? -ne 0 ]; then loge_colored_failed failed.; else loge ok.; fi; }
check_exit() {
  if [ $? -ne 0 ]; then
    log_colored_failed "failed, exiting."
    exit $RC_ERROR
  else
    loge ok.
  fi
}
# test status -> return code (e.g: OK -> 0)
get_return_code() {
  if [[ "$1" = "$ST_PASS" ]]; then
    return "$RC_OK"
  else
    return "$RC_ERROR"
  fi
}

# setup log file
if [ -n "$SCRIPT_DIR" ] ; then
  rm -f "$LOG_FILE" "$FAILED_LOG_FILE"
  log "starting test at $(date)"
  log ""
fi
