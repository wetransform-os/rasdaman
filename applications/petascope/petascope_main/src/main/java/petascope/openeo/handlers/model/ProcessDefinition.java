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

import java.util.List;

/**
 * Object represents a pre-process definition
 * e.g.   "id": "absolute" from https://openeo.org/documentation/1.0/processes.html
 */
public class ProcessDefinition {

    private final String id;
    // a process can have multiple parameters
    private final List<ProcessParameter> processParameters;
    private final String wcpsTemplate;

    public ProcessDefinition(String id, List<ProcessParameter> processParameters, String wcpsTemplate) {
        this.id = id;
        this.processParameters = processParameters;
        this.wcpsTemplate = wcpsTemplate;
    }

    public String getId() {
        return id;
    }

    public List<ProcessParameter> getProcessParameters() {
        return processParameters;
    }

    public String getWcpsTemplate() {
        return wcpsTemplate;
    }

    @Override
    public String toString() {
        return "ProcessDefinition{" + 
                "id=" + id + 
                ", processParameters count=" + Integer.toString(processParameters.size()) + 
                ", wcpsTemplate=" + (wcpsTemplate != null ? wcpsTemplate : "null") + '}';
    }
    
    
}
