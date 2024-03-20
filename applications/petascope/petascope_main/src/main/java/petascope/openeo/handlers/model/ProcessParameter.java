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
 * Object to represent a parameter of a process node, e.g.
 * https://openeo.org/documentation/1.0/developers/api/reference.html#tag/User-Defined-Processes/operation/store-custom-process
 *
 */
public class ProcessParameter {

    private String name;
    private boolean isRequired = true;
    // Set to the value of "default" property if it exists in JSON string
    // e.g. "default": true or "default": 4326 specified in JSON
    private String defaultValue = null;

    public ProcessParameter(String name, boolean isRequired, String defaultValue) {
        this.name = name;
        this.isRequired = isRequired;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
