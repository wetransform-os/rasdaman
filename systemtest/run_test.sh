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
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
#
# Copyright 2003-2016 Peter Baumann /
# rasdaman GmbH.
#
# For more information please see <http://www.rasdaman.org>
# or contact Peter Baumann via <baumann@rasdaman.com>.

PROG=$(basename $0)

SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
SYSTEST_DIR=$(echo "$SCRIPT_DIR" | sed 's|\(.*/systemtest\).*|\1|')

# load common functions
. "$SYSTEST_DIR"/util/common.sh

logf="test.log"
rm -f "$logf"

log "running rasdaman systemtest"

check_curl
check_gdal
check_python
check_netcdf
check_postgres
check_rasdaman
check_rasdaman_available
check_secore
check_petascope

ret=0

for test_case in [0-9][0-9]*; do
    SECONDS=0

    # determine test script
    TEST_SCRIPT="$test_case/test.sh"
    [ -f $TEST_SCRIPT ] || TEST_SCRIPT="$test_case/test.py"

    # print test header
    echo -e "\n==================================================================================="
    echo -e "Running test $TEST_SCRIPT\n"

    # skip test if no test script was found
    if [ ! -f $TEST_SCRIPT ]; then
        printf "%5s %4ds  $test_case\n" SKIP $SECONDS | tee -a "$logf"
        continue
    fi

    pushd "$test_case" > /dev/null
    TEST_SCRIPT="$(basename "$TEST_SCRIPT")"

    # execute test script, check result and log it along with execution time (s)
    ./$TEST_SCRIPT
    rc=$?
    case $rc in
        0) status=OK;;
        2) status=SKIP;;
        *) status=FAIL; ret=$rc;;
    esac
    printf "%5s %4ds  $TEST_SCRIPT\n" $status $SECONDS | tee -a "$logf"

    popd > /dev/null
done

echo
echo
echo -e "TEST SUMMARY\n"
cat "$logf"

exit $ret
