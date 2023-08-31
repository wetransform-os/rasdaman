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

import java.util.Map;

/**
 *  Object to represent the whole process_graph property section of this JSON
 *  https://openeo.org/documentation/1.0/developers/api/reference.html#tag/User-Defined-Processes/operation/store-custom-process
 */
public class ProcessGraph {

    private final Map<String, ProcessNode> processNodeMap;

    public ProcessGraph(Map<String, ProcessNode> processNodeMap) {
        this.processNodeMap = processNodeMap;
    }

    public Map<String, ProcessNode> getProcessNodeMap() {
        return processNodeMap;
    }

    @Override
    public String toString() {
        String res = "";
        for (Map.Entry<String, ProcessNode> e : processNodeMap.entrySet()) {
            if (!res.isEmpty())
                res += ",\n";
            res += "  " + e.getValue().toString();
        }
        return "ProcessGraph{\n" + res + "\n}";
    }
}
