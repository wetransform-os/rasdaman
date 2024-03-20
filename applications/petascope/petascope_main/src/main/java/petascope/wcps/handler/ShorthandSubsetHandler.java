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

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.util.CrsUtil;
import petascope.wcps.metadata.model.Axis;
import petascope.wcps.metadata.service.AxisIteratorAliasRegistry;
import petascope.wcps.metadata.service.WMSSubsetDimensionsRegistry;
import petascope.wcps.result.WcpsResult;
import petascope.wcps.subset_axis.model.DimensionIntervalList;
import petascope.wcps.subset_axis.model.WcpsSubsetDimension;

import petascope.util.*;

import petascope.wcps.metadata.model.WcpsCoverageMetadata;
import petascope.wcps.subset_axis.model.*;


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
    @Autowired
    private AxisIteratorAliasRegistry axisIteratorAliasRegistry;

    public ShorthandSubsetHandler() {

    }

    public ShorthandSubsetHandler create(Handler coverageExpressionHandler, Handler dimensionIntervalListHandler) {
        ShorthandSubsetHandler result = new ShorthandSubsetHandler();
        result.setChildren(Arrays.asList(coverageExpressionHandler, dimensionIntervalListHandler));
        result.subsetExpressionHandler = subsetExpressionHandler;
        result.wmsSubsetDimensionsRegistry = wmsSubsetDimensionsRegistry;
        result.axisIteratorAliasRegistry = axisIteratorAliasRegistry;

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

        List<WcpsSubsetDimension> newIntervals = new ArrayList<>();
        for (WcpsSubsetDimension subsetDimension : dimensionIntervalList.getIntervals()) {
            String axisLabel = subsetDimension.getAxisName();

            WcpsCoverageMetadata metadata = coverageExpressionResult.getMetadata();
            if (metadata.hasAxisByName(axisLabel)) {

                Axis axis = coverageExpressionResult.getMetadata().getAxisByName(axisLabel);

                if (axis.isTimeAxis() && !CrsUtil.isGridCrs(subsetDimension.getCrs())) {

                    PetascopeDateTime currentAxisLowerBound = TimeUtil.calculateDateTimeStringSubset(axis.getLowerGeoBoundRepresentation());
                    PetascopeDateTime currentAxisUpperBound = TimeUtil.calculateDateTimeStringSubset(axis.getUpperGeoBoundRepresentation());

                    if (subsetDimension instanceof WcpsTrimSubsetDimension) {
                        WcpsTrimSubsetDimension trimSubsetDimension = (WcpsTrimSubsetDimension) subsetDimension;
                        Triple<Boolean, String, String> axisIteratorLabelAndLowerBoundTriple = extractAxisIteratorLabelAndDateTimeValueTriple(trimSubsetDimension.getLowerBound());
                        Triple<Boolean, String, String> axisIteratorLabelAndUpperBoundTriple = extractAxisIteratorLabelAndDateTimeValueTriple(trimSubsetDimension.getUpperBound());

                        String axisIteratorLabelLowerBound = axisIteratorLabelAndLowerBoundTriple.getMiddle();
                        String axisIteratorLabelUpperBound = axisIteratorLabelAndUpperBoundTriple.getMiddle();

                        String lowerBound = axisIteratorLabelAndLowerBoundTriple.getRight();
                        String upperBound = axisIteratorLabelAndUpperBoundTriple.getRight();

                        PetascopeDateTime calculatedLowerBound = null;

                        // NOTE: e.g. date("2023-01":"2013-01") will be understood as date("2023-01-01T00:00:000":"2023-01-31T23:59:59.999")
                        if (shouldTimeBoundEvaluated(lowerBound)) {
                            calculatedLowerBound = TimeUtil.calculateDateTimeStringSubset(lowerBound);

                            if (calculatedLowerBound.getDateTimeMin().compareTo(currentAxisLowerBound.getDateTimeMin()) <= 0
                               && calculatedLowerBound.getDateTimeMax().compareTo(currentAxisLowerBound.getDateTimeMin()) >= 0) {
                                // e.g. axis' geo extent lower bound is "2023-01-15" and input subset is "2023-01":"2023-01" -> lower bound is set to "2023-01-15"
                                calculatedLowerBound.setDateTimeMin(currentAxisLowerBound.getDateTimeMin());
                            }


                            String timeGranularityLowerBound = calculatedLowerBound.getDateTimeMin().toString();
                            trimSubsetDimension.setLowerBound(StringUtil.quoteIfNotAlready(timeGranularityLowerBound));
                        }

                        if (shouldTimeBoundEvaluated(upperBound)) {

                            PetascopeDateTime calculatedUpperBound = TimeUtil.calculateDateTimeStringSubset(upperBound);

                            if (calculatedUpperBound.getDateTimeMin().compareTo(currentAxisUpperBound.getDateTimeMin()) <= 0
                                    && calculatedUpperBound.getDateTimeMax().compareTo(currentAxisUpperBound.getDateTimeMin()) >= 0) {
                                // e.g. axis' geo extent upper bound is "2023-01-15" and input subset is "2023-01":"2023-01" -> upper bound is set to "2023-01-15"
                                calculatedUpperBound.setDateTimeMax(currentAxisUpperBound.getDateTimeMin());
                            }

                            if (calculatedLowerBound != null) {
                                if (calculatedUpperBound.getDateTimeMin().compareTo(calculatedLowerBound.getDateTimeMin()) < 0) {
                                    throw new PetascopeException(ExceptionCode.InvalidRequest,
                                            "Subset on time axis: " + axis.getLabel()
                                                    + " has upper bound: " + calculatedUpperBound.getDateTimeMinISOFormatStr()
                                                    + " which is smaller than lower bound: " + calculatedLowerBound.getDateTimeMinISOFormatStr());
                                }
                            }

                            String timeGranularityUpperBound = calculatedUpperBound.getDateTimeMax().toString();
                            trimSubsetDimension.setUpperBound(StringUtil.quoteIfNotAlready(timeGranularityUpperBound));
                        }


                        if (axisIteratorLabelLowerBound != null) {
                            boolean isCreatedFromCondenser = !axisIteratorLabelAndLowerBoundTriple.getLeft();
                            if (isCreatedFromCondenser) {
                                String axisIteratorLabelLowerBoundRasqlRepresentation = this.axisIteratorAliasRegistry.getAxisIterator(axisIteratorLabelLowerBound).getRasqlRepresentation();
                                // e.g. c[pt[0]]
                                // NOTE: here it doesn't need the fixed rasql grid upper bound, because the proper calculated grid domain in the subset expression
                                // is put to the grid domain of axis iterator instead
                                // e.g. coverage x
                                //      over $pt ansi("2023-01-01":"2023-01-01")
                                //      values $c[ansi($pt : $pt . "P2D" )]  is translated to MARRAY over pt in [0:2] using c[pt[0]]
                                trimSubsetDimension.setFixedRasqlGridDomain(axisIteratorLabelLowerBoundRasqlRepresentation);
                            }
                        }
                    } else {
                        // Slicing on date time interval
                        WcpsSliceSubsetDimension sliceSubsetDimension = (WcpsSliceSubsetDimension) subsetDimension;

                        // NOTE: in case VALUES c[ansi($pt)] it is a slicing by axis iterator, the ansi axis will not be stripped as normal slicing
                        Triple<Boolean, String, String> axisIteratorLabelAndDateTimeBoundTriple = extractAxisIteratorLabelAndDateTimeValueTriple(sliceSubsetDimension.getBound());
                        String axisIteratorLabel = axisIteratorLabelAndDateTimeBoundTriple.getMiddle();
                        String originalBound = axisIteratorLabelAndDateTimeBoundTriple.getRight();

                        if (shouldTimeBoundEvaluated(originalBound)) {
                            PetascopeDateTime calculatedBound = TimeUtil.calculateDateTimeStringSubset(originalBound);
                            subsetDimension = new WcpsSliceTemporalSubsetDimension(subsetDimension.getAxisName(),
                                                                                    subsetDimension.getCrs(),
                                                                                    originalBound,
                                                                                    calculatedBound,
                                                                                    axisIteratorLabel
                                                                                    );
                        }

                        if (axisIteratorLabel != null) {
                            // e.g. c[pt[0]]
                            // coverage x
                            // over $pt ansi("2023-01-01":"2023-01-01")
                            // values $c[ansi($pt. "P2D")] is translated as MARRAY pt in [2:2] VALUES c[pt[0]]
                            String axisIteratorLabelBoundRasqlRepresentation = this.axisIteratorAliasRegistry.getAxisIterator(axisIteratorLabel).getRasqlRepresentation();
                            subsetDimension.setFixedRasqlGridDomain(axisIteratorLabelBoundRasqlRepresentation);
                        }
                    }
                }

            }

            newIntervals.add(subsetDimension);
        }

        dimensionIntervalList = new DimensionIntervalList(newIntervals);

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

    /**
     * Check if a string represents a time bound be evaluated
     */
    private boolean shouldTimeBoundEvaluated(String str) {
        if (BigDecimalUtil.isNumber(str)) {
            return false;
        }

        return !StringUtil.isAsterisk(str) && !str.contains("[");
    }

    private boolean isSlicedByAxisIterator(String dateTimeBound) {
        return dateTimeBound.contains(TimeUtil.INTERNAL_DELIMITER_CHARACTER);
    }

    /**
     * e.g. true|$pt|"2015-01-01"
     * first parameter is if the axis iterator belongs to a coverage constructor
     * second parameter is the axis iterator label
     * third parameter is the date time string
     */
    public static Triple<Boolean, String, String> extractAxisIteratorLabelAndDateTimeValueTriple(String dateTimeBound) {
        Triple<Boolean, String, String> triple = new ImmutableTriple<>(null, null, dateTimeBound);
        if (dateTimeBound.contains(TimeUtil.INTERNAL_DELIMITER_CHARACTER)) {
            String[] tmps = dateTimeBound.split("\\" + TimeUtil.INTERNAL_DELIMITER_CHARACTER);
            return new ImmutableTriple<>(Boolean.valueOf(tmps[0]), tmps[1], tmps[2]);
        }

        return triple;
    }

}
