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
# test_user - test rasdaman server authentication
#
# SYNOPSIS:
#	test_user.sh
#
# DESCRIPTION
#	Performs rasql test queries to check whether authentication is observed.
#
# RESPONDING TO INCIDENT
#	-none-
#
# PROCEDURE
#	Perform rasql calls performing different operations types requiring 
#	different authentication. check whether operations are recejcted/accepted
#	properly.
#
# PRECONDITIONS
# - rasql utility available
# - rasdaman up and running, with database having user/password as defined below
#
# RETURN CODES
        RC_OK=0         # everything went fine
        RC_ERROR=1      # something went wrong
#
# CHANGE HISTORY
#       2006-jan-02     P.Baumann       created
#
# RESTRICTIONS
#	-/-
#


# --- CONSTANTS -----------------------------------------------------

PROG="$(basename "$0")"

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

. "$SCRIPT_DIR"/../util/common.sh

INDENT=".."
INDENT2="$INDENT$INDENT"
INDENT3="$INDENT$INDENT$INDENT"

# --- TEST SETTINGS -------------------------------------------------

# nonex login
USER_NONEX=nonex
PASSWD_NONEX=nonex

RASTEST_USER=rastest
RASTEST_PASSWD=ras:test
RASTEST_PASSWD_ESC=ras\\:test

RASPASS_PATH="$HOME/.raspass"
RASPASS_ORIG_PATH="$HOME/.raspass-orig.tmp"

# test collection
TESTCOLL=AuthentTestCollection
TESTCOLL_TYPE=GreySet

run_succ()
{
  local query="$1"
  local user="$2"
  local pass="$3"
  local rc
  "$RMANHOME/bin/rasql" --quiet -q "$query" --user "$user" --passwd "$pass"
  rc=$?
  if [ $rc -ne 0 ]; then
    log "$INDENT3 query failed with exit code $rc: rasql -q '$query' --user $user --passwd $pass"
  fi
}
raspass_succ()
{
  local user="$2"
  local content="$1"
  local query="select a[1,1] from $TESTCOLL as a"
  local rc
  echo "$content" > $RASPASS_PATH
  "$RMANHOME/bin/rasql" --quiet -q "$query" --user "$user" > /dev/null 2>&1
  rc=$?
  if [ $rc -ne 0 ]; then
    log "$INDENT3 query failed with exit code $rc: rasql -q '$query' --user $user"
  fi
}
raspass_fail()
{
  local user="$2"
  local content="$1"
  local query="select a[1,1] from $TESTCOLL as a"
  local rc
  if [ -n "$content" ]; then
    echo "$content" > $RASPASS_PATH
  fi
  "$RMANHOME/bin/rasql" --quiet -q "$query" --user "$user" > /dev/null 2>&1
  rc=$?
  if [ $rc -ne 0 ]; then
    log "$INDENT3 recognized bad case with exit code $rc"
  else
    RC=$RC_ERROR
    log "$INDENT3 bad case did not fail as expected with non-0 exit code: rasql -q '$query' --user $user"
  fi
}
run_rw()
{
  run_succ "$1" $RASADMIN_USER $RASADMIN_PASS
}
run_ro()
{
  run_succ "$1" $RASGUEST_USER $RASGUEST_PASS
}
run_fail()
{
  local query="$1"
  local user="$2"
  local pass="$3"
  local rc
  "$RMANHOME/bin/rasql" --quiet -q "$query" --user "$user" --passwd "$pass" > /dev/null 2>&1
  rc=$?
  if [ $rc -ne 0 ]; then
    log "$INDENT3 recognized bad case with exit code $rc"
  else
    RC=$RC_ERROR
    log "$INDENT3 bad case did not fail as expected with non-0 exit code: rasql -q '$query' --user $user --passwd $pass"
  fi
}
create_raspass()
{
  mv $RASPASS_PATH $RASPASS_ORIG_PATH > /dev/null
  touch $RASPASS_PATH
  chmod -f 0600 $RASPASS_PATH
}
rollback_raspass()
{
  rm $RASPASS_PATH
  mv $RASPASS_ORIG_PATH $RASPASS_PATH > /dev/null 
}

# --- ACTION --------------------------------------------------------

log "testing rasdaman authentication"
log ""

# set default return code
RC=$RC_OK

log "$INDENT good cases"

log "$INDENT2 write"
run_rw "create collection $TESTCOLL $TESTCOLL_TYPE"
run_rw "insert into $TESTCOLL values marray x in [1:10,1:10] values (char) x[0]"
run_rw "update $TESTCOLL as m set m[1:1,1:1] assign marray x in [1:1,1:1] values 42c"
run_rw "select a[1,1] from $TESTCOLL as a"
run_ro "select a[1,1] from $TESTCOLL as a"

log "$INDENT2 write (2)"
run_rw "delete from $TESTCOLL"
# TODO: Check why the WHERE clause doesn't work
# run_rw "delete from $TESTCOLL where true"
run_rw "drop collection $TESTCOLL"

log "$INDENT2 set up test env for subsequent cases"
run_rw "create collection $TESTCOLL $TESTCOLL_TYPE"
run_rw "insert into $TESTCOLL values marray x in [1:10,1:10] values (char) x[0]"

log "$INDENT2 correct raspass"

"$RMANHOME/bin/rascontrol" -x "define user $RASTEST_USER -passwd $RASTEST_PASSWD -rights R" > /dev/null
create_raspass

RASPASS_CONTENT="foo:bar
$RASTEST_USER:$RASTEST_PASSWD_ESC
baz:baz"
raspass_succ "$RASPASS_CONTENT" $RASTEST_USER 

RASPASS_CONTENT="foo:
$RASTEST_USER:$RASTEST_PASSWD_ESC
baz:baz"
raspass_succ "$RASPASS_CONTENT" $RASTEST_USER

RASPASS_CONTENT=":
$RASTEST_USER:$RASTEST_PASSWD_ESC
baz:baz"
raspass_succ "$RASPASS_CONTENT" $RASTEST_USER

HOMEOLD=$HOME
HOME=""
RASPASS_CONTENT=":
$RASTEST_USER:$RASTEST_PASSWD_ESC
baz:baz"
raspass_succ "$RASPASS_CONTENT" $RASTEST_USER
HOME=$HOMEOLD

rollback_raspass

log "$INDENT good cases done."
log ""

# bad cases
log "$INDENT bad cases"

log "$INDENT2 nonex login"
run_fail "select a[1,1] from $TESTCOLL as a" $USER_NONEX $PASSWD_NONEX

log "$INDENT2 write op with r/o login"
run_fail "update $TESTCOLL as m set m[1:1,1:1] assign marray x in [1:1,1:1] values 42c" $RASGUEST_USER $RASGUEST_PASS
run_fail "insert into $TESTCOLL values marray x in [1:10,1:10] values (char) x[0]" $RASGUEST_USER $RASGUEST_PASS
run_fail "delete from $TESTCOLL where true" $RASGUEST_USER $RASGUEST_PASS

log "$INDENT2 wrong passwd"
run_fail "select a[1,1] from $TESTCOLL as a" $RASGUEST_USER $PASSWD_NONEX
run_fail "update $TESTCOLL as m set m[1:1,1:1] assign marray x in [1:1,1:1] values 42c" $RASADMIN_USER $RASGUEST_PASS

log "$INDENT2 wrong raspass"

create_raspass

RASPASS_CONTENT="foo:foo
$RASTEST_USER:${RASTEST_PASSWD_ESC}foo
bar:baz"
raspass_fail "$RASPASS_CONTENT" $RASTEST_USER

RASPASS_CONTENT="foo:foo
${RASTEST_USER}foo:$RASTEST_PASSWD_ESC
bar:baz"
raspass_fail "$RASPASS_CONTENT" $RASTEST_USER

RASPASS_CONTENT="foo:foo
bar:baz"
raspass_fail "$RASPASS_CONTENT" $RASTEST_USER

rollback_raspass

mv $RASPASS_PATH $RASPASS_ORIG_PATH > /dev/null
mkdir $RASPASS_PATH
raspass_fail "" $RASTEST_USER
rm -r $RASPASS_PATH
mv $RASPASS_ORIG_PATH $RASPASS_PATH > /dev/null

create_raspass
chmod -f 666 $RASPASS_PATH
raspass_fail $empty $RASTEST_USER 
rollback_raspass

"$RMANHOME/bin/rascontrol" -x "remove user $RASTEST_USER" > /dev/null

log "$INDENT bad cases done."
log ""

log "$INDENT cleanup"
run_rw "drop collection $TESTCOLL"
log ""

log "done, exiting with $RC"
exit "$RC"
