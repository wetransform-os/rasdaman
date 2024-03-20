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
package com.rasdaman.accesscontrol.service;

import java.io.IOException;
import java.util.*;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.rasdaman.CorsFilter;
import org.rasdaman.config.ConfigManager;
import static org.rasdaman.config.ConfigManager.ADMIN;
import static org.rasdaman.config.ConfigManager.AUTHENTICATION_TYPE_BASIC_HEADER;
import static org.rasdaman.config.ConfigManager.OWS;
import static org.rasdaman.config.ConfigManager.RASQL;
import org.rasdaman.config.VersionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.rasdaman.config.ConfigManager.*;
import static org.rasdaman.config.ConfigManager.PETASCOPE_ENDPOINT_URL;
import static petascope.core.KVPSymbols.WCS_SERVICE;

import petascope.controller.OapiController;
import petascope.controller.OpenEOController;

import petascope.core.Pair;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.util.ExceptionUtil;
import petascope.util.ras.RasUtil;
import static org.rasdaman.config.ConfigManager.CHECK_PETASCOPE_ENABLE_AUTHENTICATION;
import static org.rasdaman.config.ConfigManager.SECORE;
import petascope.controller.PetascopeController;

/**
 * If Shibboleth authentication is configured, any unauthenticated requests will
 * need to redirect to Shibboleth auth endpoint to login from IdP's credentials.
 *
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Component
public class RequestsFilter implements Filter {
    
    // Endpoint to check authentication and return the roles of an user to clients
    public static final String LOGIN = "login";

    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private PetascopeController petascopeController;
    @Autowired
    private AuthenticationService authenticationService;

    public static String UNAUTHENTICATED_ERROR_MESSAGE = "Missing basic authentication header with username:password encoded in Base64 string";

    // NOTE: These requests should bypass authentication in Petascope (i.e: no need to authenticate in any case)
    // Because they are sent internally by petascope / rasfed not by users
    private static final List<String> NO_NEED_TO_AUTHENTICATE_REQUESTS = new ArrayList<>(Arrays.asList(
        // Rasql Servlet as it always needs query and password parameters
        RASQL,
            
        SECORE,    

        // non-standard for checking authentication for petascope
        CHECK_PETASCOPE_ENABLE_AUTHENTICATION, LOGIN
    ));

    /**
     * Check if a request should need authentication or not
     */
    private boolean requireAuthenticationRequest(String requestMethod, String requestURI, String queryString) {
        
        // Static assets (.html, .js, .css) are not checked
        if (requestURI.matches(".*/.*\\..*")) {
            return false;
        }

        if (requestURI.contains("admin/version")) {
            return false;
        }
        
        // Request to return WSClient
        if (requestMethod.equals("GET")) {
            if (requestURI.endsWith("/" + OWS) && queryString == null) {
                return false;
            } else if (requestURI.endsWith("rasdaman/") && queryString == null) {
                // Return the HTML pages configured in petascope.properties (e.g: BigDataCube demo)
                return false;
            }
        }
        
        // endpoint to show admin page embedded in petascope (the page contains web rascontrol, access controll management and statistic)
        if (this.httpServletRequest.getMethod().equals("GET") && requestURI.endsWith("/" + ADMIN)) {
            return false;
        }

        for (String request : NO_NEED_TO_AUTHENTICATE_REQUESTS) {
            // special requests are not checked
            if (requestURI.contains("/" + request)) {
                return false;
            }
        }

//       -- OAPI

        for (String request : OapiController.NO_NEED_AUTHENTICATION_ENDPOINTS) {
            // special requests are not checked
            if (requestURI.endsWith("/" + request) || requestURI.endsWith("/" + request + "/")) {
                return false;
            }
        }        

//      -- openEO

        for (String request : OpenEOController.NO_NEED_AUTHENTICATION_ENDPOINTS) {
            // special requests are not checked
            if (requestURI.endsWith("/" + request) || requestURI.endsWith("/" + request + "/")) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if credentials are valid
     * NOTE: it allows null credentialsPair for further processing
     */
    private void checkValidCredentialsAllowNull(Pair<String, String> credentialsPair) throws PetascopeException {
        if (credentialsPair != null) {
            RasUtil.checkValidUserCredentials(credentialsPair.fst, credentialsPair.snd);
        }
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        // NOTE: no url for petascope is defined in petascope.properties, only now can have the HTTP request object to set this value
         if (StringUtils.isEmpty(PETASCOPE_ENDPOINT_URL)) {
            // use the requesting URL to Petascope (not always: http://localhost:8080/rasdaman/ows)

            // e.g. /openeo/.well-known/openeo
            String servletPath = httpServletRequest.getServletPath();
            String requestURL = httpServletRequest.getRequestURL().toString();

            // e.g. http://localhost:8080/rasdaman/ows
            String contextPathURL = requestURL.split(servletPath)[0] + "/" + OWS;

            ConfigManager.setPetascopeEndpointUrl(contextPathURL);
            String protocol = httpServletRequest.getHeader("X-Forwarded-Proto");

            if (protocol != null) {
                // e.g. in case using https in apache2 proxy for http on local tomcat
                String[] tmps = PETASCOPE_ENDPOINT_URL.split("://");
                ConfigManager.setPetascopeEndpointUrl(protocol + "://" + tmps[1]);
            }
        }

        if (INSPIRE_COMMON_URL.isEmpty()) {
            INSPIRE_COMMON_URL = PETASCOPE_ENDPOINT_URL;
        }
        
        // NOTE: must enable 'Access-Control-Allow-Origin': * in HTTP response
        // In case basic header is enabled to avoid CORS problem in web browser
        CorsFilter.setResponseHeader(httpServletRequest, httpServletResponse);
        
        // NOTE: to avoid CORS error from web browser for preflight request (OPTIONS request), these requests don't contain basic header
        // (in case petascope's basic header is enabled)
        if ("OPTIONS".equals(httpServletRequest.getMethod())) {
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        } 
        
        String unauthenticationErrorMessage = "";
        
        if (ConfigManager.enableAuthentication()) {
            
            // Based on the first authentication type to redirect to Shibboleth IdP 
            // or throw exception with basic authentcation header (requests without username and password)
            String authenticationType = ConfigManager.AUTHENTICATION_TYPE;

            // Special requests will not need to check
            if (requireAuthenticationRequest(httpServletRequest.getMethod(), httpServletRequest.getRequestURI(), httpServletRequest.getQueryString())) {
                Pair<String, String> basicAuthCredentialsPair = null;
                try {
                    basicAuthCredentialsPair = AuthenticationService.getBasicAuthUsernamePassword(httpServletRequest);
                    this.checkValidCredentialsAllowNull(basicAuthCredentialsPair);
                } catch (PetascopeException ex) {
                    ExceptionUtil.handle(VersionManager.getLatestVersion(WCS_SERVICE), ex, httpServletRequest, httpServletResponse);
                    return;
                }

                if (authenticationType.equals(AUTHENTICATION_TYPE_BASIC_HEADER)) {
                    if (basicAuthCredentialsPair == null) {
                        if (ConfigManager.RASDAMAN_USER.isEmpty()) {
                            // If rasguest is not set as rasdaman_user and basic header is enabled, then petascope throws error for unauthenticated requests
                            try {
                                String requestRepresentation = this.petascopeController.getRequestPresentationWithEncodedAmpersands(httpServletRequest);
                                unauthenticationErrorMessage = UNAUTHENTICATED_ERROR_MESSAGE + " from request '" + requestRepresentation + "'";
                            } catch (Exception ex) {
                                ExceptionUtil.handle(VersionManager.getLatestVersion(WCS_SERVICE), ex, httpServletRequest, httpServletResponse);
                            }                            
                        } else {
                            // If rasguest is set as rasdaman_user and basic header is enabled, then petascope uses rasguest's credentials for unauthenticated requests
                            MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpServletRequest);
                            String credentialsInBase64 = AuthenticationService.createBasicHeaderCredentialsInBase64String(ConfigManager.RASDAMAN_USER, ConfigManager.RASDAMAN_PASS);
                            mutableRequest.putHeader(AuthenticationService.BASIC_AUTHENTICATION_HEADER, credentialsInBase64);
                        }
                    }
                }
            }
        } else {
            // Requests to SECORE should still be ok without these missing rasguest configurations
            if (!httpServletRequest.getRequestURI().contains(ConfigManager.SECORE)) {
                Pair<String, String> basicAuthCredentialsPair = null;
                try {
                    basicAuthCredentialsPair = AuthenticationService.getBasicAuthUsernamePassword(httpServletRequest);
                    this.checkValidCredentialsAllowNull(basicAuthCredentialsPair);
                } catch (PetascopeException ex) {
                    ExceptionUtil.handle(VersionManager.getLatestVersion(WCS_SERVICE), ex, httpServletRequest, httpServletResponse);
                }
            }
        }

        if (unauthenticationErrorMessage.isEmpty()) {
            // requests has no problem with authentication
            filterChain.doFilter(request, response);
        } else {
            // request is not authenticated
            PetascopeException petascopeException = new PetascopeException(ExceptionCode.Unauthorized, unauthenticationErrorMessage);
            ExceptionUtil.handle(VersionManager.getLatestVersion(WCS_SERVICE), petascopeException, httpServletRequest, httpServletResponse);
        }
    }

    @Override
    public void init(FilterConfig fc) throws ServletException {
    }

    @Override
    public void destroy() {
    }
    
    // This is needed for adding custom header (e.g Authorization for basic header) to HttpServletRequest object
    final public class MutableHttpServletRequest extends HttpServletRequestWrapper {

        private final Map<String, String> customHeaders;

        public MutableHttpServletRequest(HttpServletRequest request){
            super(request);
            this.customHeaders = new HashMap<String, String>();
        }

        public void putHeader(String name, String value){
            this.customHeaders.put(name, value);
        }

        public String getHeader(String name) {
            String headerValue = customHeaders.get(name);

            if (headerValue != null){
                return headerValue;
            }
            return ((HttpServletRequest) getRequest()).getHeader(name);
        }

        public Enumeration<String> getHeaderNames() {
            Set<String> set = new HashSet<String>(customHeaders.keySet());

            @SuppressWarnings("unchecked")
            Enumeration<String> e = ((HttpServletRequest) getRequest()).getHeaderNames();
            while (e.hasMoreElements()) {
                String n = e.nextElement();
                set.add(n);
            }
            return Collections.enumeration(set);
        }
    }
}

