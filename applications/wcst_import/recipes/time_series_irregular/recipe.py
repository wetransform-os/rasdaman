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

import re

from master.evaluator.evaluator_slice_factory import EvaluatorSliceFactory
from master.evaluator.expression_evaluator_factory import ExpressionEvaluatorFactory
from master.evaluator.sentence_evaluator import SentenceEvaluator
from master.helper.gdal_axis_filler import GdalAxisFiller
from master.helper.gdal_range_fields_generator import GdalRangeFieldsGenerator
from master.importer.axis_subset import AxisSubset
from master.importer.coverage import Coverage
from master.importer.importer import Importer
from master.importer.multi_importer import MultiImporter
from master.importer.slice import Slice
from master.provider.data.file_data_provider import FileDataProvider
from master.provider.metadata.irregular_axis import IrregularAxis
from master.recipe.base_recipe import BaseRecipe
from master.error.validate_exception import RecipeValidationException
from recipes.general_coverage.abstract_to_coverage_converter import AbstractToCoverageConverter
from recipes.general_coverage.gdal_to_coverage_converter import GdalToCoverageConverter
from util.crs_util import CRSUtil
from util.gdal_util import GDALGmlUtil
from util.log import log
from master.helper.time_gdal_tuple import TimeFileTuple
from util.string_util import create_coverage_id_for_overview
from util.time_util import DateTimeUtil
from util.gdal_validator import GDALValidator
from config_manager import ConfigManager
from util.file_util import FileUtil
from master.importer.resumer import Resumer
from util.timer import Timer
from recipes.general_coverage.recipe import Recipe as GeneralRecipe


class Recipe(BaseRecipe):
    def __init__(self, session):
        """
            The recipe class for irregular timeseries. To get an overview of the ingredients needed for this
            recipe check ingredients/time_series_irregular
            """
        super(Recipe, self).__init__(session)
        self.options = session.get_recipe()['options'] if "options" in session.get_recipe() else {}
        self.importer = None
        self.resumer = Resumer(self.session.get_coverage_id())

        self.recipe_type = GdalToCoverageConverter.RECIPE_TYPE
        if "coverage" in self.options:
            self.options['coverage']['slicer'] = {}
            self.options['coverage']['slicer']['type'] = GdalToCoverageConverter.RECIPE_TYPE


    def validate(self):
        super(Recipe, self).validate()

        if "time_crs" not in self.options or self.options['time_crs'] == "":
            raise RecipeValidationException("No valid time crs provided")

        if 'time_parameter' not in self.options:
            raise RecipeValidationException("No valid time parameter provided")

        if 'metadata_tag' not in self.options['time_parameter'] and 'filename' not in self.options['time_parameter']:
            raise RecipeValidationException(
                "You have to provide either a metadata_tag or a filename pattern for the time parameter")

        if 'datetime_format' not in self.options['time_parameter']:
            raise RecipeValidationException("No valid datetime_format provided")

        if 'metadata_tag' in self.options['time_parameter'] and \
                        "tag_name" not in self.options['time_parameter']['metadata_tag']:
            raise RecipeValidationException("No metadata tag to extract time from gdal was provided")

        if 'filename' in self.options['time_parameter'] \
                and self.options['time_parameter']['filename']['regex'] == "" \
                and self.options['time_parameter']['filename']['group'] == "":
            raise RecipeValidationException("No filename regex and group to extract time from gdal was provided")

        if 'band_names' not in self.options:
            self.options['band_names'] = None


    def describe(self):
        """
        Implementation of the base recipe describe method
        """
        importer = self._get_importer()

        slices = importer.get_slices_for_description()
        number_of_files = len(slices)
        log.info("All files have been analyzed. Please verify that the axis subsets of the first {} files above are correct.".format(number_of_files))
        index = 1
        for slice in slices:
            log.info("Slice " + str(index) + ": " + str(slice))
            index += 1

    def ingest(self):
        """
        Ingests the input files
        """
        importer = self._get_importer()
        importer.ingest()

    def status(self):
        """
        Implementation of the status method
        :rtype (int, int)
        """
        return self._get_importer().get_progress()

    def _generate_timeseries_tuples(self, limit=None):
        """
        Generate the timeseries tuples from the original files based on the recipe.
        And sort the files in order of time.
        :rtype: list[TimeFileTuple]
        """
        ret = []
        if limit is None:
            limit = len(self.session.get_files())

        time_format = None
        if 'datetime_format' in self.options['time_parameter']:
            time_format = self.options['time_parameter']['datetime_format']

        if 'metadata_tag' in self.options['time_parameter']:
            mtag = self.options['time_parameter']['metadata_tag']['tag_name']
            for tfile in self.session.get_files():
                if len(ret) == limit:
                    break

                valid_file = True

                try:
                    gdal_file = GDALGmlUtil(tfile.get_filepath())
                except Exception as ex:
                    FileUtil.ignore_coverage_slice_from_file_if_possible(tfile.get_filepath(), ex)
                    valid_file = False

                if valid_file:
                    dtutil = DateTimeUtil(gdal_file.get_datetime(mtag), time_format, self.options['time_crs'])
                    ret.append(TimeFileTuple(dtutil, tfile))
        elif 'filename' in self.options['time_parameter'] and len(ret) < limit:
            regex = self.options['time_parameter']['filename']['regex']
            group = int(self.options['time_parameter']['filename']['group'])
            for tfile in self.session.get_files():
                if len(ret) == limit:
                    break
                dtutil = DateTimeUtil(re.search(regex, tfile.filepath).group(group),
                                      time_format, self.options['time_crs'])
                ret.append(TimeFileTuple(dtutil, tfile))
        else:
            raise RecipeValidationException("No method to get the time parameter, you should either choose "
                                            "metadata_tag or filename.")

        # Currently, only sort by datetime to import coverage slices (default is ascending), option: to sort descending
        if self.options["import_order"] == AbstractToCoverageConverter.IMPORT_ORDER_DESCENDING:
            return sorted(ret, reverse=True)

        return sorted(ret)

    def _get_coverage_slices(self, crs, gdal_coverage_converter):
        """
        Returns the slices for the collection of files given
        """
        crs_axes = CRSUtil(crs).get_axes(self.session.coverage_id)

        slices_dict = self.create_dict_of_slices(self.session.import_overviews)

        timeseries = self._generate_timeseries_tuples()
        count = 1
        for tpair in timeseries:
            file = tpair.file
            file_path = tpair.file.get_filepath()

            timer = Timer()

            # print which file is analyzing
            FileUtil.print_feedback(count, len(timeseries), file_path)

            if not FileUtil.validate_file_path(file_path):
                continue

            valid_coverage_slice = True
            try:
                gdal_file = GDALGmlUtil(file_path)

                geo_axis_crs = gdal_file.get_crs()
                CRSUtil.validate_crs(crs, geo_axis_crs)
            except Exception as ex:
                FileUtil.ignore_coverage_slice_from_file_if_possible(file.get_filepath(), ex)
                valid_coverage_slice = False

            if valid_coverage_slice:
                try:
                    subsets = GdalAxisFiller(crs_axes, gdal_file).fill(True)
                    subsets = self._fill_time_axis(tpair, subsets)
                except Exception as ex:
                    # If skip: true then just ignore this file from importing, else raise exception
                    FileUtil.ignore_coverage_slice_from_file_if_possible(file_path, ex)
                    valid_coverage_slice = False

            if valid_coverage_slice:
                # Generate local metadata string for current coverage slice
                self.evaluator_slice = EvaluatorSliceFactory.get_evaluator_slice(self.recipe_type, tpair.file)
                local_metadata = gdal_coverage_converter._generate_local_metadata(subsets, self.evaluator_slice)
                if self.session.import_overviews_only is False:
                    slices_dict["base"].append(Slice(subsets, FileDataProvider(tpair.file), local_metadata))

                # Then, create slices for selected overviews from user
                for overview_index in self.session.import_overviews:
                    subsets_overview = self.create_subsets_for_overview(subsets, overview_index, gdal_file)

                    slices_dict[str(overview_index)].append(Slice(subsets_overview, FileDataProvider(file),
                                                                  local_metadata))

            timer.print_elapsed_time()
            count += 1

        return slices_dict

    def _fill_time_axis(self, tpair, subsets):
        """
        Fills the time axis parameters
        :param TimeFileTuple tpair: the input pair
        :param list[AxisSubset] subsets: the axis subsets for the tpair
        """
        for i in range(0, len(subsets)):
            if subsets[i].coverage_axis.axis.crs_axis is not None \
                    and subsets[i].coverage_axis.axis.crs_axis.is_time_axis():
                subsets[i].coverage_axis.axis = IrregularAxis(subsets[i].coverage_axis.axis.label,
                                                              subsets[i].coverage_axis.axis.uomLabel,
                                                              tpair.time.to_string(),
                                                              tpair.time.to_string(),
                                                              tpair.time.to_string(), [0],
                                                              subsets[i].coverage_axis.axis.crs_axis)
                subsets[i].coverage_axis.grid_axis.resolution = 1
                subsets[i].interval.low = tpair.time
        return subsets

    def _get_coverages(self):
        """
        Returns the list of coverages to be used for the importer
        """
        gdal_dataset = GDALGmlUtil.open_gdal_dataset_from_any_file(self.session.get_files())
        crs = CRSUtil.get_compound_crs([self.options['time_crs'], gdal_dataset.get_crs()])

        general_recipe = GeneralRecipe(self.session)
        global_metadata_fields = general_recipe._global_metadata_fields()
        local_metadata_fields = general_recipe._local_metadata_fields()

        sentence_evaluator = SentenceEvaluator(ExpressionEvaluatorFactory())

        gdal_coverage_converter = GdalToCoverageConverter(self.resumer, self.session.get_default_null_values(),
                                                          self.recipe_type, sentence_evaluator,
                                                          self.session.get_coverage_id(),
                                                          None, self.session.get_files(),
                                                          crs, None, None,
                                                          global_metadata_fields, local_metadata_fields,
                                                          None, None, general_recipe._metadata_type(),
                                                          None, None, self.session)

        coverage_slices_dict = self._get_coverage_slices(crs, gdal_coverage_converter)
        fields = GdalRangeFieldsGenerator(gdal_dataset, self.options['band_names']).get_range_fields()

        global_metadata = None
        if len(coverage_slices_dict["base"]) > 0:
            global_metadata = gdal_coverage_converter._generate_global_metadata(coverage_slices_dict["base"][0], self.evaluator_slice)

        results = []
        base_coverage_id = self.session.get_coverage_id()
        for key, value in coverage_slices_dict.items():
            if key == "base":
                # base coverage
                coverage = Coverage(base_coverage_id, coverage_slices_dict[key], fields, crs,
                                    gdal_dataset.get_band_gdal_type(), self.options['tiling'], global_metadata)
            else:
                # overview coverage (key = overview_index)
                coverage_id = create_coverage_id_for_overview(base_coverage_id, key)
                coverage = Coverage(coverage_id, coverage_slices_dict[key], fields, crs,
                                    gdal_dataset.get_band_gdal_type(), self.options['tiling'], global_metadata,
                                    base_coverage_id, key)

            results.append(coverage)

        return results

    def _get_importer(self):
        if self.importer is None:
            grid_coverage = False
            if 'coverage' in self.options:
                grid_coverage = self.options['coverage']['grid_coverage'] if 'grid_coverage' in self.options['coverage'] else False
            wms_import = self.options['wms_import'] if 'wms_import' in self.options else False
            scale_levels = self.options['scale_levels'] if self.options['scale_levels'] is not None else None

            coverages = self._get_coverages()
            importers = []

            for coverage in coverages:
                importers.append(Importer(self.resumer, coverage, wms_import, scale_levels,
                                          grid_coverage,
                                          self.session, self.options['scale_factors']))

            self.importer = MultiImporter(importers)

        return self.importer


    @staticmethod
    def get_name():
        return "time_series_irregular"
