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
 * Copyright 2003 - 2017 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.wcps.handler;

import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.WCPSException;
import petascope.wcps.metadata.service.CoverageAliasRegistry;
import petascope.wcps.metadata.service.LetClauseAliasRegistry;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.result.WcpsResult;

/**
 Handler for the list of clauses in LET expression
e.g. LET $subset := [ansi("2017-06-02")],
         $cov := $c[ $subset ],
         $c := $cov.red;
 */
@Service
// Create a new instance of this bean for each request (so it will not use the old object with stored data)
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class LetClauseListHandler extends Handler {

    @Autowired
    private LetClauseAliasRegistry letClauseAliasRegistry;
    @Autowired
    private CoverageAliasRegistry coverageAliasRegistry;
    
    public LetClauseListHandler() {
        
    }
    
    public LetClauseListHandler create(List<Handler> childHandlers) {
        LetClauseListHandler result = new LetClauseListHandler();
        result.letClauseAliasRegistry = letClauseAliasRegistry;
        result.coverageAliasRegistry = coverageAliasRegistry;
        result.setChildren(childHandlers);
        return result;
    }
    
    
    public WcpsResult handle(List<Object> serviceRegistries) throws PetascopeException {
        WcpsResult result = null;
        for (Handler childHandler : this.getChildren()) {
            String variableName = ((StringScalarHandler)childHandler.getFirstChild()).getValue();
            this.validate(variableName);
            result = (WcpsResult) childHandler.handle(serviceRegistries);
        }
        
        return result;
    }


    private void validate(String variableName) {
        if (this.coverageAliasRegistry.getAliasByCoverageName(variableName) != null) {
            throw new WCPSException(ExceptionCode.InvalidRequest, "Variable '" + variableName + "' in LET clause must not exist in FOR clause");
        } else if (this.letClauseAliasRegistry.get(variableName) != null) {
            throw new WCPSException(ExceptionCode.InvalidRequest, "Variable '" + variableName + "' in LET clause is duplicate");
        }
    }
}
