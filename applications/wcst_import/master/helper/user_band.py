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

class UserBand:
    def __init__(self, name, description, definition, nilReason="", nilValues=None, uomCode=None, identifier=None):
        """
        Definition of a band as provided by a user in an ingredient file
        :param str name: the name of the band
        :param str description: a description for the band
        :param str definition: the definition of a band
        :param str nilReason: the reason for which the value is a nil
        :param list[str] | None nilValues: a list of nil values
        :param str uomCode: the unit of measure
        :param str identifier: the identifier of this band in the data provider (e.g. the gdal band id or the netcdf variable name)
        """
        self.name = name
        self.description = description
        self.definition = definition
        self.nilReason = nilReason
        self.nilValues = nilValues
        self.uomCode = uomCode
        self.identifier = identifier
