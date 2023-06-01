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
 * Copyright 2003 - 2018 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.wcps.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.core.Pair;
import petascope.exceptions.PetascopeException;
import petascope.util.ListUtil;
import petascope.util.StringUtil;
import static petascope.wcps.handler.ForClauseHandler.AS;
import petascope.wcps.metadata.service.CollectionAliasRegistry;
import petascope.wcps.metadata.service.CoverageAliasRegistry;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.result.WcpsResult;

/**
 * Translation node from wcps to rasql. Example:  <code>
 * for $c1 in cov1 for $c2 in cov 2 return encode($c1 + $c2, "csv")
 * </code> translates to  <code>
 * SELECT csv(c1 + c2) FROM cov1 as c1, cov2 as c2
 * </code>
 *
 * @author <a href="mailto:alex@flanche.net">Alex Dumitru</a>
 * @author <a href="mailto:vlad@flanche.net">Vlad Merticariu</a>
 * @author <a href="mailto:b.phamhuu@jacobs-university.de">Bang Pham Huu</a>
 */
@Service
// Create a new instance of this bean for each request (so it will not use the old object with stored data)
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RootHandler extends Handler {
    
    @Autowired
    private CoverageAliasRegistry coverageAliasRegistry;
    @Autowired
    private CollectionAliasRegistry collectionAliasRegistry;
    
    
    public RootHandler create(Handler forClauseListHandler, Handler letClauseListHandler, Handler whereClauseHandler, Handler returnClauseHandler) {
        RootHandler result = new RootHandler();
        result.setChildren(Arrays.asList(forClauseListHandler, letClauseListHandler, whereClauseHandler, returnClauseHandler));
        result.coverageAliasRegistry = this.coverageAliasRegistry;
        result.collectionAliasRegistry = this.collectionAliasRegistry;
        
        
        return result;
    }
    
    public VisitorResult handle(List<Object> serviceRegistries) throws PetascopeException {
        
        WcpsResult forClauseListVisitorResult = (WcpsResult) this.getFirstChild().handle(serviceRegistries);
        
        WcpsResult letClauseListVisitorResult = null;
        if (this.getSecondChild() instanceof LetClauseListHandler) {
            letClauseListVisitorResult = (WcpsResult) this.getSecondChild().handle(serviceRegistries);
        }
        
        WcpsResult whereClauseVisitorResult = null;
        if (this.getThirdChild()instanceof WhereClauseHandler) {
            whereClauseVisitorResult = (WcpsResult) this.getThirdChild().handle(serviceRegistries);
        }

        VisitorResult returnClauseVisitorResult = this.getFourthChild().handle(serviceRegistries);
        
        VisitorResult finalResult = returnClauseVisitorResult;
        
        if (returnClauseVisitorResult instanceof WcpsResult) {
            // rasql query
            finalResult = this.handle(forClauseListVisitorResult, letClauseListVisitorResult, 
                                      whereClauseVisitorResult, (WcpsResult) returnClauseVisitorResult);
        }
        
        return finalResult;
    }
    
    private WcpsResult handle(WcpsResult forClauseList, WcpsResult letClauseList, WcpsResult whereClause, WcpsResult returnClause) throws PetascopeException {
        // SELECT c1 + c2
        String selectClauseStr = returnClause.getRasql();
        String whereClauseStr = "";
        
        if (whereClause != null) {
            whereClauseStr = whereClause.getRasql();
        }
        
        String finalRasqlQuery = selectClauseStr + " " + this.coverageAliasRegistry.getRasqlFromClause() + " " + whereClauseStr;
        returnClause.setFinalRasqlQuery(finalRasqlQuery);
        
        return returnClause;
    }
}
