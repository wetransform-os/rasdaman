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
package petascope.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rasdaman.accesscontrol.service.AuthenticationService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.rasdaman.domain.openeo.ProcessGraph;
import org.rasdaman.repository.service.ProcessGraphRepositoryService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import petascope.core.Pair;
import petascope.core.response.Response;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.oapi.handlers.model.Collection;
import petascope.oapi.handlers.service.OapiHandlersService;
import petascope.oapi.handlers.service.OapiResultService;
import petascope.openeo.handlers.model.ProcessGraphsCapability;
import petascope.openeo.handlers.service.OpenEOHandlersService;
import petascope.openeo.handlers.service.OpenEOJSonToWcpsResultService;
import petascope.util.JSONUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

import static org.rasdaman.config.ConfigManager.*;
import static petascope.controller.OapiController.OAPI_CONFORMANCE;
import static petascope.core.KVPSymbols.KEY_OPENEO_PROCESS_GRAPH_ID;
import static petascope.core.KVPSymbols.KEY_OPENEO_PROCESS_GRAPH_JSON_CONTENT;
import static petascope.util.MIMEUtil.MIME_JSON;

/**
 * Controller to handle openEO coverage requests, see openEO document https://openeo.org/documentation/1.0/
 * 
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@RestController
public class OpenEOController extends AbstractController {

    private static org.slf4j.Logger log = LoggerFactory.getLogger(OpenEOController.class);

    @Autowired
    private OapiResultService oapiResultService;
    @Autowired
    private OapiHandlersService oapiHandlersService;

    @Autowired
    private OpenEOHandlersService openEOHandlersService;

    @Autowired
    private ProcessGraphRepositoryService processGraphRepositoryService;

    @Autowired
    private OpenEOJSonToWcpsResultService openEOJSonToWcpsResultService;

    // --- GDC endpoints

    public static final String GDC_CONFORMANCE = GDC + "/conformance";

    public static final String GDC_WELLKNOWN_CONTEXT_PATH = GDC + "/.well-known/openeo";
    private static final String GDC_FILE_FORMATS_CONTEXT_PATH = GDC + "/file_formats";

    private static final String GDC_PROCESSES_CONTEXT_PATH = GDC + "/processes";
    public static final String GDC_PROCESS_WCPS_CONTEXT_PATH = GDC + "/process";

    private static final String GDC_PROCESS_GRAPHS_CONTEXT_PATH = GDC +  "/process_graphs";
    private static final String GDC_RESULT = OPENEO + "/result";


    // --- openEO endpoints

    public static final String OPENEO_WELLKNOWN_CONTEXT_PATH = OPENEO + "/.well-known/openeo";
    private static final String OPENEO_FILE_FORMATS_CONTEXT_PATH = OPENEO + "/file_formats";

    private static final String OPENEO_PROCESSES_CONTEXT_PATH = OPENEO +  "/processes";

    private static final String OPENEO_PROCESS_GRAPHS_CONTEXT_PATH = OPENEO +  "/process_graphs";
    private static final String OPENEO_RESULT = OPENEO + "/result";

    // No need basic authentication to access these endpoints
    public static final List<String> NO_NEED_AUTHENTICATION_ENDPOINTS = Arrays.asList(
                                                        GDC,
                                                        OPENEO,

                                                        GDC_CONFORMANCE,

                                                        GDC_WELLKNOWN_CONTEXT_PATH,
                                                        OPENEO_WELLKNOWN_CONTEXT_PATH,

                                                        GDC_FILE_FORMATS_CONTEXT_PATH,
                                                        OPENEO_FILE_FORMATS_CONTEXT_PATH,

                                                        GDC_PROCESSES_CONTEXT_PATH,
                                                        OPENEO_PROCESSES_CONTEXT_PATH,

                                                        GDC_PROCESS_WCPS_CONTEXT_PATH);

    public OpenEOController() {
        
    }

    // --- 1. Special endpoint for GDC endpoint
    /**
     * Return landing page of /rasdaman/gdc
     */
    @RequestMapping(value = GDC)
    public void getGDCLandingPage(HttpServletRequest httpServletRequest) throws Exception {
        String gdcURLPrefix = this.getGDCBaseURL(httpServletRequest);

        RequestHandlerInterface requestHandlerInterface = () -> {

            try {
                Response response = this.oapiResultService.getJsonResponse(this.openEOHandlersService.getGDCLandingPage(gdcURLPrefix));
                this.writeResponseResult(response);
            } catch (Exception ex) {
                String errorMessage = "Error returning result for GDC landing page. Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(new HashMap<>(), requestHandlerInterface);
    }

    @RequestMapping(path = { GDC_CONFORMANCE, OAPI_CONFORMANCE })
    public void getConformanceResult(HttpServletRequest httpServletRequest) throws Exception {
        RequestHandlerInterface requestHandlerInterface = () -> {

            try {
                Response response = this.oapiResultService.getJsonResponse(this.openEOHandlersService.getGDCConformanceResult());
                this.writeResponseResult(response);
            } catch (Exception ex) {
                String errorMessage = "Error returning result for GDC conformance page. Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(new HashMap<>(), requestHandlerInterface);
    }


    // NOTE: the list of endpoints below come from https://openeo.org/documentation/1.0/developers/backends/getting-started.html
    // Some available service endpoints which implemented openEO: https://hub.openeo.org/
    // - https://openeo.eodc.eu/openeo/1.1.0
    // - https://openeo.eurac.edu
    // - https://openeocloud.vito.be/openeo/1.0.0

    /**
     * 1. List well-known documents
     *    e.g. https://openeo.eurac.edu/.well-known/openeo
     */
    @RequestMapping(path = { OPENEO_WELLKNOWN_CONTEXT_PATH, GDC_WELLKNOWN_CONTEXT_PATH })
    public void getWellknownDocument(HttpServletRequest httpServletRequest) throws Exception {

        RequestHandlerInterface requestHandlerInterface = () -> {

            try {
                Response response = this.oapiResultService.getJsonResponse(this.openEOHandlersService.getWellknownCapability());
                this.writeResponseResult(response);
            } catch (Exception ex) {
                String errorMessage = "Error returning result for returning well-known result. Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(new HashMap<>(), requestHandlerInterface);
    }

    /**
     * 2. List the list of supported endpoints (Capabilities)
     * e.g. https://openeo.eodc.eu/openeo/1.1.0/
     */
    @RequestMapping(value = OPENEO)
    public void getCapabilities(HttpServletRequest httpServletRequest) throws Exception {

        RequestHandlerInterface requestHandlerInterface = () -> {

            try {
                Response response = this.oapiResultService.getJsonResponse(this.openEOHandlersService.getEndpointsCapability());
                this.writeResponseResult(response);
            } catch (Exception ex) {
                String errorMessage = "Error returning result for listing openEO endpoints. Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(new HashMap<>(), requestHandlerInterface);
    }

    /**
     * 3. List the list of supported input / output files formats
     *  e.g. https://openeo.eodc.eu/openeo/1.1.0/file_formats
     */
    @RequestMapping(path = { OPENEO_FILE_FORMATS_CONTEXT_PATH, GDC_FILE_FORMATS_CONTEXT_PATH })
    public void getFileFormats(HttpServletRequest httpServletRequest) throws Exception {

        RequestHandlerInterface requestHandlerInterface = () -> {

            try {
                Response response = this.oapiResultService.getJsonResponse(this.openEOHandlersService.getFileFormatsCapability());
                this.writeResponseResult(response);
            } catch (Exception ex) {
                String errorMessage = "Error returning result for listing file formats. Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(new HashMap<>(), requestHandlerInterface);
    }


    /**
     * 6. List the list of pre-defined processes from JSON files
     * https://openeo.eodc.eu/openeo/1.1.0/processes
     */
    @RequestMapping(path = { OPENEO_PROCESSES_CONTEXT_PATH, GDC_PROCESSES_CONTEXT_PATH })
    public void getProcesses(HttpServletRequest httpServletRequest) throws Exception {
        RequestHandlerInterface requestHandlerInterface = () -> {

            try {
                Response response = this.oapiResultService.getJsonResponseFromString(this.openEOHandlersService.getJSONProcessesCapability());
                this.writeResponseResult(response);
            } catch (Exception ex) {
                String errorMessage = "Error returning result for listing processes. Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getJsonErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(new HashMap<>(), requestHandlerInterface);

    }

    // -------- Process Graphs
    // https://openeo.org/documentation/1.0/developers/api/reference.html#tag/User-Defined-Processes

    // 1. Create (insert / update) a new process graph to database
    @PutMapping(path = { OPENEO_PROCESS_GRAPHS_CONTEXT_PATH + "/{" + KEY_OPENEO_PROCESS_GRAPH_ID + "}", GDC_PROCESS_GRAPHS_CONTEXT_PATH + "/{" + KEY_OPENEO_PROCESS_GRAPH_ID + "}" })
    public void createProcessGraph(@PathVariable String processGraphId, HttpServletRequest httpServletRequest) throws Exception {

        Map<String, String[]> kvpParameters = new LinkedHashMap<>();
        kvpParameters.put(KEY_OPENEO_PROCESS_GRAPH_ID, new String[] { processGraphId });

        RequestHandlerInterface requestHandlerInterface = () -> {

            try {
                Pair<String, String> credentials = AuthenticationService.getBasicAuthUsernamePassword(httpServletRequest);
                if (credentials == null) {
                    throw new PetascopeException(ExceptionCode.Unauthorized, "Endpoint requires user credentials in basic authentication header.");
                }

                // parse payload from PUT method
                String inputJsonContent = IOUtils.toString(httpServletRequest.getReader());
                if (!JSONUtil.isJsonValid(inputJsonContent)) {
                    throw new PetascopeException(ExceptionCode.InvalidRequest, "Process graph content to create is not valid JSON string format.");
                }

                kvpParameters.put(KEY_OPENEO_PROCESS_GRAPH_JSON_CONTENT, new String[] { inputJsonContent });

                String username = credentials.fst;

                ProcessGraph processGraph = new ProcessGraph(processGraphId, username, inputJsonContent);
                this.processGraphRepositoryService.save(processGraph);
            } catch (Exception ex) {
                String errorMessage = "Error returning result for storing process graph: " + processGraphId + ". Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(kvpParameters, requestHandlerInterface);
    }

    // 2. Delete an existing process graph to database
    @RequestMapping(path = { OPENEO_PROCESS_GRAPHS_CONTEXT_PATH + "/{" + KEY_OPENEO_PROCESS_GRAPH_ID + "}", GDC_PROCESS_GRAPHS_CONTEXT_PATH + "/{" + KEY_OPENEO_PROCESS_GRAPH_ID + "}" }, method = RequestMethod.DELETE)
    public void deleteProcessGraph(@PathVariable String processGraphId, HttpServletRequest httpServletRequest) throws Exception {
        Map<String, String[]> kvpParameters = new LinkedHashMap<>();
        kvpParameters.put(KEY_OPENEO_PROCESS_GRAPH_ID, new String[] { processGraphId });

        RequestHandlerInterface requestHandlerInterface = () -> {

            try {
                Pair<String, String> credentials = AuthenticationService.getBasicAuthUsernamePassword(httpServletRequest);
                if (credentials == null) {
                    throw new PetascopeException(ExceptionCode.Unauthorized, "Endpoint requires user credentials in basic authentication header.");
                }

                this.processGraphRepositoryService.delete(processGraphId);
            } catch (Exception ex) {
                String errorMessage = "Error returning result for deleting process graph: " + processGraphId + ". Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(kvpParameters, requestHandlerInterface);
    }

    // 3. List an existing process graph from database
    @RequestMapping(path = { OPENEO_PROCESS_GRAPHS_CONTEXT_PATH + "/{" + KEY_OPENEO_PROCESS_GRAPH_ID + "}", GDC_PROCESS_GRAPHS_CONTEXT_PATH + "/{" + KEY_OPENEO_PROCESS_GRAPH_ID + "}" }, method = RequestMethod.GET)
    public void listProcessGraph(@PathVariable String processGraphId, HttpServletRequest httpServletRequest) throws Exception {
        Map<String, String[]> kvpParameters = new LinkedHashMap<>();
        kvpParameters.put(KEY_OPENEO_PROCESS_GRAPH_ID, new String[] { processGraphId });

        RequestHandlerInterface requestHandlerInterface = () -> {

            try {
                Pair<String, String> credentials = AuthenticationService.getBasicAuthUsernamePassword(httpServletRequest);
                if (credentials == null) {
                    throw new PetascopeException(ExceptionCode.Unauthorized, "Endpoint requires user credentials in basic authentication header.");
                }

                String username = credentials.fst;
                ProcessGraph processGraph = this.processGraphRepositoryService.getProcessGraph(username, processGraphId);
                String jsonContent = processGraph.getContent();
                Response response = this.oapiResultService.getJsonResponseFromString(jsonContent);
                this.writeResponseResult(response);
            } catch (Exception ex) {
                String errorMessage = "Error returning result for listing content of process graph: " + processGraphId + ". Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(kvpParameters, requestHandlerInterface);
    }

    // 4. List all process graphs from database created by the authenticated user
    @RequestMapping(path = { OPENEO_PROCESS_GRAPHS_CONTEXT_PATH, GDC_PROCESS_GRAPHS_CONTEXT_PATH }, method = RequestMethod.GET)
    public void listAllProcessGraphs(HttpServletRequest httpServletRequest) throws Exception {
        RequestHandlerInterface requestHandlerInterface = () -> {

            Pair<String, String> credentials = AuthenticationService.getBasicAuthUsernamePassword(httpServletRequest);
            if (credentials == null) {
                throw new PetascopeException(ExceptionCode.Unauthorized, "Endpoint requires user credentials in basic authentication header.");
            }
            String username = credentials.fst;

            Set<ProcessGraph> processGraphs = this.processGraphRepositoryService.getAllCachedProcessGraphsByUsername(username);
            ProcessGraphsCapability processGraphsCapability = new ProcessGraphsCapability();

            try {
                Response response = this.oapiResultService.getJsonResponseFromString(processGraphsCapability.getJSONResult(processGraphs));
                this.writeResponseResult(response);
            } catch (Exception ex) {
                String errorMessage = "Error returning result for listing all process graphs. Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(new HashMap<>(), requestHandlerInterface);
    }

    // 5. Receive a submitted POST JSON process and generates WCPS query accordingly and returns a result
    @PostMapping(path = { OPENEO_RESULT, GDC_RESULT })
    public void processJSONToGetResult(HttpServletRequest httpServletRequest) throws Exception {
        Map<String, String[]> kvpParameters = new LinkedHashMap<>();

        RequestHandlerInterface requestHandlerInterface = () -> {

            try {
                Pair<String, String> credentials = AuthenticationService.getBasicAuthUsernamePassword(httpServletRequest);
                if (credentials == null) {
                    throw new PetascopeException(ExceptionCode.Unauthorized, "Endpoint requires user credentials in basic authentication header.");
                }

                String username = credentials.fst;
                // parse payload from POST method
                String inputJsonContent = IOUtils.toString(httpServletRequest.getReader());
                kvpParameters.put(KEY_OPENEO_PROCESS_GRAPH_JSON_CONTENT, new String[] { inputJsonContent });

                Response response = this.openEOJSonToWcpsResultService.handle(inputJsonContent, username);
                this.writeResponseResult(response);
            } catch (Exception ex) {
                String errorMessage = "Error returning result for processing result. Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(kvpParameters, requestHandlerInterface);
    }

    /**
     * get the base URL
     */
    private String getGDCBaseURL(HttpServletRequest httpServletRequest) {
        // petascope endpoint proxy is configured in petascope.properies
        if (StringUtils.isNotEmpty(PETASCOPE_ENDPOINT_URL)) {
            String result = PETASCOPE_ENDPOINT_URL.replace(CONTEXT_PATH + "/" + OWS, CONTEXT_PATH + "/" + GDC);
            return result;
        } else {
            String result = httpServletRequest.getRequestURL().toString().split("/" + GDC)[0] + "/" + GDC;
            return result;
        }

    }


    @Override
    protected void handleGet(HttpServletRequest httpServletRequest) throws Exception {
    }

    @Override
    protected void requestDispatcher(HttpServletRequest httpServletRequest, Map<String, String[]> kvpParameters) throws PetascopeException {
    }
}
