#!/bin/bash
# This file is part of rasdaman community.
#
# Rasdaman community is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Rasdaman community is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.    See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with rasdaman community.    If not, see <http://www.gnu.org/licenses/>.
#
# Copyright 2003 - 2019 Peter Baumann /
# rasdaman GmbH.
#
# For more information please see <http://www.rasdaman.org>
# or contact Peter Baumann via <baumann@rasdaman.com>.
# SYNOPSIS
#    test.sh
# Description
#    This script tests petascope embedded with an input parameter (--petascope.confDir)
#    to a folder containing customized petascope.properties

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# shellcheck source=../../util/common.sh
. "$SCRIPT_DIR"/../../util/common.sh

skip_test_if_not_postgresql_petascopedb
prepare_output_dir

port="9090"
petascope_war_file="$RMANHOME/share/rasdaman/war/rasdaman.war"

etc_dir="$RMANHOME/etc"
etc_dir_tmp="/tmp/etc_tmp"
rm -rf "$etc_dir_tmp"
cp -r "$etc_dir" "$etc_dir_tmp"

temp_petascope_properties="$etc_dir_tmp/petascope.properties"
secore_properties="$etc_dir/secore.properties"
bak_secore_properties="$etc_dir/secore.properties.backup"

cp "$secore_properties" "$bak_secore_properties"


tmp_secore_dir="/tmp/secore"
mkdir -p "$tmp_secore_dir"


cleanup()
{
  mv "$bak_secore_properties" "$secore_properties"
  rm -rf "$tmp_secore_dir"
}
trap cleanup SIGINT  # cleanup on control-c


log_file="$OUTPUT_DIR/petascope.log"

# replace port from default one to 9090

check_petascope_started () {
  grep -q "Started ApplicationMain" "$log_file"
  status_code=$?
  echo $status_code
}


sed -i "s@secore_urls=internal@secore_urls=http://crs.rasdaman.com/def/@g" "$temp_petascope_properties"
sed -i "s@server.port=.*@server.port=$port@g" "$temp_petascope_properties"
sed -i "s@allow_write_requests_from=127.0.0.1@allow_write_requests_from=1.2.3.4@g" "$temp_petascope_properties"
sed -i "s@log4j.appender.rollingFile.File=.*@log4j.appender.rollingFile.File=$log_file@g" "$temp_petascope_properties"
sed -i "s@log4j.appender.rollingFile.rollingPolicy.ActiveFileName=.*@log4j.appender.rollingFile.rollingPolicy.ActiveFileName=$log_file@g" "$temp_petascope_properties"

sed -i "s@secoredb.path=/opt/rasdaman/data/secore@secoredb.path=$tmp_secore_dir@g" "$secore_properties"

logn "Starting embedded petascope..."

nohup java -jar "$petascope_war_file" --petascope.confDir="$etc_dir_tmp" > "$OUTPUT_DIR/nohup.out" 2>&1 &
pid=$!

# Wait embedded petascope to start
sleep 30
if [ ! -f "$log_file" ]; then
  log "$log_file not found."
else
  status_code=$(check_petascope_started)

  if [ "$status_code" -ne 0 ]; then
    log "1. Checking petascope status again in 5 seconds ..."
    sleep 5
    
    status_code=$(check_petascope_started)
    if [ "$status_code" -ne 0 ]; then
      log "2. Checking petascope status again in another 5 seconds ..."
      sleep 5
    fi

    status_code=$(check_petascope_started)
    if [ "$status_code" -ne 0 ]; then
      log "3. Checking petascope status again in another 5 seconds ..."
      sleep 5
    fi
    
    status_code=$(check_petascope_started)
    if [ "$status_code" -ne 0 ]; then
      log "4. Checking petascope status again in another 5 seconds ..."
      sleep 5
    fi
    
    status_code=$(check_petascope_started)
    if [ "$status_code" -ne 0 ]; then
      log "5. Checking petascope status again in another 5 seconds ..."
      sleep 5
    fi
    
    status_code=$(check_petascope_started)
    if [ "$status_code" -ne 0 ]; then
      log "6. Checking petascope status again in another 5 seconds ..."
      sleep 5
    fi          
    
    status_code=$(check_petascope_started)
    if [ "$status_code" -ne 0 ]; then
      log "7. Checking petascope status again in another 10 seconds ..."
      sleep 10
    fi            
    
    status_code=$(check_petascope_started)
    if [ "$status_code" -ne 0 ]; then
      log "8. Checking petascope status again in another 10 seconds ..."
      sleep 10
    fi    
    
    status_code=$(check_petascope_started)
    if [ "$status_code" -ne 0 ]; then
      log "9. Checking petascope status again in another 10 seconds ..."
      sleep 10
    fi    
    
    status_code=$(check_petascope_started)
    if [ "$status_code" -ne 0 ]; then
      log "10. Checking petascope status again in another 10 seconds ..."
      sleep 10
    fi    
    
    status_code=$(check_petascope_started)
    if [ "$status_code" -ne 0 ]; then
        log "startup timed out, petascope.log:"
        cat "$log_file"
        exit $RC_ERROR
    fi
  fi
  
fi

$WGET -q --spider "http://localhost:$port/rasdaman/ows?service=WCS&version=2.0.1&request=GetCapabilities"
# defined in common.sh
check_result 0 $? "test embedded petascope with customized etc dir"

# Try to insert a test coverage with rasadmin and it should bypass IP check
fileref="file:/$SCRIPT_DIR/testdata/insertcoverage.gml"
$WGET -q --spider "http://localhost:$port/rasdaman/ows?SERVICE=WCS&VERSION=2.0.1&REQUEST=InsertCoverage&coverageRef=$fileref"
check_result 0 $? "test write request: InsertCoverage is allowed for rasadmin user"

# Try to delete a test coverage with rasadmin and it should bypass IP check
$WGET -q --spider "http://localhost:$port/rasdaman/ows?SERVICE=WCS&VERSION=2.0.1&REQUEST=DeleteCoverage&coverageId=test_mr_TO_BE_DELETED"
check_result 0 $? "test write request: DeleteCoverage is allowed for rasadmin user"

# Then kill embedded petascope
kill -9 "$pid"
#rm -rf "$etc_dir_tmp"

cleanup

# print summary from util/common.sh
print_summary
exit "$RC"
