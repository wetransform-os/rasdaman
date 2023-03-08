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
# SYNOPSIS
#    common.sh
# Description
#    Common functionality used by test scripts, like
#   - shortcuts for commands
#   - logging functions
#   - various check functions
#   - test data import
#   - generic test case runner
#
################################################################################

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
UTIL_SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# shellcheck source=./test.cfg
. "$UTIL_SCRIPT_DIR"/test.cfg
# shellcheck source=./logging.sh
. "$UTIL_SCRIPT_DIR"/logging.sh
# shellcheck source=./measure.sh
. "$UTIL_SCRIPT_DIR"/measure.sh
# shellcheck source=./tools.sh
. "$UTIL_SCRIPT_DIR"/tools.sh
# shellcheck source=./parallel.sh
. "$UTIL_SCRIPT_DIR"/parallel.sh

# testing statistics
NUM_TOTAL=0 # the number of tests
NUM_FAIL=0  # the number of failed tests
NUM_SUC=0   # the number of successful tests

# name of service currently tested
SVC_NAME=

# ------------------------------------------------------------------------------
# print test summary
# exports $RC - return code that the caller can use in: exit $RC
#
print_summary()
{
  if [ $NUM_TOTAL -eq 0 ]; then
    NUM_TOTAL=$((NUM_FAIL + NUM_SUC))
  fi

  local test_name="$0"
  if [ -n "$SVC_NAME" ]; then
    test_name="$SVC_NAME"
  elif [ -n "$SCRIPT_DIR" ]; then
    test_name="$(basename "$SCRIPT_DIR")"
  fi

  log
  log "-------------------------------------------------------"
  log "Test summary for $test_name"
  log ""
  log "  Test finished at: $(date)"
  if [ -n "$total_timer_start" ]; then
  timer_start="$total_timer_start"
  stop_timer
  log "  Elapsed time    : $(get_time_s)s"
  fi
  log "  Total tests run : $NUM_TOTAL"
  if [ $NUM_SUC -gt 0 ]; then
  log "  Successful tests: $NUM_SUC"
  fi
  if [ $NUM_FAIL -gt 0 ]; then
  log_colored_failed "  Failed tests    : $NUM_FAIL"
  fi
  local skipped_tests=$((NUM_TOTAL - (NUM_FAIL + NUM_SUC)))
  if [ $skipped_tests -gt 0 ]; then
  log "  Skipped tests   : $skipped_tests"
  fi
  log "  Detail test log : $LOG_FILE"
  log "-------------------------------------------------------"

  if [ $NUM_FAIL -ne 0 ]; then
    RC=$RC_ERROR
  else
    RC=$RC_OK
  fi
  export RC
}

# Print the test result of a test with elapsed time format
# e.g: test.sh:   1/103    OK   .81s   3D_Timeseries_Regular
#
# $1 test case name
# $2 result of test case (OK/FAIL)
# $3 total number of test cases
# $4 index of test case in list of test cases
print_testcase_result() {
  local test_case_name=$1
  local status=$2
  local total_test_no=$3
  local curr_test_no=$4
  local c_on
  c_on=$(get_status_color "$status")
  local msg
  msg=$(printf "%3d/$total_test_no ${c_on}%5s${c_off} %5ss   $test_case_name\n" "$curr_test_no" "$status" "$(get_time_s)")
  log_colored "$msg"
}

#
# check if result matches expected result, automatically updating the number of
# failed/successfull tests.log
# arg 1: expected result
# arg 2: actual result
# arg 3: message to print
#
check_result()
{
  local exp="$1"
  local res="$2"
  local msg="$3"

  [ -n "$msg" ] && logn "$msg... "
  if [ "$exp" != "$res" ]; then
    NUM_FAIL=$((NUM_FAIL + 1))
    loge_colored_failed "failed, expected: '$exp', got '$res'."
  else
    NUM_SUC=$((NUM_SUC + 1))
    loge "ok."
  fi
  NUM_TOTAL=$((NUM_TOTAL + 1))
}
#
# check if result matches expected result, automatically updating the number of
# failed/successfull tests.log
# arg 1: expected result
# arg 2: actual result
# arg 3: message to print
#
check_result_fuzzy()
{
  local exp="$1"
  local res="$2"
  local msg="$3"

  [ -n "$msg" ] && logn "$msg... "
  if [[ "$res" != *"$exp"* ]]; then
    NUM_FAIL=$((NUM_FAIL + 1))
    loge_colored_failed "failed, expected: '$exp', got '$res'."
  else
    NUM_SUC=$((NUM_SUC + 1))
    loge "ok."
  fi
  NUM_TOTAL=$((NUM_TOTAL + 1))
}
# this test case is failed (and cannot check by the return of $?)
# if $1 is specified then the test is silent (doesn't print anything)
check_failed()
{
  loge_colored_failed failed.
  NUM_FAIL=$((NUM_FAIL + 1))
  NUM_TOTAL=$((NUM_TOTAL + 1))
  return $RC_ERROR
}
# this test case is passed (and does not check by the return of $?)
# if $1 is specified then the test is silent (doesn't print anything)
check_passed()
{
  loge ok.
  NUM_SUC=$((NUM_SUC + 1))
  NUM_TOTAL=$((NUM_TOTAL + 1))
  return $RC_OK
}
# check the result of previously executed command ($? variable)
# and print failed/ok accordingly + update NUM_* variables
check() {
  if [ $? -ne 0 ]; then
    check_failed
  else
    check_passed
  fi
}

# if "$f" is found in known_fails, then return 0, otherwise to 1
check_known_fail()
{
  local testcase="$f"
  if [[ -f "$KNOWN_FAILS" && -n "$testcase" ]]; then
    grep -F "$testcase" "$KNOWN_FAILS" --quiet
    return $? # 0 if "$f" is a known fail
  fi
  return 1
}

# exit test script with/without error code
exit_script() { if [ $NUM_FAIL -ne 0 ]; then exit $RC_ERROR; else exit $RC_OK; fi; }

# $1 is a URL; return the HTTP code from the URL by curl
get_http_return_code() { $CURL -sL -w "%{http_code}\\n" "$1" -o /dev/null; }

#
# Check return code ($?) and update variables tracking number of
# failed ($NUM_FAIL) / ($NUM_SUC) successfull tests.
# In case of known fail TEST SKIPPED is printed instead of TEST FAILED.
#
update_result()
{
  local rc=$?
  check_known_fail
  local known_fail=$?

  if [ $rc -ne 0 ]; then 
    # failed
    if [ $known_fail -eq 0 ]; then
      status=$ST_SKIP
    else
      status=$ST_FAIL
      NUM_FAIL=$((NUM_FAIL + 1))
      [[ -n "$FAILED_LOG_FILE" && -n "$f" ]] && echo "$f" >> "$FAILED_LOG_FILE"
    fi
  else
    # passed
    if [ $known_fail -eq 0 ]; then
      status=$ST_FIX
    else
      status=$ST_PASS
    fi
    NUM_SUC=$((NUM_SUC + 1))
  fi
  NUM_TOTAL=$((NUM_TOTAL + 1))
  return $rc
}

# create output dir to store outputs from test scripts
prepare_output_dir() {
  if [ -n "$SCRIPT_DIR" ]; then
    export OUTPUT_DIR="$SCRIPT_DIR/output"
    [ -d "$OUTPUT_DIR" ] && rm -rf "$OUTPUT_DIR"
    mkdir -p "$OUTPUT_DIR"
  else
    echo "warning: SCRIPT_DIR must be set before calling prepare_output_dir()."
  fi
}

prepare_json_file()
{
  local json_file="$1"
  if [[ -n "$json_file" && -f "$json_file" ]]; then
    sed -i -e '/href/d' \
           -e 's/ *Result element [[:digit:]]: *//' \
           -e 's/-9.223372036854776e+18/-9.22337204e+18/g' \
           "$json_file"
  fi
}

# Remove URLs and some prefixes which might break a tests during oracle comparison
prepare_xml_file()
{
  local xml_file="$1"
  if [[ -n "$xml_file" && -f "$xml_file" ]]; then
    sed -i -e 's/gml://g' \
           -e $'s/\r//g' \
           -e '/xlink:href/d' \
           -e '/identifier /d' \
           -e 's@rasdaman/def@def@g' \
           -e 's|xmlns[^"]*"[^"]*"||g' \
           -e 's|xsi:schemaLocation="[^"]*"||g' \
           -e 's#http:\/\/\(\w\|[.-]\)\+\(:[0-9]\+\)\?\/def##g' \
           -e 's|at=[^ ]*||g' \
           -e '/fileReferenceHistory/d' \
           -e '/PostCode/d' \
           -e '/PostalCode/d' \
           -e 's/Long/Lon/g' \
           -e 's/Point /Point\n/g' \
           -e 's/ReferenceableGridCoverage /ReferenceableGridCoverage\n/g' \
           -e 's/^[[:space:]]*//' \
           -e '/^[[:space:]]*$/d' \
           -e 's/[[:space:]]>/>/g' \
           -e '/<valid_range>/s/ //g' \
           -e '/_NCProperties/d' \
           -e '/version=".*"/d' \
           -e 's/-9.223372036854776e+18/-9.22337204e+18/g' \
           -e 's/-3.3999999521443642e+38/-3.4e+38/g' \
           "$xml_file"
  fi
}

# prepare a "gdal" file for comparison
# $1: input file
# $2: tmp prefix (output/oracle)
# stdout: the prepared filepath
prepare_gdal_file()
{
  local tmpf="$1.tmp"
  # only for gdal file (e.g: tiff, png, jpeg, jpeg2000)
  # here we compare the metadata and statistic values on output and oracle files directly
  # $p removes the first different lines of gdalinfo output until "Size is"
  gdalinfo -approx_stats "$1" 2> /dev/null | sed -n \
         -e '/Size is/,$p' > "$tmpf"
  sed -i -e '/TOWGS84\[/d' \
         -e '/fileReferenceHistory/d' \
         -e '/Mask Flags/d' \
         -e 's/ColorInterp=.*//g' \
         "$tmpf"

  rm -f "$1.aux.xml" "$1" && mv "$tmpf" "$1"
}

prepare_netcdf_file()
{
  local tmpf="$1.tmp"
  ncdump -c "$1" | sed \
    -e '/fileReferenceHistory/d' \
    -e '/_NCProperties/d' \
    -e '/global attributes/d' \
    -e 's/_ /0 /g' \
    -e '/^$/d' \
    -e '/valid_range =/s/ //g' \
    -e 's/Long/Lon/g' > "$tmpf"

  rm -f "$1" && mv "$tmpf" "$1"
}


# -----------------------------------------------------------------------------
# WCS 2.0.1 utility requests

# Encode a query which contains special characters to be valid string for GET request
urlencode() {
    # urlencode <string>

    old_lc_collate=$LC_COLLATE
    LC_COLLATE=C

    local length="${#1}"
    for (( i = 0; i < length; i++ )); do
        local c="${1:$i:1}"
        case $c in
            [a-zA-Z0-9.~_-]) printf '%s' "$c" ;;
            *) printf '%%%02X' "'$c" ;;
        esac
    done

    LC_COLLATE=$old_lc_collate
}

# Get all available coverage Ids
get_coverage_ids() {
  $WGET -qO- "$PETASCOPE_URL?service=WCS&version=2.0.1&request=GetCapabilities" | \
    grep -oP "(?<=<wcs:CoverageId>)[^<]+" | \
    tr '\n' ' '
}

delete_coverage() {
  # $1 is the coverageId to be deleted
  local input_coverage_id="$1"
  # It it is set to true, make a GetCapabitilies request first before deleting it
  local check_coverage_exist="$2"

  local delete_coverage=false
  if [ "$check_coverage_exist" = "true" ]; then
    local coverage_id=
    for coverage_id in $(get_coverage_ids); do
      if [ "$coverage_id" == "$input_coverage_id" ]; then
        delete_coverage=true
        break
      fi
    done
  else
    delete_coverage=true
  fi

  if [[ "$delete_coverage" = "true" ]]; then
    local OUTPUT_FILE="$OUTPUT_DIR/DeleteCoverage-$1.out"
    # Store the result of deleting request to a temp file
    local delete_request="$PETASCOPE_URL?service=WCS&version=2.0.1&request=DeleteCoverage&CoverageId=$input_coverage_id"
    $CURL -i "$delete_request" > "$OUTPUT_FILE"

    # Check HTTP code is 200, coverage is deleted successfully
    if ! head -n1 < "$OUTPUT_FILE" | grep "200" --quiet; then
      # In case of error, put error message from Petascope to test.log
      tail -n6 <"$OUTPUT_FILE" >> "$LOG_FILE"
      return 1
    fi
  fi

  return 0
}

# -----------------------------------------------------------------------------

# Only use it GET parameter is too long (e.g: WCPS big query) (file name convention: *.post.test)
# $1 is servlet endpoint (e.g: localhost:8080/rasdaman/ows)
# $2 is the query file containing KVP parameters (e.g: service=WCS&version=2.0.1&query=....);
#    new lines are removed from the file.
# $3 is output file
post_request_kvp() {
  local url="$1"
  local kvpValues
  kvpValues="$(tr -d '\n' < "$2")"  # remove new lines from $2
  $CURL -X POST --data-urlencode "$kvpValues" "$url" > "$3"
}

# this function will send a POST request to server with an upload file
# e.g: used by WCS clipping extension with POST request
# $1 is servlet endpoint (e.g: localhost:8080/rasdaman/ows)
# $2 is file with KVP parameters (e.g: service=WCS&version=2.0.1&request=GetCoverage&clip=$1...)
# $3 is the path to the file to be uploaded to server
# $4 is output file from HTTP response
post_request_file() {
  local url="$1"
  local kvpValues
  # $ is not valid character for curl, it must be escaped inside test request file;
  # also new lines are removed
  kvpValues="$(sed 's/\$/%24/g' "$2" | tr -d '\n')"
  local upload_file="$3"
  $CURL -F "file=@$upload_file" "$url$kvpValues" > "$4"
}

# check if the output produced by the test ($1) matches the expected oracle ($2)
# and update the comparison result (NUM_SUC/NUM_FAIL/NUM_TOTAL global variables)
compare_output_to_oracle() {
  local out="$1"
  local ora="$2"

  # QUERIES_PATH comes from test_runner.sh
  local check_script="$QUERIES_PATH/${f%\.*}.check.sh"
  if [[ -f "$check_script" ]]; then
    export -f prepare_xml_file
    "$check_script" "$out" "$ora"
  else
    local ora_tmp="$out.oracle_tmp"
    local out_tmp="$out.output_tmp"
    cp "$ora" "$ora_tmp"
    cp "$out" "$out_tmp"
    local orafiletype
    orafiletype=$(file "$ora" | awk -F ':' '{print $2;}')

    # normalize files to remove irrelevant differences across systems
    if gdalinfo "$ora" &> /dev/null; then
      if [[ "$orafiletype" =~ "NetCDF" || "$orafiletype" =~ "Hierarchical Data" ]]; then
        prepare_netcdf_file "$out_tmp"
        prepare_netcdf_file "$ora_tmp"
      else
        prepare_gdal_file "$out_tmp"
        prepare_gdal_file "$ora_tmp"
      fi
    elif [[ "$orafiletype" == *XML* ]]; then
      prepare_xml_file "$out_tmp"
      prepare_xml_file "$ora_tmp"
    elif [[ "$orafiletype" == *JSON* || "$orafiletype" == *ASCII* ]]; then
      # e.g: for file type is json
      prepare_json_file "$out_tmp"
      prepare_json_file "$ora_tmp"
    fi

    # diff comparison ignoring EOLs [see ticket #551]
    diff -b "$out_tmp" "$ora_tmp" > /dev/null 2>&1
    rc=$?

    if [[ "$rc" != 0 && "$orafiletype" == *XML* ]]; then
      # ignore random error by SECORE BaseX
      # the grep below will result in $? == 0 if the random error is found in 
      # the output, so it will pass in update_result
      grep --quiet "Your feedback is welcome" "$out_tmp"
    else
      # this is necessary in order to set $? for update_result, as the $? from 
      # diff was lost because of the if checks above
      [ "$rc" -eq 0 ]
    fi

  fi

  # any previous statement must have been a comparison (diff, check_script, ...),
  # so that at this point the $? points to the result of that comparison.
  update_result
}

# arg $1: pre/post script to be executed
# arg $2: script name to be logged in warning in case the script fails
run_script() {
  if [[ -f "$1" ]]; then
    "$1" || log "warning: $2 script failed execution - $1"
  fi
}


# ------------------------------------------------------------------------------
# run a test suite, expected global vars:
#  $SVC_NAME - service name, e.g. wcps, wcs, etc.
#  "$f"        - test file
run_test()
{
  local f="$1"
  [[ -f "$f" ]] || error "test case not found: $f"
  # get test type - file extension
  local test_type="${f##*.}"
  # oracle file has an .oracle extensions, but may be
  # set to a custom OS oracle file if it exists (e.g test.oracle.ubuntu2204)
  local oracle="$ORACLE_PATH/${f%\.*}.oracle"
  [[ -f "$oracle.$OS_VERSION" ]] && oracle="$oracle.$OS_VERSION"
  # test outputs
  local out="$OUTPUT_DIR/$f.out"
  local err="$OUTPUT_DIR/$f.err"

  # run pre script if present
  run_script "$QUERIES_PATH/${f%\.*}.pre.sh" "pre"

  if [[ "$f" == *.sh ]]; then

    # 0. run custom test script if present
    "$f" "$out" "$oracle"
    update_result

  else

    # 1. execute test query
    local QUERY
    case "$SVC_NAME" in

      servlet)
        # test_rasql_servlet
        case "$test_type" in
          kvp)
              post_request_kvp "$RASQL_SERVLET" "$f" "$out"
              ;;
          input)
              local templateFile="$SCRIPT_DIR/queries/post-upload.template"
              local inputFile="$SCRIPT_DIR/queries/$f"
              # read parameters from *.input file (NOTE: need to escape special characters like: &)
              local parameters
              parameters=$(cat "$inputFile")
              # replace the parameters from current .input file into templateFile
              sed "s#PARAMETERS#$parameters#g" "$templateFile" > "$templateFile.tmp.sh"
              # run the replaced script to upload file and rasql query to 
              # rasql-servlet and redirect output to /out directory
              bash "$templateFile.tmp.sh" > "$out" 2> "$err"
              # remove the temp bash script
              rm -f "$templateFile.tmp.sh"
        esac
        ;;

      wcps)
        case "$test_type" in
          post_file)
              # File to upload to server, same name with test request file but with .file                
              local upload_file="${f%.*}.file"
              post_request_file "$PETASCOPE_URL?query=" "$f" "$upload_file" "$out"
              ;;
          test|xml)
              QUERY="$(cat "$f")"
              $CURL -X POST --data-urlencode "query=$QUERY" "$PETASCOPE_URL" > "$out"
              ;;
          *)  error "unknown wcps test type: $test_type";;
        esac
        ;;

      wcs)
        case "$test_type" in
          post_file)
              # File to upload to server, same name with test request file but with .file                
              local upload_file="${f%.*}.file"
              post_request_file "$PETASCOPE_URL?" "$f" "$upload_file" "$out"
              ;;
          kvp)
              post_request_kvp "$PETASCOPE_URL" "$f" "$out"
              ;;
          xml|xml_wcps) 
              QUERY=$(tr -d '\n' < "$f")
              local url="$PETASCOPE_URL"
              if [[ $test_type = xml_wcps ]]; then
                [[ -f "$f" ]] || return
                url="$url?service=WCS&version=2.0.1&request=ProcessCoverages"
              fi
              $CURL -X POST --data-urlencode "query=$QUERY" "$url" > "$out"
              ;;
          *)  error "unknown wcs test type: $test_type";;
        esac
        ;;

      wms|wmts)
        case "$f" in
          *insert*) endpoint="$PETASCOPE_ADMIN_URL/layer/style/add";;
          *update*) endpoint="$PETASCOPE_ADMIN_URL/layer/style/update";;
          *delete*) endpoint="$PETASCOPE_ADMIN_URL/layer/style/remove";;
                 *) endpoint="$PETASCOPE_URL";;
        esac
        post_request_kvp "$endpoint" "$f" "$out"
        ;;          

      oapi)
        QUERY=$(tr -d '\n' < "$f")
        $CURL -G -X GET "$PETASCOPE_OAPI/$QUERY" > "$out"
        ;;

      secore)
        QUERY=$(sed 's|%SECORE_URL%|'"$SECORE_URL"'|g' "$f" | tr -d '\n')
        $CURL -X GET "$SECORE_URL$QUERY" > "$out"
        ;;

      select|nullvalues|subsetting|clipping|insitu|rasdapy3)  # rasql queries
        QUERY=$(cat "$f")
        local RASQL_CMD="$RASQL"
        [ "$SVC_NAME" = "rasdapy3" ] && RASQL_CMD="$PY_RASQL"

        $RASQL_CMD -q "$QUERY" --out file --outfile "$out" > "${out}_stdout" 2> "$err"

        # if an exception was thrown, then the err file has non-zero size
        # however, ignore GDAL warnings
        if [[ $? -ne 0 && -s "$err" ]] && ! grep -q "Warning 6: PNG" "$err"; then
          mv "$err" "$out"
        else
          # move the file produced by --outfile to $out 
          # (e.g output.rasql.out.uknown to output.rasql.out)
          local tmpf
          for tmpf in "$out".*; do
              [ -f "$tmpf" ] || continue
              mv "$tmpf" "$out"
              break
          done
          # if no output exists then the query didn't return any results; create
          # an empty file as the oracle will be an empty file as well.
          [[ -f "$out" ]] || touch "$out"
        fi
        ;;

      *)
        error "unknown service: $SVC_NAME"
    esac

    # 2a. create $oracle from $output, if missing
    if [ ! -f "$oracle" ]; then
      status=$ST_COPY
      cp "$out" "$oracle"
    fi

    # 2b. check result
    compare_output_to_oracle "$out" "$oracle" "$f"

  fi # end of if not sh file

  # run post script if present
  run_script "$QUERIES_PATH/${f%\.*}.post.sh" "post"
}

#
# load all modules
#
. "$UTIL_SCRIPT_DIR"/rasql.sh
. "$UTIL_SCRIPT_DIR"/py_rasql.sh
