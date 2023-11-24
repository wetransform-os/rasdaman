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
from master.error.validate_exception import RecipeValidationException

OBSERVATION_TYPE_NUMERIC = "numeric"
OBSERVATION_TYPE_CATEGORIAL = "categorial"

VALID_OBSERVATION_TYPES = [OBSERVATION_TYPE_NUMERIC, OBSERVATION_TYPE_CATEGORIAL]

class UserBand:
    def __init__(self, identifier, name, description, definition, nilValues=[], uomCode=None, filterMessagesMatching=None, observationType=OBSERVATION_TYPE_NUMERIC):
        """
        Definition of a band as provided by a user in an ingredient file
        :param str identifier: the identifier of this band in the data provider (e.g. the gdal band id or the netcdf variable name)
        :param str name: the name of the band
        :param str description: a description for the band
        :param str definition: the definition of a band
        :param list[RangeTypeNilValue] nilValues: a list of nil values
        :param str uomCode: the unit of measure
        :param filterMessagesMatching a dict of keys:values to filter GRIB messages, in case, messages contain user input GRIB keys
                which contain user input values

        """
        # NOTE: band identifier must be defined in ingredient file
        self.identifier = identifier
        if identifier is None or identifier == "":
            raise RecipeValidationException("Band identifier of band name {} has not been specified.".format(name))
        # band name is the name which is shown in DescribeCoverage bands (so normally band name = band identifier)
        self.name = name
        self.description = description
        self.definition = definition
        self.nilValues = nilValues
        self.uomCode = uomCode
        self.filterMessagesMatching = filterMessagesMatching
        self.observationType = observationType
