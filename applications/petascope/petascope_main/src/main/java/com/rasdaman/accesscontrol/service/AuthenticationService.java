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
package com.rasdaman.accesscontrol.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.rasdaman.config.ConfigManager;
import petascope.controller.AuthenticationController;
import org.springframework.stereotype.Service;
import petascope.core.Pair;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.util.ras.RasUtil;

/**
 * Utility for authentication handlers
 * 
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */

@Service
public class AuthenticationService {

    public static final String BASIC_AUTHENTICATION_HEADER = "Authorization";
    public static final String BASIC_HEADER = "Basic";

    // openEO style https://openeo.org/documentation/1.0/developers/api/reference.html#tag/Account-Management/operation/authenticate-basic
    public static final String BEARER_BASIC_AUTHENTICATION_TOKEN = "Bearer basic//";

    // Stored the credentials of valid authenticated user
    // token -> (username, password)
    private static final Map<String, Pair<String, String>> basicCredentialsAccessTokensMap = new ConcurrentSkipListMap<>();
    
    /**
     * Parse header to get username and password encoded in Base64 for basic authentication header
     */
    public static Pair<String, String> getBasicAuthUsernamePassword(HttpServletRequest httpServletRequest) throws PetascopeException {
        
        final String authorization = httpServletRequest.getHeader(BASIC_AUTHENTICATION_HEADER);
        Pair<String, String> result = null;
        
        if (authorization != null && authorization.startsWith(BASIC_HEADER)) {
            // Example from curl (encoded username:password in Base64)
            // Authorization:Basic dXNlcm5hbWU6cGFzc3dvcmQ=
            String base64Credentials = authorization.substring(BASIC_HEADER.length()).trim();
            byte[] credDecoded = Base64.decodeBase64(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            if (!credentials.contains(":")) {
                throw new PetascopeException(ExceptionCode.InvalidRequest, "Basic authentication header credentials must be encoded in Base64 string with format username:password");
            }
            // decoded username:password
            final String[] values = credentials.split(":", 2);
            result = new Pair<>(values[0].trim(), values[1].trim());
        }

        if (authorization != null && result == null) {
            // Then, check if it may contain openEO basic authentication access headers
            // in this format: Authorization: Bearer basic//TOKEN
            if (authorization.startsWith(BEARER_BASIC_AUTHENTICATION_TOKEN)) {
                // uuid
                String accessToken = authorization.split(BEARER_BASIC_AUTHENTICATION_TOKEN)[1];

                // access token is valid -> returns the stored username / password in cache
                result = AuthenticationService.getStoredCredentialsByBasicHeaderAccessToken(accessToken);
            }
        }
        
        return result;
    }
    
    public static String createBasicHeaderCredentialsInBase64String(String username, String password) throws RuntimeException {
        Base64 base64 = new Base64();
        
        // e.g Basic dXNlcm5hbWU6cGFzc3dvcmQ=
        String tmp = username + ":" + password;
        return BASIC_HEADER + " " + new String(base64.encode(tmp.getBytes()));
    }
    
    /**
     * If credentials don't exist in the request, return pair of rasguest credentials instead
     */
    public static Pair<String, String> getBasicAuthCredentialsOrRasguest(HttpServletRequest httpServletRequest) throws PetascopeException {
        String username = ConfigManager.RASDAMAN_USER;
        String passwd = ConfigManager.RASDAMAN_PASS;
        Pair<String, String> pair = AuthenticationService.getBasicAuthUsernamePassword(httpServletRequest);
        if (pair != null) {
            username = pair.fst;
            passwd = pair.snd;
        }
        
        return new Pair<>(username, passwd);
    }
    
    /**
     * 
     * Set basic authentication header credentials to a URL to request and return input stream
     * 
    **/    
    public static InputStream getInputStreamWithBasicAuthCredentials(URL url, HttpServletRequest httpServletRequest) throws IOException, PetascopeException {
        URLConnection urlConnection;
        urlConnection = url.openConnection();
        
        Pair<String, String> credentialsPair = getBasicAuthUsernamePassword(httpServletRequest);
        
        if (credentialsPair != null) {
            String userpass = credentialsPair.fst + ":" + credentialsPair.snd;
            String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
            urlConnection.setRequestProperty("Authorization", basicAuth);
        }
        
        InputStream inputStream = urlConnection.getInputStream();
        return inputStream;
    }

    /**
     * Return rasdaman user credentials from a request.
     * NOTE: used only in deeper classes which are behind this requests filter.
     */
    public static Pair<String, String> getRasUserCredentials(HttpServletRequest httpServletRequest) throws PetascopeException {
        String username = ConfigManager.RASDAMAN_USER;
        String password = ConfigManager.RASDAMAN_PASS;
        
        Pair<String, String> basicAuthCredentialsPair = getBasicAuthUsernamePassword(httpServletRequest);
        if (basicAuthCredentialsPair != null) {
            username = basicAuthCredentialsPair.fst;
            password = basicAuthCredentialsPair.snd;
        }
        
        if (ConfigManager.enableAuthentication()) {

            // If request with basic authentication header then just use credentials from it
            
            if (basicAuthCredentialsPair != null) {
                // Basic authentication, credentials always from header
                username = basicAuthCredentialsPair.fst;
                password = basicAuthCredentialsPair.snd;
            }            
        }
        
        Pair<String, String> credentailsPair = new Pair<>(username, password);
        
        return credentailsPair;
    }
    
    // -------------- openEO API

    /**
     * Check if given basic header authentication token is valid, then return the stored credentials
     */
    public static Pair<String, String> getStoredCredentialsByBasicHeaderAccessToken(String token) throws PetascopeException {
        if (!basicCredentialsAccessTokensMap.containsKey(token)) {
            throw new PetascopeException(ExceptionCode.InvalidRequest, "Basic authentication access token does not exist in the registry. Hint: try to create a new access token via the dedicated endpoint.");
        };

        Pair<String, String> credentialsPair = basicCredentialsAccessTokensMap.get(token);
        return credentialsPair;
    }

    /**
     * Generate basic authentication access token from the valid credentials
     */
    public static String generateBasicCredentialsAccessToken(String username, String password) {
        // remove the old existing access token
        removeBasicCredentialsAccessToken(username);

        String accessToken = java.util.UUID.randomUUID().toString();
        basicCredentialsAccessTokensMap.put(accessToken, new Pair<String, String> (username, password));

        return accessToken;
    }

    /**
    * In case, user is removed or password's user is changed -> existing access token is removed
     */
    public static void removeBasicCredentialsAccessToken(String username) {
        String token = "";
        for (Map.Entry<String, Pair<String, String>> entry : basicCredentialsAccessTokensMap.entrySet()) {
            token = entry.getKey();
            String existingUsername = entry.getValue().fst;
            if (existingUsername.equals(username)) {
                break;
            }
        }

        if (!token.isEmpty()) {
            basicCredentialsAccessTokensMap.remove(username);
        }
    }

}
