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

PROG="$(basename "$0")"

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

. "$SCRIPT_DIR"/../util/common.sh

KNOWN_FAILS="$SCRIPT_DIR/known_fails"

ORACLE_PATH="$SCRIPT_DIR/oracle"
[ -d "$ORACLE_PATH" ] || error "Expected results directory not found: $ORACLE_PATH"
QUERY_PATH="$SCRIPT_DIR/queries"
[ -d "$QUERY_PATH" ] || error "Rasql query dir not found: $QUERY_PATH"
# OUTPUT_DIR from common.sh
prepare_output_dir

QUERY="" # query
Q_ID=""  # query identifier (file name)

TEST_COLL=test_coll
TEST_COLL3=test_coll3

# ------------------------------------------------------------------------------
# test dependencies
#
check_rasdaman

# check data types
check_type GreySet
check_type GreySet3


# ------------------------------------------------------------------------------
# test function
#
# arg 1: query id
# arg 2: only specify for dbinfo test
function run_test()
{
  local q_id="$1"
  local f
  
  grep "$q_id" "$KNOWN_FAILS" &> /dev/null
  local known_fail=$?

  f=$(find ./ -maxdepth 1 -name 'tmp.*' | head -n1)

  if [[ -z "$f" || ! -e "$f" ]]; then
    if [ $known_fail -ne 0 ]; then
      log "Failed executing select query."
      NUM_FAIL=$((NUM_FAIL + 1))
    else
      log "Failed executing select query. Case is a known fail: skipping test."
    fi
    NUM_TOTAL=$((NUM_TOTAL + 1))

    log_failed "----------------------------------------------------------------------"
    log_failed "$q_id"
    return
  fi
  if [ $# -gt 1 ]; then
    sed -i -e '/oid/d' -e '/baseType/d' -e 's/ *Result element 1: *//' "$f"
  fi
  mv "$f" "$q_id"
  if [ ! -f "$ORACLE_PATH/$q_id" ]; then
    cp "$q_id" "$ORACLE_PATH/$q_id" > /dev/null
  fi

  # Compare the result byte by byte with the expected result in oracle folder
  local out_file="$OUTPUT_DIR/$q_id"
  local ora_file="$ORACLE_PATH/$q_id"
  mv "$q_id" "$out_file"
  if [ ! -f "$ora_file" ]; then
    log "  warning: oracle not found - $ora_file; output will be copied."
    cp "$out_file" "$ora_file"
  fi
  sort "$out_file" | sed 's/,$//g' > "$OUTPUT_DIR/tmp_out"
  sort "$ora_file" | sed 's/,$//g' > "$OUTPUT_DIR/tmp_ora"
  cmp "$OUTPUT_DIR/tmp_ora" "$OUTPUT_DIR/tmp_out" > /dev/null
  local rc=$?

  if [ $known_fail -ne 0 ]; then
    if [ $rc -ne 0 ]; then
      # Test failed,  not skipped
      NUM_FAIL=$((NUM_FAIL + 1))
      log_colored_failed "TEST FAILED: $q_id"
      log_failed "TEST FAILED: $q_id"
      log_failed "    Result of query contains error."
    else
      # Test successful, not skipped.
      log "TEST PASSED: $q_id"
      NUM_SUC=$((NUM_SUC + 1))
    fi
  else
    # we still check if it is fixed
    if [ $rc -ne 0 ]; then
      # Test failed, but is a known fail
      log "TEST SKIPPED: $q_id"
      log_failed "TEST SKIPPED: $q_id"
      log_failed "    Result of query contains error."
    else
      # Test passed, but is a known fail
      log "KNOWN FAIL PASSED: $q_id"
    fi
  fi
  # Regardless, we add to the total # tests run
  NUM_TOTAL=$((NUM_TOTAL + 1))
}


# ------------------------------------------------------------------------------
# test by queries
#
      
rm -f tmp.unknown tmp.csv "$FAILED_LOG_FILE"
# Query by query for extracting some aspects of tested data
for i in "$QUERY_PATH"/*.rasql; do

  # Send query in query folder.
  Q_ID="$(basename "$i")"

  log "----------------------------------------------------------------------"
  log ""
  log "running queries in $Q_ID"
  log ""

  # initialize collections
  drop_colls $TEST_COLL $TEST_COLL3
  create_coll $TEST_COLL GreySet
  create_coll $TEST_COLL3 GreySet3

  counter=0
  while read -r QUERY; do
    q_id=$Q_ID.$counter

    # first run insert/update query
    log "----------------------------------------------------------------------"
    log "$q_id:"

    
    if ! $RASQL -q "$QUERY" --quiet > /dev/null 2> update_query.log; then
      run_test "$q_id.query"
      log "Update query throws the expected error."
      log "----------------------------------------------------------------------"
      counter=$((counter+1))
      QUERY=""
      continue
    fi

    # test inserted data
    coll_name="$TEST_COLL"
    if echo "$QUERY" | grep -q "$TEST_COLL3"; then
      coll_name="$TEST_COLL3"
    fi

    # test result contents
    $RASQL -q "select avg_cells(c) from $coll_name as c" --out string | grep Result | awk '{ print $4; }' > tmp.txt
    run_test "$q_id.txt"

    # test dbinfo for tile structure
    $RASQL -q "select dbinfo(c, \"printtiles=1\") from $coll_name as c" --out file --outfile tmp > /dev/null
    run_test "$q_id" "dbinfo"

    counter=$((counter+1))
    QUERY=""
  done < "$i"


done

drop_colls $TEST_COLL $TEST_COLL3


# ------------------------------------------------------------------------------
# test summary
#
print_summary
exit $RC
