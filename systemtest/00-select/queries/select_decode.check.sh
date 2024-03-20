#!/bin/bash
#
# Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Peter Baumann /
# rasdaman GmbH.
#
# For more information please see <http://www.rasdaman.org>
# or contact Peter Baumann via <baumann@rasdaman.com>.

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
CHECK_SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

testdata_dir="$CHECK_SCRIPT_DIR"/../testdata/

res=$($RASQL -q "select add_cells(decode(\$1))" -f "$testdata_dir"/rgb.png --out string | grep Result)
exp="  Result element 1: { 15337413, 24354808, 26792546 }"
[ "$res" = "$exp" ] || exit 1

res=$($RASQL -q "select add_cells(decode(\$1))" -f "$testdata_dir"/mr_1.png --out string | grep Result)
exp="  Result element 1: 2151797"
[ "$res" = "$exp" ] || exit 1

res=$($RASQL -q "select add_cells(\$1)" -f "$testdata_dir"/50k.bin --mdddomain "[1:25000]" --mddtype ShortString --out string | grep Result)
exp="  Result element 1: 216757812"
[ "$res" = "$exp" ] || exit 1
