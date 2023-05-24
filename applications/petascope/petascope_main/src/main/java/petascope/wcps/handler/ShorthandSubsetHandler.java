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
 * Copyright 2003 - 2022 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.wcps.handler;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.WCPSException;
import petascope.wcps.result.WcpsResult;
import petascope.wcps.subset_axis.model.DimensionIntervalList;

/**
Handler for this expression:

//  coverageExpression LEFT_BRACKET dimensionIntervalList RIGHT_BRACKET
// e.g: c[Lat(0:20)] - Trim
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ShorthandSubsetHandler extends Handler {
    
    @Autowired
    private SubsetExpressionHandler subsetExpressionHandler;

    
    public ShorthandSubsetHandler() {
        
    }
    
    public ShorthandSubsetHandler create(Handler coverageExpressionHandler, Handler dimensionIntervalListHandler) {
        ShorthandSubsetHandler result = new ShorthandSubsetHandler();
        result.setChildren(Arrays.asList(coverageExpressionHandler, dimensionIntervalListHandler));
        result.subsetExpressionHandler = subsetExpressionHandler;
        
        return result;
    }
    
    @Override
    public WcpsResult handle(List<Object> serviceRegistries) throws PetascopeException {
        // e.g. encode((c + c)[i(0:30)] [i(0)], "csv")
        // to change first to (c[i(0:30) + c[i(0:30)]) before applying [i(0)]
        boolean isCurrentNodeRemoved = false;

        if (!this.isQueryTreeUpdated) {

            if (!this.getChildren().isEmpty()) {
                this.updateQueryTree(this.getFirstChild(), this.getSecondChild());

                if (this.isQueryTreeUpdated) {
                    // remove the current shortHandSubset node from the query tree
                    int currentNodeIndex = this.getChildIndexInParentsList();

                    Handler parentHandler = this.getParent();
                    Handler firstChildHandler = this.getFirstChild();

                    firstChildHandler.setParent(parentHandler);
                    parentHandler.getChildren().set(currentNodeIndex, firstChildHandler);

                    isCurrentNodeRemoved = true;
                }
            }

            this.isQueryTreeUpdated = true;
        }

        //  coverageExpression LEFT_BRACKET dimensionIntervalList RIGHT_BRACKET
        // e.g: c[Lat(0:20), Long(0:30)] - Trim
        WcpsResult coverageExpressionResult = (WcpsResult) this.getFirstChild().handle(serviceRegistries);
        DimensionIntervalList dimensionIntervalList = (DimensionIntervalList)this.getSecondChild().handle(serviceRegistries);


        if (!isCurrentNodeRemoved) {
            coverageExpressionResult = subsetExpressionHandler.handle(coverageExpressionResult, dimensionIntervalList);
        }

        return coverageExpressionResult;

    }

}
