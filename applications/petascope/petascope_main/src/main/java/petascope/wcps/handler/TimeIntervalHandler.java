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

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.core.Pair;
import petascope.exceptions.PetascopeException;
import petascope.util.TimeUtil;
import petascope.wcps.metadata.model.Axis;
import petascope.wcps.metadata.model.IrregularAxis;
import petascope.wcps.metadata.model.WcpsCoverageMetadata;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.result.WcpsMetadataResult;
import petascope.wcps.result.WcpsResult;
import petascope.wcps.subset_axis.model.IntervalExpression;
import petascope.wcps.subset_axis.model.PetascopeDateTimeListResult;

import java.util.List;

/**
 * Handler for expression
 * years("2015-01-01":"2015-01-02")
 * or allyears(domain($c, ansi)))
 * which returns the time interval: "2015-01-01":"2015-01-02"
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class TimeIntervalHandler extends Handler {

    public TimeIntervalHandler() {

    }

    public TimeIntervalHandler create(List<Handler> handlers) {
        TimeIntervalHandler result = new TimeIntervalHandler();
        result.setChildren(handlers);

        return result;
    }

    @Override
    public VisitorResult handle(List<Object> serviceRegistries) throws PetascopeException {
        // Get the time lowerBound : upperBound and parse the lower bound / upper bound to get the datetime values
        // then pass it to parent handler (e.g. time extractor)

        String lowerBound = null;
        String upperBound = null;
        List<String> directPositions = null;

        Handler firstChildHandler = this.getFirstChild();
        if (firstChildHandler instanceof DomainExpressionHandler) {
            // e.g. domain($c, "ansi")
            WcpsMetadataResult wcpsMetadataResult = (WcpsMetadataResult)firstChildHandler.handle(serviceRegistries);
            String timeIntervalStr = wcpsMetadataResult.getResult();
            Pair<String, String> timeIntervalPair = TimeUtil.parseBounds(timeIntervalStr);
            lowerBound = timeIntervalPair.fst;
            upperBound = timeIntervalPair.snd;

            WcpsCoverageMetadata wcpsCoverageMetadata = ((WcpsResult)firstChildHandler.getFirstChild().handle(serviceRegistries)).getMetadata();
            // e.g. date
            String axisLabel = firstChildHandler.getSecondChild().handle(serviceRegistries).getResult();

            Axis dateTimeAxis = wcpsCoverageMetadata.getAxisByName(axisLabel);
            if (dateTimeAxis instanceof IrregularAxis) {
                // Here, get all the direct positions of it, not just the lower and upper bound
                IrregularAxis irregularAxis = (IrregularAxis)dateTimeAxis;
                directPositions = irregularAxis.getCoefficientValues();
            }
        } else {
            // e.g. "2015-01-01":"2015-02-03"
            Handler secondChildHandler = this.getSecondChild();

            // e.g. "2015-01-01"
            lowerBound = firstChildHandler.handle(serviceRegistries).getResult();
            upperBound = lowerBound;

            if (secondChildHandler != null) {
                // e.g. "2015-01-02"
                upperBound = secondChildHandler.handle(serviceRegistries).getResult();
            }
        }

        // Return this ParameterResult so timeExtractor / timeTruncator can continue to convert to datetime object
        IntervalExpression result = new IntervalExpression(lowerBound, upperBound, directPositions);
        return result;
    }

}
