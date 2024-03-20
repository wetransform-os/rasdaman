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

import com.rasdaman.admin.layer.style.service.AdminCreateOrUpdateStyleService;
import com.rasdaman.admin.layer.style.service.AdminDeleteStyleService;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import static org.rasdaman.config.ConfigManager.ADMIN;
import static org.rasdaman.config.ConfigManager.LAYER;
import static org.rasdaman.config.ConfigManager.STYLE;
import org.rasdaman.config.VersionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import petascope.controller.AbstractController;
import petascope.controller.AuthenticationController;
import petascope.controller.RequestHandlerInterface;
import petascope.core.KVPSymbols;
import petascope.exceptions.PetascopeException;
import petascope.util.ExceptionUtil;

/**
 * Controller to manage WMS styles for admin
 * 
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */

@RestController
public class AdminStyleManagementController extends AbstractController {
    
    private static final String ADD_STYLE_PATH = ADMIN + "/" + LAYER + "/" + STYLE + "/add";
    private static final String UPDATE_STYLE_PATH = ADMIN + "/" + LAYER + "/" + STYLE + "/update";
    private static final String REMOVE_STYLE_PATH = ADMIN + "/" + LAYER + "/" + STYLE + "/remove";
    
    @Autowired
    private AdminCreateOrUpdateStyleService createOrUpdateStyleService;
    @Autowired
    private AdminDeleteStyleService adminDeleteStyleService;
    
    // -- 1. Add a new style to an existing layer
    
    @RequestMapping(path = ADD_STYLE_PATH,  method = RequestMethod.GET)
    protected void handleAddStyleGet(HttpServletRequest httpServletRequest) throws Exception {
        this.handleAddStyle(httpServletRequest);
    }
    
    @RequestMapping(path = ADD_STYLE_PATH,  method = RequestMethod.POST)
    protected void handleAddStylePost(HttpServletRequest httpServletRequest) throws Exception {
        this.handleAddStyle(httpServletRequest);
    }
    
    private void handleAddStyle(HttpServletRequest httpServletRequest) throws Exception {
        Map<String, String[]> kvpParameters = this.parseKvpParametersFromRequest(httpServletRequest);
        
        RequestHandlerInterface requestHandlerInterface = () -> {
            try {
                this.validateWriteRequestByRoleOrAllowedIP(httpServletRequest, AuthenticationController.READ_WRITE_RIGHTS);

                this.createOrUpdateStyleService.handleAdd(httpServletRequest, kvpParameters);
            } catch (Exception ex) {
                ExceptionUtil.handle(VersionManager.getLatestVersion(KVPSymbols.WMS_SERVICE), ex, httpServletRequest, this.injectedHttpServletResponse);
            }
        };
        
        super.handleRequest(kvpParameters, requestHandlerInterface);
    }
    
    // -- 2. Update an existing style of an existing layer
    
    @RequestMapping(path = UPDATE_STYLE_PATH,  method = RequestMethod.GET)
    protected void handleUpdateStyleGet(HttpServletRequest httpServletRequest) throws Exception {
        this.handleUpdateStyle(httpServletRequest);
    }
    
    @RequestMapping(path = UPDATE_STYLE_PATH,  method = RequestMethod.POST)
    protected void handleUpdateStylePost(HttpServletRequest httpServletRequest) throws Exception {
        this.handleUpdateStyle(httpServletRequest);
    }
    
    private void handleUpdateStyle(HttpServletRequest httpServletRequest) throws Exception {
        Map<String, String[]> kvpParameters = this.parseKvpParametersFromRequest(httpServletRequest);
        
        RequestHandlerInterface requestHandlerInterface = () -> {
            try {
                this.validateWriteRequestByRoleOrAllowedIP(httpServletRequest, AuthenticationController.READ_WRITE_RIGHTS);

                this.createOrUpdateStyleService.handleUpdate(httpServletRequest, kvpParameters);
            } catch (Exception ex) {
                ExceptionUtil.handle(VersionManager.getLatestVersion(KVPSymbols.WMS_SERVICE), ex, httpServletRequest, this.injectedHttpServletResponse);
            }
        };
        
        super.handleRequest(kvpParameters, requestHandlerInterface);
    }
    
    // -- 3. Remove an existing style of an existing layer
    
    @RequestMapping(path = REMOVE_STYLE_PATH,  method = RequestMethod.GET)
    protected void handleRemoveStyleGet(HttpServletRequest httpServletRequest) throws Exception {
        this.handleRemoveStyle(httpServletRequest);
    }
    
    @RequestMapping(path = REMOVE_STYLE_PATH,  method = RequestMethod.POST)
    protected void handleRemoveStylePost(HttpServletRequest httpServletRequest) throws Exception {
        this.handleRemoveStyle(httpServletRequest);
    }
    
    private void handleRemoveStyle(HttpServletRequest httpServletRequest) throws Exception {
        Map<String, String[]> kvpParameters = this.parseKvpParametersFromRequest(httpServletRequest);
        
        RequestHandlerInterface requestHandlerInterface = () -> {
            try {
                this.validateWriteRequestByRoleOrAllowedIP(httpServletRequest, AuthenticationController.READ_WRITE_RIGHTS);

                this.adminDeleteStyleService.handle(httpServletRequest, kvpParameters);
            } catch (Exception ex) {
                ExceptionUtil.handle(VersionManager.getLatestVersion(KVPSymbols.WMS_SERVICE), ex, httpServletRequest, this.injectedHttpServletResponse);
            }
        };
        
        super.handleRequest(kvpParameters, requestHandlerInterface);
    }

    @Override
    protected void handleGet(HttpServletRequest httpServletRequest) throws Exception {
    }

    @Override
    protected void requestDispatcher(HttpServletRequest httpServletRequest, Map<String, String[]> kvpParameters) throws PetascopeException {
    }
    
}
