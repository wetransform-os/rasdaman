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
 * Copyright 2003 - 2023 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.wcps.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.WCPSException;
import petascope.util.CrsUtil;
import petascope.wcps.metadata.model.Axis;
import petascope.wcps.metadata.model.CrsTransformTargetGeoXYBoundingBox;
import petascope.wcps.metadata.model.CrsTransformTargetGeoXYResolutions;
import petascope.wcps.metadata.model.WcpsCoverageMetadata;
import petascope.wcps.result.WcpsMetadataResult;
import petascope.wcps.result.WcpsResult;

import java.util.Arrays;
import java.util.List;

import static petascope.util.MIMEUtil.MIME_BINARY;

/**
 * Class to handle polygonize(coverageExpression, "vectorEncodingFormat")
 * @author <a href="mailto:b.phamhuu@jacobs-university.de">Bang Pham Huu</a>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class PolygonizeHandler extends Handler {

    public static final String OPERATOR = "polygonize";

    public PolygonizeHandler() {
        
    }
    
    public PolygonizeHandler create(Handler coverageExpressionHandler,
                                    StringScalarHandler encodingFormat,
                                    StringScalarHandler connectednessNumber) {
        PolygonizeHandler result = new PolygonizeHandler();
        result.setChildren(Arrays.asList(coverageExpressionHandler, encodingFormat, connectednessNumber));
        return result;
    }
    
    @Override
    public WcpsResult handle(List<Object> serviceRegistries) throws PetascopeException {
        WcpsResult coverageExpression = ((WcpsResult)this.getFirstChild().handle(serviceRegistries));
        WcpsCoverageMetadata metadata = coverageExpression.getMetadata();
        
        String errorMessage = "The coverage operand of " + OPERATOR + "() must be 2D with geo-spatial X and Y axes.";
        if (!metadata.hasXYAxes()) {
            throw new WCPSException(ExceptionCode.InvalidRequest, errorMessage);
        } else if (metadata.getAxes().size() != 2) {
            throw new WCPSException(ExceptionCode.InvalidRequest, errorMessage);
        }

        String encodingFormat = ((WcpsResult)this.getSecondChild().handle(serviceRegistries)).getRasql();
        // Default value is 4
        int connectednessNumber = 4;
        if (this.getThirdChild() != null) {
            connectednessNumber = Integer.parseInt(  ((WcpsResult)this.getThirdChild().handle(serviceRegistries)).getRasql() );
        }


        String xmin = metadata.getXYAxes().get(0).getGeoBounds().getLowerLimit().toPlainString();
        String ymin = metadata.getXYAxes().get(1).getGeoBounds().getLowerLimit().toPlainString();
        String xmax = metadata.getXYAxes().get(0).getGeoBounds().getUpperLimit().toPlainString();
        String ymax = metadata.getXYAxes().get(1).getGeoBounds().getUpperLimit().toPlainString();
        String bbox = "\"" + xmin + ", " + ymin + ", " + xmax + ", " + ymax + "\"";

        String rasqlContent = coverageExpression.getRasql() + ", " + encodingFormat + ", " + connectednessNumber;
        if (!CrsUtil.isGridCrs(metadata.getXYAxes().get(0).getNativeCrsUri())) {
            // geo-referenced coverage
            String epsgCode = CrsUtil.getAuthorityCode(metadata.getXYCrs());
            rasqlContent = coverageExpression.getRasql()
                         + ", " + encodingFormat + ", " + connectednessNumber
                         + ", \"" + epsgCode + "\", " + bbox;
        }

        String resultRasql = OPERATOR + "(" + rasqlContent + ")";

        // NOTE: this polygonize is equivalent to encode()
        WcpsResult wcpsResult = new WcpsResult(coverageExpression.getMetadata(), resultRasql);
        wcpsResult.setMimeType(MIME_BINARY);

        return wcpsResult;
    }

}
