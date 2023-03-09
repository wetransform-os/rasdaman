#!/usr/bin/env python

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

import os 
import glob
import collections
import subprocess
import sys
import shutil

from shutil import copyfile
from osgeo import gdal


def binary_to_string(s):
    if s is not None:
        try:
            if type(s) == bytes:
                return s.decode("utf-8", "replace")
            else:
                return s
        except Exception as e:
            return "Failed to serialize to UTF-8: " + e
    else:
        return ""


dir_path = os.path.dirname(os.path.realpath(__file__))

prog="main.py: "

###### configurations
petascope_endpoint = sys.argv[1]
coverage_id = "test_overlapping_map_mosaic_compare_gdal_merge_and_petascope"

RASADMIN_USER = sys.argv[2]
RASADMIN_PASS = sys.argv[3]
RASADMIN_CREDENTIALS_FILE = sys.argv[4]

input_files_path = dir_path + "/" + "testdata"
output_dir = dir_path + "/" + "output"
ingredient_file = output_dir + "/ingest.json"

merged_file_prefix = "merged_"
# all input files are merged by gdal_merge.py to this file
final_merged_file = ""
# output from rasdaman after all input files are imported
output_file = output_dir + "/" + "output.tif"

copied_files = []


def exit_error(error_message):
    print(prog + error_message)
    print(prog + "with the list of copied files as below:")
    for f in copied_files:
        print(prog + f)
    exit(1)

###### main process

list_files = []
# Collect all the input files to a list
for f in glob.glob(input_files_path + "/*.tif"):
    list_files.append(f)
list_files = collections.deque(list_files)

print(prog + "testing results from gdal_merge.py and petascope WCS-T ...")

# Rotate the list of files to import and check with result from gdal_merge
for x in range(len(list_files)):
    print(prog + "testing import files with rotation number: " + str(x + 1) + " ...")

    copied_files.clear()

    # Then, import all files in this rotation
    for i in range(len(list_files)):

        if i < len(list_files) - 1:
            tmp_file = output_dir + "/" + merged_file_prefix + str(i) + ".tiff"
            final_merged_file = tmp_file

            if i == 0:
                file_1 = list_files[i]
            else:
                # input is a gdal_merged file
                file_1 = output_dir + "/" + merged_file_prefix + str(i - 1) + ".tiff"
            file_2 = list_files[i + 1]
            subprocess.call("gdal_merge.py -o " + tmp_file + " " + file_1 + " " + file_2, shell=True, stdout=open(os.devnull, 'wb'))

        # Copy files to this tmp folder to be used for WCST_Import
        src_file = list_files[i]
        dst_file = output_dir + "/input_" + str(i + 1) + ".tif"

        copied_files.append("Copied file from: {} to: {}".format(os.path.basename(src_file), os.path.basename(dst_file)))
        copyfile(src_file, dst_file)

    # When everything is done, now import files with WCST_Import and check the result from GetCoverage request with gdal_merge.py
    subprocess.call("wcst_import.sh -i " + RASADMIN_CREDENTIALS_FILE + " -q " + ingredient_file, shell=True, stdout=open(os.devnull, 'wb'))

    get_coverage_request = petascope_endpoint + "?SERVICE=WCS&VERSION=2.0.1&REQUEST=GetCoverage&COVERAGEID=" + coverage_id + "&FORMAT=image/tiff"
    subprocess.call("wget --auth-no-challenge --user '" + RASADMIN_USER + "' --password '" + RASADMIN_PASS + "' -q '" + get_coverage_request + "' -O " + output_file, shell=True, stdout=open(os.devnull, 'wb'))

    # now delete the imported coverage and check the result
    delete_coverage_request = petascope_endpoint + "?SERVICE=WCS&VERSION=2.0.1&REQUEST=DeleteCoverage&COVERAGEID=" + coverage_id
    subprocess.call("wget --auth-no-challenge --user '" + RASADMIN_USER + "' --password '" + RASADMIN_PASS + "' -q '" + delete_coverage_request + "' -O /dev/null", shell=True, stdout=open(os.devnull, 'wb'))

    from osgeo import gdal
    src_gt = gdal.Open(final_merged_file).GetGeoTransform()
    dst_gt = gdal.Open(output_file).GetGeoTransform()
    
    for i, value in enumerate(src_gt):
        a = src_gt[i]
        b = dst_gt[i]
        if round(a, 15) - round(b, 15) != 0:
            exit_error(prog + "failed, the geo transforms are different even with round(value, 15), gdal_merge: " + str(src_gt) + ", petascope: " + str(dst_gt))
        

    # Finally, rotate the list_files and continue testing different file combinations
    list_files.rotate(1)

exit(0)
