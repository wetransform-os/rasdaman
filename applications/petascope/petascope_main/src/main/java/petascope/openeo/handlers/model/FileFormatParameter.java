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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class to represent parameter of output file format for openEO
 * e.g.
 *
 				"colormap": {
* 					"default": null,
* 					"description": "Allows specifying a colormap, for single band geotiffs. The colormap is a dictionary mapping band values to colors, specified by an integer.",
* 					"type": ["object", "null"]
*              }
 *
 */
public class FileFormatParameter {

    private String parameterName;

    private String defaultProperty = null;
    private String description = null;
    // String or list of string
    private Object type = null;

    public FileFormatParameter() {

    }

    public FileFormatParameter(String parameterName, String defaultProperty, String description, Object type) {
        this.parameterName = parameterName;
        this.defaultProperty = defaultProperty;
        this.description = description;
        this.type = type;
    }

    @JsonIgnore
    public String getParameterName() {
        return parameterName;
    }

    @JsonProperty("default")
    public String getDefaultProperty() {
        return defaultProperty;
    }

    public String getDescription() {
        return description;
    }

    public Object getType() {
        return type;
    }
}
