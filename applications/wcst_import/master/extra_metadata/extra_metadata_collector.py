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

from master.evaluator.evaluator_slice import EvaluatorSlice
from master.evaluator.sentence_evaluator import SentenceEvaluator
from master.extra_metadata.extra_metadata import GlobalExtraMetadata
from master.extra_metadata.extra_metadata import LocalExtraMetadata
from master.extra_metadata.extra_metadata_ingredient_information import ExtraGlobalMetadataIngredientInformation
from util.string_util import escape_metadata_nested_dicts
from util.file_util import FileUtil
from util.xml_util import XMLUtil


class ExtraMetadataEntry:
    def __init__(self, evalutor_slice, slice_subsets):
        """
        Defines an input element for the extra metadata collector
        :param EvaluatorSlice evalutor_slice: the slice on which sentences will be evaluated
        :param list[ExtraMetadataSliceSubset] slice_subsets: the spatio-temporal position of the slice
        """
        self.evalutor_slice = evalutor_slice
        self.slice_subsets = slice_subsets


class ExtraGlobalMetadataCollector:
    def __init__(self, evaluator, extra_metadata_info, metadata_entry):
        """
        The global metadata collector provides functionality for extracting metadata according to a user defined metadata description
        from a dataset of slices
        :param SentenceEvaluator evaluator : a list of expression evaluators that should be used to parse the given expression
        :param ExtraGlobalMetadataIngredientInformation extra_metadata_info: the extra metadata information that the user wants to harvest
        :param ExtraMetadataEntry metadata_entry: First slice from which the evaluator can extract the information request in extra_metadata_info
        :return:
        """
        self.evaluator = evaluator
        self.extra_metadata_info = extra_metadata_info
        self.metadata_entry = metadata_entry

    def collect(self):
        """
        Collects the metadata supplied in the constructor
        :rtype: GlobalExtraMetadata
        """
        global_metadata = {}
        for key, value in self.extra_metadata_info.global_attributes.items():
            # if value is empty (e.g: metadata "time_of_coverage": "") then should not evaluate this value
            # output of extra metadata should be string in any cases
            if isinstance(value, dict):
                global_metadata[key] = {}
                for sub_key, sub_value in value.items():
                    global_metadata[key][sub_key] = str(self.evaluator.evaluate(sub_value, self.metadata_entry.evalutor_slice))
            else:
                if str(value) != "":
                    global_metadata[key] = str(self.evaluator.evaluate(value, self.metadata_entry.evalutor_slice))
                else:
                    global_metadata[key] = str(value)

        global_metadata = escape_metadata_nested_dicts(global_metadata)

        # NOTE: this bands's metadata is added to gmlcov:metadata not swe:field
        bands_metadata = {}
        # band_attributes is a dict of keys, values
        if self.extra_metadata_info.bands_attributes is not None:
            for band, band_attributes in self.extra_metadata_info.bands_attributes.items():
                bands_metadata[band] = {}
                for key, value in band_attributes.items():
                    # if value is empty (e.g: metadata "time_of_coverage": "") then should not evaluate this value
                    # output of extra metadata should be string in any cases
                    if str(value) != "":
                        bands_metadata[band][key] = str(self.evaluator.evaluate(value, self.metadata_entry.evalutor_slice))
                    else:
                        bands_metadata[band][key] = str(value)

            bands_metadata = escape_metadata_nested_dicts(bands_metadata)

        # Axes metadata (dimension's metadata)
        axes_metadata = {}
        if self.extra_metadata_info.axes_attributes is not None:
            # axes_attributes is a dict of keys, values
            for axis, axis_attributes in self.extra_metadata_info.axes_attributes.items():
                axes_metadata[axis] = {}
                if type(axis_attributes) is dict:
                    for key, value in axis_attributes.items():
                        # if value is empty (e.g: metadata "time_of_coverage": "") then should not evaluate this value
                        # output of extra metadata should be string in any cases
                        if isinstance(value, list):
                            axes_metadata[axis][key] = value
                        elif str(value) != "":
                            axes_metadata[axis][key] = str(self.evaluator.evaluate(value, self.metadata_entry.evalutor_slice))
                        else:
                            axes_metadata[axis][key] = str(value)
                else:
                    # It should be a string (e.g: ${netcdf:variable:lat:metadata}) and need to be evaluated
                    axes_metadata[axis] = self.evaluator.evaluate(axis_attributes, self.metadata_entry.evalutor_slice)

            axes_metadata = escape_metadata_nested_dicts(axes_metadata)

        return GlobalExtraMetadata(global_metadata, bands_metadata, axes_metadata)


class ExtraLocalMetadataCollector:
    def __init__(self, evaluator, extra_metadata_info, metadata_entry, axes_metadata):
        """
        The local metadata collector provides functionality for extracting metadata according to a user defined metadata description
        from a dataset of slices
        :param SentenceEvaluator evaluator : a list of expression evaluators that should be used to parse the given expression
        :param ExtraLocalMetadataIngredientInformation extra_metadata_info: the extra metadata information that the user wants to harvest
        :param ExtraMetadataEntry metadata_entry: A coverage slice from which the evaluator can extract the information request in extra_metadata_info
        :return:
        """
        self.evaluator = evaluator
        self.extra_metadata_info = extra_metadata_info
        self.metadata_entry = metadata_entry
        self.axes_metadata = axes_metadata

    def collect(self):
        """
        Collects the metadata supplied in the constructor
        :rtype: LocalExtraMetadata
        """
        local_metadata = {}

        # if we have local metadata go ahead
        if len(self.extra_metadata_info.local_attributes.items()) > 0:

            for meta_key, sentence in self.extra_metadata_info.local_attributes.items():
                if meta_key == "metadata_file":
                    self.collect_local_metadata_file(local_metadata, sentence)
                else:
                    # output of extra metadata should be string in any cases
                    local_metadata[meta_key] = str(self.evaluator.evaluate(sentence, self.metadata_entry.evalutor_slice))

        local_metadata = escape_metadata_nested_dicts(local_metadata)

        if self.axes_metadata is not None:
            if "axes" in self.axes_metadata:
                local_metadata["axes"] = self.axes_metadata["axes"]

        return LocalExtraMetadata(local_metadata, self.metadata_entry.slice_subsets, self.axes_metadata)

    def collect_local_metadata_file(self, local_metadata, sentence):
        """
        Collect local metadata from external metadata files if it is set in the ingredient
        :param dict local_metadata
        """
        if "root_element" not in sentence or "path" not in sentence:
            raise RuntimeError("'metadata_file' setting must contain one 'root_element'"
                               " and one 'path' children settings in the ingredient file")

        root_element = str(self.evaluator.evaluate(sentence["root_element"], self.metadata_entry.evalutor_slice))
        metadata_file = str(self.evaluator.evaluate(sentence["path"], self.metadata_entry.evalutor_slice))
        if not FileUtil.validate_file_path(metadata_file):
            raise RuntimeError("Cannot access local metadata file '" + metadata_file + "' to read")
        else:
            local_metadata[root_element] = XMLUtil.read_file_and_remove_xml_header(metadata_file)


