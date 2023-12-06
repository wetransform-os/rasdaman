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

package petascope.openeo.handlers.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion;
import org.rasdaman.repository.service.ProcessGraphRepositoryService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.core.KVPSymbols;
import petascope.core.response.Response;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.openeo.handlers.model.*;
import petascope.util.JSONUtil;

import java.io.IOException;
import java.util.*;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupString;
import petascope.util.ListUtil;
import petascope.util.StringUtil;
import petascope.wcps.metadata.model.Axis;
import petascope.wcps.metadata.model.WcpsCoverageMetadata;
import petascope.wcps.metadata.service.WcpsCoverageMetadataTranslator;
import petascope.wcs2.handlers.kvp.KVPWCSProcessCoverageHandler;

/**
 * This service will get a JSON string containing process_graph property
 * https://openeo.org/documentation/1.0/developers/api/reference.html#tag/User-Defined-Processes/operation/store-custom-process
 * and handle it to generate a WPCS query and execute it to return a result.
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class OpenEOJSonToWcpsResultService {

    @Autowired
    private ProcessGraphRepositoryService processGraphRepositoryService;
    @Autowired
    private KVPWCSProcessCoverageHandler kvpwcsProcessCoverageHandler;
    @Autowired
    private WcpsCoverageMetadataTranslator wcpsCoverageMetadataTranslator;

    // NOTE: this schema only validates the content under "process_graph" JSON element
    private static final String PROCESS_GRAPH_JSON_SCHEMA_URL = "https://openeo.org/documentation/1.0/developers/api/assets/pg-schema.json";

    private static final String PROCESS_GRAPH_PROPERTY_NAME = "process_graph";

    private static org.slf4j.Logger log = LoggerFactory.getLogger(OpenEOJSonToWcpsResultService.class);

    // cov1 -> $c0
    private Map<String, String> loadCollectionCoverageIdAliasMap = new LinkedHashMap<>();

    /**
     * Given a JSON string, generates a WCPS query and returns result from the query to the client.
     *
     * @param json     JSON string from POST content
     * @param username authenticated user
     * @return WCPS query's response
     */
    public Response handle(String json, String username) throws Exception {

        // ### Step 1. Validate JSON and parse it to get the content of process_graph
        JsonNode rootProcessGraphJsonNode = this.parseJsonToGetProcessGraphNode(json);

        // ### Step 2, 3, 4
        ProcessGraph processGraph = this.parseProcessGraphNode(rootProcessGraphJsonNode, username);

        // ### Step 5
        String wcpsQuery = this.translateToWcpsQuery(processGraph);

        log.info("Generated WCPS query for openEO /result: " + wcpsQuery);

        Map<String, String[]> kvpParametersMap = new LinkedHashMap<>();
        kvpParametersMap.put(KVPSymbols.KEY_QUERY, new String[] {wcpsQuery});
        Response response = this.kvpwcsProcessCoverageHandler.handle(kvpParametersMap);
        return response;
    }


    // ------ step 1

    private JsonNode parseJsonToGetProcessGraphNode(String json) throws PetascopeException, IOException {

        if (!JSONUtil.isJsonValid(json)) {
            throw new PetascopeException(ExceptionCode.InvalidRequest, "Process graph content to execute is not valid JSON string format.");
        }

        // parse payload from POST method and extract the content of "process_graph" property to validate to the schema
        log.trace("parsing json string: " + json);
        JsonNode processGraphNode = JSONUtil.getJsonNodeByPropertyName(json, PROCESS_GRAPH_PROPERTY_NAME);

        JSONUtil.validate(processGraphNode.toPrettyString(), PROCESS_GRAPH_JSON_SCHEMA_URL, SpecVersion.VersionFlag.V7);

        return processGraphNode;
    }


    // ------ step 2

    /**
     * From the content of JsonNode processGraphJsonNode, build a ProcessGraph node which later will translate it to
     * a complete WCPS query
     */
    private ProcessGraph parseProcessGraphNode(JsonNode processGraphNode, String username) throws PetascopeException, IOException {

        log.debug("parsing process graph");

        Map<String, ProcessNode> processNodeMap = new LinkedHashMap<>();

        Iterator<Map.Entry<String, JsonNode>> fields = processGraphNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> jsonField = fields.next();

            // e.g. p1
            String processNodeId = jsonField.getKey();

            JsonNode processNodeJsonNode = jsonField.getValue();

            // e.g. sum
            String processId = processNodeJsonNode.get("process_id").asText();

            boolean isResultNode = false;
            if (processNodeJsonNode.has("result")) {
                isResultNode = processNodeJsonNode.get("result").asBoolean();
            }

            String wcpsExpression = null;

            Map<String, ParameterValue> argumentsMap = parseArgumentsMap(processNodeJsonNode, username);

            // TODO this should be configurable in the request?
            ProcessNode.Namespace namespace = ProcessNode.Namespace.BOTH;

            ProcessDefinition processDefinition = getProcessDefinition(processId, ProcessNode.Namespace.BOTH, username);

            // final obj
            ProcessNode processNodeTmp = new ProcessNode(processNodeId, processId, ProcessNode.Namespace.BOTH,
                argumentsMap, processDefinition,
                wcpsExpression, isResultNode);
            processNodeMap.put(processNodeId, processNodeTmp);
        }

        ProcessGraph result = new ProcessGraph(processNodeMap);
        log.debug("Parsed process graph: " + result);
        return result;
    }

    private String getOptionalText(JsonNode node, String property) {
        return node.has(property) ? node.get(property).asText() : null;
    }

    /**
     * Parse a map of arguments (an argument is a parameter) of a processNode
     * e.g.
     *               "arguments": {
     * 				"x": 6,
     * 				"y": {
     * 					"from_parameter": "red"
     *                   }
     *               }
     */
    private Map<String, ParameterValue> parseArgumentsMap(JsonNode processNode, String username) throws PetascopeException, IOException {
        Map<String, ParameterValue> results = new LinkedHashMap<>();

        String processId = processNode.get("process_id").asText();
        JsonNode argumentsJsonNode = processNode.get("arguments");
        Iterator<Map.Entry<String, JsonNode>> fields = argumentsJsonNode.fields();
        while (fields.hasNext()) {
            // parse each argument, e.g. "x": 6 or "y": { "from_parameter": "red" }
            Map.Entry<String, JsonNode> jsonField = fields.next();

            // the argument name, e.g. x or y
            String parameterName = jsonField.getKey();
            ParameterValue.ParameterType parameterType = null;
            JsonNode argValueNode = jsonField.getValue();

            // one of these will be set below in the if/else block
            Object scalarValue = null;
            List<Object> valueArray = null;
            String fromParameter = null;
            String fromNode = null;
            ProcessGraph processGraph = null;

            // e.g. "x": 3.5
            if (argValueNode.isDouble()) {
                scalarValue = argValueNode.doubleValue();
                parameterType = ParameterValue.ParameterType.DOUBLE;
            } else if (argValueNode.isLong()) {
                scalarValue = argValueNode.longValue();
                parameterType = ParameterValue.ParameterType.LONG;
            } else if (argValueNode.isBoolean()) {
                scalarValue = argValueNode.booleanValue();
                parameterType = ParameterValue.ParameterType.BOOLEAN;
            }

            // e.g. "arguments": { "data": [1, 2, 3] }
            else if (argValueNode.isArray()) {
                log.debug("value of argument " + parameterName + " is an array");
                valueArray = new ArrayList<>();
                parameterType = ParameterValue.ParameterType.ARRAY;
                Iterator<JsonNode> valueElements = argValueNode.elements();
                while (valueElements.hasNext()) {
                    Object valueObj = null;

                    JsonNode valueElement = valueElements.next();
                    log.debug("item value type: " + valueElement.getNodeType());
                    if (valueElement.isDouble()) {
                        valueObj = valueElement.doubleValue();
                    } else if (valueElement.isLong()) {
                        valueObj = valueElement.longValue();
                    } else if (valueElement.isBoolean()) {
                        valueObj = valueElement.booleanValue();
                    } else if (valueElement.isObject()) {
                        if (parameterName.equals("subset") || parameterName.equals("scale_spec")) {
                            String lower = getOptionalText(valueElement, "lower");
                            if (lower == null)
                                lower = getOptionalText(valueElement, "factor");
                            if (lower == null)
                                throw new RuntimeException("Required argument missing: lower");
                            valueObj = new ParameterSubset(
                                valueElement.get("dimension").asText(),
                                lower,
                                getOptionalText(valueElement, "upper"),
                                getOptionalText(valueElement, "crs"));
                        } else {
                            log.debug("Cannot handle object array item of parameter '" + parameterName + "'.");
                            valueObj = valueElement.asText();
                        }
                    } else {
                        valueObj = valueElement.asText();
                    }

                    if (valueObj == null) {
                        throw new RuntimeException("Could not determine array item value of parameter: " + parameterName);
                    }

                    // NOTE: this case with each element is an object with "from_" property is not supported (!)
                    // "arguments": {  "data": [  	1,  	{  		"from_parameter": "nir"  	},  	{  		"from_node": "p1"  	},  	{  		"from_node": "p2"  	}  ] 			}

                    log.debug("item value: " + valueObj);
                    valueArray.add(valueObj);
                }
            }

            // e.g. "arguments": { "x": { "from_parameter": "red" } }
            else if (argValueNode.isObject()) {
                parameterType = ParameterValue.ParameterType.OBJECT;
                if (argValueNode.has("from_parameter")) {
                    parameterType = ParameterValue.ParameterType.FROM_PARAMETER;
                    fromParameter = argValueNode.get("from_parameter").asText();
                } else if (argValueNode.has("from_node")) {
                    parameterType = ParameterValue.ParameterType.FROM_NODE;
                    fromNode = argValueNode.get("from_node").asText();
                } else if (argValueNode.has("process_graph")) {
                    // NOTE: an argument can contain a process_graph
                    parameterType = ParameterValue.ParameterType.PROCESS_GRAPH;
                    processGraph = this.parseProcessGraphNode(argValueNode, username);
                } else if (parameterName.equals("spatial_extent")) {
                    // TODO special case for load_collection
                    parameterType = ParameterValue.ParameterType.BOUNDING_BOX;
                    String west = argValueNode.get("west").asText();
                    String east = argValueNode.get("east").asText();
                    String south = argValueNode.get("south").asText();
                    String north = argValueNode.get("north").asText();
                    String base = getOptionalText(argValueNode, "base");
                    String height = getOptionalText(argValueNode, "height");
                    String crs = getOptionalText(argValueNode, "crs");
                    scalarValue = new ParameterBoundingBox(west, east, south, north, base, height, crs);
                } else if (processId.equals("save_result") && parameterName.equals("options")) {
                    scalarValue = argValueNode.toString().replaceAll("\"", "\\\\\"");
                } else {
                    scalarValue = argValueNode;
                }
            }

            else {
                // e.g. "x": "EPSG:4326"
                // If don't know the type of the argument value -> string
                parameterType = ParameterValue.ParameterType.STRING;
                scalarValue = argValueNode.asText();
            }

            ParameterValue parameterValue = new ParameterValue(parameterName,
                parameterType, scalarValue,
                valueArray,
                fromNode,
                fromParameter, processGraph);

            // e.g. x -> ParameterValue{ scalarValue:5 }
            results.put(parameterName, parameterValue);

            log.trace("parsed: " + parameterName + " -> " + parameterValue);
        }

        return results;
    }


    // ------ step 3

    /**
     * Parse a process definition from petascopedb; exmaple process definition:
     *
     * ```
     * {
     "id": "load_collection",
     "summary": "Load a collection",
     "parameters": [
     {
     "name": "id", ...
     },
     "wcps": "..."
     }
     * ```
     */
    ProcessDefinition getProcessDefinition(String processId, ProcessNode.Namespace namespace, String username) throws PetascopeException, IOException {
        // 1. load JSON for processId from petascopedb based on the processId and namespace

        String processDefJson = null;

        if (namespace == ProcessNode.Namespace.BOTH) {
            processDefJson = ProcessesCapability.getJsonContent(processId);
            if (processDefJson == null) {
                org.rasdaman.domain.openeo.ProcessGraph processGraph = processGraphRepositoryService.getProcessGraph(username, processId);
                processDefJson = processGraph.getContent();
            }
        } else if (namespace == ProcessNode.Namespace.PREDEFINED_ONLY) {
            processDefJson = ProcessesCapability.getJsonContent(processId);
        } else {
            org.rasdaman.domain.openeo.ProcessGraph processGraph = processGraphRepositoryService.getProcessGraph(username, processId);
            processDefJson = processGraph.getContent();
        }

        // 2. parseJSON, and translate to ProcessDefinition
        JsonNode processDefNode = JSONUtil.getJsonNode(processDefJson);

        /* The ProcessDefinition must have these properties from the parsed JSON:
           - String id - process id
           - List<ProcessParameter> parameters - an array of parameters
           - String wcpsTemplate - the WCPS template (see here)
        */
        String id = processDefNode.get("id").asText();
        String wcpsTemplate = getOptionalText(processDefNode, "wcps");
        List<ProcessParameter> parameters = new ArrayList<>();

        JsonNode parametersNode = processDefNode.get("parameters");
        Iterator<Map.Entry<String, JsonNode>> parametersIt = parametersNode.fields();
        while (parametersIt.hasNext()) {
            // parse each parameter
            JsonNode parameterNode = parametersIt.next().getValue();

            /* The ProcessParameter must have these properties:
               - String name - the parameter name
               - Boolean isRequired - by default true, unless "optional": true is set in the JSON
               - String defaultValue - by default null (no default value),
                 otherwise the value of "default" in the JSON as a String
            */
            String name = parameterNode.get("name").asText();
            boolean isRequired = true;
            if (parameterNode.has("optional"))
                isRequired = !parameterNode.get("optional").asBoolean();
            String defaultValue = getOptionalText(parameterNode, "default");

            ProcessParameter param = new ProcessParameter(name, isRequired, defaultValue);
            parameters.add(param);
        }

        ProcessDefinition ret = new ProcessDefinition(id, parameters, wcpsTemplate);
        log.trace("got process definition: " + ret);
        return ret;
    }


    // ------ step 4

    // Render a ProcessGraph into a WCPS query expression and return it
    private String renderProcessGraph(ProcessGraph g) throws PetascopeException {
        // Will be set to true when all processing have a wcpsExpr which is not null.
        boolean allRendered = false;
        // Check that at least one node is rendered in the nested for loop below;
        // If no nodes are rendered in the loop  and allRendered is still false,
        // then the processing must be terminated to avoid an infinite cycle loop.
        boolean atLeastOneRendered = true;
        // The result node
        ProcessNode result = null;

        // Loop until all nodes are resolved
        while (!allRendered && atLeastOneRendered) {
            // Assume all nodes are rendered, until proven false in the loop below
            allRendered = true;
            // Assume no node at all got rendered until proven false in the loop below
            atLeastOneRendered = false;

            for (Map.Entry<String, ProcessNode> entry : g.getProcessNodeMap().entrySet()) {
                ProcessNode p = entry.getValue();
                if (p.wcpsExpression != null) {
                    continue; // already rendered, skip
                }
                p.wcpsExpression = renderProcessNode(p, g);

                if (p.wcpsExpression == null) {
                    // At least one node could not be rendered, set this to false in order
                    // to retry again at the next while loop
                    allRendered = false;
                } else {
                    log.trace("rendered node " + p.processNodeId + " to: " + p.wcpsExpression);
                    // At least one node was rendered, we know for sure there's no cycle (yet)
                    atLeastOneRendered = true;
                    if (p.result) {
                        // We rendered the result node, so we're done
                        result = p;
                        allRendered = true;
                        break;
                    }
                }
            }
        }

        // TODO fix to correct exceptions
        if (!allRendered) {
            throw new RuntimeException("Encountered an infinite cycle in the processing graph.");
        }
        if (result == null) {
            throw new RuntimeException("No result process node specified in the process graph");
        }

        return result.wcpsExpression;
    }

    private String renderProcessNode(ProcessNode p, ProcessGraph g) throws PetascopeException {
        // 0. StringTemplate object which will render the WCPS expression
        String wcpsTemplate = p.processDefinition.getWcpsTemplate();
        if (wcpsTemplate == null)
            wcpsTemplate = "";
        STGroupString stg = new STGroupString(wcpsTemplate);
        ST template = stg.getInstanceOf("f");
        if (template == null)
            template = new ST(wcpsTemplate);

        // put the values also in a map, so we can use them to resolve load_collection
        Map<String, Object> argValues = new HashMap<String, Object>();

        // 1. Add optional parameters with default values to the template; they will
        // be overridden in 2. if a corresponding argument was actually specified
        for (ProcessParameter param : p.processDefinition.getProcessParameters()) {
            Object defaultValue = param.getDefaultValue();
            if (!param.isRequired() && defaultValue != null) {
                template.add(param.getName(), defaultValue);
                argValues.put(param.getName(), defaultValue);
            }
        }

        // 2. Resolve all arguments to a value (a literal number or a WCPS expression),
        // and add to the template engine
        for (Map.Entry<String, ParameterValue> entry : p.arguments.entrySet()) {
            ParameterValue arg = entry.getValue();
            String argName = entry.getKey();
            Object value = null;
            log.trace("rendering argument to WCPS expression: " + arg);
            switch (arg.getParameterType()) {
                case FROM_NODE:
                    // get the referenced node
                    ProcessNode fromNode = g.getProcessNodeMap().get(arg.getFromNode());
                    if (fromNode.wcpsExpression == null) {
                        // the fromNode WCPS expression is not resolved yet,
                        // we cannot use it yet to render the WCPS for this node p.
                        return null;
                    }
                    value = fromNode.wcpsExpression;
                    break;
                case FROM_PARAMETER:
                    // Example: "arguments": { "x": {"from_parameter": "x"} }
                    // We just add the parameter name, "x"
                    value = arg.getFromParameter();
                    break;
                case PROCESS_GRAPH:
                    String wcpsExpr = renderProcessGraph(arg.getProcessGraph());
                    value = wcpsExpr;
                    break;
                case ARRAY:
                    value = arg.getValueArray();
                    break;
                default:
                    value = arg.getScalarValue();
                    break;
            }
            log.trace("WCPS expression: " + value);
            template.add(argName, value);
            argValues.put(argName, value);
        }

        // 3. Check that required parameters are specified
        for (ProcessParameter param : p.processDefinition.getProcessParameters()) {
            if (param.isRequired()) {
                boolean found = false;
                for (Map.Entry<String, ParameterValue> entry : p.arguments.entrySet()) {
                    if (entry.getValue().parameterName.equals(param.getName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // TODO fix correct exception
                    throw new RuntimeException("Cannot call process " + p.processId + " with process node "
                        + p.processNodeId + ", required parameter is not specified as an argument: "
                        + param.getName());
                }
            }
        }

        // 4. Render the WCPS template
        String wcpsExpr = null;
        if (p.processId.equals("load_collection")) {
            // Manually render load_collection as it's too complex for a string template
            wcpsExpr = renderLoadCollection(argValues);
        } else if (p.processId.equals("scale")) {
            // Manually render scale
            wcpsExpr = renderScale(argValues.get("data").toString(), argValues.get("scale_spec"));
        } else {
            // Render StringTemplate
            wcpsExpr = template.render();
            if (wcpsExpr == null && "".equals(wcpsExpr)) {
                throw new RuntimeException("Failed translating process node " + p.processNodeId + " to WCPS.");
            }
        }

        return wcpsExpr;
    }

    String renderLoadCollection(Map<String, Object> argValues) throws PetascopeException {
        String coverageId = argValues.get("id").toString();
        String coverageAlias = loadCollectionCoverageIdAliasMap.get(coverageId);
        if (coverageAlias == null) {
            coverageAlias = "$c" + loadCollectionCoverageIdAliasMap.size();
            loadCollectionCoverageIdAliasMap.put(coverageId, coverageAlias);
        }

        WcpsCoverageMetadata coverageMetadata = this.wcpsCoverageMetadataTranslator.translate(coverageId);

        String result = coverageAlias;

        if (argValues.containsKey("spatial_extent")) {
            Object value = argValues.get("spatial_extent");
            if (value instanceof ParameterBoundingBox) {
                String timeSubset = this.renderTimeSubset(argValues, coverageMetadata);
                result += renderBoundingBoxWithDateTimeIfAny(coverageMetadata, (ParameterBoundingBox) value, timeSubset);
            } else if (value != null && value instanceof String &&
                !((String)value).equals("null") && !((String)value).isEmpty()) {
                result = "clip(" + result + ", " + value.toString() + ")";
            }
        } else if (argValues.containsKey("temporal_extent")) {
            result += "[" + this.renderTimeSubset(argValues, coverageMetadata) + "]";
        }

        if (argValues.containsKey("bands")) {
            result = renderFilterBands((List<Object>) argValues.get("bands"), result);
        }

        return result;
    }

    /**
     * e.g. t("20215-01-01":"2015-01-03")
     */
    private String renderTimeSubset(Map<String, Object> argValues, WcpsCoverageMetadata coverageMetadata) {
        String result = null;

        if (argValues.containsKey("temporal_extent")) {
            List<Object> dates = (List<Object>) argValues.get("temporal_extent");
            String lowerBound = StringUtil.enquoteIfNotEnquotedAlready(dates.get(0).toString());
            String timeSubsets = "(" + lowerBound + ")";
            String upperBound = null;
            if (dates.size() > 1) {
                upperBound = StringUtil.enquoteIfNotEnquotedAlready(dates.get(1).toString());
                timeSubsets = "(" + lowerBound + ":" + upperBound + ")";
            }

            Axis timeAxis = coverageMetadata.getTimeAxis();

            result = timeAxis.getLabel() + timeSubsets;
        }

        return result;
    }

    /**
     * @return a WCPS subset expression:
     * [x:"EPSG:v.crs"(v.west:v.east), y:"EPSG:v.crs"(v.south:v.north)]
     */
    String renderBoundingBoxWithDateTimeIfAny(WcpsCoverageMetadata coverageMetadata, ParameterBoundingBox v, String timeSubset) throws PetascopeException {
        List<Axis> xyAxes = coverageMetadata.getXYAxes();
        Axis axisX = xyAxes.get(0);
        Axis axisY = xyAxes.get(1);

        String crs = ""; // native CRS
        if (v.crs != null)
            crs = ":\"EPSG:" + v.crs + "\"";
        String spatialExtent = axisX.getLabel() + crs + "(" + v.west + ":" + v.east + "), " +
            axisY.getLabel() + crs + "(" + v.south + ":" + v.north + ")";
        if (v.base != null) {
            spatialExtent += ", z(" + v.base + ":" + v.height + ")";
        }

        if (timeSubset != null) {
            spatialExtent += ", " + timeSubset;
        }

        spatialExtent = "[" + spatialExtent + "]";
        return spatialExtent;
    }

    String renderFilterBands(List<Object> bands, String wcpsExpr) {
        String result = wcpsExpr;
        if (bands.size() == 1) {
            result += "." + bands.get(0).toString();
        } else {
            String tmp = "";
            for (Object band: bands) {
                if (tmp.isEmpty())
                    tmp += "; ";
                tmp += band.toString() + ": " + result;
            }
            result = "{ " + tmp + " }";
        }
        return result;
    }

    String renderScale(String data, Object scaleSpec) {
        String target = "";

        if (scaleSpec instanceof String) {
            // another coverage
            target = "{ imageCrsDomain(" + scaleSpec + ") }";
        } else if (scaleSpec instanceof List) {
            List<ParameterSubset> listSpec = (List<ParameterSubset>) scaleSpec;
            for (ParameterSubset ps: listSpec) {
                if (!target.isEmpty())
                    target += ", ";
                target += ps.dimension;
                if (ps.upper != null) {
                    // target extents
                    target += ":\"CRS:1\"(" + ps.lower + ":" + ps.upper + ")";
                } else {
                    // scale factors
                    target += "(" + ps.lower + ")";
                }
                target = "{ " + target + " }";
            }
        } else {
            // single factor
            target = scaleSpec.toString();
        }

        return "scale(" + data + ", " + target + ")";
    }


    // ------ step 5


    private String translateToWcpsQuery(ProcessGraph g) throws PetascopeException {
        // return clause
        log.debug("translating process graph to WCPS query");
        String returnExpr = renderProcessGraph(g);
        String returnClause = " return " + returnExpr;
        log.debug("return clause: " + returnClause);

        List<String> forClauseExpressions = new ArrayList<>();
        for (Map.Entry<String, String> entry : loadCollectionCoverageIdAliasMap.entrySet()) {
            forClauseExpressions.add(entry.getValue() + " IN (" + entry.getKey() + ") ");
        }

        String forClause = "FOR " + ListUtil.join(forClauseExpressions, ", ");
        // TODO let, where clauses?

        // final result
        String wcpsQuery = forClause + returnClause;
        log.debug("Generated final WCPS query: " + wcpsQuery);
        return wcpsQuery;
    }

}
