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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.util.*;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.result.WcpsResult;
import petascope.wcps.subset_axis.model.*;

/** 
 * Class to handler WCPS:
 * 
 * coverageVariableName dimensionIntervalElement, e.g. $px X(0:20)
 * 
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AxisIteratorHandler extends Handler {
    
    public AxisIteratorHandler() {
        
    }
    
    public AxisIteratorHandler create(Handler axisIteratorNameHandler, 
                                    Handler axisNameHandler,
                                    Handler coverageExpressionLowerBoundHandler,
                                    Handler coverageExpressionUpperBoundHandler,
                                    Handler regularTimeStepHandler) {
        AxisIteratorHandler result = new AxisIteratorHandler();

        if (coverageExpressionUpperBoundHandler == null) {
            coverageExpressionUpperBoundHandler = coverageExpressionLowerBoundHandler;
        }

        result.setChildren(Arrays.asList(axisIteratorNameHandler, axisNameHandler, 
                                        coverageExpressionLowerBoundHandler, coverageExpressionUpperBoundHandler, regularTimeStepHandler));
        
        return result;
    }

    @Override
    public VisitorResult handle(List<Object> serviceRegistries) throws PetascopeException {
        // e.g. $px X( 0:20 )
        
        // e.g. $px
        String axisIteratorName = ((WcpsResult)this.getFirstChild().handle(serviceRegistries)).getRasql();
        // e.g. X
        String axisName = ((WcpsResult)this.getSecondChild().handle(serviceRegistries)).getRasql();

        VisitorResult thirdChildResult = this.getThirdChild().handle(serviceRegistries);
        VisitorResult fourthChildResult = this.getFourthChild().handle(serviceRegistries);

        String gridLowerBound = thirdChildResult.getResult();
        String gridUpperBound = fourthChildResult.getResult();

        PetascopeDateTime calculateTimeLowerBound = null;
        PetascopeDateTime calculateTimeUpperBound = null;

        boolean isTemporalAxis = false;
        if (!BigDecimalUtil.isNumber(gridLowerBound)) {
            // not a number, then try to see if the bounds are temporal values
            // e.g. "2015-01-01 P1D" -> try to evaluate it
            gridLowerBound = ShorthandSubsetHandler.extractAxisIteratorLabelAndDateTimeValueTriple(gridLowerBound).getRight();
            gridUpperBound = ShorthandSubsetHandler.extractAxisIteratorLabelAndDateTimeValueTriple(gridUpperBound).getRight();

            calculateTimeLowerBound = TimeUtil.calculateDateTimeStringSubset(gridLowerBound);
            calculateTimeUpperBound = TimeUtil.calculateDateTimeStringSubset(gridUpperBound);
            isTemporalAxis = true;
        }

        AxisIterator result = null;
        if (isTemporalAxis) {
            // temporal bounds -> special axis iterator with a list of datetimeIntervals
            result = this.getTemporalAxisIterator(serviceRegistries, axisIteratorName, axisName,
                                                    calculateTimeLowerBound.getDateTimeMinInItsGranularity(),
                                                    calculateTimeUpperBound.getDateTimeMaxInItsGranularity());
        } else {
            // normal grid bounds
            WcpsSubsetDimension subsetDimension = new WcpsTrimSubsetDimension(axisName, CrsUtil.GRID_CRS, gridLowerBound, gridUpperBound);
            result = new AxisIterator(axisIteratorName, axisName, subsetDimension, null);
        }

        return result;
    }

    /**
     * In case axis lowerBound:upperBound of axis iterator is datetime
     * -> return a temporal axis iterator with a list of petascopeDateTime intervals
     *
     */
    private AxisIterator getTemporalAxisIterator(List<Object> serviceRegistries,
                                               String axisIteratorName,
                                               String axisName,
                                               String calculatedDateTimeLowerBound,
                                               String calculatedDateTimeUpperBound) throws PetascopeException {

        // ----- Time handler

        PetascopeDateTime.Granularity gridLowerBoundGranularity = TimeUtil.getGranularity(calculatedDateTimeLowerBound);;
        PetascopeDateTime.Granularity gridUpperBoundGranularity = TimeUtil.getGranularity(calculatedDateTimeUpperBound);;

        // e.g. P1YT1H
        String timeStep = null;

        if (this.getFifthChild() == null) {
            if (!gridLowerBoundGranularity.equals(gridUpperBoundGranularity)) {
                throw new PetascopeException(ExceptionCode.InvalidRequest, "Granularities of temporal lower and upper bounds in axis iterator: " + axisIteratorName + " must match together. " +
                        "Given two granularities: " + gridLowerBoundGranularity + " and " + gridUpperBoundGranularity + " respectively.");
            }
        } else {
            VisitorResult fifthChildResult = this.getFifthChild().handle(serviceRegistries);
            // e.g. P1D or PT12H03M
            timeStep = fifthChildResult.getResult();
        }

        if (timeStep == null) {
            // timeStep is not defined, derived it from the granularity of lowerBound:upperBound
            timeStep = TimeUtil.getPeriodicityRepresentation(gridLowerBoundGranularity);
        }

        // e.g. "2015-01" (NOTE: here granularity is influenced by timeStep)
        PetascopeDateTime timeLowerBound = TimeUtil.calculateDateTimeStringSubset(calculatedDateTimeLowerBound);
        // e.g. "2015-12"
        PetascopeDateTime timeUpperBound = TimeUtil.calculateDateTimeStringSubset(calculatedDateTimeUpperBound);

        // Get list of datetimeIntervals (e.g. a timeInterval = "2015-01-01T00:00:000" up to "2015-03-31T23:59:59.999")
        List<PetascopeDateTime> dateTimeIntervalsList = TimeUtil.getListOfDateTimes(timeLowerBound, timeUpperBound, timeStep);

        List<WcpsSliceTemporalSubsetDimension> temporalSlicingCoefficientSubsets = new ArrayList<>();
        for (PetascopeDateTime petascopeDateTime : dateTimeIntervalsList) {
            String originalBound = petascopeDateTime.getDateTimeMin().toString();
            temporalSlicingCoefficientSubsets.add(new WcpsSliceTemporalSubsetDimension(axisName, CrsUtil.DEFAULT_DATETIME_CRS, originalBound, petascopeDateTime, axisIteratorName));
        }

        AxisIterator axisIterator = new AxisIterator(axisIteratorName, axisName, null, null, temporalSlicingCoefficientSubsets);
        return axisIterator;
    }
    
}
