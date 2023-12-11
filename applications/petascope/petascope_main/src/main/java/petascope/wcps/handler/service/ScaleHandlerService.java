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
package petascope.wcps.handler.service;

import com.rasdaman.accesscontrol.service.AuthenticationService;
import org.rasdaman.config.ConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.core.Pair;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.WCPSException;
import petascope.util.BigDecimalUtil;
import petascope.util.CrsUtil;
import petascope.util.ras.RasUtil;
import petascope.wcps.exception.processing.InvalidScaleExtentException;
import petascope.wcps.exception.processing.ScaleValueLessThanZeroException;
import petascope.wcps.metadata.model.*;
import petascope.wcps.metadata.service.CollectionAliasRegistry;
import petascope.wcps.metadata.service.WcpsCoverageMetadataGeneralService;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.result.WcpsResult;
import petascope.wcps.subset_axis.model.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static petascope.util.CrsUtil.GRID_CRS;

@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ScaleHandlerService {

    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    CollectionAliasRegistry collectionAliasRegistry;
    @Autowired
    private WcpsCoverageMetadataGeneralService wcpsCoverageMetadataService;



    // ---- Utility functions


    /**
     * Check if scaling axis exists in the input coverage
     */
    public void validateAxisLabelExist(WcpsCoverageMetadata metadata, String scaleAxisLabel) {
        List<Axis> coverageAxes = metadata.getAxes();

        boolean result = false;
        for (Axis axis : coverageAxes) {
            if (CrsUtil.axisLabelsMatch(axis.getLabel(), scaleAxisLabel)) {
                result = true;
                break;
            }
        }

        if (!result) {
            throw new WCPSException(ExceptionCode.InvalidAxisLabel, "Scaling axis label '" + scaleAxisLabel + "' does not exist in coverage '" + metadata.getCoverageName() + "'.");
        }
    }

    /**
     * Special case, only 1 X or Y axis specified, find the grid domain for another axis implicitly from the specified axis
     */
    public void handleScaleWithOnlyXorYAxis(WcpsResult coverageExpression, List<Subset> subsets, boolean implicitScaleByXorYAxis) {
        // e.g: for c in (test_mean_summer_airtemp) return encode(scale( c, { Long:"CRS:1"(0:10)} ), "png")
        Subset subset1 = subsets.get(0);
        BigDecimal lowerLimit1 = subset1.getNumericSubset().getLowerLimit();
        BigDecimal upperLimit1 = subset1.getNumericSubset().getUpperLimit();

        // e.g: Long axis has grid bounds: 30:50
        Axis axis1 = coverageExpression.getMetadata().getAxisByName(subset1.getAxisName());
        List<Axis> xyAxes = coverageExpression.getMetadata().getXYAxes();
        Axis axis2 = null;
        for (Axis axis : xyAxes) {
            if (!CrsUtil.axisLabelsMatch(axis.getLabel(), subset1.getAxisName())) {
                axis2 = axis;
                break;
            }
        }

        if (!implicitScaleByXorYAxis) {
            // NOTE: for example scaleextent() of WCS scale extension, it doesn't have this auto implicitly scale ratio by X or Y axis
            NumericTrimming numericTrimming = new NumericTrimming(axis2.getGridBounds().getLowerLimit(), axis2.getGridBounds().getUpperLimit());
            Subset subset2 = new Subset(numericTrimming, axis2.getNativeCrsUri(), axis2.getLabel());
            subsets.add(subset2);
            return;
        }

        BigDecimal gridDistance1 = axis1.getGridBounds().getUpperLimit().subtract(axis1.getGridBounds().getLowerLimit());
        if (gridDistance1.compareTo(BigDecimal.ZERO) == 0) {
            gridDistance1 = BigDecimal.ONE;
        }
        // scale ratio is: (10 - 0) / (50 - 30) = 10 / 20 = 0.5 (downscale)
        BigDecimal scaleRatio = BigDecimalUtil.divide(upperLimit1.subtract(lowerLimit1), gridDistance1);

        // Lat axis has grid bounds: 60:70
        // -> scale on Lat axis: 0:(70 - 60) * 0.5 = 0:5
        BigDecimal gridDistance2 = axis2.getGridBounds().getUpperLimit().subtract(axis2.getGridBounds().getLowerLimit());
        if (gridDistance2.compareTo(BigDecimal.ZERO) == 0) {
            gridDistance2 = BigDecimal.ONE;
        }
        BigDecimal lowerLimit2 = BigDecimal.ZERO;
        BigDecimal upperLimit2 = gridDistance2.multiply(scaleRatio);
        NumericTrimming numericTrimming = new NumericTrimming(lowerLimit2, upperLimit2);

        Subset subset2 = new Subset(numericTrimming, subset1.getCrs(), axis2.getLabel());
        subsets.add(subset2);
    }


    /**
     * Add each axis's grid domains which is not decleared in the scale's interval explicitly
     */
    public void addImplicitScaleGridIntervals(WcpsCoverageMetadata metadata, List<Subset> gridNumericSubsets) {
        for (Axis axis : metadata.getAxes()) {
            boolean exists = false;
            for (Subset subset : gridNumericSubsets) {
                if (CrsUtil.axisLabelsMatch(axis.getLabel(), subset.getAxisName())) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                NumericTrimming numericTrimming = new NumericTrimming(axis.getGridBounds().getLowerLimit(), axis.getGeoBounds().getUpperLimit());
                Subset subset = new Subset(numericTrimming, axis.getNativeCrsUri(), axis.getLabel());
                gridNumericSubsets.add(subset);
            }
        }
    }

    /**
     * Revert some values after applying subset from scale's intervals
     */
    public void revertAfterScale(WcpsCoverageMetadata metadata, List<Pair> geoBoundAxes, Map<String, List<BigDecimal>> directPositionsMap) {
        // Revert the changed axes' geo bounds as before applying scale subsets.
        // e.g: scale(c, {Lat:"CRS:1"(0:20), Long:"CRS:1"(0:20)} and before scale,
        // coverage has geo domains: Lat(-40, 40), Long(-30, 30), grid domains: Lat":CRS:1"(0:300), Long:"CRS:1"(0:200)
        // After scale, the geo domains are kept and grid domain will be: Lat":CRS1:"(0:20), Long:"CRS:1"(0:20)
        for (Axis axis : metadata.getAxes()) {
            for (Pair<String, NumericTrimming> pair : geoBoundAxes) {
                if (CrsUtil.axisLabelsMatch(axis.getLabel(), pair.fst)) {
                    axis.getGeoBounds().setLowerLimit(pair.snd.getLowerLimit());
                    axis.getGeoBounds().setUpperLimit(pair.snd.getUpperLimit());

                    this.wcpsCoverageMetadataService.updateGeoResolutionByGridBound(axis);
                }
            }

            if (ConfigManager.OGC_CITE_OUTPUT_OPTIMIZATION) {
                // NOTE: OGC CITE tests requires that all axes after scaling has grid domain starting at 0
                // e.g. SCALEEXTENT=Lat(10:20),Lon(100:200) then the grid bound of Lat should be(0:10)
                BigDecimal gridLowerBound = axis.getGridBounds().getLowerLimit();
                BigDecimal gridUpperBound = axis.getGridBounds().getUpperLimit();
                axis.setGridBounds(new NumericTrimming(BigDecimal.ZERO, gridUpperBound.subtract(gridLowerBound)));
            }
        }

        // Revert the direct positions for irregular axes to the ones before applying scaling intervals
        for (Axis axis : metadata.getAxes()) {
            if (axis instanceof IrregularAxis) {
                IrregularAxis irregularAxis = ((IrregularAxis)axis);
                List<BigDecimal> directPositions = directPositionsMap.get(axis.getLabel());
                irregularAxis.setDirectPositions(directPositions);
            }
        }
    }

    /**
     * For irregular axes, when scaling, the coefficients must be filtered (scale down, typical case) or added (scale up).
     *
     */
    public void applyScaleOnIrregularAxes(WcpsCoverageMetadata metadata, List<Pair> gridBoundAxes) {
        for (Axis axis : metadata.getAxes()) {
            for (Pair<String, NumericTrimming> pair : gridBoundAxes) {
                String axisLabel = pair.fst;
                if (axis instanceof IrregularAxis && CrsUtil.axisLabelsMatch(axis.getLabel(), axisLabel)) {
                    // e.g: [0:10]
                    NumericTrimming scaleGridTrimming = pair.snd;
                    long sourceGridLowerBound = scaleGridTrimming.getLowerLimit().longValue();
                    long sourceGridUpperBound = scaleGridTrimming.getUpperLimit().longValue();
                    long sourceGridPoints = sourceGridUpperBound - sourceGridLowerBound + 1;

                    // e.g: [0:4]
                    long destGridLowerBound = axis.getGridBounds().getLowerLimit().longValue();
                    long destGridUpperBound = axis.getGridBounds().getUpperLimit().longValue();
                    long destGridPoints = destGridUpperBound - destGridLowerBound + 1;

                    if (sourceGridPoints >= destGridPoints) {
                        // scale down [0:11] -> [0:3]
                        this.applyScaleDownOnIrregularAxis((IrregularAxis)axis, scaleGridTrimming);
                    } else {
                        // scale up [0:11] -> [0:300]
                        // e.g: before scale time("2001":"2010") has 6 coefficients: 2001, 2002, 2005, 2007, 2008, 2009, 2010 with grid [0:5]
                        //      after scale  time("2001":"2010") has 301 coefficients: 2001, ... 2010 with grid [0:300]
                        // @TODO: how to calculate the newly added coefficients in the middle of irregular axis?
                        if (CrsUtil.isGridCrs(axis.getNativeCrsUri())) {
                            throw new WCPSException(ExceptionCode.NoApplicableCode,
                                    "Cannot scale up on irregular axis '" + axisLabel + "', only scale down is supported.");
                        } else {
                            this.applyScaleUpOnIrregularAxisWithGridCRS((IrregularAxis) axis);
                        }
                    }

                }
            }
        }
    }

    /**
     * e.g: irregular time axis has 11 coefficients (time slices) with grid bounds [0:10] and scaling's grid interval is [0:3]
     * then after scaling, only 4 coefficients are left on time axis
     */
    private void applyScaleDownOnIrregularAxis(IrregularAxis axis, NumericTrimming sourceGridTrimming) {
        // e.g: [0:11]
        long sourceLowerBound = sourceGridTrimming.getLowerLimit().longValue();
        long sourceUpperBound = sourceGridTrimming.getUpperLimit().longValue();
        long sourceGridPoints = sourceUpperBound - sourceLowerBound;
        sourceLowerBound = 0;
        sourceUpperBound = sourceGridPoints;

        // e.g: scale to [0:3]
        long destLowerBound = axis.getGridBounds().getLowerLimit().longValue();
        long destUpperBound = axis.getGridBounds().getUpperLimit().longValue();
        long destGridPoints = destUpperBound - destLowerBound;
        destLowerBound = 0;
        destUpperBound = destGridPoints;

        BigDecimal scaleRatio = BigDecimalUtil.divide(new BigDecimal(sourceUpperBound - sourceLowerBound + 1), new BigDecimal(destUpperBound - destLowerBound + 1));
        BigDecimal realIndex = BigDecimal.ZERO;
        int intIndex = 0;
        List<BigDecimal> selectedCoefficients = new ArrayList<>();
        selectedCoefficients.add(axis.getDirectPositions().get(0));

        while (intIndex <= sourceUpperBound) {
            realIndex = realIndex.add(scaleRatio);
            intIndex = realIndex.intValue();

            if (intIndex <= sourceUpperBound) {
                BigDecimal coefficient = axis.getDirectPositions().get(intIndex);
                selectedCoefficients.add(coefficient);
            }
        }

        axis.setDirectPositions(selectedCoefficients);
    }

    /**
     * e.g: irregular time axis has 11 coefficients (time slices) with grid bounds [0:10] and scaling's grid interval is [0:3]
     * then after scaling, only 4 coefficients are left on time axis
     */
    private void applyScaleUpOnIrregularAxisWithGridCRS(IrregularAxis axis) {
        // e.g: scale grid domain to [0:10]
        long destLowerBound = axis.getGridBounds().getLowerLimit().longValue();
        long destUpperBound = axis.getGridBounds().getUpperLimit().longValue();
        long destGridPoints = destUpperBound - destLowerBound;
        axis.setDirectPositions(new ArrayList<>());

        for (int i = 0; i <= destGridPoints; i++) {
            axis.getDirectPositions().add(new BigDecimal(i));
        }
    }

    /**
     * Check if the coverage contains X and Y axes, but one only specifies
     * X or Y axis for scale()
     */
    public boolean processXOrYAxisImplicitly(WcpsCoverageMetadata metadata, List<Subset> numericSubsets) {
        if (metadata.hasXYAxes()) {
            // NOTE: in case
            Axis axisX = metadata.getXYAxes().get(0);
            Axis axisY = metadata.getXYAxes().get(1);
            List<Boolean> hasXYAxes = new ArrayList<>();

            for (Subset subset : numericSubsets) {
                if (CrsUtil.axisLabelsMatch(subset.getAxisName(), axisX.getLabel())
                        || CrsUtil.axisLabelsMatch(subset.getAxisName(), axisY.getLabel())) {
                    hasXYAxes.add(true);
                }

                if (hasXYAxes.size() == 2) {
                    break;
                }
            }

            // Coverage has X and Y axes, but user only specifies one of X or Y for the scale(), then the domain for the other axis
            // will be determined from the specified X/Y axis.
            if (hasXYAxes.size() == 1) {
                return true;
            }
        }

        return false;
    }



    // ---- WCS Scale extensions

    /**
     * e.g. WCS GetCoverage scaleFactor=0.5, then it means scale(c, {Lat:"CRS:1"(gridLo*0.5: gridHigh*0.5), Long:"CRS:1"(gridLo*0.5: gridHigh*0.5)}
     * or WCPS: scale(c, {i(3.5 - 2.5), j(avg(c) / 10)})
     *
     */
    public DimensionIntervalList createDimensionalIntervalListByScalingFactors(WcpsCoverageMetadata metadata,
                                                                               VisitorResult secondChildHandlerResult) throws PetascopeException {

        WcpsScaleDimensionIntevalList scaleDimensionIntevalList;

        if (secondChildHandlerResult instanceof WcpsResult) {
            // Scale by 1 factor
            List<AbstractWcpsScaleDimension> elementsList = new ArrayList<>();
            for (Axis axis : metadata.getAxes()) {
                WcpsSliceScaleDimension element = new WcpsSliceScaleDimension(axis.getLabel(), ((WcpsResult)secondChildHandlerResult).getRasql());
                elementsList.add(element);
            }

            scaleDimensionIntevalList = new WcpsScaleDimensionIntevalList(elementsList);
        } else {
            // Scale by a list of axisLabel(factor) - NOTE: WCS called: scaleaxes()
            scaleDimensionIntevalList = (WcpsScaleDimensionIntevalList)secondChildHandlerResult;

            for (Axis axis : metadata.getAxes()) {
                boolean exist = false;
                for (AbstractWcpsScaleDimension element : scaleDimensionIntevalList.getIntervals()) {
                    if (CrsUtil.axisLabelsMatch(element.getAxisName(), axis.getLabel())) {
                        exist = true;
                        break;
                    }
                }

                if (!exist) {
                    // Other axes are not mentioned -> keep the same ration
                    scaleDimensionIntevalList.getIntervals().add(new WcpsSliceScaleDimension(axis.getLabel(), "1"));
                }
            }
        }

        Map<String, BigDecimal> axisScaleFactorsMap = new LinkedHashMap<>();

        // Store the processed scale factors, so no need to ask rasdaman multiple times if they are the same expression, e.g. scale(c, (3 + 5))
        Map<String, BigDecimal> resultsMapTmp = new LinkedHashMap<>();

        for (AbstractWcpsScaleDimension scaleDimension : scaleDimensionIntevalList.getIntervals()) {

            String scaleFactorStr = ((WcpsSliceScaleDimension)scaleDimension).getBound();
            BigDecimal scaleFactor = resultsMapTmp.get(scaleFactorStr);

            if (scaleFactor == null) {
                if (BigDecimalUtil.isNumber(scaleFactorStr)) {
                    scaleFactor = new BigDecimal(scaleFactorStr);
                } else {
                    // e.g. avg(c)
                    // NOTE: this is required, to get the correct scale factor by calculating it from rasdaman
                    // then to update the grid domains of the current coverage object c after scale(c, avg(c))
                    Pair<String, String> rasUserCredentialsPair = AuthenticationService.getRasUserCredentials(this.httpServletRequest);

                    String query = "SELECT " + scaleFactorStr + " " + this.collectionAliasRegistry.getFromClause();
                    String result = RasUtil.executeQueryToReturnString(query, rasUserCredentialsPair.fst, rasUserCredentialsPair.snd);
                    scaleFactor = new BigDecimal(result);
                }
            }

            String axisLabel = scaleDimension.getAxisName();
            axisScaleFactorsMap.put(axisLabel, scaleFactor);

            resultsMapTmp.put(scaleFactorStr, scaleFactor);
        }


        List<WcpsSubsetDimension> wcpsSubsetDimensions = new ArrayList<>();
        List<Pair> gridBoundAxes = new ArrayList();

        for (Map.Entry<String, BigDecimal> entry : axisScaleFactorsMap.entrySet()) {
            String axisLabel = entry.getKey();
            BigDecimal scaleFactor = entry.getValue();

            if (scaleFactor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ScaleValueLessThanZeroException(axisLabel, scaleFactor.toPlainString());
            }

            Axis axis = metadata.getAxisByName(axisLabel);

            NumericTrimming gridNumericTrimming = new NumericTrimming(new BigDecimal(axis.getGridBounds().getLowerLimit().toPlainString()),
                    new BigDecimal(axis.getGridBounds().getUpperLimit().toPlainString()));
            Pair<String, NumericTrimming> gridPair = new Pair(axis.getLabel(), gridNumericTrimming);
            gridBoundAxes.add(gridPair);

            String scaledLowerBound = String.valueOf(BigDecimalUtil.multiple(axis.getGridBounds().getLowerLimit(), scaleFactor).longValue());
            String scaledUpperBound = String.valueOf(BigDecimalUtil.multiple(axis.getGridBounds().getUpperLimit(), scaleFactor).longValue());

            wcpsSubsetDimensions.add(new WcpsTrimSubsetDimension(axisLabel, GRID_CRS, scaledLowerBound, scaledUpperBound));
        }


        DimensionIntervalList result = new DimensionIntervalList(wcpsSubsetDimensions);
        return result;
    }


    /**
     * e.g. WCS GetCoverage scaleExtent=E(0,10) then it means scale(c, {E:"CRS:1"(0:10)})
     */
    public DimensionIntervalList createDimensionIntervalListByScalingExtends(WcpsCoverageMetadata metadata, Object secondChildHandlerResult) {
        List<WcpsSubsetDimension> wcpsSubsetDimensions = new ArrayList<>();

        WcpsScaleDimensionIntevalList scaleDimensionIntevalList = (WcpsScaleDimensionIntevalList) secondChildHandlerResult;

        for (AbstractWcpsScaleDimension dimension : scaleDimensionIntevalList.getIntervals()) {
            Axis axis = metadata.getAxisByName(dimension.getAxisName());

            // NOTE: scaleextent must be low:hi
            if (dimension instanceof WcpsSliceScaleDimension) {
                throw new InvalidScaleExtentException(axis.getLabel(), ((WcpsSliceScaleDimension) dimension).getBound());
            }

            WcpsTrimScaleDimension trimScaleDimension = ((WcpsTrimScaleDimension) dimension);
            // e.g scale_extent(Lat, [10:20]) -> output in grid domain of Lat is: [0:10]
            Long lowerBound = 0l;
            Long upperBound = new BigDecimal(trimScaleDimension.getUpperBound()).longValue()
                            - new BigDecimal(trimScaleDimension.getLowerBound()).longValue();
            WcpsTrimSubsetDimension trimSubsetDimension = new WcpsTrimSubsetDimension(axis.getLabel(), GRID_CRS,
                    lowerBound.toString(), upperBound.toString());

            wcpsSubsetDimensions.add(trimSubsetDimension);
        }



        DimensionIntervalList result = new DimensionIntervalList(wcpsSubsetDimensions);
        return result;
    }

    /**
     * e.g. WCS GetCoverage scaleSize=E(10) then it means scale(c, {E:"CRS:1"(0:9)})
     */
    public DimensionIntervalList createDimensionalIntervalListByScalingSizes(WcpsCoverageMetadata metadata, Object secondChildHandlerResult) {
        List<WcpsSubsetDimension> wcpsSubsetDimensions = new ArrayList<>();

        WcpsScaleDimensionIntevalList scaleDimensionIntevalList = (WcpsScaleDimensionIntevalList) secondChildHandlerResult;

        for (AbstractWcpsScaleDimension dimension : scaleDimensionIntevalList.getIntervals()) {
            Axis axis = metadata.getAxisByName(dimension.getAxisName());
            String axisLabel = axis.getLabel();

            if (dimension instanceof WcpsTrimScaleDimension) {
                throw new WCPSException(ExceptionCode.InvalidRequest, "scaleSize for axis '" + axisLabel + "' must be a slice, not a trim.");
            }

            WcpsSliceScaleDimension sliceScaleDimension = ((WcpsSliceScaleDimension) dimension);
            // e.g scaleSize=Lat(10) -> output in grid domain of Lat is: [0:9]
            Long lowerBound = 0l;
            Long upperBound = new BigDecimal(sliceScaleDimension.getBound()).longValue() - 1L;

            if (upperBound < 0) {
                throw new WCPSException(ExceptionCode.InvalidRequest,
                        "Scaling value for axis '"  + axisLabel + "' must be > 0. Given: " + sliceScaleDimension.getBound());
            }

            WcpsTrimSubsetDimension trimSubsetDimension = new WcpsTrimSubsetDimension(axis.getLabel(), GRID_CRS,
                    lowerBound.toString(), upperBound.toString());

            wcpsSubsetDimensions.add(trimSubsetDimension);

        }


        DimensionIntervalList result = new DimensionIntervalList(wcpsSubsetDimensions);
        return result;
    }
}
