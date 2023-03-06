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
# SYNOPSIS
#    test.sh
# Description
#    This script tests importing a new coverage from GML tuple list instead of file as wcst_import.sh does normally.

# get script name
PROG=$( basename $0 )

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

. "$SCRIPT_DIR"/../../util/common.sh

# from common.sh
prepare_output_dir

COVERAGE_ID="test_C0001"

log "Test import coverage from a tuple list in GML file"

# update local SECORE URL from template file
GML_TEMPLATE_FILE="$SCRIPT_DIR/exampleRectifiedGridCoverage-1.xml.in"
GML_FILE="$SCRIPT_DIR/exampleRectifiedGridCoverage-1.xml"
sed "s@SECORE_URL@$SECORE_URL@g" "$GML_TEMPLATE_FILE" > "$GML_FILE"

# this query will be encoded in python script with urllib
logn "InsertCoverage from file $GML_FILE ..."
req="$PETASCOPE_URL?service=WCS&version=2.0.1&request=InsertCoverage&coverageRef=file://$GML_FILE"
$CURL "$req" -o "$OUTPUT_DIR/insert_coverage_request_output.txt" >> "$LOG_FILE" 2>&1
check

# then, check the imported coverage encoded in TIFF
OUTPUT_FILE="$OUTPUT_DIR/output.tiff"
logn "GetCoverage to tiff output $OUTPUT_FILE ..."
req="$PETASCOPE_URL?service=WCS&version=2.0.1&request=GetCoverage&coverageId=$COVERAGE_ID&format=tiff"
$CURL "$req" -o "$OUTPUT_FILE" >> "$LOG_FILE" 2>&1
check

logn "compare output to oracle... "
ORACLE_FILE="$SCRIPT_DIR/oracle.tiff"
cmp "$ORACLE_FILE" "$OUTPUT_FILE" >> "$LOG_FILE" 2>&1
check

log "delete test coverage $COVERAGE_ID"
delete_coverage "$COVERAGE_ID"

rm -fv "$GML_FILE" >> "$LOG_FILE" 2>&1

# print summary from util/common.sh
print_summary
exit $RC
