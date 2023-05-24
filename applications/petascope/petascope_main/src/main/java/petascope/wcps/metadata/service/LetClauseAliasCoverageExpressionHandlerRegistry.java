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
package petascope.wcps.metadata.service;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.WCPSException;
import petascope.util.JSONUtil;
import petascope.wcps.handler.Handler;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.result.WcpsResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class has the purpose of keeping information about LET clause variables aliases inside 1 query.
 * e.g: LET $a = $c[Lat(30:50), Long(60:70)],
 *          $b = $c + 2
 * 
 * $a and $b are variables in LET clause.
 *
 * @author <a href="mailto:b.phamhuu@jacobs-university.de">Bang Pham Huu</a>
 */
@Service
// Create a new instance of this bean for each request (so it will not use the old object with stored data)
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class LetClauseAliasCoverageExpressionHandlerRegistry {

    // NOTE: a coverage variable can be alias for multiple coverage names
    private Map<String, Handler> variablesMap = new LinkedHashMap<>();

    public LetClauseAliasCoverageExpressionHandlerRegistry() {
        
    }
    
    /**
     * Add a new variable and its coverage expression handler
     * e.g: $a -> coverageExpressionHandler: [Lat(20:30), Long(40:60)]
     */
    public void add(String variableName, Handler coverageExpressionHandler) {
        this.variablesMap.put(variableName, coverageExpressionHandler);
    }

    public void remove(String variableName) {
        this.variablesMap.remove(variableName);
    }

    /**
     * Get the coverage expression handler by variable name
     */
    public Handler get(String variableName) {
        Handler tmp = this.variablesMap.get(variableName);
        return tmp;
    }

    public Map<String, Handler> getMap() {
        return this.variablesMap;
    }
}
