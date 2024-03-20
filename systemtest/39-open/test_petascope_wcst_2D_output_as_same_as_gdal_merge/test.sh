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
# Copyright 2003 - 2018 Peter Baumann /
# rasdaman GmbH.
#
# For more information please see <http://www.rasdaman.org>
# or contact Peter Baumann via <baumann@rasdaman.com>.

# get script name
PROG=$( basename $0 )

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

. "$SCRIPT_DIR"/../../util/common.sh

prepare_output_dir

# NOTE: script_dir is folder of children test cases and it needs to be specified in path variables
sed "s@PETASCOPE_URL@$PETASCOPE_URL@g" "$SCRIPT_DIR/ingest.template.json" > "$OUTPUT_DIR/ingest.json"

# Run the import test and compare with result from gdal_merge
$PYTHONBIN "$SCRIPT_DIR/main.py" "$PETASCOPE_URL" "$RASADMIN_USER" "$RASADMIN_PASS" \
           "$RASADMIN_CREDENTIALS_FILE" >> "$LOG_FILE" 2>&1
check_result 0 $? "check import = gdal_merge result"

print_summary
exit_script
