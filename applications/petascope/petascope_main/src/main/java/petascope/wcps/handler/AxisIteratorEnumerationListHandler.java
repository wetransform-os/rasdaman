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

import org.apache.commons.lang3.tuple.Triple;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.util.CrsUtil;
import petascope.util.PetascopeDateTime;
import petascope.util.TimeUtil;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.result.WcpsMetadataResult;
import petascope.wcps.result.WcpsResult;
import petascope.wcps.subset_axis.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class to handler WCPS:
 *
 * coverageVariableName axisName LEFT_PARENTHESIS coverageExpression (COMMA coverageExpression)* RIGHT_PARENTHESIS
 *
 * e.g. $pt date("2015-01-02", "2016-03-06", "2018-09-05")
 *
 * NOTE: only used for calendar feature for now
 *
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AxisIteratorEnumerationListHandler extends Handler {

    public AxisIteratorEnumerationListHandler() {

    }

    public AxisIteratorEnumerationListHandler create(Handler axisIteratorNameHandler,
                                                     Handler axisNameHandler,
                                                     List<Handler> coefficientHandlers) {
        AxisIteratorEnumerationListHandler result = new AxisIteratorEnumerationListHandler();
        List<Handler> handlers = new ArrayList<>(Arrays.asList(axisIteratorNameHandler, axisNameHandler));
        handlers.addAll(coefficientHandlers);
        result.setChildren(handlers);

        return result;
    }

    @Override
    public VisitorResult handle(List<Object> serviceRegistries) throws PetascopeException {
        // e.g. $px X( 0:20 )

        // e.g. $px
        String axisIteratorName = ((StringScalarHandler)this.getFirstChild()).getOriginalValue();
        // e.g. X
        String axisName = ((WcpsResult)this.getSecondChild().handle(serviceRegistries)).getRasql();

        List<VisitorResult> coefficientVisitorResults = new ArrayList<>();
        for (int i = 2; i < this.getChildren().size(); i++) {
            VisitorResult visitorResult = this.getChildren().get(i).handle(serviceRegistries);
            coefficientVisitorResults.add(visitorResult);
        }


        List<String> bounds = new ArrayList<>();
        // e.g. $pt ansi("2015-02-03", "2015-06-07", "2015-08-09")
        List<WcpsSliceTemporalSubsetDimension> temporalSlicingCoefficientSubsets = new ArrayList<>();

        boolean canCalculateDateTime = true;
        for (VisitorResult visitorResult : coefficientVisitorResults) {
            String bound = null;

            if (visitorResult instanceof WcpsResult) {
                bound = ((WcpsResult) visitorResult).getRasql();
            } else if (visitorResult instanceof WcpsMetadataResult) {
                bound = ((WcpsMetadataResult) visitorResult).getResult();
            } else if (visitorResult instanceof ListStringResult) {
                // e.g. OVER $pt t (months("2023")) which returns the list of {01,02,..12}
                ListStringResult results = (ListStringResult)visitorResult;
                bounds.addAll(results.getList());
            } else if (visitorResult instanceof BoundResult) {
                bound = ((BoundResult) visitorResult).getResult();
            }

            if (bound != null) {
                // e.g, in case $pt1 ansi("true|$pt|"2023-01 P1M") -> return only "2023-01 P1M"
                Triple<Boolean, String, String> triple = ShorthandSubsetHandler.extractAxisIteratorLabelAndDateTimeValueTriple(bound);
                bound = triple.getRight();

                bounds.add(bound);
            }
        }

        for (String bound : bounds) {
            PetascopeDateTime petascopeDateTime = null;

            // e.g. "2015-01/P1M" -> "2015-01"
            String dateTimeTmp = bound.split("/")[0];

            if (TimeUtil.isValidTimestamp(dateTimeTmp)) {
                petascopeDateTime = TimeUtil.calculateDateTimeStringSubset(bound);
            }

            temporalSlicingCoefficientSubsets.add(new WcpsSliceTemporalSubsetDimension(axisName,
                                                                                        CrsUtil.DEFAULT_DATETIME_CRS, bound, petascopeDateTime, axisIteratorName));
        }

        AxisIterator axisIterator = new AxisIterator(axisIteratorName, axisName, null, null, temporalSlicingCoefficientSubsets);
        return axisIterator;
    }

}
