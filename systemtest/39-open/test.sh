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
# Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 Peter Baumann /
# rasdaman GmbH.
#
# For more information please see <http://www.rasdaman.org>
# or contact Peter Baumann via <baumann@rasdaman.com>.
#
# SYNOPSIS
#    stress.sh
# Description
#    This script tests the connections from WMSServlet in Petascope to petascopedb from Postgresql. By default, Postgresql supports 100 connections at the same time.
#    The problem is fixed when WMSServlet releases connection as soon as possible.

################################################################################
# get script name
PROG=$( basename $0 )

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

. "$SCRIPT_DIR"/../util/common.sh

# list all the subdirectories of test_open
total_test_no=$(ls -ld test_*/ | wc -l)
curr_test_no=0
ret=0

for d in test_*/ ; do
  test_case_name="$d"
  curr_test_no=$(($curr_test_no + 1))

  testscript="$d/test.sh"

  if [ ! -f "$testscript" ]; then
  	log "$testscript not found."
    status="$ST_SKIP"
  else
    start_timer
    "./$testscript" >> /dev/null
    rc=$?
    case $rc in
        0) status="$ST_PASS";;
        2) status="$ST_SKIP";;
        *) status="$ST_FAIL"; ret=$rc;;
    esac
    stop_timer
  fi
  # print result of this test case
  print_testcase_result "$test_case_name" "$status" "$total_test_no" "$curr_test_no"
done

exit $ret
