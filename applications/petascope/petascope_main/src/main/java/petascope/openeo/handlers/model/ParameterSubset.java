/*
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
 * Copyright 2003 - 2023 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.openeo.handlers.model;

/**
 * Object to represent subset-spec, properties:
 * 
    "properties": {
        'dimension': {'type': 'string', 'description': 'The name of the dimension to be subsetted.'},
        'lower': {'type': ['number', 'string'], 'description': 'The slice or lower trim coordinate.'},
        'upper': {'type': ['number', 'string', 'null'], 'description': 'The upper trim coordinate.', 'default': None, 'optional': True},
        'crs': {'type': ['string', 'null'], 'description': 'A specific CRS for the `lower`/`upper coordinates.', 'default': None, 'optional': True}
    }
 */
public class ParameterSubset {

    public String dimension;
    public String lower;
    public String upper;
    public String crs;

    public ParameterSubset(String dimension, String lower, String upper, String crs) {
        this.dimension = dimension;
        this.lower = lower;
        this.upper = upper;
        this.crs = crs;
    }

    @Override
    public String toString() {
        return "ParameterSubset{" + "dimension=" + dimension + ", lower=" + lower + ", upper=" + upper + ", crs=" + crs + '}';
    }
    
}
