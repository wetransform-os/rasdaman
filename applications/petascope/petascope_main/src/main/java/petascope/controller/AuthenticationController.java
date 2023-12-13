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

import com.rasdaman.admin.model.AuthIsActiveResult;
import com.rasdaman.accesscontrol.service.AuthenticationService;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.rasdaman.rasnet.util.DigestUtils;
import javax.servlet.http.HttpServletRequest;
import org.rasdaman.config.ConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import static org.rasdaman.config.ConfigManager.CHECK_PETASCOPE_ENABLE_AUTHENTICATION_CONTEXT_PATH;
import org.rasdaman.rasnet.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.rasdaman.config.ConfigManager.*;

import petascope.controller.model.BasicCredentialsToken;
import petascope.core.Pair;
import petascope.core.response.Response;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.util.JSONUtil;
import petascope.util.ListUtil;
import petascope.util.MIMEUtil;
import petascope.util.ras.RasUtil;

/**
 * Endpoints to validate credentials from clients.
 * 
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@RestController
public class AuthenticationController extends AbstractController {
    
    public static final String LOGIN = "login";

    @Autowired
    private AuthenticationService authenticationService;

    @RequestMapping(value = { CREDENTIALS_BASIC, OPENEO_CREDENTIALS_BASIC, GDC_CREDENTIALS_BASIC })
    /**
     * 7. Return an access token for the valid credentials
     * see https://openeo.org/documentation/1.0/developers/api/reference.html#tag/Account-Management/operation/authenticate-basic
     */
    private void handleCredentialsBasic() throws Exception {
        Pair<String, String> credentialsPair = this.authenticationService.getBasicAuthUsernamePassword(this.injectedHttpServletRequest);
        if (credentialsPair == null) {
            throw new PetascopeException(ExceptionCode.InvalidRequest, "Missing required credentials in basic authentication format.");
        } else {
            String username = credentialsPair.fst;
            String password = credentialsPair.snd;
            RasUtil.checkValidUserCredentials(username, password);

            // this access token is generated new everytime
            String accessToken = this.authenticationService.generateBasicCredentialsAccessToken(username, password);

            BasicCredentialsToken basicCredentialsToken = new BasicCredentialsToken(accessToken);
            Response response = new Response(Arrays.asList(JSONUtil.serializeObjectToJSONString(basicCredentialsToken).getBytes()), MIMEUtil.MIME_JSON);
            this.writeResponseResult(response);
        }

    }


    // -- rasdaman enterprise begin
    
    public static final String READ_WRITE_RIGHTS = "RW";

    /**
     * Check if petascope has being enabled authentication in petascope.properties,
     * then WSClient shows a login form.
     */
    @RequestMapping(value = CHECK_PETASCOPE_ENABLE_AUTHENTICATION_CONTEXT_PATH)
    private void handleCheckEnableAuthentication() throws Exception {
        if (startException != null) {
            throw startException;
        }

        boolean basicAuthenticationHeaderEnabled = false;
        String rasdamanUser = "";

        if (ConfigManager.enableAuthentication()) {
            basicAuthenticationHeaderEnabled = true;
        }

        if (!ConfigManager.RASDAMAN_USER.trim().isEmpty()
                && !ConfigManager.RASDAMAN_PASS.trim().isEmpty()) {
            rasdamanUser = ConfigManager.RASDAMAN_USER;
        }

        AuthIsActiveResult result = new AuthIsActiveResult(basicAuthenticationHeaderEnabled, rasdamanUser);
        Response response = new Response(Arrays.asList(JSONUtil.serializeObjectToJSONString(result).getBytes()), MIMEUtil.MIME_JSON);
        this.writeResponseResult(response);
    }
    
    /**
     * Check the credentials provided by the user. If the credentials are valid, return the list of roles for the requesting user.
     */
    @RequestMapping(value = LOGIN)
    private void handleLogin(HttpServletRequest httpServletRequest) throws PetascopeException, IOException, Exception {
        Pair<String, String> resultPair = AuthenticationService.getBasicAuthUsernamePassword(httpServletRequest);
        
        if (resultPair == null) {
            throw new PetascopeException(ExceptionCode.InvalidRequest, "Missing Authorization basic header from login request.");
        }
        
        String username = resultPair.fst;
        String password = resultPair.snd;

        String result = "";
        
        RasUtil.checkValidUserCredentials(username, password);
        // NOTE: with rasdaman community, if user has permissions CASIRW permissions in rascontrol, then he is admin user
        // export RASLOGIN=rasadmin:d293a15562d3e70b6fdc5ee452eaed40 && rascontrol -q -e -x list user -rights

        Set<String> roleNames = this.parseRolesFromRascontrol(username);

        if (!roleNames.isEmpty()) {
            // Return the list of rolenames for this user
            // e.g: admin,info,readwrite,PRIV_TYPE_MGMT,PRIV_COLLECTION_MGMT,PRIV_TRIGGER_MGMT,PRIV_USER_MGMT,PRIV_OWS_ADMIN,...
            result = ListUtil.join(new ArrayList(roleNames), ",");
        }
        
        Response response = new Response(Arrays.asList(result.getBytes()), MIMEUtil.MIME_TEXT);
        this.writeResponseResult(response);
    }
    
    /**
     * Return the list of roles for the requesting user via rascontrol
     * @TODO: this can be done faster and better with protobuf/grpc
     */
    public static Set<String> parseRolesFromRascontrol(String username) throws PetascopeException {
        try {
            // export RASLOGIN=rasadmin:d293a15562d3e70b6fdc5ee452eaed40 && rascontrol -q -e -x list user -rights
            Runtime runtime = Runtime.getRuntime();
            
            Set<String> roleNames = new LinkedHashSet<>();
            
            String loginEnv = ConfigManager.RASDAMAN_ADMIN_USER + ":" + DigestUtils.MD5(ConfigManager.RASDAMAN_ADMIN_PASS);
            String[] envp = new String[] {"RASLOGIN=" + loginEnv};
            Process process = runtime.exec(ConfigManager.RASDAMAN_BIN_PATH + "/rascontrol -q -e -x list user -rights", envp);
            
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            Pattern p = Pattern.compile("\\[(.*?)\\]");
            
            while ((line = stdInput.readLine()) != null) {
                if (line.contains("[")) {
                    String[] tmps = line.trim().split(" ");
                    String usernameTmp = tmps[1].trim();
                    
                    if (usernameTmp.equals(username)) {
                        
                        Matcher m = p.matcher(line);
                        String rights = "";
                        while(m.find()) {
                            rights += m.group(1);
                        }
                        
                        if (rights.contains(".")) {
                            // Here user has a missing right, e.g [R.] so he is not admin
                            break;
                        } else if (rights.contains(READ_WRITE_RIGHTS)) {
                            // e.g rasadmin with rights [AISC] -[RW]
                            
                            // then, the user is admin and it has these mapping roles - NOTE: it is used *internaly* only for WSClient
                            roleNames = new LinkedHashSet<>(Arrays.asList(READ_WRITE_RIGHTS));
                        }
                        
                        break;
                    }
                }
            }

            return roleNames;
        } catch (IOException ex) {
            throw new PetascopeException(ExceptionCode.IOConnectionError, 
                    "Cannot get rights for user '" + username + "' via rascontrol with bash command. Reason: " + ex.getMessage(), ex);
        }
    }
    
    @Override
    protected void handleGet(HttpServletRequest httpServletRequest) throws Exception {
    }

    @Override
    protected void requestDispatcher(HttpServletRequest httpServletRequest, Map<String, String[]> kvpParameters) throws PetascopeException {
    }
    
}
