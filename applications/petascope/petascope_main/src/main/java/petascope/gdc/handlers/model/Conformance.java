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
import petascope.openeo.handlers.model.Endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Conformance {

    public static final List<String> conformsTo = Arrays.asList(
                "http://www.opengis.net/spec/ogcapi-coverages-1/1.0/req/geodata-coverage",
                "http://www.opengis.net/spec/ogcapi-coverages-1/1.0/req/coverage-subset",
                "http://www.opengis.net/spec/ogcapi-coverages-1/1.0/req/coverage-scaling",
                "http://www.opengis.net/spec/ogcapi-coverages-1/1.0/req/coverage-rangesubset",
                "http://www.opengis.net/spec/ogcapi-coverages-1/1.0/req/domainset-subset",

                // encoding conformance classes, NOTE: see https://docs.ogc.org/DRAFTS/19-087.html#_summary_of_conformance_uris
                "http://www.opengis.net/spec/ogcapi-coverages-1/1.0/conf/png",
                "http://www.opengis.net/spec/ogcapi-coverages-1/1.0/conf/jpegxl",
                "http://www.opengis.net/spec/ogcapi-coverages-1/1.0/conf/geotiff",
                "http://www.opengis.net/spec/ogcapi-coverages-1/1.0/conf/netcdf",
                "http://www.opengis.net/spec/ogcapi-coverages-1/1.0/conf/cisjson",
                "http://www.opengis.net/spec/ogcapi-coverages-1/1.0/conf/jpeg2000"
            );

    public Conformance() {

    }

    @JsonProperty("conformsTo")
    public List<String> getConformsTo() {
        return conformsTo;
    }



}
