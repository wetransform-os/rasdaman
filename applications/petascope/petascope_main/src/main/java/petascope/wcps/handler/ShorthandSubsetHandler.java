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

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.WCPSException;
import petascope.util.CrsUtil;
import petascope.wcps.metadata.model.Axis;
import petascope.wcps.metadata.service.WMSSubsetDimensionsRegistry;
import petascope.wcps.result.WcpsResult;
import petascope.wcps.subset_axis.model.DimensionIntervalList;
import petascope.wcps.subset_axis.model.WcpsSubsetDimension;


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
    @Autowired
    private WMSSubsetDimensionsRegistry wmsSubsetDimensionsRegistry;

    
    public ShorthandSubsetHandler() {
        
    }
    
    public ShorthandSubsetHandler create(Handler coverageExpressionHandler, Handler dimensionIntervalListHandler) {
        ShorthandSubsetHandler result = new ShorthandSubsetHandler();
        result.setChildren(Arrays.asList(coverageExpressionHandler, dimensionIntervalListHandler));
        result.subsetExpressionHandler = subsetExpressionHandler;
        result.wmsSubsetDimensionsRegistry = wmsSubsetDimensionsRegistry;
        
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
            if (coverageExpressionResult.getMetadata() != null) {
                this.addWMSSubsetsIfPossible(coverageExpressionResult, dimensionIntervalList);
            }
            coverageExpressionResult = subsetExpressionHandler.handle(coverageExpressionResult, dimensionIntervalList);
        }

        return coverageExpressionResult;

    }


    /**
     * If this is the last subsetting node in the query tree branch and the query is generated for WMS style,
     * then it should check which parameters coming from WMS GetMap request should be added to the list of subsettings
     * in the DimensionIntervalList object
     */
    private void addWMSSubsetsIfPossible(WcpsResult coverageExpressionResult, DimensionIntervalList dimensionIntervalList) {
        boolean shouldAdd = false;

        String layerName = coverageExpressionResult.getMetadata().getCoverageName();
        List<WcpsSubsetDimension> wmsLayerSubsetDimensions = this.wmsSubsetDimensionsRegistry.getSubsetDimensions(layerName);
        if (wmsLayerSubsetDimensions == null) {
            // this node is not created for WMS style
            return;
        } else {
            // Check if this node is the last (parent of all subsetting nodes)
            Handler parentNode = this.getParent();
            while (parentNode != null) {
                if (parentNode instanceof ShorthandSubsetHandler) {
                    // this node: c[ansi("2015-08-09":"2018-08-09")]  is a child node of
                    // another Subset handler node (covExpr)[ansi("2015-08-09")]
                    // e.g. ( c[ansi("2015-08-09":"2018-08-09")] ) [ansi("2015-08-09")]
                    return;
                }

                parentNode = parentNode.getParent();
            }

            shouldAdd = true;
        }

        if (shouldAdd) {
            List<WcpsSubsetDimension> currentSubsetDimensions = dimensionIntervalList.getIntervals();
            List<WcpsSubsetDimension> newSubsetDimensions = new ArrayList<>(currentSubsetDimensions);

            for (WcpsSubsetDimension wmsSubsetDimension : wmsLayerSubsetDimensions) {
                boolean subsetExist = false;
                String wmsSubsetDimensionAxisLabel = wmsSubsetDimension.getAxisName();

                for (WcpsSubsetDimension currentSubsetDimension : currentSubsetDimensions) {
                    String currentSubsetDimensionAxisLabel = currentSubsetDimension.getAxisName();

                    if (CrsUtil.axisLabelsMatch(currentSubsetDimensionAxisLabel, wmsSubsetDimensionAxisLabel)) {
                        subsetExist = true;
                        break;
                    }
                }

                if (subsetExist == false) {

                    for (Axis axis : coverageExpressionResult.getMetadata().getAxes()) {
                        if (CrsUtil.axisLabelsMatch(wmsSubsetDimension.getAxisName(), axis.getLabel())) {
                            // Add the subsetting coming from WMS GetMap request parameters as well as it doesn't exist in the style subsetting fragment
                            // e.g. $c[ansi("2015-06-07")] and forecast=50 from GetMap request, then the result is
                            // $c[ansi("2015-06-07"), forecast(50)]
                            newSubsetDimensions.add(wmsSubsetDimension);
                            break;
                        }
                    }

                }
            }

            dimensionIntervalList.setIntervals(newSubsetDimensions);
        }

    }

}
