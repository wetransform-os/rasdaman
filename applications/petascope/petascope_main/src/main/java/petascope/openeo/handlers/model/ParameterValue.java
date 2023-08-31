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
 * Object to represent the value of an argument of a ProcessNode (under process_graph property section of this JSON
 * https://openeo.org/documentation/1.0/developers/api/reference.html#tag/User-Defined-Processes/operation/store-custom-process)
 * e.g.
 * "sum": {
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
 *  here, ParameterValue contains a list of scalarValue: 1, fromParameter and two fromNode values
 *
 */
public class ParameterValue {

    public static enum ParameterType {
        STRING,
        DOUBLE,
        LONG,
        BOOLEAN,
        ARRAY,
        FROM_NODE,
        FROM_PARAMETER,
        PROCESS_GRAPH,
        OBJECT,
        BOUNDING_BOX,
        UNKNOWN
    }

    public final String parameterName;
    public final ParameterType parameterType;
    public final Object scalarValue;
    public final List<Object> valueArray;
    public final String fromNode;
    public final String fromParameter;
    public final ProcessGraph processGraph;


    public ParameterValue(String parameterName,
                          ParameterType parameterType,
                          Object scalarValue,
                          List<Object> valueArray,
                          String fromNode, String fromParameter,
                          ProcessGraph processGraph) {
        this.parameterName = parameterName;
        this.parameterType = parameterType;
        this.scalarValue = scalarValue;
        this.valueArray = valueArray;
        this.fromNode = fromNode;
        this.fromParameter = fromParameter;
        this.processGraph = processGraph;
    }

    public String getParameterName() {
        return parameterName;
    }

    public ParameterType getParameterType() {
        return parameterType;
    }

    public Object getScalarValue() {
        return scalarValue;
    }

    public List<Object> getValueArray() {
        return valueArray;
    }

    public String getFromNode() {
        return fromNode;
    }

    public String getFromParameter() {
        return fromParameter;
    }

    public ProcessGraph getProcessGraph() {
        return processGraph;
    }

    @Override
    public String toString() {
        String value = "";
        if (scalarValue != null) {
            value = ", scalarValue=" + scalarValue;
        } else if (valueArray != null) {
            value = ", valueArray count=" + Integer.toString(valueArray.size());
        } else if (fromNode != null) {
            value = ", fromNode=" + fromNode;
        } else if (fromParameter != null) {
            value = ", fromParameter=" + fromParameter;
        } else if (processGraph != null) {
            value = ", processGraph=" + processGraph;
        } else {
            value = ", unknown value, error!";
        }
        return "ParameterValue{"
                + "parameterName=" + parameterName
                + ", parameterType=" + (parameterType != null ? parameterType.toString() : "null")
                + value
                + "}";
    }
    
    
}
