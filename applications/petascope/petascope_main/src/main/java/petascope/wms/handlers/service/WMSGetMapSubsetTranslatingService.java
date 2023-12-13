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
 * Copyright 2003 - 2020 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.wms.handlers.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.core.BoundingBox;
import petascope.core.GeoTransform;
import petascope.exceptions.PetascopeException;
import petascope.util.BigDecimalUtil;
import petascope.util.CrsProjectionUtil;
import petascope.wcps.handler.CrsTransformHandler;
import petascope.wcps.handler.SubsetExpressionHandler;
import petascope.wcps.metadata.model.Axis;
import petascope.wcps.metadata.model.NumericSubset;
import petascope.wcps.metadata.model.NumericTrimming;
import petascope.wcps.metadata.model.RegularAxis;
import petascope.wcps.metadata.model.WcpsCoverageMetadata;
import petascope.wcps.metadata.service.CollectionAliasRegistry;
import petascope.wcps.metadata.service.CoordinateTranslationService;
import petascope.wcps.result.WcpsResult;
import petascope.wcps.subset_axis.model.DimensionIntervalList;
import petascope.wcps.subset_axis.model.WcpsSubsetDimension;
import petascope.wcps.subset_axis.model.WcpsTrimSubsetDimension;
import petascope.wms.handlers.model.WMSLayer;
import static petascope.wms.handlers.service.WMSGetMapService.COLLECTION_EXPRESSION_TEMPLATE;
import static petascope.wms.handlers.service.WMSGetMapService.TRANSPARENT_DOMAIN;
import static petascope.wms.handlers.service.WMSGetMapStyleService.EXTEND;
import static petascope.wms.handlers.service.WMSGetMapStyleService.SCALE;

/**
 * Service for translate geo domains to grid domains in WMS GetMap request
 *
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
// Create a new instance of this bean for each request (so it will not use the old object with stored data)
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class WMSGetMapSubsetTranslatingService {
    
    @Autowired
    private WMSGetMapWCPSMetadataTranslatorService wmsGetMapWCPSMetadataTranslatorService;
    @Autowired
    private SubsetExpressionHandler subsetExpressionHandler;
    @Autowired
    private CoordinateTranslationService coordinateTranslationService;
    
    @Autowired
    private CollectionAliasRegistry collectionAliasRegistry;

    private static final String WIDTH = "$width";
    private static final String HEIGHT = "$height";

    private static final String RESAMPLE_ALG = "$resampleAlg";
    private static final String ERR_THRESHOLD = "$errThreshold";

    private static final String NATIVE_CRS = "$nativeCRS";

    private static final String XMIN_NATIVCE_CRS = "$xMinNativeCRS";
    private static final String YMIN_NATIVCE_CRS = "$yMinNativeCRS";
    private static final String XMAX_NATIVCE_CRS = "$xMaxNativeCRS";
    private static final String YMAX_NATIVCE_CRS = "$yMaxNativeCRS";

    private static final String OUTPUT_CRS = "$outputCRS";

    private static final String XMIN_OUTPUT_CRS = "$xMinOutputCRS";
    private static final String YMIN_OUTPUT_CRS = "$yMinOutputCRS";
    private static final String XMAX_OUTPUT_CRS = "$xMaxOutputCRS";
    private static final String YMAX_OUTPUT_CRS = "$yMaxOutputCRS";
    
    // Default value for project()
    private static final String DEFAULT_ERR_THRESHOLD = "0.125";

    private static final String PROJECT_TEMPLATE = "project( " + COLLECTION_EXPRESSION_TEMPLATE
            + ", \"" + XMIN_NATIVCE_CRS + ", " + YMIN_NATIVCE_CRS + ", " + XMAX_NATIVCE_CRS + ", " + YMAX_NATIVCE_CRS + "\" "
            + ", \"" + NATIVE_CRS + "\" "
            + ", \"" + XMIN_OUTPUT_CRS + ", " + YMIN_OUTPUT_CRS + ", " + XMAX_OUTPUT_CRS + ", " + YMAX_OUTPUT_CRS + "\" "
            + ", \"" + OUTPUT_CRS + "\" "
            + ", " + WIDTH + ", " + HEIGHT + ", " + RESAMPLE_ALG + ", " + ERR_THRESHOLD + " )";
    
    public static final String COLLECTION_ALIAS_PREFIX = "c";

    private static Logger log = LoggerFactory.getLogger(WMSGetMapSubsetTranslatingService.class);
    
    /**
     * Return a list of WCPS subsets from input subsets parameter of GetMap request
     */
    public List<WcpsSubsetDimension> parseWcpsSubsetDimensions(WcpsCoverageMetadata wcpsCoverageMetadata,
                                                               BoundingBox fittedBBox) {
        
        List<WcpsSubsetDimension> wcpsSubsetDimensions = new ArrayList<>();
         List<Axis> xyAxes = wcpsCoverageMetadata.getXYAxes();
        Axis axisX = xyAxes.get(0);
        Axis axisY = xyAxes.get(1);

        WcpsTrimSubsetDimension trimSubsetDimensionX = new WcpsTrimSubsetDimension(axisX.getLabel(), axisX.getNativeCrsUri(),
                                                                                  fittedBBox.getXMin().toPlainString(), fittedBBox.getXMax().toPlainString());

        WcpsTrimSubsetDimension trimSubsetDimensionY = new WcpsTrimSubsetDimension(axisY.getLabel(), axisX.getNativeCrsUri(),
                                                                                  fittedBBox.getYMin().toPlainString(), fittedBBox.getYMax().toPlainString());
        wcpsSubsetDimensions.add(trimSubsetDimensionX);
        wcpsSubsetDimensions.add(trimSubsetDimensionY);
        
        return wcpsSubsetDimensions;        
    }
    
    /**
     * Translate geo domains to grid domains for a WCPS coverage metadata
     */
    public WcpsResult applyWCPSGeoSubsets(WcpsCoverageMetadata wcpsCoverageMetadata, 
                                          List<WcpsSubsetDimension> wcpsSubsetDimensions) throws PetascopeException {
        String rasql = null;

        String collectionName = wcpsCoverageMetadata.getRasdamanCollectionName();
        if (this.collectionAliasRegistry.getAliasName(collectionName) != null) {
            // e.g: c0 -> collectionA
            for (String alias : this.collectionAliasRegistry.getAliasMap().keySet()) {
                if (this.collectionAliasRegistry.getAliasMap().get(alias).fst.equals(collectionName)) {
                    rasql = alias;
                    break;
                }
            }
        } else {                
            // In case this collection does not exist in the registry then add it to the registry for the rasql FROM clause
            String alias = COLLECTION_ALIAS_PREFIX + this.collectionAliasRegistry.getAliasMap().size();
            String layerName = wcpsCoverageMetadata.getCoverageName();
            this.collectionAliasRegistry.add(alias, wcpsCoverageMetadata.getRasdamanCollectionName(), layerName);
            rasql = alias;
        }
            
        WcpsResult wcpsResult = new WcpsResult(wcpsCoverageMetadata, rasql);
        
        DimensionIntervalList dimensionIntervalList = new DimensionIntervalList(wcpsSubsetDimensions);
        WcpsResult outputWcpsResult = subsetExpressionHandler.handle(wcpsResult, dimensionIntervalList, false);
        
        return outputWcpsResult;
    }

    /**
     * Apply subset() and extend() on top of collection subsetting expression if
     * needed in case of no-projection.
     */
    public String createGridScalingOutputNonProjection(String subsetCollectionExpression,
                                                       WMSLayer wmsLayer, 
                                                       BoundingBox originalRequestBBox, String outputCRS,
                                                       String backgroundColorFragment)
            throws PetascopeException {
        
        WcpsCoverageMetadata wcpsCoverageMetadata = this.wmsGetMapWCPSMetadataTranslatorService.createWcpsCoverageMetadataForDownscaledLevelByExtendedRequestBBox(wmsLayer);
        
        int width = wmsLayer.getWidth();
        int height = wmsLayer.getHeight();

        BigDecimal widthDecimal = new BigDecimal(width);
        BigDecimal heightDecimal = new BigDecimal(height);

        // Comes from the original WMS GetMap request BBOX
        BigDecimal orgBBoxXMin = wmsLayer.getRequestBBox().getXMin();
        BigDecimal orgBBoxYMin = wmsLayer.getRequestBBox().getYMin();
        BigDecimal orgBBoxXMax = wmsLayer.getRequestBBox().getXMax();
        BigDecimal orgBBoxYMax = wmsLayer.getRequestBBox().getYMax();

        // Extended request BBOX and they are not the aligned BBOX when doing subsets based on the upper left corner
        BigDecimal extendedBBoxXMin = wmsLayer.getExtendedRequestBBox().getXMin();
        BigDecimal extendedBBoxYMin = wmsLayer.getExtendedRequestBBox().getYMin();
        BigDecimal extendedBBoxXMax = wmsLayer.getExtendedRequestBBox().getXMax();
        BigDecimal extendedBBoxYMax = wmsLayer.getExtendedRequestBBox().getYMax();

        // Extended and aligned BBOX when doing subsets based on the upper left corner
        BigDecimal extendedAlignedBBoxXMin = wmsLayer.getExtendedAlignedRequestBBox().getXMin();
        BigDecimal extendedAlignedBBoxYMin = wmsLayer.getExtendedAlignedRequestBBox().getYMin();
        BigDecimal extendedAlignedBBoxXMax = wmsLayer.getExtendedAlignedRequestBBox().getXMax();
        BigDecimal extendedAlignedBBoxYMax = wmsLayer.getExtendedAlignedRequestBBox().getYMax();

        BigDecimal ratioX = BigDecimal.ONE;
        BigDecimal distanceX = orgBBoxXMax.subtract(orgBBoxXMin);
        if (distanceX.compareTo(BigDecimal.ZERO) != 0) {
            ratioX = BigDecimalUtil.divide(extendedBBoxXMax.subtract(extendedBBoxXMin), orgBBoxXMax.subtract(orgBBoxXMin));
        }

        if (ratioX.compareTo(BigDecimal.TEN) > 0) {
            ratioX = BigDecimal.TEN.add(ratioX.remainder(BigDecimal.ONE));
        }

        BigDecimal ratioY = BigDecimal.ONE;
        BigDecimal distanceY = orgBBoxYMax.subtract(orgBBoxYMin);
        if (distanceY.compareTo(BigDecimal.ZERO) != 0) {
            ratioY = BigDecimalUtil.divide(extendedBBoxYMax.subtract(extendedBBoxYMin), orgBBoxYMax.subtract(orgBBoxYMin));
        }

        if (ratioY.compareTo(BigDecimal.TEN) > 0) {
            ratioY = BigDecimal.TEN.add(ratioY.remainder(BigDecimal.ONE));
        }

        // NOTE: with the original request BBOX -> the result is width x height (e.g. 256 x 256)
        // but with the extended BBOX -> the result will be larged than that by width * ratioX x height * ratioY (e.g. 433 x 450)
        long extendedWidth = BigDecimalUtil.shiftToInteger(widthDecimal.multiply(ratioX));
        long extendedHeight = BigDecimalUtil.shiftToInteger(heightDecimal.multiply(ratioY));

        List<Axis> originalXYAxes = wcpsCoverageMetadata.getXYAxes();

//      Transform ***layer BBox*** from layer's native CRS to output CRS (default EPSG:4326) as original request BBox is also in EPSG:4326
        GeoTransform sourceGeoTransformLayerBBox = CrsTransformHandler.createGeoTransform(originalXYAxes);
        GeoTransform targetGeoTransformLayerBBox = CrsProjectionUtil.getGeoTransformInTargetCRS(sourceGeoTransformLayerBBox, outputCRS);

        List<Axis> transformedXYAxesLayerBBox = CrsTransformHandler.createGeoXYAxes(originalXYAxes, targetGeoTransformLayerBBox);

        Axis axisX = transformedXYAxesLayerBBox.get(0);
        Axis axisY = transformedXYAxesLayerBBox.get(1);

        // Default grid domains for scale, extend if request BBox is intersected with layer's geo bounds.
        String scaleX = "0:" + (width - 1);
        String extendX = scaleX;

        String scaleY = "0:" + (height - 1);
        String extendY = scaleY;
        
        boolean invalidQuery = false;
        
        // If request BBox is not within the layer's geo bounds
        if (!((originalRequestBBox.getXMin().compareTo(axisX.getGeoBounds().getLowerLimit()) >= 0)
                && (originalRequestBBox.getXMax().compareTo(axisX.getGeoBounds().getUpperLimit()) <= 0)
                && (originalRequestBBox.getYMin().compareTo(axisY.getGeoBounds().getLowerLimit()) >= 0)
                && (originalRequestBBox.getYMax().compareTo(axisY.getGeoBounds().getUpperLimit()) <= 0))) {

            // ********* X axis: e.g: Long
            BigDecimal geoOriginX = BigDecimal.ONE;
            BigDecimal lengthGeoIntersectionX = BigDecimal.ONE;

            BigDecimal lengthBBoxX = originalRequestBBox.getXMax().subtract(originalRequestBBox.getXMin());

            if (!(originalRequestBBox.getXMin().compareTo(axisX.getGeoBounds().getLowerLimit()) >= 0 && originalRequestBBox.getXMax().compareTo(axisX.getGeoBounds().getUpperLimit()) <= 0)) {
                if (originalRequestBBox.getXMin().compareTo(axisX.getGeoBounds().getLowerLimit()) < 0 && originalRequestBBox.getXMax().compareTo(axisX.getGeoBounds().getUpperLimit()) <= 0) {
                    // e.g: BBox of Long is [10:30] intersects with axis Long [20:40] from [20:30], originX: 20
                    geoOriginX = axisX.getGeoBounds().getLowerLimit();
                    lengthGeoIntersectionX = originalRequestBBox.getXMax().subtract(geoOriginX);
                } else if (originalRequestBBox.getXMin().compareTo(axisX.getGeoBounds().getLowerLimit()) >= 0 && originalRequestBBox.getXMax().compareTo(axisX.getGeoBounds().getUpperLimit()) > 0) {
                    // e.g: BBox of Long is [30:50] intersects with axis Long [20:40] from [30:40], originX: 30
                    geoOriginX = originalRequestBBox.getXMin();
                    lengthGeoIntersectionX = axisX.getGeoBounds().getUpperLimit().subtract(geoOriginX);
                } else if (originalRequestBBox.getXMin().compareTo(axisX.getGeoBounds().getLowerLimit()) < 0 && originalRequestBBox.getXMax().compareTo(axisX.getGeoBounds().getUpperLimit()) > 0) {
                    // e.g: BBox of Long is [10:50] intersects with axis Long [20:40] from [20:40], originX: 20
                    geoOriginX = axisX.getGeoBounds().getLowerLimit();
                    lengthGeoIntersectionX = axisX.getGeoBounds().getUpperLimit().subtract(geoOriginX);
                }

                // Calculate the portion of intersection's length and bbox's length on X axis for the domains of scale and extend
                BigDecimal portionIntersectionX = BigDecimalUtil.divide(lengthGeoIntersectionX, lengthBBoxX);
                Long scaleUpperBoundX = BigDecimalUtil.shiftToInteger(portionIntersectionX.multiply(new BigDecimal(width)));
                scaleX = "0:" + scaleUpperBoundX;

                // Calculate the portion of originX in the geo bboxX
                BigDecimal portionGeoOriginX = BigDecimalUtil.divide((geoOriginX.subtract(originalRequestBBox.getXMin())), lengthBBoxX);
                Long gridOriginX = BigDecimalUtil.shiftToInteger(portionGeoOriginX.multiply(new BigDecimal(width)));
                Long extendLowerBoundX = 0L - Math.abs(gridOriginX);
                Long extendUpperBoundX = width + extendLowerBoundX - 1;
                extendX = extendLowerBoundX + ":" + extendUpperBoundX;
                
                if (scaleUpperBoundX < 0) {
                    log.warn("Calculated scaling upper bound X is less than 0. Hint: Make sure that the coverage: " + wmsLayer.getLayerName() + " was imported correctly for axis type: X. Given: " + scaleUpperBoundX);
                    invalidQuery = true;
                }
            }

            // ********* Y axis: e.g: Lat
            if (!(originalRequestBBox.getYMin().compareTo(axisY.getGeoBounds().getLowerLimit()) >= 0 && originalRequestBBox.getYMax().compareTo(axisY.getGeoBounds().getUpperLimit()) <= 0)) {
                BigDecimal geoOriginY = BigDecimal.ONE;
                BigDecimal lengthGeoIntersectionY = BigDecimal.ONE;

                BigDecimal lengthBBoxY = originalRequestBBox.getYMax().subtract(originalRequestBBox.getYMin());

                if (originalRequestBBox.getYMin().compareTo(axisY.getGeoBounds().getLowerLimit()) < 0 && originalRequestBBox.getYMax().compareTo(axisY.getGeoBounds().getUpperLimit()) <= 0) {
                    // e.g: BBox of Lat is [-50:-30] intersects with axis Lat [-40:-20] from [-40:-30], originY: -30
                    geoOriginY = originalRequestBBox.getYMax();
                    lengthGeoIntersectionY = originalRequestBBox.getYMax().subtract(axisY.getGeoBounds().getLowerLimit());
                } else if (originalRequestBBox.getYMin().compareTo(axisY.getGeoBounds().getLowerLimit()) >= 0 && originalRequestBBox.getYMax().compareTo(axisY.getGeoBounds().getUpperLimit()) > 0) {
                    // e.g: BBox of Lat is [-30:-10] intersects with axis Lat [-40:-20] from [-30:-20], originY: -20
                    geoOriginY = axisY.getGeoBounds().getUpperLimit();
                    lengthGeoIntersectionY = axisY.getGeoBounds().getUpperLimit().subtract(originalRequestBBox.getYMin());
                } else if (originalRequestBBox.getYMin().compareTo(axisY.getGeoBounds().getLowerLimit()) < 0 && originalRequestBBox.getYMax().compareTo(axisY.getGeoBounds().getUpperLimit()) > 0) {
                    // e.g: BBox of Lat is [-50:-10] intersects with axis Lat [-40:-20] from [-40:-20], originY: -20
                    geoOriginY = axisY.getGeoBounds().getUpperLimit();
                    lengthGeoIntersectionY = geoOriginY.subtract(axisY.getGeoBounds().getLowerLimit());
                }

                // Calculate the portion of intersection's length and bbox's length on Y axis for the domains of scale and extend
                BigDecimal portionIntersectionY = BigDecimalUtil.divide(lengthGeoIntersectionY, lengthBBoxY);
                Long scaleUpperBoundY = BigDecimalUtil.shiftToInteger(portionIntersectionY.multiply(new BigDecimal(height)));
                scaleY = "0:" + scaleUpperBoundY;

                // Calculate the portion of originX in the geo bboxY
                BigDecimal portionGeoOriginY = BigDecimalUtil.divide(originalRequestBBox.getYMax().subtract(geoOriginY), lengthBBoxY);
                Long gridOriginY = BigDecimalUtil.shiftToInteger(portionGeoOriginY.multiply(new BigDecimal(height)));
                Long extendLowerBoundY = 0L - Math.abs(gridOriginY);
                Long extendUpperBoundY = height + extendLowerBoundY - 1;
                extendY = extendLowerBoundY + ":" + extendUpperBoundY;
                
                if (scaleUpperBoundY < 0) {
                    // NOTE: This can happen when importing netCDF coverage and the lat axis has coordinates from south to north
                    // hence, the axis resolution is not negative but positive.
                    // In this case, lat axis needs to be flipped by cdo tool before importing to rasdaman.
                    log.warn("Calculated scaling upper bound Y is less than 0. " +
                            "Hint: Make sure that the coverage: " + wmsLayer.getLayerName() + " was imported correctly for axis type: Y. Given: " + scaleUpperBoundY);
                    invalidQuery = true;
                }
            }
        }

        if (originalXYAxes.get(0).getRasdamanOrder() > originalXYAxes.get(1).getRasdamanOrder()) {
            // NOTE: This case is layer imported with YX grid order (e.g: netCDF lat, long) not GeoTiff (long, lat).
            // Hence, it must need swap scale, extend and add transpose in encode to return correct result
            String temp = scaleX;
            scaleX = scaleY;
            scaleY = temp;

            temp = extendX;
            extendX = extendY;
            extendY = temp;
        }
        
        boolean needExtend = !(extendX.equals(scaleX) && extendY.equals(scaleY));
        
        String finalCollectionExpressionLayer = subsetCollectionExpression;
        if (!subsetCollectionExpression.toLowerCase().contains("project")) { // -- rasdaman enterprise for virtual coverage )
            // In case no needs to have extend (i.e. zoom inside the layer)
            long defaultLowerGridBound = 0;
            long defaultUpperGridBound = 0;

            String scaledExpression = "[" + defaultLowerGridBound + ":" + (extendedWidth - 1) + ", " + defaultUpperGridBound + ":" + (extendedHeight - 1) + "]";
            if (!wmsLayer.getWcpsCoverageMetadata().isXYGridOrder()) {
                scaledExpression = "[" + defaultLowerGridBound + ":" + (extendedHeight - 1) + ", " + defaultUpperGridBound + ":" + (extendedWidth - 1) + "]";
            }

            subsetCollectionExpression = "scale( " + subsetCollectionExpression +  ", " + scaledExpression + " ) ";

            // Then, apply the original request BBOX on the scaled grid domains based on the extended aligned request BBOX
            NumericSubset geoBoundsXTmp = new NumericTrimming(extendedAlignedBBoxXMin, extendedAlignedBBoxXMax);
            NumericSubset gridBoundsXTmp = new NumericTrimming(BigDecimal.ZERO, new BigDecimal(extendedWidth - 1));
            Axis axisXTmp = new RegularAxis(axisX, geoBoundsXTmp, gridBoundsXTmp, gridBoundsXTmp);

            NumericSubset geoBoundsYTmp = new NumericTrimming(extendedAlignedBBoxYMin, extendedAlignedBBoxYMax);
            NumericSubset gridBoundsYTmp = new NumericTrimming(BigDecimal.ZERO, new BigDecimal(extendedHeight - 1));
            Axis axisYTmp = new RegularAxis(axisY, geoBoundsYTmp, gridBoundsYTmp, gridBoundsYTmp);

            BoundingBox originalGridBBoxInScaledExpression = this.coordinateTranslationService.calculageGridXYBoundingBox(false, false, axisXTmp, axisYTmp, originalRequestBBox);

            long gridXMin = BigDecimalUtil.shiftToInteger(originalGridBBoxInScaledExpression.getXMin());
            long gridYMin = BigDecimalUtil.shiftToInteger(originalGridBBoxInScaledExpression.getYMin());
            long gridXMax = BigDecimalUtil.shiftToInteger(originalGridBBoxInScaledExpression.getXMax());
            long gridYMax = BigDecimalUtil.shiftToInteger(originalGridBBoxInScaledExpression.getYMax());

            if (gridXMin < defaultLowerGridBound) {
                gridXMin = defaultLowerGridBound;
            }
            if (gridXMax > (extendedWidth - 1)) {
                gridXMax = extendedWidth - 1;
            }

            if (gridYMin < defaultLowerGridBound) {
                gridYMin = defaultLowerGridBound;
            }
            if (gridYMax > (extendedHeight - 1)) {
                gridYMax = extendedHeight - 1;
            }

            String finalSizeExpression = "";

            if (wmsLayer.getWcpsCoverageMetadata().isXYGridOrder()) {
                subsetCollectionExpression += " [" + gridXMin + ":" + gridXMax
                        + ", " + gridYMin + ":" + gridYMax + "]";
                finalSizeExpression = ", [0:" + (width - 1) + ", 0:" + (height - 1) + "]";
            } else {
                subsetCollectionExpression += " [" + gridYMin + ":" + gridYMax
                        + ", " + gridXMin + ":" + gridXMax + "]";
                finalSizeExpression = ", [0:" + (height - 1) + ", 0:" + (width - 1) + "]";
            }

            subsetCollectionExpression = "scale( " + subsetCollectionExpression + finalSizeExpression + " )";
            finalCollectionExpressionLayer = subsetCollectionExpression;
        }

        if (needExtend) {
            subsetCollectionExpression = "scale( " + subsetCollectionExpression + ", [" + scaleX + ", " + scaleY + "] )";
            finalCollectionExpressionLayer = EXTEND + "( " + subsetCollectionExpression + ", [" + extendX + ", " + extendY + "] )";

        }
        
        if (invalidQuery) {
            // return transparent image
            finalCollectionExpressionLayer = backgroundColorFragment;
        }
        
        return finalCollectionExpressionLayer;
    }

    /**
     * Using feature of project() to scale subsetting expression and then
     * transform result from nativeCRS to outputCRS.
     */
    public String createGridScalingOutputProjection(String nativeCRS, String subsetCollectionExpression, 
                                                    WMSLayer wmsLayer,
                                                    BoundingBox originalRequestBBox,
                                                    String outputCRS,
                                                    String interpolation) throws PetascopeException {
        
        // e.g. EPSG:4326 or WKT of COSMO:101
        String sourceCRS = CrsTransformHandler.getEscapedAuthorityEPSGCodeOrWKT(nativeCRS);
        String targetCRS = CrsTransformHandler.getEscapedAuthorityEPSGCodeOrWKT(outputCRS);
        
        // NOTE: this one is in coverage's native CRS (e.g: EPSG:32632), while originalRequestBBox is in request CRS (e.g: EPSG:4326)
        BoundingBox extendedAlignedFittedGeoBBbox = wmsLayer.getExtendedAlignedRequestBBox();

        String finalCollectionExpressionLayer = PROJECT_TEMPLATE.replace(COLLECTION_EXPRESSION_TEMPLATE, subsetCollectionExpression)
                .replace(XMIN_NATIVCE_CRS, extendedAlignedFittedGeoBBbox.getXMin().toPlainString())
                .replace(YMIN_NATIVCE_CRS, extendedAlignedFittedGeoBBbox.getYMin().toPlainString())
                .replace(XMAX_NATIVCE_CRS, extendedAlignedFittedGeoBBbox.getXMax().toPlainString())
                .replace(YMAX_NATIVCE_CRS, extendedAlignedFittedGeoBBbox.getYMax().toPlainString())
                .replace(NATIVE_CRS, sourceCRS)
                .replace(XMIN_OUTPUT_CRS, originalRequestBBox.getXMin().toPlainString())
                .replace(YMIN_OUTPUT_CRS, originalRequestBBox.getYMin().toPlainString())
                .replace(XMAX_OUTPUT_CRS, originalRequestBBox.getXMax().toPlainString())
                .replace(YMAX_OUTPUT_CRS, originalRequestBBox.getYMax().toPlainString())
                .replace(OUTPUT_CRS, targetCRS)
                .replace(WIDTH, wmsLayer.getWidth().toString())
                .replace(HEIGHT, wmsLayer.getHeight().toString())
                .replace(RESAMPLE_ALG, interpolation)
                .replace(ERR_THRESHOLD, DEFAULT_ERR_THRESHOLD);

        return finalCollectionExpressionLayer;
    }

}
