/*
  *  This file is part of rasdaman community.
  * 
  *  Rasdaman community is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 3 of the License, or
  *  (at your option) any later version.
  * 
  *  Rasdaman community is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *  See the GNU  General Public License for more details.
  * 
  *  You should have received a copy of the GNU  General Public License
  *  along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
  * 
  *  Copyright 2003 - 2017 Peter Baumann / rasdaman GmbH.
  * 
  *  For more information please see <http://www.rasdaman.org>
  *  or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package org.rasdaman.secore.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.rasdaman.secore.req.ResolveRequest;
import org.rasdaman.secore.req.ResolveResponse;
import org.rasdaman.secore.Resolver;
import net.opengis.ows.v_1_0_0.ExceptionReport;
import net.opengis.ows.v_1_0_0.ExceptionType;
import static org.rasdaman.secore.Constants.EMPTY;
import static org.rasdaman.secore.Constants.QUERY_SEPARATOR;

import org.rasdaman.secore.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.rasdaman.secore.util.StringUtil;
import org.rasdaman.secore.db.DbManager;
import static org.rasdaman.secore.Constants.NEW_LINE;
import static org.rasdaman.secore.Constants.XML_DECL;
import static org.rasdaman.secore.util.HttpUtil.*;

import org.rasdaman.secore.util.ExceptionCode;
import org.rasdaman.secore.util.SecoreException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

/**
 * Controller for all client requests
 *
 * @author <a href="mailto:bphamhuu@jacobs-university.net">Bang Pham Huu</a>
 */
@Component
@Controller

// NOTE: Only enable this controller when controller.enabled=true in application.properties 
// (i.e. when running SECORE as a standalone web application, separated from petascope)
@ConditionalOnProperty(value="secore.controller.enabled", havingValue = "true")
public class SecoreController {

    private static final Logger log = LoggerFactory.getLogger(SecoreController.class);

    private static final String DEFAULT_CONTENT_TYPE_XML = "text/xml; charset=utf-8";
    private static final String CONTENT_TYPE_GML = "application/gml+xml";
    /**
     * cached the request and the result of the request
     */
    private static Map<String, String> cache = new HashMap<String, String>();
    
    /**
     * NOTE: Any rest, kvp requests which will not return .jsp files will be handled in this method
     * 
     */
    @RequestMapping("/**")
    public void handle(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SecoreException {
        this.handleRequest(req, resp);
    }
    
    public void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SecoreException {
        String uri = req.getRequestURL().toString();        
        String qs = req.getQueryString();
        if (qs != null && !qs.equals(EMPTY)) {
            uri += QUERY_SEPARATOR + qs;
        }
        log.debug("Received request: " + uri);
        long start = System.currentTimeMillis();
        
        try {
            // Check if last query modified the baseX (insert/update/delete definitions) then it have to clear cache both in BaseX and on Servlet
            if (DbManager.getNeedToClearCache()) {
                cache = new HashMap<>();
                DbManager.clearedCache();
                log.debug("Cleared cache on servlet.");
            }

            // NOTE: use the cache instead of querying collections if URI does exist in the cache
            String result;
            if (cache.containsKey(uri)) {
                log.debug("Query data from *cache* for URI request: {}", uri);
                result = cache.get(uri);
            } else {
                ResolveRequest request = new ResolveRequest(uri);
                ResolveResponse res = Resolver.resolve(request);
                result = res.getData();
                log.debug("Query data from *collections* for URI request: {}", uri);                
                cache.put(uri, result);
            }

            if (result.equals(XML_DECL + NEW_LINE)) {
                throw new SecoreException(ExceptionCode.NoSuchDefinition, "The requested resource was not found");
            }

            long end = System.currentTimeMillis();
            long totalTime = end - start;
            log.debug("Request processed in '" + String.valueOf(totalTime) + "' ms.");
            
            writeResult(req, resp, result);
        } catch (SecoreException ex) {
            log.error("Exception caught when resolving URI: " + uri + ". Reason: " + ex.getExceptionText(), ex);
            String result = exceptionToXml(ex);
            writeResult(req, resp, result);
        }
    }

    public void writeResult(HttpServletRequest req, HttpServletResponse resp, String result) throws ServletException, IOException, SecoreException {
        byte[] bytesArray = result.getBytes();

        String compressionHeaderValue = req.getHeader(ACCEPT_ENCODING_HEADER_NAME);
        if (HttpUtil.containGZIPHeaderValue(compressionHeaderValue)) {
            resp.addHeader(CONTENT_ENCODING_HEADER_NAME, COMPRESSION_GZIP_HEADER);
            bytesArray = HttpUtil.compressBytesArrayToGZIP(bytesArray);
        }

        OutputStream os = resp.getOutputStream();
        String requestAcceptHeaderValue = req.getHeader("Accept");
        if (requestAcceptHeaderValue != null && requestAcceptHeaderValue.contains(CONTENT_TYPE_GML)) {
            resp.setContentType(CONTENT_TYPE_GML);
        } else {
            resp.setContentType(DEFAULT_CONTENT_TYPE_XML);
        }
                
        IOUtils.write(bytesArray, os);
    }

    public static void clearCache() {
        cache.clear();
    }

    private String exceptionToXml(SecoreException ex) {
        ExceptionReport report = ex.getReport();
        StringBuilder ret = new StringBuilder(500);
        ret.append(XML_DECL);
        ret.append(NEW_LINE);
        ret.append("<ows:ExceptionReport version=\"2.0.0\""
                + " xsd:schemaLocation=\"http://www.opengis.net/ows/2.0 http://schemas.opengis.net/ows/2.0/owsExceptionReport.xsd\""
                + " xmlns:ows=\"http://www.opengis.net/ows/2.0\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n");
        for (ExceptionType exception : report.getException()) {
            ret.append("  <ows:Exception exceptionCode=\"").append(exception.getExceptionCode()).append("\"");
            if (exception.getLocator() != null) {
                ret.append(" locator=\"").append(exception.getLocator()).append("\"");
            }
            ret.append(">\n");
            ret.append("    <ows:ExceptionText>").append(StringUtil.ltos(exception.getExceptionText(), '\n')).append("</ows:ExceptionText>\n");
            ret.append("  </ows:Exception>\n");
        }
        ret.append("</ows:ExceptionReport>");

        return ret.toString();
    }
}
