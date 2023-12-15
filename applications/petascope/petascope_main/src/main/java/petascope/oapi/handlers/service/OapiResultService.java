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
package petascope.oapi.handlers.service;

import java.util.Arrays;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import petascope.core.response.Response;
import petascope.exceptions.PetascopeException;
import petascope.oapi.handlers.model.HttpErrorResponse;
import petascope.rasdaman.exceptions.RasdamanException;
import petascope.util.ExceptionUtil;
import petascope.util.JSONUtil;
import petascope.util.MIMEUtil;
import static petascope.util.MIMEUtil.MIME_JSON;
import static petascope.util.MIMEUtil.MIME_TEXT;

/**
 * Class to return the response entity for OAPI requests
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
public class OapiResultService {
    
    private static org.slf4j.Logger log = LoggerFactory.getLogger(OapiResultService.class);
    
    private final HttpHeaders httpHeaders = new HttpHeaders();
    
    /**
     * Return the result in JSON format
     */
    public Response getJsonResponse(Object object) throws PetascopeException {
        String objectRepresentation = JSONUtil.serializeObjectToJSONString(object);
        return new Response(Arrays.asList(objectRepresentation.getBytes()), MIME_JSON);
    }

    /**
     * The input is already a JSON string, it just adds the header to return as json
     */
    public Response getJsonResponseFromString(String json) {
        return new Response(Arrays.asList(json.getBytes()), MIME_JSON);
    }
    
    /**
     * Return the error result in JSON format
     */
    public Response getJsonErrorResponse(HttpStatus httpStatus, String errorMessage) throws PetascopeException {
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(httpStatus, errorMessage);
        String objectRepresentation = JSONUtil.serializeObjectToJSONString(httpErrorResponse);
        return new Response(Arrays.asList(objectRepresentation.getBytes()), MIME_JSON, HttpStatus.OK.value());
    }

    public Response getErrorResponse(Exception ex, String errorMessage) throws PetascopeException {
        int httpErrorCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        String exceptionText = "";
        if (ex instanceof PetascopeException) {
            httpErrorCode = ((PetascopeException)ex).getExceptionCode().getHttpErrorCode();
            exceptionText = ((PetascopeException)ex).getExceptionText();
        } else if (ex instanceof RasdamanException) {
            httpErrorCode = ((RasdamanException)ex).getExceptionCode().getHttpErrorCode();
            exceptionText = ((RasdamanException)ex).getExceptionText();
        }

        if (!exceptionText.isEmpty()) {
            errorMessage += " with detailed reason: " + exceptionText;
        }

        log.error(errorMessage, ex);
        Response response = new Response(Arrays.asList(errorMessage.getBytes()), MIME_TEXT, httpErrorCode);
        return response;
    }
    
}
