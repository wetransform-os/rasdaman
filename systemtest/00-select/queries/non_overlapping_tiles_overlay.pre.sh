#!/bin/bash
#
# Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Peter Baumann /
# rasdaman GmbH.
#
# For more information please see <http://www.rasdaman.org>
# or contact Peter Baumann via <baumann@rasdaman.com>.

$RASQL -q "create collection test_non_overlapping_tiles GreySet3" > /dev/null
$RASQL -q "insert into test_non_overlapping_tiles values marray i in [0:0,0:10,0:10] values 10c" > /dev/null
$RASQL -q "update test_non_overlapping_tiles as c set c assign marray i in [1:1,100:110,100:110] values 10c" > /dev/null