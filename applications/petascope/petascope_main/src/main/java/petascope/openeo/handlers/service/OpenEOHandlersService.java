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

import org.rasdaman.config.ConfigManager;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.gdc.handlers.model.Conformance;
import petascope.gdc.handlers.model.LandingPage;
import petascope.openeo.handlers.model.EndpointsCapability;
import petascope.openeo.handlers.model.FileFormatsCapability;
import petascope.openeo.handlers.model.ProcessesCapability;
import petascope.openeo.handlers.model.WellknownCapability;

import java.io.IOException;

import static org.rasdaman.config.ConfigManager.OPENEO;
import static org.rasdaman.config.ConfigManager.OWS;

@Service
public class OpenEOHandlersService {

    private static org.slf4j.Logger log = LoggerFactory.getLogger(OpenEOHandlersService.class);

    public WellknownCapability getWellknownCapability() {
        String endpoint = getOpenEOEndpoint();
        WellknownCapability wellknown = new WellknownCapability(endpoint);
        return wellknown;
    }

    public LandingPage getGDCLandingPage(String urlPrefix) {
        LandingPage gdcLandingPage = new LandingPage(urlPrefix);
        return gdcLandingPage;
    }

    public Conformance getGDCConformanceResult() {
        Conformance conformance = new Conformance();
        return conformance;
    }

    public EndpointsCapability getEndpointsCapability() {
        EndpointsCapability endpointsCapability = new EndpointsCapability();
        return endpointsCapability;
    }

    public FileFormatsCapability getFileFormatsCapability() {
        FileFormatsCapability fileFormats = new FileFormatsCapability();
        return fileFormats;
    }

    public String getJSONProcessesCapability() throws IOException, PetascopeException {
        ProcessesCapability processesCapability = new ProcessesCapability();
        String jsonResult = processesCapability.getJSONResult();

        return jsonResult;
    }


    public static String getOpenEOEndpoint() {
        String firstPath = ConfigManager.PETASCOPE_ENDPOINT_URL.substring(0,
                ConfigManager.PETASCOPE_ENDPOINT_URL.lastIndexOf("/" + OWS));
        String result = firstPath + "/" + OPENEO;
        return result;
    }


}
