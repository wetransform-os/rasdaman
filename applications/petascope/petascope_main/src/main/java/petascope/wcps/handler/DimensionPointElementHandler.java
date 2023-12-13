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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.util.CrsUtil;
import petascope.util.StringUtil;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.result.WcpsMetadataResult;
import petascope.wcps.result.WcpsResult;
import petascope.wcps.subset_axis.model.WcpsSliceSubsetDimension;
import petascope.wcps.subset_axis.model.WcpsSubsetDimension;
import petascope.wcps.subset_axis.model.WcpsTrimSubsetDimension;

/**
 * Handler for expression
    - axisName (COLON crsName)? LEFT_PARENTHESIS coverageExpression RIGHT_PARENTHESIS
       e.g: i(5) - Slicing point
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class DimensionPointElementHandler extends Handler {

    private static final Pattern timeIntervalPattern = Pattern.compile("(\".*\"):(\".*\")");

    public DimensionPointElementHandler() {
        
    }
    
    public DimensionPointElementHandler create(StringScalarHandler axisNameHandler, StringScalarHandler crsHandler, Handler coverageExpressionHandler) {
        DimensionPointElementHandler result = new DimensionPointElementHandler();
        result.setChildren(Arrays.asList(axisNameHandler, crsHandler, coverageExpressionHandler));
        
        return result;
    }

    @Override
    public VisitorResult handle(List<Object> serviceRegistries) throws PetascopeException {
        String axisName = ((WcpsResult)this.getFirstChild().handle(serviceRegistries)).getRasql();
        String crs = ((WcpsResult)this.getSecondChild().handle(serviceRegistries)).getRasql();
        VisitorResult coverageExpression = this.getThirdChild().handle(serviceRegistries);
        
        VisitorResult result = this.handle(axisName, crs, coverageExpression);
        return result;
    }
    
    private VisitorResult handle(String axisName, String crs, VisitorResult coverageExpression) {
        String bound;
         
        if (coverageExpression instanceof WcpsResult) {
            bound = ((WcpsResult)coverageExpression).getRasql();
        } else {
            bound = ((WcpsMetadataResult)coverageExpression).getResult();
        }
        
        WcpsSubsetDimension subsetDimension;
        
        if (StringUtils.countMatches(bound, ":") == 1) {
            // NOTE: This only happens for this case (subsetting from result of domain()/imageCrsdomain()), with result of domain is an interval, e.g: -20:-10
            // e.g: c[Lat(domain(c[Lat(-20:-10)], Lat))] -> c[Lat(-20:-10)]
            bound = StringUtil.stripParentheses(bound);

            String[] tmp = bound.split(":");
            String lowerBound = tmp[0];
            String upperBound = tmp[1];

            if (bound.contains("\"")) {
                // time string in quotes
                // e.g. "2015-01-01T00:00" or "2015-01-01T00:00":"2015-01-03T00:00"
                Matcher matcher = timeIntervalPattern.matcher(bound);
                if (matcher.find()) {
                    lowerBound = matcher.group(1);
                    upperBound = matcher.group(2);

                    // e.g. "2015-01-01":"2015-02-02" -> trim
                    subsetDimension = new WcpsTrimSubsetDimension(axisName, crs, lowerBound, upperBound);
                } else {
                    // e.g. "2015-01-01" -> slice
                    subsetDimension = new WcpsSliceSubsetDimension(axisName, crs, bound);
                }
            } else {
                // e.g. -20:-10 -> trim()
                subsetDimension = new WcpsTrimSubsetDimension(axisName, crs, lowerBound, upperBound);
            }

        } else {
            subsetDimension = new WcpsSliceSubsetDimension(axisName, crs, bound);
        }
  
        return subsetDimension;
    }
    
}
