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
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import static petascope.wcps.handler.AbstractOperatorHandler.checkOperandIsCoverage;
import petascope.wcps.metadata.model.Axis;
import petascope.wcps.metadata.model.NumericTrimming;
import petascope.wcps.result.WcpsMetadataResult;
import petascope.wcps.result.WcpsResult;

/**
 * Translator class for the imageCrsDomain(coverageExpression, axisLabel)
 * operation in wcps
 *
 * imageCrsDomain (coverageExpr): for c in (eobstest) return
 * imageCrsDomain(c[Lat(20:30), Long(30:40)]) returns (0:5, 60:70, 20:30) in
 * grid-coordinate (t, Long, Lat) respectively
 *
 *
 * @author <a href="mailto:bphamhuu@jacobs-university.de">Bang Pham Huu</a>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ImageCrsDomainExpressionHandler extends Handler {
    
    public static final String OPERATOR = "imageCrsDomain";
    
    public ImageCrsDomainExpressionHandler() {
        
    }
    
    public ImageCrsDomainExpressionHandler create(Handler coverageExpressionHandler) {
        ImageCrsDomainExpressionHandler result = new ImageCrsDomainExpressionHandler();
        result.setChildren(Arrays.asList(coverageExpressionHandler));
        
        return result;
    }
    
    public WcpsMetadataResult handle(List<Object> serviceRegistries) throws PetascopeException {
        WcpsResult coverageExpression = (WcpsResult) this.getFirstChild().handle(serviceRegistries);
        WcpsMetadataResult result = this.handle(coverageExpression);
        
        return result;
    }

    private WcpsMetadataResult handle(WcpsResult coverageExpression) {
        
        checkOperandIsCoverage(coverageExpression, OPERATOR); 
        
        // just iterate the axes and get the grid bound for each axis
        String rasql = "";
        List<String> axisBounds = new ArrayList<>();

        // NOTE: imageCrsDomain(c) will return the axes' domains in Rasdaman order (e.g: Long, Lat) (not CRS order, e.g: Lat, Long)
        for (Axis axis : coverageExpression.getMetadata().getSortedAxesByGridOrder()) {
            // This is used to set bounding box in case of scale() or extend() with imageCrsdomain()
            String tmp = "";
            if (axis.getGridBounds() instanceof NumericTrimming) {
                // Trimming
                // NOTE: not add slice for bounding box e.g: Lat(0), need to check if NumericSubset is slicing or trimming
                String lowBound = ((NumericTrimming) axis.getGridBounds()).getLowerLimit().toPlainString();
                String highBound = ((NumericTrimming) axis.getGridBounds()).getUpperLimit().toPlainString();
                tmp = TRIMMING_TEMPLATE.replace("$lowBound", lowBound).replace("$highBound", highBound);

                // Only add trimming domain interval to Rasql
                // e.g: imageCrsDomain(c[t("1950-01-01"), Long(43:44), Lat(24:25)]) - return (43:44,24:25)
                axisBounds.add(tmp);
            }
        }

        // (0:5,0:100,0:231)
        rasql = "(" + StringUtils.join(axisBounds, ",") + ")";
        WcpsMetadataResult wcpsResult = new WcpsMetadataResult(coverageExpression.getMetadata(), rasql);
        return wcpsResult;
    }

    private final String TRIMMING_TEMPLATE = "$lowBound:$highBound";
    public final String IMAGE_CRS_DOMAIN = "imageCrsdomain";
}
