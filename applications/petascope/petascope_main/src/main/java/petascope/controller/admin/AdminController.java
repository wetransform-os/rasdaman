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
 * Copyright 2003 - 2021 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.controller.admin;

import java.util.Arrays;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import static org.rasdaman.config.ConfigManager.ADMIN;

import org.rasdaman.config.ConfigManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import petascope.controller.AbstractController;
import petascope.core.response.Response;
import petascope.exceptions.PetascopeException;
import petascope.util.ras.RasUtil;

import static petascope.util.MIMEUtil.MIME_HTML;
import static petascope.util.MIMEUtil.MIME_TEXT;

/**
 * End point to receive requests for admin features in petascope
 * 
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@RestController
public class AdminController extends AbstractController {
    
    @Override
    @RequestMapping(path = ADMIN, method = RequestMethod.GET)
    protected void handleGet(HttpServletRequest httpServletRequest) throws Exception {
        this.returnAdminHomePage();
    }

    @Override
    protected void requestDispatcher(HttpServletRequest httpServletRequest, Map<String, String[]> kvpParameters) throws PetascopeException {
    }

    @RequestMapping(path = ADMIN + "/version")
    protected void handleGetVersion(HttpServletRequest httpServletRequest) throws Exception {
        // e.g. result is: Result element 1: rasdaman 10.2.12 on x86_64-linux-gnu, compiled by g++ (Ubuntu 11.4.0-1ubuntu1~22.04) 11.4.0
        String version = RasUtil.executeQueryToReturnString("SELECT version()",
                                                            ConfigManager.RASDAMAN_ADMIN_USER, ConfigManager.RASDAMAN_ADMIN_PASS);
        // e.g. 10.2.11
        version = version.trim().split("rasdaman ")[1].split(" ")[0];
        Response response = new Response(Arrays.asList(version.getBytes()), MIME_TEXT);
        this.writeResponseResult(response);
    }
    
    /**
     * Just return the admin home page.
     */
    private void returnAdminHomePage() throws Exception {
        byte[] bytes = IOUtils.toString(this.getClass().getResourceAsStream("/public/interface-admin-servlet.html")).getBytes();
        Response response = new Response(Arrays.asList(bytes), MIME_HTML);
        this.writeResponseResult(response);
    } 
  
}

