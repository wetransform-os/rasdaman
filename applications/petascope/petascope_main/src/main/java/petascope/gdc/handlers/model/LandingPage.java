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
package petascope.gdc.handlers.model;

/**
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
import com.fasterxml.jackson.annotation.JsonProperty;
import petascope.oapi.handlers.model.Link;
import petascope.openeo.handlers.model.Endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LandingPage {

    private String gdcVersion = "1.0.0-beta";
    private String id = "rasdaman";
    private String title = "rasdaman GDC API";
    private String description = "rasdaman GDC API with support for OGC API - Coverages - Part 1, and openEO processes with synchronous execution";
    private static List<Link> links;
    private static List<String> conformsTo = Conformance.conformsTo;
    private static List<Endpoint> endpoints;

    public LandingPage(String urlPrefix) {

        if (links == null) {
            links = Arrays.asList(
                    new Link("service-desc", ""),
                    new Link("data", urlPrefix + "/collections")
            );
        }

        if (endpoints == null) {

            endpoints = new ArrayList<>();

            endpoints.add(new Endpoint("/", Arrays.asList("GET")));
            endpoints.add(new Endpoint("/conformance", Arrays.asList("GET")));
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


        }
    }

    @JsonProperty("gdc_version")
    public String getGdcVersion() {
        return gdcVersion;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<Link> getLinks() {
        return links;
    }

    @JsonProperty("endpoints")
    public List<Endpoint> getEndpoints() {
        return this.endpoints;
    }

    @JsonProperty("conformsTo")
    public List<String> getConformsTo() {
        return this.conformsTo;
    }
}
