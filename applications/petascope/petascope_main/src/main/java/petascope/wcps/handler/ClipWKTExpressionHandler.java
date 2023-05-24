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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.core.Pair;
import petascope.exceptions.PetascopeException;
import petascope.util.StringUtil;
import petascope.wcps.exception.processing.ClipExpressionException;
import petascope.wcps.exception.processing.Coverage0DMetadataNullException;
import petascope.wcps.exception.processing.InvalidWKTClippingException;
import petascope.wcps.metadata.model.Axis;
import petascope.wcps.metadata.model.WcpsCoverageMetadata;
import petascope.wcps.result.WcpsResult;
import petascope.wcps.subset_axis.model.AbstractWKTShape;
import petascope.wcps.subset_axis.model.WKTLineString;

/**
 *
 * Handler for the clip WKT expression to crop a coverage by a WKT (polygon,
 * linestring,...) e.g: clip(c, Polygon((0 20, 20 20, 20 10, 0 20)))
 *
 * @author <a href="mailto:b.phamhuu@jacobs-university.de">Bang Pham Huu</a>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ClipWKTExpressionHandler extends AbstractClipExpressionHandler {
    
    // e.g: clip( c, POLYGON((...)) )
    private final String RASQL_TEMPLATE = this.OPERATOR 
                                         + "( " + this.TRANSLATED_COVERAGE_EXPRESSION_RASQL_TEMPLATE + ", "
                                         +  this.TRANSLATED_WKT_EXPRESSION_RASQL_TEMPLATE + " )";
    
    // Current only with encode( clip(c, Linestring(...) WITH COORDINATES, "csv/json") 
    public static final String WITH_COORDINATES = " WITH COORDINATES ";
    
    
    public ClipWKTExpressionHandler() {
        
    }
    
    public ClipWKTExpressionHandler create(Handler coverageExpressionHandler, Handler wktShapeHandler,
                                            Handler wktCRSHandler, Handler withCoordinatesBooleanStringHandler) {
        ClipWKTExpressionHandler result = new ClipWKTExpressionHandler();
        result.setChildren(Arrays.asList(coverageExpressionHandler, wktShapeHandler, wktCRSHandler, withCoordinatesBooleanStringHandler));
        
        result.httpServletRequest = this.httpServletRequest;
        result.wcpsCoverageMetadataGeneralService = this.wcpsCoverageMetadataGeneralService;
        result.subsetParsingService = this.subsetParsingService;
        result.coverageAliasRegistry = this.coverageAliasRegistry;
        result.coordinateTranslationService = this.coordinateTranslationService;
        
        return result;
    }
    
    @Override
    public WcpsResult handle(List<Object> serviceRegistries) throws PetascopeException {
        // Handle clipWKTExpression: CLIP LEFT_PARENTHESIS coverageExpression COMMA wktExpression (COMMA crsName)? RIGHT_PARENTHESIS
        // e.g: clip(c[i(0:20), j(0:20)], Polygon((0 10, 20 20, 20 10, 0 10)), "http://opengis.net/def/CRS/EPSG/3857")
        WcpsResult coverageExpressionResult = (WcpsResult) this.getFirstChild().handle(serviceRegistries);
        AbstractWKTShape wktShape = (AbstractWKTShape) this.getSecondChild().handle(serviceRegistries);
               
        int numberOfDimensions = wktShape.getWktCompoundPointsList().get(0).getNumberOfDimensions();
        if (coverageExpressionResult.getMetadata() == null) {
            throw new Coverage0DMetadataNullException(ClipWKTExpressionHandler.OPERATOR);
        }
        int coverageDimensions = coverageExpressionResult.getMetadata().getAxes().size();
        if (numberOfDimensions != coverageDimensions) {
            throw new InvalidWKTClippingException("Number of dimensions in WKT '" + numberOfDimensions + "' is different from coverage's '" + coverageDimensions + "'.");
        }
        
        // NOTE: This one is optional parameter, if specified, XY coordinates in WKT will be translated from this CRS to coverage's native CRS for XY axes.
        String wktCRS = null;
        if (this.getThirdChild() != null) {
            String value = ((WcpsResult)this.getThirdChild().handle(serviceRegistries)).getRasql();
            wktCRS = StringUtil.stripFirstAndLastQuotes(value);
        }        
        
        // Optional parameter for encoding in JSON/CSV to show grid coordinates and their values (e.g: "x1 y1 value1", "x2 y2 value2", ...)
        boolean withCoordinate = false;
        if (this.getFourthChild() != null) {
            if (!(wktShape instanceof WKTLineString)) {
                throw new InvalidWKTClippingException("'" + ClipWKTExpressionHandler.WITH_COORDINATES + "' can be only applied with clipping by WKT 'LineString'.");
            }
            withCoordinate = true;
        }
        
        WcpsResult result = this.handle(coverageExpressionResult, wktShape, wktCRS, withCoordinate);
        result.setWithCoordinates(withCoordinate);
        
        return result;        
    }
    

    /**
     * Handle the clip operator from current collection and input WKT to be
     * applied on it.
     *
     * @param coverageExpression a coverage expression valid to clip
     * @param wktShape an abstract class for all parsable WKT (e.g:
     * @param wktCRS an optional parameter to determine the coordinate of WKT
     * shape. If it is different from coverage's native XY axes CRS, transform
     * from WKT CRS to native coverage's XY axes CRS. Polygon((...)),
     * LineString((..)))
     * @return WcpsResult an object to be used in upper parsing tree.
     */
    private WcpsResult handle(WcpsResult coverageExpression, AbstractWKTShape wktShape, String wktCRS, boolean withCoordinate) throws PetascopeException {
        // NOTE: Coverage's dimensions must match with number of listed dimensions in each vertex of WKT,
        // then, translate each vertex's coordinates on all coverage's axes.
        List<String> axisNames = new ArrayList<>();
        for (Axis axis : coverageExpression.getMetadata().getAxes()) {
            axisNames.add(axis.getLabel());
        }
        
        String rasqlTemplate = RASQL_TEMPLATE;
        if (withCoordinate) {
            rasqlTemplate += WITH_COORDINATES;
        }
        
        // Store the calculated bounding box of clipped output from a coverage and a WKT shape
        Map<String, Pair<BigDecimal, BigDecimal>> clippedCoverageAxesGeoBounds = new HashMap<>();
        
        WcpsResult result = this.mainHandle(clippedCoverageAxesGeoBounds, coverageExpression, axisNames, wktShape, wktCRS, rasqlTemplate);
        
        WcpsCoverageMetadata metadata = result.getMetadata();
        String rasql = result.getRasql();
        
        // number of coverage's dimension used in WKT shape (e.g: polygon((20 30, 30 40)), number is 2, polygon((20 30 40, 30 50 70)), number is 3
        int numberOfDimensions = wktShape.getWktCompoundPointsList().get(0).getNumberOfDimensions();
        
        // NOTE: if clip on 2D oblique polygon of a 3D+ coverage (e.g: Lat, Long, Time for 3D), the output will be a 2D collection with only Index2D CRS.
        // as this polygon doesn't belong to any plane containing original axes and the bounding box (geo, grid domains) is unknown (!!!) and set to a constant value.
        if (numberOfDimensions > wktShape.getDefaultNumberOfDimensions()) {
            // NOTE: Domains of ouput of oblique polygon or linestring are unknown, 
            // then it must send rasql query first to get the sdom to create the geo, grid bounds for the output coverage.
            List<Pair<String, String>> gridDomains = this.getSdomOfClippedOutput(rasql);
            metadata = this.wcpsCoverageMetadataGeneralService.createCoverageByIndexAxes(metadata, gridDomains);
        } else {
            // NOTE: Coverage's axes' domains are reduced after clipping (e.g: clip a small polygon from a big 2D image)
            // Then, geo domains need to be updated for bounding box of this polygon.
            this.updateOuputCoverageGeoAxesDomains(clippedCoverageAxesGeoBounds, metadata);
        }
        
        // Update coverag's native CRS after subsetting (e.g: 3D -> 2D, then CRS=compound?time&4326 -> 4326)
        metadata.updateCrsUri();
        
        // Finished everything
        result.setMetadata(metadata);
        
        return result;
    }
}
