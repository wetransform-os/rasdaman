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

import java.util.*;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.util.CrsUtil;
import petascope.util.MIMEUtil;
import petascope.util.ras.RasConstants;
import petascope.wcps.exception.processing.CoverageNotEncodedInReturnClauseException;
import petascope.wcps.metadata.model.WcpsCoverageMetadata;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.result.WcpsResult;

import static petascope.util.MIMEUtil.ENCODE_DEM;
import static petascope.util.ras.RasConstants.RASQL_ENCODE;

/**
 * Translation node from wcps to rasql for the return clause. Example:  <code>
 * return $c1 + $c2
 * </code> translates to  <code>
 * SELECT c1 + c2
 * </code>
 *
 * @author <a href="mailto:alex@flanche.net">Alex Dumitru</a>
 * @author <a href="mailto:vlad@flanche.net">Vlad Merticariu</a>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ReturnClauseHandler extends Handler {
    
    public ReturnClauseHandler() {
        
    }
    
    public ReturnClauseHandler create(List<Handler> childHandlers) {
        ReturnClauseHandler result = new ReturnClauseHandler();
        result.setChildren(childHandlers);
        return result;
    }
    
    public VisitorResult handle(List<Object> serviceRegistries) throws PetascopeException {

        // Try to optimize the query tree with subsets pushed down first
        Queue<Handler> queue = new ArrayDeque<>(this.getNonNullChildren());

        while (!queue.isEmpty()) {
            Handler childHandler = queue.remove();
            if (childHandler instanceof ShorthandSubsetHandler) {
                this.pushDownDimensionSubsetsIfPossible(childHandler);
            }

            queue.addAll(childHandler.getNonNullChildren());
        }

        VisitorResult temp = this.getFirstChild().handle(serviceRegistries);
        
        VisitorResult result = temp;
        if (temp instanceof WcpsResult) {
            result = this.handle((WcpsResult) temp);
        }
        
        return result;
    }

    /**
     * For example: $c[i(20:30)][j(40:50)] can be written as:
     * $c[i(20:30),j(40:50)]
     *
     * or ( ($c + $c)[i(20:30)] )[j(40:50)]
     * can be written as ( ($c + $c)[i(20:30), j(40:50)] )
     *
     * or ( $c[j(30:40)] + avg($c) )[j(40:50)]
     * will not be changed
     */
    private void pushDownDimensionSubsetsIfPossible(Handler inputShortHandSubsetHandler) throws PetascopeException {

        // e.g. ($c + $c) or $c[i(30:40)]
        Handler firstChildHandler = inputShortHandSubsetHandler.getFirstChild();
        Handler dimensionIntervalsHandler = inputShortHandSubsetHandler.getSecondChild();

        List<Handler> unPushedDownSubsetDimensionHandlers = new ArrayList<>();

        for (Handler subsetDimensionHandler : dimensionIntervalsHandler.getChildren()) {

            // Now try to put it down on other shorthand subset node's dimensionIntervalsList node
            Queue<Handler> queue = new ArrayDeque<>(firstChildHandler.getNonNullChildren());

            String inputAxisLabel = ((StringScalarHandler)subsetDimensionHandler.getFirstChild()).getValue();
            boolean pushedDownNode = false;

            while (!queue.isEmpty()) {
                Handler childHandler = queue.remove();

                if (this.stopToTraverseFurtherInTree(childHandler)) {
                    break;
                }

                Handler parentHandler = childHandler.getParent();

                if (childHandler instanceof DimensionIntervalListHandler
                   && parentHandler instanceof ShorthandSubsetHandler) {
                    // e.g. Current childHandler is j(0:30) and parentNode is shortHandSubsetHandler(c[j(0:30)])
                    List<Handler> subsetDimensionHandlersTmp = childHandler.getChildren();

                    boolean isAxisLabelExists = false;
                    for (Handler subsetDimensionHandlerTmp : subsetDimensionHandlersTmp) {
                        String axisLabel = ((StringScalarHandler)subsetDimensionHandlerTmp.getFirstChild()).getValue();
                        if (CrsUtil.axisLabelsMatch(inputAxisLabel, axisLabel)) {
                            isAxisLabelExists = true;
                            break;
                        }
                    }

                    if (!isAxisLabelExists) {
                        // ok, $c[i(20:30)] can push j(40:50) into it
                        childHandler.getChildren().add(subsetDimensionHandler);
                        pushedDownNode = true;
                    }
                }

                queue.addAll(childHandler.getNonNullChildren());

            }

            if (!pushedDownNode) {
                unPushedDownSubsetDimensionHandlers.add(subsetDimensionHandler);
            }

        }

        if (unPushedDownSubsetDimensionHandlers.size() == 0) {
            // All the subset dimensions can be pushed down -> remove the current shorthand subset node as it is not used anymore
            Handler inputParentNodeTmp = inputShortHandSubsetHandler.getParent();
            int indexOfShortHandSubsetHandlerNodeInParentChildrenList = inputShortHandSubsetHandler.getChildIndexInParentsList();
            inputParentNodeTmp.getChildren().set(indexOfShortHandSubsetHandlerNodeInParentChildrenList, inputShortHandSubsetHandler.getFirstChild());
        } else {
            // remove the pushed down subset dimension from the current dimensionIntervalsList node
            Handler dimensionalIntervalsListHandlerTmp = new DimensionIntervalListHandler();
            dimensionalIntervalsListHandlerTmp.setChildren(unPushedDownSubsetDimensionHandlers);

            inputShortHandSubsetHandler.getChildren().set(1, dimensionalIntervalsListHandlerTmp);

        }
    }


    private VisitorResult handle(WcpsResult processingExpr) {
        String template = TEMPLATE_RASQL.replace("$processingExpression", processingExpr.getRasql());
        WcpsCoverageMetadata metadata = processingExpr.getMetadata();
        // NOTE: If result in RETURN clause is scalar (E.g: return 2, return 2 + 3, return avg($c))
        // then WCPS coverage metadata object is null. If not, according to WCPS document, result should be encoded.
        if (metadata != null) {
            if (!metadata.getAxes().isEmpty()) {
                // Coverage result is not scalar (it has axis domain(s)) so it must start with encode()
                // NOTE: dem() is a special one in rasql and does not need encode().
                String tmp = processingExpr.getRasql().toLowerCase().trim();
                boolean needEncode = true;
                for (String function : KEYWORDS_FUNCTIONS) {
                    if (tmp.startsWith(function)) {
                        needEncode = false;
                    }
                }

                if (needEncode) {
                    throw new CoverageNotEncodedInReturnClauseException();
                }

            }
        }
        processingExpr.setMetadata(metadata);
        processingExpr.setRasql(template);
        return processingExpr;
    }

    private final Set<String> KEYWORDS_FUNCTIONS = Set.of(RASQL_ENCODE, ENCODE_DEM, PolygonizeHandler.OPERATOR);
    private final String TEMPLATE_RASQL = "SELECT $processingExpression ";
}
