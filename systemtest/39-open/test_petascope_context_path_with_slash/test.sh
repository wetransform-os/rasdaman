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
# Copyright 2003-2018 Peter Baumann /
# rasdaman GmbH.
#
# For more information please see <http://www.rasdaman.org>
# or contact Peter Baumann via <baumann@rasdaman.com>.
#
# SYNOPSIS
#    test.sh
# Description
#    Check if context path to Petascope with slash can work (e.g: http://localhost:8080/rasdaman/ows/).
#
################################################################################

# get script name
PROG=$( basename $0 )

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

. "$SCRIPT_DIR"/../../util/common.sh

prepare_output_dir

logn "sending GET request to $PETASCOPE_URL/ ... "
$WGET -q -O "$OUTPUT_DIR/GET_request.txt" --spider "$PETASCOPE_URL/" >> "$LOG_FILE" 2>&1
check

logn "sending POST request to $PETASCOPE_URL/ ... "
$WGET -q -O "$OUTPUT_DIR/POST_request.txt" "$PETASCOPE_URL/" --post-data "" >> "$LOG_FILE" 2>&1
check

# print summary from util/common.sh
print_summary
exit $RC
