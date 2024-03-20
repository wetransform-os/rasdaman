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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.rasdaman.config.ConfigManager;
import petascope.oapi.handlers.model.Link;
import petascope.util.MIMEUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static petascope.openeo.handlers.service.OpenEOHandlersService.getOpenEOEndpoint;

public class EndpointsCapability {

    private static final String API_VERSION = "1.2.0";
    private static final String BACKEND_VERSION = ConfigManager.PETASCOPE_VERSION;
    private static final String STAC_VERSION = "1.0.0";
    private static final String ID = "rasdaman";
    private static final String TITLE = "rasdaman openEO API";
    private static final String DESCRIPTION = "The rasdaman backend provides EO data available for processing";
    private static final boolean PRODUCTION = false;

    private static final List<Endpoint> endpoints = new ArrayList<>();
    private static final List<Link> links = new ArrayList<>();

    public static final String CAPABILITIES_WELLKNOWN_CONTEXT_PATH = "/.well-known/openeo";
    private static final String COLLECTIONS = "collections";
    public static final String CAPABILITIES_COLLECTIONS_CONTEXT_PATH = "/" + COLLECTIONS;

    public EndpointsCapability() {
        if (endpoints.isEmpty()) {

            // endpoints
            endpoints.add(new Endpoint("/", Arrays.asList("GET")));
            endpoints.add(new Endpoint("/credentials/basic", Arrays.asList("GET")));

            endpoints.add(new Endpoint("/.well-known/openeo", Arrays.asList("GET")));
            endpoints.add(new Endpoint("/file_formats", Arrays.asList("GET")));
            endpoints.add(new Endpoint("/collections", Arrays.asList("GET")));
            endpoints.add(new Endpoint("/collections/{coverage_id}", Arrays.asList("GET")));
            endpoints.add(new Endpoint("/processes", Arrays.asList("GET")));

            endpoints.add(new Endpoint("/result", Arrays.asList("POST")));

            endpoints.add(new Endpoint("/process_graphs/{process_graph_id}", Arrays.asList("PUT")));
            endpoints.add(new Endpoint("/process_graphs/{process_graph_id}", Arrays.asList("DELETE")));
            endpoints.add(new Endpoint("/process_graphs/{process_graph_id}", Arrays.asList("GET")));
            endpoints.add(new Endpoint("/process_graphs", Arrays.asList("GET")));

            // links
            links.add(new Link("self", getOpenEOEndpoint(), MIMEUtil.MIME_JSON, "URL to openEO API Service"));
            links.add(new Link("self", getOpenEOEndpoint() + CAPABILITIES_WELLKNOWN_CONTEXT_PATH, MIMEUtil.MIME_JSON, "Well-Known URL"));
            links.add(new Link("data", getOpenEOEndpoint() + CAPABILITIES_COLLECTIONS_CONTEXT_PATH, MIMEUtil.MIME_JSON, "Collections"));
        }
    }

    @JsonProperty("api_version")
    public String getApiVersion() {
        return API_VERSION;
    }

    @JsonProperty("backend_version")
    public String getBackendVersion() {
        return BACKEND_VERSION;
    }

    @JsonProperty("stac_version")
    public String getStacVersion() {
        return STAC_VERSION;
    }

    @JsonProperty("id")
    public String getId() {
        return ID;
    }

    @JsonProperty("title")
    public String getTitle() {
        return TITLE;
    }

    @JsonProperty("description")
    public String getDescription() {
        return DESCRIPTION;
    }

    @JsonProperty("production")
    public boolean getProduction() {
        return PRODUCTION;
    }

    @JsonProperty("endpoints")
    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    @JsonProperty("links")
    public List<Link> getLinks() {
        return links;
    }

}
