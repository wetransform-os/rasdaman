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
 * Copyright 2003 - 2020 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.rasdaman.config.ConfigManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.rasdaman.config.ConfigManager.*;

import petascope.core.KVPSymbols;
import petascope.core.json.cis11.model.metadata.Metadata;
import petascope.core.response.Response;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.SecoreException;
import petascope.exceptions.WMSException;
import petascope.oapi.handlers.model.Collection;
import petascope.oapi.handlers.service.OapiHandlersService;
import petascope.oapi.handlers.service.OapiResultService;
import petascope.oapi.handlers.service.OapiSubsetParsingService;
import petascope.util.ExceptionUtil;
import petascope.util.JSONUtil;
import petascope.util.MIMEUtil;

import static petascope.core.KVPSymbols.*;
import static petascope.util.MIMEUtil.MIME_JSON;

/**
 * Controller to handle OAPI coverage requests, see OAPI PDF document on https://github.com/opengeospatial/ogc_api_coverages/blob/master/
 * 
 * @author Vlad Merticariu <v.merticariu@jacobs-university.de>
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@RestController
public class OapiController extends AbstractController {
    
    private static org.slf4j.Logger log = LoggerFactory.getLogger(OapiController.class);
    
    @Autowired
    private OapiHandlersService oapiHandlersService;
    @Autowired
    private OapiResultService oapiResultService;
    @Autowired
    private OapiSubsetParsingService opaiParsingService;
    @Autowired
    private HttpServletResponse httpServletResponse;
    
    // e.g: localhost:8080/rasdaman/oapi
    private static String BASE_URL;
    
    public static final String WCPS = "wcps";
    public static final String WCPS_CONTEXT_PATH = OAPI + "/" + WCPS;
    public static final String COLLECTIONS = "collections";
    // so it can match with /rasdaman/openeo/collections or /rasdaman/oapi/collections
    public static final String COLLECTIONS_CONTEXT_PATH = "/{symbolicName:" + GDC + "|" + OPENEO + "|" + OAPI + "}/" + COLLECTIONS;

    public static final String COVERAGE_ID = "{coverageId}";
    public static final String COVERAGE_ID_CONTEXT_PATH = COLLECTIONS_CONTEXT_PATH + "/" + COVERAGE_ID;

    public static final String OAPI_CONFORMANCE = OAPI + "/conformance";
    
    public static final String COVERAGE = "coverage";
    public static final String COVERAGE_CONTEXT_PATH = COVERAGE_ID_CONTEXT_PATH + "/" + COVERAGE;
    
    public static final String COVERAGE_DOMAIN_SET = "domainset";
    public static final String COVERAGE_DOMAIN_SET_CONTEXT_PATH = COVERAGE_CONTEXT_PATH + "/" + COVERAGE_DOMAIN_SET;
    public static final String COVERAGE_RANGE_TYPE = "rangetype";
    public static final String COVERAGE_RANGE_TYPE_CONTEXT_PATH = COVERAGE_CONTEXT_PATH + "/" + COVERAGE_RANGE_TYPE;
    
    public static final String COVERAGE_RANGE_SET = "rangeset";
    public static final String COVERAGE_RANGE_SET_CONTEXT_PATH = COVERAGE_CONTEXT_PATH + "/" + COVERAGE_RANGE_SET;
    public static final String COVERAGE_METADATA = "metadata";
    public static final String COVERAGE_METADATA_CONTEXT_PATH = COVERAGE_CONTEXT_PATH + "/" + COVERAGE_METADATA;

    // No need basic authentication to access these endpoints
    public static final List<String> NO_NEED_AUTHENTICATION_ENDPOINTS = Arrays.asList(
            OAPI,
            OAPI_CONFORMANCE

            );


    public OapiController() {
        
    }
    
    /**
     * List the general information about the service endpoint
     * 
     * e.g: https://oapi.rasdaman.org/rasdaman/oapi (7.3.1. API landing page), see: Landing Page Response Schema
     */
    @RequestMapping(path = OAPI)
    public void getLandingPage(HttpServletRequest httpServletRequest) throws Exception {
        this.setBaseURL(httpServletRequest);

        RequestHandlerInterface requestHandlerInterface = () -> {
            try {
                Response response = this.oapiResultService.getJsonResponse(oapiHandlersService.getLandingPageResult(BASE_URL));
                this.writeResponseResult(response);
            } catch (Exception ex) {
                String errorMessage = "Error returning landing page. Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }
        };

        super.handleRequest(new HashMap<>(), requestHandlerInterface);
    }

    /**
     * Execute a WCPS query, 
     * e.g: https://oapi.rasdaman.org/rasdaman/oapi/wcps?Q=for c in (mean_summer_airtemp) return encode(c, "png")
     */
    @RequestMapping(path = WCPS_CONTEXT_PATH)
    public void getProcessingWcpsResult(HttpServletRequest httpServletRequest) throws Exception {
        
        this.setBaseURL(httpServletRequest);
        
        Map<String, String[]> kvpParameters = this.parsePostRequestToKVPMap(httpServletRequest);
        String query = getValueByKeyAllowNull(kvpParameters, KEY_QUERY_SHORT_HAND);

        RequestHandlerInterface requestHandlerInterface = () -> {
            if (query == null) {
                String errorMessage = "Query parameter '" + KEY_QUERY_SHORT_HAND.toUpperCase() + "' is missing.";
                Response response = new Response(Arrays.asList(errorMessage.getBytes()), MIME_JSON, HttpStatus.BAD_REQUEST.value());
                this.writeResponseResult(response);
            } else {
                try {
                    Response response = oapiHandlersService.executeWcpsQuery(kvpParameters);
                    this.writeResponseResult(response);
                } catch (Exception ex) {
                    String errorMessage = "Error processing WCPS query. Reason: " + ex.getMessage();
                    Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                    this.writeResponseResult(response);
                }
            }
        };

        super.handleRequest(kvpParameters, requestHandlerInterface);
    }

    /**
     * 
     * The Collections operation returns a set of metadata which describes the collections (coverages) available from this API
     * e.g: https://oapi.rasdaman.org/rasdaman/oapi/collections (7.4.1. Collections -> 7.4.1.2. Response)
     * 
     * It is comparable to a WCS GetCapabilities.
     * 
     * NOTE: it also allows to filter the coverages by temporal (datetime parameter) and spatial (bbox parameter) 
     * e.g: https://oapi.rasdaman.org/rasdaman/oapi/collections?bbox=-175,-80,180,90&datetime="01-01-2015"
     * 
     * The collections array property contains the list of object (e.g: https://oapi.rasdaman.org/rasdaman/oapi/collections/mean_summer_airtemp)
     */
    @RequestMapping(path = COLLECTIONS_CONTEXT_PATH)
    public void getCollections(HttpServletRequest request,
                                         @RequestParam(value = KEY_OAPI_BBOX, required = false) String bbox,
                                         @RequestParam(value = KEY_OAPI_DATETIME, required = false) String datetime,
                                         HttpServletRequest httpServletRequest) throws Exception {

        Map<String, String[]> kvpParameters = this.parseKvpParametersFromRequest(httpServletRequest);

        this.setBaseURL(httpServletRequest);

        RequestHandlerInterface requestHandlerInterface = () -> {

            try {
                Response response = this.oapiResultService.getJsonResponse(oapiHandlersService.getCollectionsResult(BASE_URL, bbox, datetime));
                this.writeResponseResult(response);
            } catch (Exception ex) {
                String errorMessage = "Error returning list of coverages. Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }
        };

        super.handleRequest(kvpParameters, requestHandlerInterface);
    }

    /**
     * Collection Information is the set of metadata which describes a single collection, or in the the case of API-Coverages, a single Coverage.
     * It is comparable to a **WCS DescribeCoverage**.
     * e,g: https://oapi.rasdaman.org/rasdaman/oapi/collections/mean_summer_airtemp (7.4.2. Collection Information -> Collection Information Resource Example)
     */
    @RequestMapping(path = COVERAGE_ID_CONTEXT_PATH)
    public void getCoverageInformation(@PathVariable String coverageId,
                                       HttpServletRequest httpServletRequest) throws Exception {
        this.setBaseURL(httpServletRequest);

        RequestHandlerInterface requestHandlerInterface = () -> {

            try {
                Collection collection = oapiHandlersService.getCollectionInformationResult(coverageId, BASE_URL);
                Response response = this.oapiResultService.getJsonResponse(collection);
                this.writeResponseResult(response);
            } catch (Exception ex) {
                String errorMessage = "Error returning coverage information. Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(new HashMap<>(), requestHandlerInterface);
    }

    /**
     * Returns the coverage including all of its components (domain set, range type, range set and metadata).
     * It is comparable to a **WCS GetCoverage**.
     * 
     * e.g: https://oapi.rasdaman.org/rasdaman/oapi/collections/S2_FALSE_COLOR_84/coverage?subset=Lat(51.9:52.1),Long(-4.1:-3.9),ansi("2018-11-14")&f=gml
     */
    @RequestMapping(path = COVERAGE_CONTEXT_PATH, method = RequestMethod.GET)
    public void getCoverageObject(@PathVariable String coverageId,
                                  HttpServletRequest httpServletRequest) throws Exception {
        
        this.setBaseURL(httpServletRequest);
        Map<String, String[]> inputKVPMap = buildGetRequestKvpParametersMap(httpServletRequest.getQueryString());

        RequestHandlerInterface requestHandlerInterface = () -> {

            String[] inputSubsets = inputKVPMap.get(KEY_OAPI_SUBSET);
            String outputFormat = getValueByKeyAllowNull(inputKVPMap, KEY_OAPI_GET_COVERAGE_OUTPUT_FORMAT);
            String acceptHeaderValue = httpServletRequest.getHeader(ACCEPT_HEADER_KEY);

            // bbox and datetime are required parameters for subsetting at https://docs.ogc.org/DRAFTS/19-087.html#bbox-parameter-domainset-subset-requirements
            String bbox = getValueByKeyAllowNull(inputKVPMap, KEY_OAPI_BBOX);
            String datetime = getValueByKeyAllowNull(inputKVPMap, KEY_OAPI_DATETIME);
            String bboxCRS = getValueByKeyAllowNull(inputKVPMap, KEY_OAPI_BBOX_CRS);

            if (outputFormat == null && acceptHeaderValue != null) {
                // content negotiation if *f* parameter is missing from the request
                // then select the supported MIME type from left to right
                outputFormat = this.getSupportedMIMETypeForContentNegotiation(acceptHeaderValue);
            }

            boolean outputGeneralGridCoverageInJSON = false;

            if (outputFormat == null) {
                // In this case, petascope will return the best format for OAPI request (e.g. 1D without output format -> JSON)
                this.injectedHttpServletRequest.setAttribute(KEY_INTERNAL_OAPI_GET_COVERAGE, KEY_INTERNAL_OAPI_GET_COVERAGE);
            } else {
                // e.g. f=image/png
                this.injectedHttpServletRequest.setAttribute(KEY_INTERNAL_OAPI_GET_COVERAGE, outputFormat);
                if (outputFormat.equals(MIME_JSON)) {
                    outputGeneralGridCoverageInJSON = true;
                }

            }

            String[] parsedSubsets = this.opaiParsingService.parseGetCoverageSubsets(coverageId, bbox, bboxCRS, datetime, inputSubsets);
            try {
                // Internally it translates to WCS GetCoverage request
                Response response = oapiHandlersService.getCoverageSubsetResult(coverageId, parsedSubsets, outputFormat, inputKVPMap, outputGeneralGridCoverageInJSON);
                this.writeResponseResult(response);
            } catch (Exception ex) {
                String errorMessage = "Error returning coverage data. Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(inputKVPMap, requestHandlerInterface);
    }
    
    /**
     * Return a coverage's domain set object in CIS 1.1 JSON format
     * 
     * e.g: https://oapi.rasdaman.org/rasdaman/oapi/collections/mean_summer_airtemp/coverage/domainset
     */
    @RequestMapping(path = COVERAGE_DOMAIN_SET_CONTEXT_PATH, method = RequestMethod.GET)
    public void getDomainSet(@PathVariable String coverageId,
                             HttpServletRequest httpServletRequest) throws Exception {
        
        this.setBaseURL(httpServletRequest);

        Map<String, String[]> inputKVPMap = buildGetRequestKvpParametersMap(httpServletRequest.getQueryString());

        RequestHandlerInterface requestHandlerInterface = () -> {

            // bbox and datetime are required parameters for subsetting at https://docs.ogc.org/DRAFTS/19-087.html#bbox-parameter-domainset-subset-requirements
            String bbox = getValueByKeyAllowNull(inputKVPMap, KEY_OAPI_BBOX);
            String datetime = getValueByKeyAllowNull(inputKVPMap, KEY_OAPI_DATETIME);
            String bboxCRS = getValueByKeyAllowNull(inputKVPMap, KEY_OAPI_BBOX_CRS);

            String[] inputSubsets = inputKVPMap.get(KEY_OAPI_SUBSET);
            String[] parsedSubsets = this.opaiParsingService.parseGetCoverageSubsets(coverageId, bbox, bboxCRS, datetime, inputSubsets);

            // this request should not run the rasql query
            this.injectedHttpServletRequest.setAttribute(KEY_INTERNAL_WCPS_NOT_RUN_RASQL_QUERY, true);

            try {
                Response response = oapiHandlersService.getCoverageSubsetResult(coverageId, parsedSubsets, null, inputKVPMap, true);
                String json = new String(response.getDatas().get(0));
                JsonNode jsonNode = JSONUtil.getJsonNode(json);

                // Extract the content of "domainSet" JSON node and return to client
                String domainSetJsonStr = jsonNode.get("domainSet").toPrettyString();
                response.setDatas(Arrays.asList(domainSetJsonStr.getBytes()));

                this.writeResponseResult(response);
            } catch (Exception ex) {
                String errorMessage = "Error returning coverage's domainset. Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(inputKVPMap, requestHandlerInterface);
    }
    
    /**
     * Return a coverage's range type object in CIS 1.1 JSON format
     * 
     * e.g: https://oapi.rasdaman.org/rasdaman/oapi/collections/S2_FALSE_COLOR_84/coverage/rangetype
     *
     */
    @RequestMapping(path = COVERAGE_RANGE_TYPE_CONTEXT_PATH)
    public void getRangeType(@PathVariable String coverageId, HttpServletRequest httpServletRequest) throws Exception {
        
        this.setBaseURL(httpServletRequest);

        RequestHandlerInterface requestHandlerInterface = () -> {

            try {
                Object rangeType = oapiHandlersService.getJSONCoreCIS11Result(coverageId).getRangeType();
                Response response = this.oapiResultService.getJsonResponse(rangeType);
                this.writeResponseResult(response);
            } catch (Exception ex) {
                String errorMessage = "Error returning coverage's rangetype. Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(new HashMap<>(), requestHandlerInterface);
    }

    /**
     * Return only the coverage's range set object in CIS 1.1 JSON
     * 
     * e.g: https://oapi.rasdaman.org/rasdaman/oapi/collections/mean_summer_airtemp/coverage/rangeset     
     */
    @RequestMapping(path = COVERAGE_RANGE_SET_CONTEXT_PATH, method = RequestMethod.GET)
    public void getRangeSet(@PathVariable String coverageId,
                            HttpServletRequest httpServletRequest) throws Exception {
        
        this.setBaseURL(httpServletRequest);
        
        Map<String, String[]> inputKVPMap = buildGetRequestKvpParametersMap(httpServletRequest.getQueryString());

        RequestHandlerInterface requestHandlerInterface = () -> {

            // bbox and datetime are required parameters for subsetting at https://docs.ogc.org/DRAFTS/19-087.html#bbox-parameter-domainset-subset-requirements
            String bbox = getValueByKeyAllowNull(inputKVPMap, KEY_OAPI_BBOX);
            String datetime = getValueByKeyAllowNull(inputKVPMap, KEY_OAPI_DATETIME);
            String bboxCRS = getValueByKeyAllowNull(inputKVPMap, KEY_OAPI_BBOX_CRS);

            String[] inputSubsets = getValuesByKeyAllowNull(inputKVPMap, KEY_OAPI_SUBSET);
            String[] parsedSubsets = this.opaiParsingService.parseGetCoverageSubsets(coverageId, bbox, bboxCRS, datetime, inputSubsets);
            try {
                Object rangeSet = oapiHandlersService.getCoverageRangeSetResult(coverageId, parsedSubsets, inputKVPMap);
                Response response = this.oapiResultService.getJsonResponse(rangeSet);
                this.writeResponseResult(response);
            } catch (Exception ex) {
                String errorMessage = "Error returning coverage's rangeset. Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(inputKVPMap, requestHandlerInterface);
    }


    /**
     * Return only the coverage's metadata object in CIS 1.1 JSON
     * 
     * e.g: https://oapi.rasdaman.org/rasdaman/oapi/collections/S2_FALSE_COLOR_84/coverage/metadata
     */
    @RequestMapping(path = COVERAGE_METADATA_CONTEXT_PATH)
    public void getMetadata(@PathVariable String coverageId, 
                            HttpServletRequest httpServletRequest) throws Exception {
        
        this.setBaseURL(httpServletRequest);

        RequestHandlerInterface requestHandlerInterface = () -> {

            try {
                Object metadata = oapiHandlersService.getJSONCoreCIS11Result(coverageId).getMetadata();
                if (metadata != null) {
                    Response response = this.oapiResultService.getJsonResponse(metadata);
                    this.writeResponseResult(response);
                } else {
                    Response response = this.oapiResultService.getJsonResponse(new Metadata(null));
                    this.writeResponseResult(response);
                }
            } catch (Exception ex) {
                String errorMessage = "Error returning coverage's metadata. Reason: " + ex.getMessage();
                Response response = this.oapiResultService.getErrorResponse(ex, errorMessage);
                this.writeResponseResult(response);
            }

        };

        super.handleRequest(new HashMap<>(), requestHandlerInterface);
    }

    /**
     * Set the BaseURL (e.g: http://localhost:8080/rasdaman/oapi) to have the endpoint in the results
     */
    private void setBaseURL(HttpServletRequest httpServletRequest) {
        if (StringUtils.isEmpty(BASE_URL)) {
            // petascope endpoint proxy is configured in petascope.properies
            if (StringUtils.isNotEmpty(PETASCOPE_ENDPOINT_URL)) {
                BASE_URL = PETASCOPE_ENDPOINT_URL.replace(CONTEXT_PATH + "/" + OWS, CONTEXT_PATH + "/" + OAPI);
            } else {
                BASE_URL = httpServletRequest.getRequestURL().toString().split("/" + OAPI)[0] + "/" + OAPI;
            }
        }
    }
    
    /**
     * If Accept header exists, then parse its values to return the supported MIME type from rasdaman
     **/
    private String getSupportedMIMETypeForContentNegotiation(String acceptHeaderValue) throws PetascopeException {
        // e.g: image/tiff;application=geotiff;q=1.0,image/png;q=0.5, */*; q=0.1
        // if image/tiff is not supported, then check image/png

        String[] values = acceptHeaderValue.split(",");
        for (String value : values) {
            // e.g: image/tiff;application=geotiff;q=1.0
            String[] subValues = value.split(";");
            for (String subValue : subValues) {
                if (subValue.contains("/")) {
                    // e.g: image/tiff
                    if (MIMEUtil.isSupported(subValue)) {
                        return subValue;
                    }
                }
            }
        }

        if (acceptHeaderValue.contains("*/*")) {
            // No content negotiation is selected, then later it is marked as the json CIS 1.1 output format
            return null;
        }
        
        throw new PetascopeException(ExceptionCode.NoApplicableCode, 
                                    "There is no supported MIME type from server for the content negotiation request of client.");
    }

    @Override
    protected void handleGet(HttpServletRequest httpServletRequest) throws Exception {
    }

    @Override
    protected void requestDispatcher(HttpServletRequest httpServletRequest, Map<String, String[]> kvpParameters) throws PetascopeException {
    }
}
