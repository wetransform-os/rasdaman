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

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import static petascope.wcps.handler.AbstractOperatorHandler.checkOperandIsCoverage;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.result.WcpsMetadataResult;
import petascope.wcps.result.WcpsResult;

/**
 * Translator class for CrsSet of the coverage (e.g list all coverage's axis CRS
 * from crsSet($c))

 * for c in (mean_summer_airtemp) return crsSet(c) return the nativeCRS of the coverage
 *
 * http://localhost:8080/def/crs/EPSG/0/4326
 *
 * @author <a href="mailto:bphamhuu@jacobs-university.de">Bang Pham Huu</a>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CoverageCrsSetHandler extends Handler {
    
    public static final String OPERATOR = "crsset";
    
    public CoverageCrsSetHandler() {
        
    }
    
    public CoverageCrsSetHandler create(Handler coverageExpressionHandler) {
        CoverageCrsSetHandler result = new CoverageCrsSetHandler();
        result.setChildren(Arrays.asList(coverageExpressionHandler));
        return result;
    }
    
    @Override
    public VisitorResult handle(List<Object> serviceRegistries) throws PetascopeException {
        WcpsResult coverageExpression = (WcpsResult) this.getFirstChild().handle(serviceRegistries);
        WcpsMetadataResult result = this.handle(coverageExpression);
        return result;
    }

    private WcpsMetadataResult handle(WcpsResult coverageExpression) {
        checkOperandIsCoverage(coverageExpression, OPERATOR); 
        
        String result = coverageExpression.getMetadata().getCrsUri();

        return new WcpsMetadataResult(coverageExpression.getMetadata(), result.trim());
    }
}
