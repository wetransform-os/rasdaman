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

from master.generator.model.model import Model
from master.generator.model.range_type_nill_value import RangeTypeNilValue

from master.helper.user_band import OBSERVATION_TYPE_NUMERIC, OBSERVATION_TYPE_CATEGORIAL


class RangeTypeField(Model):
    def __init__(self, name, definition="", description="", nilValues=[], uom=None, observationType=OBSERVATION_TYPE_NUMERIC, codeSpace=None, dataType=None,
                 chunk_sizes_from_file=None):
        """
        Class to represent the range type field element of range type
        :param str name: the name of the field
        :param str definition: the definition of the field
        :param str description: the description of the field
        :param list[RangeTypeNilValue] nilValues: the nil values for this field
        :param str uom: the unit of measure for the field
        :param str observationType: if omitted -> swe:Quantity, else if set to categorial -> swe:Category
        :param str dataType: the data type of the input band
        :param list[int] chunk_sizes_from_file: data block in TIFF file or chunking of a band in netCDF
        """
        self.name = name
        self.definition = definition
        self.description = description
        self.nilValues = nilValues
        # NOTE: used only for swe:Quantity
        self.uom = uom

        # default it is swe:Quantity
        if observationType is None:
            observationType = OBSERVATION_TYPE_NUMERIC
        self.observationType = observationType
        # NOTE: used only for swe:Category
        self.codeSpace = codeSpace
        self.dataType = dataType
        self.chunk_sizes_from_file = chunk_sizes_from_file

    def get_template_name(self):
        if self.observationType == OBSERVATION_TYPE_NUMERIC:
            # swe:Quantity element
            return "gml_range_type_field_quantity.xml"
        elif self.observationType == OBSERVATION_TYPE_CATEGORIAL:
            # swe:Category element
            return "gml_range_type_field_category.xml"
