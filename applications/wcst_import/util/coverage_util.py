"""
 *
 * This file is part of rasdaman community.
 *
 * Rasdaman community is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Rasdaman community is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU  General Public License for more details.
 *
 * You should have received a copy of the GNU  General Public License
 * along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2003 - 2020 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 *
"""

from config_manager import ConfigManager
from master.error.runtime_exception import RuntimeException
from session import Session
from util.url_util import validate_and_read_url, url_read_exception
from util.import_util import decode_res
from lxml import etree
from util.gdal_util import GDALGmlUtil
from util.file_util import FileUtil


def generate_tiling(number_of_axes,
                    spatial_axes_grid_indices,
                    band_base_type_sizes,
                    chunk_sizes_from_file=None):
    """
    Generate a rasdaman aligned tiling scheme based on the provided parameters before importing a created coverage

    @param number_of_axes the coverage's number of axes, e.g. 3 (time, lat, long axes)
    @param spatial_axes_grid_indices a list of grid indices (0-based) of the spatial X/Y axes (e.g. Long, Lat axes in TIFF file);
                                     note: set to None if coverage has no X/Y spatial axes
    @param band_base_type_sizes a list of sizes in bytes of the datacube bands
    @param chunk_sizes_from_file an optional chunk specification read from
    one of the input files; when the input file is chunked, e.g. netcdf or tiff (note: set to None if file is GRIB format),
    this parameter should be set as a list of sizes for each axis. If a tiff file
    has 512x512 Block size, this parameter would be a list [512,512].
    @return if the tiling can be determined, a string specifying the tiling, e.g.
    "aligned [0:0,0:511,0:511] tile size 4194304". If the tiling cannot be
    determined, None is returned.
    """
    '''
    TODO: This function should be filled in next release to generate proper tiling if possible
    '''
    return None


def get_spatial_axes_grid_indices(axis_subsets):
    """
    Get the list of axes XY grid indices if they exist in a coverage
    :param axis_subsets: List[AxisSubset] from the coverage
    :return: e.g. [1,2] for XY grid orders or None if coverage does not contain XY axes
    """
    grid_index_axis_x = -1
    grid_index_axis_y = -1

    for axis_subset in axis_subsets:
        crs_axis = axis_subset.coverage_axis.axis.crs_axis
        axis_type = crs_axis.type
        if axis_type == crs_axis.AXIS_TYPE_X:
            grid_index_axis_x = crs_axis.grid_order
        elif axis_type == crs_axis.AXIS_TYPE_Y:
            grid_index_axis_y = crs_axis.grid_order

    if grid_index_axis_x == -1 or grid_index_axis_y == -1:
        # coverage does not have spatial XY axes
        spatial_axes_grid_indices = None
    else:
        spatial_axes_grid_indices = [grid_index_axis_x, grid_index_axis_y]

    return spatial_axes_grid_indices


def get_band_base_type_sizes(range_type_fields):
    """
    :param range_type_fields: List[RangeTypeField]
    :return: the list of size corresponding to data type of each specified band of the newly created coverage
    """
    results = []
    for field in range_type_fields:
        band_size = GDALGmlUtil.get_data_type_size(field.dataType)
        results.append(band_size)

    return results


def get_chunk_sizes_from_file(range_type_fields):
    """
    If an input file has chunk (e.g. TIFF or netCDF) then get the chunk as a list of elements
    """
    first_band = range_type_fields[0]
    result = first_band.chunk_sizes_from_file

    return result


class CoverageUtil:
    def __init__(self, coverage_id):
        """
        Class to retrieve axis labels from an already existing coverage id
        :param str coverage_id: the coverage id
        """
        self.wcs_service = ConfigManager.wcs_service
        self.admin_service = ConfigManager.admin_service
        self.coverage_id = coverage_id
        # this value is cached in CoverageUtilCache
        self.cov_exist = None

    def exists(self):
        """
        Returns true if the coverage exist, false otherwise
        :rtype bool
        """
        if self.cov_exist is None:
            try:
                # Check if coverage exists via the Non-standard REST endpoint
                service_call = self.admin_service + "/coverage/exist?COVERAGEID=" + self.coverage_id
                response = decode_res(validate_and_read_url(service_call))

                self.cov_exist = (response == "true")
                return self.cov_exist
            except Exception as ex:
                # Something is wrong, try with the standard WCS DescribeCoverage request
                pass

            try:
                # Check if coverage exists in WCS DescribeCoverage result
                service_call = self.wcs_service + "?service=WCS&request=DescribeCoverage&version=" + \
                               Session.get_WCS_VERSION_SUPPORTED() \
                               + "&coverageId=" + self.coverage_id

                response = validate_and_read_url(service_call)
                if decode_res(response).strip() != "":
                    self.cov_exist = True
                else:
                    self.cov_exist = False
            except Exception as ex:
                exception_text = str(ex)

                if not "NoSuchCoverage" in exception_text:
                    raise RuntimeException("Could not check if the coverage exists. "
                                       "Reason: {}".format(exception_text))
                else:
                    # coverage doesn't exist
                    self.cov_exist = False

        return self.cov_exist

    def __describe_coverage(self):
        """
        Send a DescribeCoverage request to petascope
        """
        try:
            service_call = self.wcs_service + "?service=WCS&request=DescribeCoverage&version=" + \
                       Session.get_WCS_VERSION_SUPPORTED() + "&coverageId=" + self.coverage_id
            response = validate_and_read_url(service_call)

            return response
        except Exception as ex:
            exception_text = str(ex)

            if "Missing basic authentication header" in exception_text:
                raise RuntimeException("Endpoint '{}' requires valid rasdaman credentials with format username:password in a text file. \n"
                                       "Hint: Create this identify file first with read permission for user running wcst_import, \n"
                                       "then rerun wcst_import.sh ingredients.json -i path_to_the_identity_file.".format(self.wcs_service))

            raise RuntimeException("Could not retrieve the axis labels by WCS DescribeCoverage request. \n"                                   
                                   "Reason: {}".format(str(ex)))

    def get_axes_labels(self):
        """
        Return axes labels as a list
        :rtype list[str]
        """
        response = decode_res(self.__describe_coverage())
        return response.split("axisLabels=\"")[1].split('"')[0].split(" ")

    def get_axes_lower_bounds(self):
        """
        Return axes lower bounds as a list
        :return: list[str]
        """
        response = self.__describe_coverage()
        root = etree.fromstring(response)
        value = root.xpath(".//*[contains(local-name(), 'lowerCorner')]")[0].text
        lower_bounds = value.split(" ")

        return lower_bounds


class CoverageUtilCache:
    COVERAGE_UTIL_CACHES_DICT = {}

    @staticmethod
    def get_cov_util(cov_id):
        if cov_id not in CoverageUtilCache.COVERAGE_UTIL_CACHES_DICT:
            CoverageUtilCache.COVERAGE_UTIL_CACHES_DICT[cov_id] = CoverageUtil(cov_id)

        return CoverageUtilCache.COVERAGE_UTIL_CACHES_DICT[cov_id]

    @staticmethod
    def clear_caches():
        CoverageUtilCache.COVERAGE_UTIL_CACHES_DICT.clear()
