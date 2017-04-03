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
 * Copyright 2003 - 2015 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 *
"""
from master.error.runtime_exception import RuntimeException
from master.evaluator.evaluator_slice import GDALEvaluatorSlice
from master.evaluator.evaluator_slice import NetcdfEvaluatorSlice
from master.evaluator.evaluator_slice import GribMessageEvaluatorSlice
from util.gdal_util import GDALGmlUtil


class EvaluatorSliceFactory:

    @staticmethod
    def get_evaluator_slice(recipe_type, slice_file):
        """
        Get the evaluator slice based on recipe_type
        :param str recipe_type: (gdal|grib|netcdf)
        :param File slice_file: the current file which is used to evaluate for metadata of bands
        :return: EvaluatorSlice
        """
        # NOTE: to avoid circular dependency import, use importing at function level
        from recipes.general_coverage.gdal_to_coverage_converter import GdalToCoverageConverter
        from recipes.general_coverage.netcdf_to_coverage_converter import NetcdfToCoverageConverter
        from recipes.general_coverage.grib_to_coverage_converter import GRIBToCoverageConverter

        if recipe_type == GdalToCoverageConverter.RECIPE_TYPE:
            # NOTE: warp file to a wrapper class as old GDAL recipes
            evaluator_slice = GDALEvaluatorSlice(GDALGmlUtil(slice_file.filepath))
        elif recipe_type == NetcdfToCoverageConverter.RECIPE_TYPE:
            evaluator_slice = NetcdfEvaluatorSlice(slice_file)
        elif recipe_type == GRIBToCoverageConverter.RECIPE_TYPE:
            # use first grib_message of grib file to evaluate metadata
            evaluator_slice = GribMessageEvaluatorSlice(None, slice_file, None)
        else:
            raise RuntimeException("Cannot generate metadata for recipe_type: {}".format(recipe_type))

        return evaluator_slice