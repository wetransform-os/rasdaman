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
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion;
import com.rasdaman.accesscontrol.service.AuthenticationService;
import org.apache.commons.io.IOUtils;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.rasdaman.config.ConfigManager.*;
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

    public static final String WELLKNOWN_CONTEXT_PATH = OPENEO + "/.well-known/openeo";
    private static final String FILE_FORMATS_CONTEXT_PATH = OPENEO + "/file_formats";

    private static final String COLLECTIONS_CONTEXT_PATH = OPENEO + "/collections";
    private static final String PROCESSES_CONTEXT_PATH = OPENEO +  "/processes";

    // --- Process graphs

    private static final String PROCESS_GRAPHS_CONTEXT_PATH = OPENEO +  "/process_graphs";
    private static final String RESULT = OPENEO + "/result";
    private static final String VALIDATION = OPENEO + "/validation";

    // No need basic authentication to access these endpoints
    public static final List<String> NO_NEED_AUTHENTICATION_ENDPOINTS = Arrays.asList(CREDENTIALS_BASIC,
                                                        OPENEO,
                                                        WELLKNOWN_CONTEXT_PATH,
                                                        FILE_FORMATS_CONTEXT_PATH,
                                                        PROCESSES_CONTEXT_PATH);

    public OpenEOController() {
        
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
    @RequestMapping(path = WELLKNOWN_CONTEXT_PATH)
    public void getWellknownDocument(HttpServletRequest httpServletRequest) throws PetascopeException, JsonProcessingException, IOException {
        try {
            Response response = this.oapiResultService.getJsonResponse(this.openEOHandlersService.getWellknownCapability());
            this.writeResponseResult(response);
        } catch (Exception ex) {
            String errorMessage = "Error returning result for returning well-known result. Reason: " + ex.getMessage();
            log.error(errorMessage, ex);
            Response response = this.oapiResultService.getJsonErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            this.writeResponseResult(response);
        }
    }

    /**
     * 2. List the list of supported endpoints (Capabilities)
     * e.g. https://openeo.eodc.eu/openeo/1.1.0/
     */
    @RequestMapping(path = OPENEO)
    public void getCapabilities(HttpServletRequest httpServletRequest) throws PetascopeException {
        try {
            Response response = this.oapiResultService.getJsonResponse(this.openEOHandlersService.getEndpointsCapability());
            this.writeResponseResult(response);
        } catch (Exception ex) {
            String errorMessage = "Error returning result for listing openEO endpoints. Reason: " + ex.getMessage();
            log.error(errorMessage, ex);
            Response response = this.oapiResultService.getJsonErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            this.writeResponseResult(response);
        }
    }

    /**
     * 3. List the list of supported input / output files formats
     *  e.g. https://openeo.eodc.eu/openeo/1.1.0/file_formats
     */
    @RequestMapping(path = FILE_FORMATS_CONTEXT_PATH)
    public void getFileFormats(HttpServletRequest httpServletRequest) throws PetascopeException, JsonProcessingException, IOException {
        try {
            Response response = this.oapiResultService.getJsonResponse(this.openEOHandlersService.getFileFormatsCapability());
            this.writeResponseResult(response);
        } catch (Exception ex) {
            String errorMessage = "Error returning result for listing file formats. Reason: " + ex.getMessage();
            log.error(errorMessage, ex);
            Response response = this.oapiResultService.getJsonErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            this.writeResponseResult(response);
        }
    }


    /**
     * 4. List the available collections
     * e.g. https://openeo.eodc.eu/openeo/1.1.0/collections
     */
    @RequestMapping(path = COLLECTIONS_CONTEXT_PATH)
    public void getCollections(HttpServletRequest httpServletRequest,
                               @RequestParam(value = "bbox", required = false) String bbox,
                               @RequestParam(value = "datetime", required = false) String datetime) throws PetascopeException, JsonProcessingException, IOException {
        String openEOEndpoint = openEOHandlersService.getOpenEOEndpoint();
        try {
            Response response = this.oapiResultService.getJsonResponse(oapiHandlersService.getCollectionsResult(openEOEndpoint, bbox, datetime));
            this.writeResponseResult(response);
        } catch (Exception ex) {
            String errorMessage = "Error returning for list collections. Reason: " + ex.getMessage();
            log.error(errorMessage, ex);
            Response response = new Response(Arrays.asList(errorMessage.getBytes()), MIME_JSON, HttpStatus.INTERNAL_SERVER_ERROR.value());
            this.writeResponseResult(response);
        }
    }

    /**
     * 5. Get the description of a specific coverage
     * e.g. https://openeo.eodc.eu/openeo/1.1.0/collections/SENTINEL1_GRD
     */
    @RequestMapping(path = COLLECTIONS_CONTEXT_PATH + "/{coverageId}")
    public void getCoverageInformation(@PathVariable String coverageId, HttpServletRequest httpServletRequest) throws PetascopeException, JsonProcessingException, IOException {
        String openEOEndpoint = openEOHandlersService.getOpenEOEndpoint();
        try {
            Collection collection = oapiHandlersService.getCollectionInformationResult(coverageId, openEOEndpoint);
            Response response = this.oapiResultService.getJsonResponse(collection);
            this.writeResponseResult(response);
        } catch (Exception ex) {
            String errorMessage = "Error returning coverage information. Reason: " + ex.getMessage();
            log.error(errorMessage, ex);
            Response response = new Response(Arrays.asList(errorMessage.getBytes()), MIME_JSON, HttpStatus.INTERNAL_SERVER_ERROR.value());
            this.writeResponseResult(response);
        }
    }


    /**
     * 6. List the list of pre-defined processes from JSON files
     * https://openeo.eodc.eu/openeo/1.1.0/processes
     */
    @RequestMapping(path = PROCESSES_CONTEXT_PATH)
    public void getProcesses(HttpServletRequest httpServletRequest) throws PetascopeException {
        try {
            Response response = this.oapiResultService.getJsonResponseFromString(this.openEOHandlersService.getJSONProcessesCapability());
            this.writeResponseResult(response);
        } catch (Exception ex) {
            String errorMessage = "Error returning result for listing processes. Reason: " + ex.getMessage();
            log.error(errorMessage, ex);
            Response response = this.oapiResultService.getJsonErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            this.writeResponseResult(response);
        }
    }

    // -------- Process Graphs
    // https://openeo.org/documentation/1.0/developers/api/reference.html#tag/User-Defined-Processes

    // 1. Create (insert / update) a new process graph to database
    @PutMapping(path = PROCESS_GRAPHS_CONTEXT_PATH + "/{processGraphId}")
    public void createProcessGraph(@PathVariable String processGraphId, HttpServletRequest httpServletRequest) throws Exception {
        try {
            Pair<String, String> credentials = AuthenticationService.getBasicAuthUsernamePassword(httpServletRequest);
            if (credentials == null) {
                throw new PetascopeException(ExceptionCode.InvalidRequest, "Endpoint requires user credentials in basic authentication header.");
            }


            // parse payload from PUT method
            String jsonContent = IOUtils.toString(httpServletRequest.getReader());
            if (!JSONUtil.isJsonValid(jsonContent)) {
                throw new PetascopeException(ExceptionCode.InvalidRequest, "Process graph content to create is not valid JSON string format.");
            }

            String username = credentials.fst;

            ProcessGraph processGraph = new ProcessGraph(processGraphId, username, jsonContent);
            this.processGraphRepositoryService.save(processGraph);
        }  catch (Exception ex) {
            String errorMessage = "Error returning result for storing process graph: " + processGraphId + ". Reason: " + ex.getMessage();
            log.error(errorMessage, ex);
            Response response = this.oapiResultService.getJsonErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            this.writeResponseResult(response);
        }
    }

    // 2. Delete an existing process graph to database
    @RequestMapping(path = PROCESS_GRAPHS_CONTEXT_PATH + "/{processGraphId}", method = RequestMethod.DELETE)
    public void deleteProcessGraph(@PathVariable String processGraphId, HttpServletRequest httpServletRequest) throws PetascopeException {
        try {
            Pair<String, String> credentials = AuthenticationService.getBasicAuthUsernamePassword(httpServletRequest);
            if (credentials == null) {
                throw new PetascopeException(ExceptionCode.InvalidRequest, "Endpoint requires user credentials in basic authentication header.");
            }


            this.processGraphRepositoryService.delete(processGraphId);
        } catch (Exception ex) {
            String errorMessage = "Error returning result for deleting process graph: " + processGraphId + ". Reason: " + ex.getMessage();
            log.error(errorMessage, ex);
            Response response = this.oapiResultService.getJsonErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            this.writeResponseResult(response);
        }
    }

    // 3. List an existing process graph from database
    @RequestMapping(path = PROCESS_GRAPHS_CONTEXT_PATH + "/{processGraphId}", method = RequestMethod.GET)
    public void listProcessGraph(@PathVariable String processGraphId, HttpServletRequest httpServletRequest) throws PetascopeException {
        try {
            Pair<String, String> credentials = AuthenticationService.getBasicAuthUsernamePassword(httpServletRequest);
            if (credentials == null) {
                throw new PetascopeException(ExceptionCode.InvalidRequest, "Endpoint requires user credentials in basic authentication header.");
            }

            String username = credentials.fst;
            ProcessGraph processGraph = this.processGraphRepositoryService.getProcessGraph(username, processGraphId);
            String jsonContent = processGraph.getContent();
            Response response = this.oapiResultService.getJsonResponseFromString(jsonContent);
            this.writeResponseResult(response);
        }  catch (Exception ex) {
            String errorMessage = "Error returning result for listing content of process graph: " + processGraphId + ". Reason: " + ex.getMessage();
            log.error(errorMessage, ex);
            Response response = this.oapiResultService.getJsonErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            this.writeResponseResult(response);
        }
    }

    // 4. List all process graphs from database created by the authenticated user
    @RequestMapping(path = PROCESS_GRAPHS_CONTEXT_PATH, method = RequestMethod.GET)
    public void listAllProcessGraphs(HttpServletRequest httpServletRequest) throws PetascopeException {
        Pair<String, String> credentials = AuthenticationService.getBasicAuthUsernamePassword(httpServletRequest);
        if (credentials == null) {
            throw new PetascopeException(ExceptionCode.InvalidRequest, "Endpoint requires user credentials in basic authentication header.");
        }
        String username = credentials.fst;

        Set<ProcessGraph> processGraphs = this.processGraphRepositoryService.getAllCachedProcessGraphsByUsername(username);
        ProcessGraphsCapability processGraphsCapability = new ProcessGraphsCapability();        ;
        try {
            Response response = this.oapiResultService.getJsonResponseFromString(processGraphsCapability.getJSONResult(processGraphs));
            this.writeResponseResult(response);
        } catch (Exception ex) {
            String errorMessage = "Error returning result for listing all process graphs. Reason: " + ex.getMessage();
            log.error(errorMessage, ex);
            Response response = this.oapiResultService.getJsonErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            this.writeResponseResult(response);
        }
    }

    // 5. Receive a submitted POST JSON process and generates WCPS query accordingly and returns a result
    @PostMapping(path = RESULT)
    public void processJSONToGetResult(HttpServletRequest httpServletRequest) throws Exception {
        try {
            Pair<String, String> credentials = AuthenticationService.getBasicAuthUsernamePassword(httpServletRequest);
            if (credentials == null) {
                throw new PetascopeException(ExceptionCode.InvalidRequest, "Endpoint requires user credentials in basic authentication header.");
            }


            String username = credentials.fst;
            // parse payload from POST method
            String jsonContent = IOUtils.toString(httpServletRequest.getReader());

            Response response = this.openEOJSonToWcpsResultService.handle(jsonContent, username);
            this.writeResponseResult(response);
        }  catch (Exception ex) {
            String errorMessage = "Error returning result for processing result. Reason: " + ex.getMessage();
            log.error(errorMessage, ex);
            Response response = this.oapiResultService.getJsonErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            this.writeResponseResult(response);
        }
    }


    @Override
    protected void handleGet(HttpServletRequest httpServletRequest) throws Exception {
    }

    @Override
    protected void requestDispatcher(HttpServletRequest httpServletRequest, Map<String, String[]> kvpParameters) throws PetascopeException {
    }
}
