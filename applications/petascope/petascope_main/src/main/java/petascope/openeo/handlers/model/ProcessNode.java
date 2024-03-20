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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Object to represent the parsed JSON of a process node under
 * process_graph property section
 * see JSON example https://openeo.org/documentation/1.0/developers/api/reference.html#tag/User-Defined-Processes/operation/store-custom-process
 * e.g.
 * 	"p1": {
 * 			"process_id": "sum",
 * 			"arguments": {
 * 				"data": [
 * 					1,
 *                    {
 * 						"from_parameter": "nir"
 *                    },
 *                    {
 * 						"from_node": "p1"
 *                    },
 *                    {
 * 						"from_node": "p2"
 *                    }
 * 				]
 * 			}
 * 		},
 */



public class ProcessNode {

    public enum Namespace {
        BOTH,
        PREDEFINED_ONLY,
        USERDEFINED_ONLY
    }

    // e.g. p1
    public String processNodeId;
    // e.g. sum
    public String processId;

    public Namespace namespace = Namespace.BOTH;
    public boolean result = false;

    // e.g. load_collection -> { "id": "mean_summer_airtemp", more properties }
    public Map<String, ParameterValue> arguments = new LinkedHashMap<>();

    public ProcessDefinition processDefinition;

    // the WCPS expression "rendered" from wcpsTemplate
    public String wcpsExpression;

    public ProcessNode(String processNodeId, String processId, Namespace namespace,
                       Map<String, ParameterValue> arguments,
                       ProcessDefinition processDefinition,
                       String wcpsExpression, boolean result) {
        this.processNodeId = processNodeId;
        this.processId = processId;
        this.namespace = namespace;
        this.arguments = arguments;
        this.processDefinition = processDefinition;
        this.wcpsExpression = wcpsExpression;
        this.result = result;
    }

    @Override
    public String toString() {
        return "ProcessNode{" + 
                "processNodeId=" + processNodeId + 
                ", processId=" + processId + 
                ", result=" + Boolean.valueOf(result) + 
                ", arguments count=" + String.valueOf(arguments.size()) + 
                ", processDefinition=" + (processDefinition != null ? processDefinition : "null") + 
                ", wcpsExpression=" + (wcpsExpression != null ? wcpsExpression : "null") + '}';
    }
    
}


