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
# ------------------------------------------------------------------------------

#
# A common test runner that can be linked in a particular directory and
# automatically execute tests in a queries/ subdir, output results in
# output/ subdir, and compare the results with expected outcomes saved in the
# oracle/ subdir.
#
# Data to be imported in rasdaman should be put in a testdata/ subdir. Data is
# automatically imported unless --no-ingest was specified, but will not be
# dropped at the end of the test run unless --drop is specified.
#
# Cases known to fail can be temporarily put in known_fails file and will be
# skipped the by the script.
#
# Failed test cases, if any, are logged in the failed_cases.txt file.
#

PROG="$(basename "$0")"
export PROG

# get dir of linking script
SOURCE="${BASH_SOURCE[0]}"
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# get systemtest dir and load common.sh
SYSTEST_DIR="${SCRIPT_DIR/systemtest*/systemtest}"
[ -d "$SYSTEST_DIR/util" ] || echo "error: could not determine system test dir: $SYSTEST_DIR"
. "$SYSTEST_DIR/util/common.sh"

# determine actual service: 00-select -> select, 36-rasql_servlet -> servlet
SVC_NAME=${SCRIPT_DIR##*[-_]}
log "Testing service: $SVC_NAME"

# paths
TESTDATA_PATH="$SCRIPT_DIR/testdata"
QUERIES_PATH="$SCRIPT_DIR/queries"
[ -d "$QUERIES_PATH" ] || error "queries directory not found: $QUERIES_PATH"
ORACLE_PATH="$SCRIPT_DIR/oracle"
[ -d "$ORACLE_PATH" ] || error "oracles directory not found: $ORACLE_PATH"
export KNOWN_FAILS="$SCRIPT_DIR/known_fails"
# OUTPUT_DIR from common.sh
prepare_output_dir

# indicates whether to drop data before/after running tests: 0 = no, 1 = yes
# --drop turns this option on
DROP_DATA=0
# indicates whether to ingest data before running tests: 0 = no, 1 = yes
# --no-ingest turns this option off
INGEST_DATA=1
# indicates whether to run test cases in parallel or not
ENABLE_PARALLEL=1

#
# Data mgmt
#

# Drop coverage imported by wcst_import.sh to Petascope
drop_petascope_data()
{
  "$SYSTEST_DIR/40-dropcovs/test.sh"
}

drop_data()
{
  [ $DROP_DATA -eq 0 ] && return
  [[ "$SCRIPT_DIR" == [012]* ]] && drop_rasql_data
  [[ "$SCRIPT_DIR" == [34]* ]] && drop_petascope_data
  [[ "$SVC_NAME" = "nullvalues" ]] && drop_nullvalues_data
  [[ "$SVC_NAME" = "subsetting" ]] && drop_subsetting_data
}

ingest_data()
{
  [ $INGEST_DATA -eq 0 ] && return
  case "$SVC_NAME" in
    select|clipping|rasdapy3) import_rasql_data "$TESTDATA_PATH";;
    nullvalues)               import_nullvalues_data "$TESTDATA_PATH";;
    subsetting)               import_subsetting_data "$TESTDATA_PATH";;
  esac
}

cleanup()
{
  drop_data
  print_summary
  exit_script
}
trap cleanup SIGINT  # cleanup on control-c


# ------------------------------------------------------------------------------
# work
# ------------------------------------------------------------------------------

# print usage
usage() {
  cat <<EOF
Usage: ./test.sh [ OPTION... ]

Supported options:

  <queryfile>         test only the given query file name (e.g. "project.rasql")

  --drop              drop the testing data after the test finishes
  --no-ingest         do not ingest testing data before running the test
  --disable-parallel  do not run test queries in parallel

  -h, --help    show this message
EOF
  exit "$RC_SKIP"
}

if [[ "$SVC_NAME" = "wms" || "$SVC_NAME" = "wmts" || "$SVC_NAME" = "servlet" ]]; then
  # disable parallel queries for wms/wmts/rasql_servlet tests as they are order dependent,
  # and the straightforward parallelization below cannot handle that
  ENABLE_PARALLEL=0
fi

# parse arguments
test_single_file=
for i in $*; do
  case $i in
    --help|-h)          usage;;
    --drop)             DROP_DATA=1;;
    --no-ingest)        INGEST_DATA=0;;
    --disable-parallel) ENABLE_PARALLEL=0;;
    *)                  test_single_file=$i;;
  esac
done

if [ -n "$test_single_file" ]; then
  [ -f "$QUERIES_PATH/$test_single_file" ] || error "$test_single_file not found."
else
  rm -rf "$OUTPUT_DIR"
  mkdir -p "$OUTPUT_DIR"
fi

if [[ $ENABLE_PARALLEL = 1 ]]; then
    # make sure temp result files from parallel subprocesses are removed on exit
    res_file_prefix="$OUTPUT_DIR/test_${SVC_NAME}-"
    remove_tmp_files() {
      rm -f "$res_file_prefix"*
    }
    trap remove_tmp_files EXIT
fi

# run import if necessary
start_timer
drop_data
ingest_data
stop_timer
loge
log "$(printf '%4s %5ss   data preparation' '' "$(get_time_s)")"

pushd "$QUERIES_PATH" > /dev/null || exit "$RC_ERROR"
loge

# estimate total number of tests to be executed
total_test_no=$(ls | grep -E -v '\.(pre|post|check)\.sh$' | grep -E -v '\.(template|file)$' | wc -l)
curr_test_no=0

if [ -n "$test_single_file" ]; then
  files="$test_single_file"
else
  files=*
fi

for f in $files; do

  if [[ ! -f "$f" || "$f" == *.sh || "$f" == *.template || "$f" == *.file ]]; then
    continue  # skip non-files and scripts
  fi

  curr_test_no=$((curr_test_no + 1))

  if [[ $ENABLE_PARALLEL = 1 ]]; then

    # if tests executing in the background are >= $PARALLEL_QUERIES, then we wait
    # for at least one to finish execution 
    wait_for_background_jobs

    # run the test query in background; up to $PARALLEL_QUERIES will run in parallel
    {
      start_timer
      run_test "$f"
      stop_timer
      print_testcase_result "$f" "$status" "$total_test_no" "$curr_test_no"
      # add current test results to the output file
      echo "$NUM_TOTAL $NUM_SUC $NUM_FAIL" > "$res_file_prefix$curr_test_no"
    } &

  else
      start_timer
      run_test "$f"
      stop_timer
      print_testcase_result "$f" "$status" "$total_test_no" "$curr_test_no"
  fi

done

if [[ $ENABLE_PARALLEL = 1 ]]; then
  # wait for all remaining tests executing in the background to finish
  wait

  # aggregate statistics from result files
  for ff in "$res_file_prefix"*; do
    res=($(head -n1 "$ff"))
    total=${res[0]}
    suc=${res[1]}
    fail=${res[2]}
    NUM_TOTAL=$((NUM_TOTAL+total))
    NUM_SUC=$((NUM_SUC+suc))
    NUM_FAIL=$((NUM_FAIL+fail))
  done
fi

popd > /dev/null || exit "$RC_ERROR"

cleanup
