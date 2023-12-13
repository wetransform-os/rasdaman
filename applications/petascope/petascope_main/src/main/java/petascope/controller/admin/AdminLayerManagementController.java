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

import com.rasdaman.admin.layer.service.AdminActivateLayerService;
import com.rasdaman.admin.layer.service.AdminDeactivateLayerService;
import com.rasdaman.admin.layer.service.AdminLayerIsActiveService;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import static org.rasdaman.config.ConfigManager.ADMIN;
import static org.rasdaman.config.ConfigManager.LAYER;
import org.rasdaman.config.VersionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import petascope.controller.AbstractController;
import petascope.controller.AuthenticationController;
import petascope.controller.RequestHandlerInterface;
import petascope.core.KVPSymbols;
import petascope.core.response.Response;
import petascope.exceptions.PetascopeException;
import petascope.util.ExceptionUtil;

/**
 * Controller to manage WMS layers for admin (to check if a layer exists, to activate a non-existing WMS layer of a coverage
 * or deactive an existing WMS layer of a coverage)
 * 
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */

@RestController
public class AdminLayerManagementController extends AbstractController {
    
    private static final String LAYER_IS_ACTIVE_PATH = ADMIN + "/" + LAYER + "/isactive";
    private static final String LAYER_ACTIVATE_PATH = ADMIN + "/" + LAYER + "/activate";
    private static final String LAYER_DEACTIVATE_PATH = ADMIN + "/" + LAYER + "/deactivate";
    
    @Autowired
    private AdminLayerIsActiveService layerIsActiveService;
    @Autowired
    private AdminActivateLayerService activateLayerService;
    @Autowired
    private AdminDeactivateLayerService deactivateLayerService;
    
    // -- 1. Layer is active
    
    @RequestMapping(path = LAYER_IS_ACTIVE_PATH,  method = RequestMethod.GET)
    protected void handleLayerIsActiveGet(HttpServletRequest httpServletRequest) throws Exception {
        this.handleLayerIsActive(httpServletRequest);        
    }
    
    @RequestMapping(path = LAYER_IS_ACTIVE_PATH,  method = RequestMethod.POST)
    protected void handleLayerIsActivePost(HttpServletRequest httpServletRequest) throws Exception {
        this.handleLayerIsActive(httpServletRequest);
    }
     
    private void handleLayerIsActive(HttpServletRequest httpServletRequest) throws Exception {
        Map<String, String[]> kvpParameters = this.parseKvpParametersFromRequest(httpServletRequest);
        
        RequestHandlerInterface requestHandlerInterface = () -> {
            try {
                Response response = this.layerIsActiveService.handle(httpServletRequest, kvpParameters);
                this.writeResponseResult(response);
            } catch (Exception ex) {
                ExceptionUtil.handle(VersionManager.getLatestVersion(KVPSymbols.WMS_SERVICE), ex, this.injectedHttpServletResponse);
            }
        };
        
        super.handleRequest(kvpParameters, requestHandlerInterface);
    }
    
    // -- 2. Activate layer
    
    @RequestMapping(path = LAYER_ACTIVATE_PATH,  method = RequestMethod.GET)
    protected void handleLayerActivationGet(HttpServletRequest httpServletRequest) throws Exception {
        this.handleLayerActivation(httpServletRequest);
    }
    
    @RequestMapping(path = LAYER_ACTIVATE_PATH,  method = RequestMethod.POST)
    protected void handleLayerActivationPost(HttpServletRequest httpServletRequest) throws Exception {
        this.handleLayerActivation(httpServletRequest);
    }
    
    private void handleLayerActivation(HttpServletRequest httpServletRequest) throws Exception {
        Map<String, String[]> kvpParameters = this.parseKvpParametersFromRequest(httpServletRequest);
        
        RequestHandlerInterface requestHandlerInterface = () -> {
            try {
                this.validateWriteRequestByRoleOrAllowedIP(httpServletRequest, AuthenticationController.READ_WRITE_RIGHTS);
                this.activateLayerService.handle(httpServletRequest, kvpParameters);
            } catch (Exception ex) {
                ExceptionUtil.handle(VersionManager.getLatestVersion(KVPSymbols.WMS_SERVICE), ex, this.injectedHttpServletResponse);
            }
        };
        
        super.handleRequest(kvpParameters, requestHandlerInterface);
    }
    
    // -- 3. Deactivate layer
    
    @RequestMapping(path = LAYER_DEACTIVATE_PATH,  method = RequestMethod.GET)
    protected void handleLayerDeactivationGet(HttpServletRequest httpServletRequest) throws Exception {
        this.handleLayerDeactivation(httpServletRequest);
    }
    
    @RequestMapping(path = LAYER_DEACTIVATE_PATH,  method = RequestMethod.POST)
    protected void handleLayerDeactivationPost(HttpServletRequest httpServletRequest) throws Exception {
        this.handleLayerDeactivation(httpServletRequest);
    }
    
    private void handleLayerDeactivation(HttpServletRequest httpServletRequest) throws Exception {
        Map<String, String[]> kvpParameters = this.parseKvpParametersFromRequest(httpServletRequest);
        
        RequestHandlerInterface requestHandlerInterface = () -> {
            try {
                this.validateWriteRequestByRoleOrAllowedIP(httpServletRequest, AuthenticationController.READ_WRITE_RIGHTS);
                this.deactivateLayerService.handle(httpServletRequest, kvpParameters);
            } catch (Exception ex) {
                ExceptionUtil.handle(VersionManager.getLatestVersion(KVPSymbols.WMS_SERVICE), ex, this.injectedHttpServletResponse);
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
