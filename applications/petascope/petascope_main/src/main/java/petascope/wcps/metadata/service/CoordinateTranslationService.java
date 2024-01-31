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
package petascope.wcps.metadata.service;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import petascope.util.BigDecimalUtil;
import petascope.wcps.metadata.model.ParsedSubset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import petascope.core.BoundingBox;
import petascope.core.GeoTransform;
import petascope.core.Pair;
import petascope.wcps.metadata.model.IrregularAxis;
import petascope.core.service.CrsComputerService;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.util.CrsUtil;
import petascope.util.TimeUtil;
import petascope.wcps.exception.processing.IrreguarAxisCoefficientNotFoundException;
import petascope.wcps.exception.processing.IrregularAxisTrimmingCoefficientNotFoundException;
import petascope.wcps.metadata.model.Axis;
import petascope.wcps.metadata.model.NumericSubset;
import petascope.wcps.metadata.model.RegularAxis;
import petascope.wcps.subset_axis.model.WcpsSliceSubsetDimension;
import petascope.wcps.subset_axis.model.WcpsSliceTemporalSubsetDimension;
import petascope.wcps.subset_axis.model.WcpsSubsetDimension;
import petascope.wcps.subset_axis.model.WcpsTrimSubsetDimension;

/**
 * Translate the coordinates from geo bound to grid bound for trimming/slicing and vice versa if using CRS:1 in trimming/slicing
 * i.e: Lat(0:20) ->
 * @author <a href="merticariu@rasdaman.com">Vlad Merticariu</a>
 * @author <a href="mailto:b.phamhuu@jacobs-university.de">Bang Pham Huu</a>
 */
@Service
// Create a new instance of this bean for each request (so it will not use the old object with stored data)
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CoordinateTranslationService {
    
    private final BigDecimal GDAL_EPSILON_MIN = new BigDecimal("0.001");
    private final BigDecimal GDAL_EPSILON_MAX = new BigDecimal("0.5");
    
    // ----------------------------------- X Axis 
    
    /**
     * From a geo bound on X axis -> grid index on X axis
     */
    private Long getGridXIndex(BigDecimal geoSliceX, BigDecimal geoResolutionX, BigDecimal geoAxisYMin) {
        BigDecimal gridIndexXApproximate = BigDecimalUtil.divide(geoSliceX.subtract(geoAxisYMin),
                                                                      geoResolutionX);
        Long gridIndexX = BigDecimalUtil.shiftToInteger(gridIndexXApproximate);    
        
        return gridIndexX;
    }
    
    
    /**
     * From a geo bounds for X axis (e.g: lon1:lon2]
     * return the grid bounds of the input geo bounds
     */
    private Pair<Long, Long> calculateGridXBounds(GeoTransform adfGeoTransform, BigDecimal geoSubsetXMin, BigDecimal geoSubsetXMax) {
        BigDecimal geoAxisYMin = adfGeoTransform.getUpperLeftGeoX();
        
        BigDecimal shiftedGeoLowerBoundX = geoSubsetXMin.add(new BigDecimal("0.5").multiply(adfGeoTransform.getGeoXResolution()));
        Long gridLowerBoundX = this.getGridXIndex(shiftedGeoLowerBoundX, adfGeoTransform.getGeoXResolution(), geoAxisYMin);
        
        BigDecimal shiftedGeoUpperBoundX = geoSubsetXMax.subtract(new BigDecimal("0.5").multiply(adfGeoTransform.getGeoXResolution()));
        Long gridUpperBoundX = this.getGridXIndex(shiftedGeoUpperBoundX, adfGeoTransform.getGeoXResolution(), geoAxisYMin);
        
        return new Pair<>(gridLowerBoundX, gridUpperBoundX);
    }
    
    /**
     * Get a pair of lower:upper bounds for geo and grid by geo trimming subset of X axis
     */
    public Pair<ParsedSubset<BigDecimal>, ParsedSubset<Long>> calculateGeoGridXBounds(boolean checkBoundary, boolean checkGridBound, Axis axisX, GeoTransform adfGeoTransform, 
                                                                                      BigDecimal geoXMinSubset, BigDecimal geoXMaxSubset) {
        Pair<Long, Long> gridPair = this.calculateGridXBounds(adfGeoTransform, geoXMinSubset, geoXMaxSubset);
        
        Long gridXMin = gridPair.fst;
        BigDecimal girdLowerBoundDecimal = new BigDecimal(gridXMin);
        // NOTE: gird bound origin can be negative (in case of mosaic files), not always 0
        gridXMin = axisX.getGridBounds().getLowerLimit().longValue() + gridXMin;

        Long gridXMax = gridPair.snd;
        gridXMax = axisX.getGridBounds().getLowerLimit().longValue() + gridXMax;

        Long totalPixels = gridXMax - gridXMin + 1L;
        
        if (checkGridBound) {
            gridXMin = gridXMin > axisX.getGridBounds().getLowerLimit().longValue() ? gridXMin : axisX.getGridBounds().getLowerLimit().longValue();
            gridXMax = gridXMax < axisX.getGridBounds().getUpperLimit().longValue() ? gridXMax : axisX.getGridBounds().getUpperLimit().longValue();
        }
        
        if (gridXMin > gridXMax) {
            gridXMin = gridXMax;
        }        

        BigDecimal updatedGeoXMin = adfGeoTransform.getUpperLeftGeoXDecimal().add(girdLowerBoundDecimal.multiply(adfGeoTransform.getGeoXResolution()));
        BigDecimal updatedGeoXMax = updatedGeoXMin.add(adfGeoTransform.getGeoXResolution().multiply(new BigDecimal(totalPixels.toString())));
        
        ParsedSubset<Long> gridSubset = new ParsedSubset<>(gridXMin, gridXMax);
        ParsedSubset<BigDecimal> geoSubset = new ParsedSubset<>(updatedGeoXMin, updatedGeoXMax);
        
        return new Pair<>(geoSubset, gridSubset);
    }
    
    
    
    // ----------------------------------- Y Axis 
    
    /**
     * From a geo bound on Y axis -> grid index on Y axis
     */
    private Long getGridYIndex(BigDecimal geoSliceY, BigDecimal geoResolutionY, BigDecimal geoAxisYMax) {
        BigDecimal gridIndexYApproximate = BigDecimalUtil.divide(geoAxisYMax.subtract(geoSliceY),
                                                                      geoResolutionY.abs());
        Long gridIndexY = BigDecimalUtil.shiftToInteger(gridIndexYApproximate);    
        
        return gridIndexY;
    }
    
    /**
     * From a geo bounds for Y axis (e.g: lat1:lat2]
     * return the grid bounds of the input geo bounds.
     * 
     * NOTE: Y axis, grid coordinate 0 is at the top (geoYMax), while geoYMin is at the bottom of grid coordinate (i.e. dom(griY).hi)
     */
    private Pair<Long, Long> calculateGridYBounds(GeoTransform adfGeoTransform, BigDecimal geoSubsetYMin, BigDecimal geoYMaxSubset) {
        BigDecimal geoAxisYMax = adfGeoTransform.getUpperLeftGeoY();
        
        BigDecimal shiftedGeoUpperBoundY = geoYMaxSubset.subtract(new BigDecimal("0.5").multiply(adfGeoTransform.getGeoYResolution().abs()));
        Long gridLowerBoundY = this.getGridYIndex(shiftedGeoUpperBoundY, adfGeoTransform.getGeoYResolution(), geoAxisYMax);
        
        BigDecimal shiftedGeoLowerBoundY = geoSubsetYMin.add(new BigDecimal("0.5").multiply(adfGeoTransform.getGeoYResolution().abs()));
        Long gridUpperBoundY = this.getGridYIndex(shiftedGeoLowerBoundY, adfGeoTransform.getGeoYResolution(), geoAxisYMax);
        
        return new Pair<>(gridLowerBoundY, gridUpperBoundY);
    }
    
    /**
     * Get a pair of lower:upper bounds for geo and grid by geo trimming subset of Y axis
     */
    public Pair<ParsedSubset<BigDecimal>, ParsedSubset<Long>> calculateGeoGridYBounds(boolean checkBoundary, boolean checkGridBound, Axis axisY, GeoTransform adfGeoTransform, 
                                                                                      BigDecimal geoYMinSubset, BigDecimal geoYMaxSubset) {
        Pair<Long, Long> gridPair = this.calculateGridYBounds(adfGeoTransform, geoYMinSubset, geoYMaxSubset);
        
        Long gridYMin = gridPair.fst;
        BigDecimal girdLowerBoundDecimal = new BigDecimal(gridYMin);

        // NOTE: gird bound origin can be negative (in case of mosaic files), not always 0
        gridYMin = axisY.getGridBounds().getLowerLimit().longValue() + gridYMin;

        Long gridYMax = gridPair.snd;
        gridYMax = axisY.getGridBounds().getLowerLimit().longValue() + gridYMax;

        Long totalPixels = gridYMax - gridYMin + 1L;
        
        if (checkGridBound) {
            gridYMin = gridYMin > axisY.getGridBounds().getLowerLimit().longValue() ? gridYMin : axisY.getGridBounds().getLowerLimit().longValue();
            gridYMax = gridYMax < axisY.getGridBounds().getUpperLimit().longValue() ? gridYMax : axisY.getGridBounds().getUpperLimit().longValue();
        }
        
        if (gridYMin.compareTo(gridYMax) > 0) {
            gridYMin = gridYMax;
        }
        
        BigDecimal updatedGeoYMax = adfGeoTransform.getUpperLeftGeoY().add(girdLowerBoundDecimal.multiply(adfGeoTransform.getGeoYResolutionDecimal()));
        BigDecimal updatedGeoYMin = updatedGeoYMax.add(adfGeoTransform.getGeoYResolutionDecimal().multiply(new BigDecimal(totalPixels.toString())));
        
        ParsedSubset<Long> gridSubset = new ParsedSubset<>(gridYMin, gridYMax);
        ParsedSubset<BigDecimal> geoSubset = new ParsedSubset<>(updatedGeoYMin, updatedGeoYMax);
        
        return new Pair<>(geoSubset, gridSubset);
    }
    
    /**
     * Return a pair of geo / grid bboxes as same as gdal from an input bbox on XY axes
     */
    public Pair<BoundingBox, BoundingBox> calculateGridGeoXYBoundingBoxes(boolean checkBoundary, boolean checkGridBound, Axis axisX, Axis axisY, BoundingBox inputBBoxNativeCRS) throws PetascopeException {
        String sourceCRS = axisX.getNativeCrsUri();
        String sourceCRSWKT = CrsUtil.getWKT(sourceCRS);
        
        // axis X
        int gridWidth = axisX.getTotalNumberOfGridPixels();

        GeoTransform adfGeoTransformX = new GeoTransform(sourceCRSWKT, axisX.getGeoBounds().getLowerLimit(), BigDecimal.ZERO,
                                                         gridWidth, 0, axisX.getResolution(), BigDecimal.ZERO);
        Pair<ParsedSubset<BigDecimal>, ParsedSubset<Long>> pairX = this.calculateGeoGridXBounds(checkBoundary, checkGridBound, axisX, adfGeoTransformX,
                                                                                                inputBBoxNativeCRS.getXMin(), inputBBoxNativeCRS.getXMax());
        
        // axis Y
        int gridHeight = axisY.getTotalNumberOfGridPixels();

        GeoTransform adfGeoTransformY = new GeoTransform(sourceCRSWKT, BigDecimal.ZERO, axisY.getGeoBounds().getUpperLimit(),
                                                         0, gridHeight, BigDecimal.ZERO, axisY.getResolution());
        Pair<ParsedSubset<BigDecimal>, ParsedSubset<Long>> pairY = this.calculateGeoGridYBounds(checkBoundary, checkGridBound, axisY, adfGeoTransformY,
                                                                                                inputBBoxNativeCRS.getYMin(), inputBBoxNativeCRS.getYMax());
        
        BoundingBox geoBBox = new BoundingBox(new BigDecimal(pairX.fst.getLowerLimit().toString()), new BigDecimal(pairY.fst.getLowerLimit().toString()),
                                               new BigDecimal(pairX.fst.getUpperLimit().toString()), new BigDecimal(pairY.fst.getUpperLimit().toString()));
        
        
        BoundingBox gridBBox = new BoundingBox(new BigDecimal(pairX.snd.getLowerLimit().toString()), new BigDecimal(pairY.snd.getLowerLimit().toString()),
                                               new BigDecimal(pairX.snd.getUpperLimit().toString()), new BigDecimal(pairY.snd.getUpperLimit().toString()));
        
        if (gridBBox.getXMin().compareTo(gridBBox.getXMax()) > 0) {
            gridBBox.setXMax(new BigDecimal(gridBBox.getXMin().toPlainString()));
        }
        if (gridBBox.getYMin().compareTo(gridBBox.getYMax()) > 0) {
            gridBBox.setYMax(new BigDecimal(gridBBox.getYMax().toPlainString()));
        }
        
        return new Pair<>(geoBBox, gridBBox);
    }
    
    /**
     * From a geo XY bounding box with geo bounds calculates an adjusted XY bounding box with geo bounds.
     */
    public BoundingBox calculageGeoXYBoundingBox(boolean checkBoundary, boolean checkGridBound, Axis axisX, Axis axisY, BoundingBox inputBBoxNativeCRS) throws PetascopeException {
        BoundingBox geoBBox = this.calculateGridGeoXYBoundingBoxes(checkBoundary, checkGridBound, axisX, axisY, inputBBoxNativeCRS).fst;
        return geoBBox;
    }
    
     /**
     * From a geo XY bounding box with geo bounds calculates a grid XY bounding box with grid bounds.
     */
    public BoundingBox calculageGridXYBoundingBox(boolean checkBoundary, boolean checkGridBound, Axis axisX, Axis axisY, BoundingBox inputBBoxNativeCRS) throws PetascopeException {
        BoundingBox gridBBox = this.calculateGridGeoXYBoundingBoxes(checkBoundary, checkGridBound, axisX, axisY, inputBBoxNativeCRS).snd;
        return gridBBox;
    }
    
    /**
     * Translate a geo subset on an axis to grid subset accordingly.
     * e.g: Lat(0:20) -> c[10:15]
     */
    public ParsedSubset<Long> geoToGridSpatialDomain(Axis axis, WcpsSubsetDimension subsetDimension, ParsedSubset<BigDecimal> parsedGeoSubset) throws PetascopeException {
        ParsedSubset<Long> parsedGridSubset;
        if (axis instanceof RegularAxis) {
            parsedGridSubset = this.geoToGridForRegularAxis(parsedGeoSubset, axis.getGeoBounds().getLowerLimit(),
                                                            axis.getGeoBounds().getUpperLimit(), axis.getResolution(), axis.getGridBounds().getLowerLimit(), axis.getGridBounds().getUpperLimit());
        } else {
            parsedGridSubset = this.geoToGridForIrregularAxes(subsetDimension, parsedGeoSubset, axis.getResolution(), axis.getGridBounds().getLowerLimit(), 
                                                            axis.getGridBounds().getUpperLimit(), axis.getGeoBounds().getLowerLimit(), (IrregularAxis)axis);
        }
        
        return parsedGridSubset;
    }
    
    
    /**
     * Computes the pixel indices for a subset on a regular axis.
     *
     * @param numericSubset:     the geo subset to be converted to pixel indices.
     * @param geoDomainMin:      the geo minimum on the axis.
     * @param geoDomainMax:      the geo maximum on the axis.
     * @param resolution:        the signed cell width (negative if the axis is linear negative)
     * @param gridDomainMin:     the grid coordinate of the first pixel of the axis
     * @return the pair of grid coordinates corresponding to the given geo subset.
     */
    public ParsedSubset<Long> geoToGridForRegularAxis(ParsedSubset<BigDecimal> numericSubset, BigDecimal geoDomainMin,
        BigDecimal geoDomainMax, BigDecimal resolution, BigDecimal gridDomainMin, BigDecimal gridDomainMax) {
        boolean zeroIsMin = resolution.compareTo(BigDecimal.ZERO) > 0;
        
        BigDecimal lowerBound = numericSubset.getLowerLimit();
        BigDecimal upperBound = numericSubset.getUpperLimit();
        
        if (numericSubset.isSlicing()) {
            lowerBound = numericSubset.getSlicingCoordinate();
            upperBound = numericSubset.getSlicingCoordinate();
        }

        boolean isSlicing = false;
        if (lowerBound.equals(upperBound)) {
            isSlicing = true;
        }

        BigDecimal returnLowerLimit, returnUpperLimit;
        if (zeroIsMin) {
            // closed interval on the lower limit, open on the upper limit - use floor and ceil - 1 repsectively
            // e.g: Long(0:20) -> c[0:50]
            BigDecimal lowerLimit = BigDecimalUtil.divide(lowerBound.subtract(geoDomainMin), resolution);
            if (!isSlicing) {
                lowerLimit = CrsComputerService.shiftToNearestGridPointWCPS(lowerLimit);
            }
            returnLowerLimit = lowerLimit.setScale(0, RoundingMode.FLOOR).add(gridDomainMin);
            
            BigDecimal upperLimit = BigDecimalUtil.divide(upperBound.subtract(geoDomainMin), resolution);
            if (!isSlicing) {
                upperLimit = CrsComputerService.shiftToNearestGridPointWCPS(upperLimit);
            }

            if (upperLimit.equals(lowerLimit)) {
                returnUpperLimit = upperLimit.add(gridDomainMin);
            } else {
                returnUpperLimit = upperLimit.setScale(0, RoundingMode.CEILING).subtract(BigDecimal.ONE).add(gridDomainMin);
            }

        } else {
            // Linear negative axis (eg northing of georeferenced images)
            // First coordHi, so that left-hand index is the lower one
            // e.g: axis with 4 pixels in rasdaman, geo limits are 80 and 0, res = -20.
            // ras:    0   1   2   3
            //        --- --- --- ---
            // geo:  80  60  40  20  0
            // user subset 58: count how many resolution-sized interval are between 80 and 58 (1.1), and floor it to get 1
            BigDecimal lowerLimit = BigDecimalUtil.divide(upperBound.subtract(geoDomainMax), resolution);
            if (!isSlicing) {
                lowerLimit = CrsComputerService.shiftToNearestGridPointWCPS(lowerLimit);
            }
            returnLowerLimit = lowerLimit.setScale(0, RoundingMode.FLOOR).add(gridDomainMin);
            
            BigDecimal upperLimit = BigDecimalUtil.divide(lowerBound.subtract(geoDomainMax), resolution);
            if (!isSlicing) {
                upperLimit = CrsComputerService.shiftToNearestGridPointWCPS(upperLimit);
            }

            if (upperLimit.equals(lowerLimit)) {
                returnUpperLimit = upperLimit.add(gridDomainMin);
            } else {
                returnUpperLimit = upperLimit.setScale(0, RoundingMode.CEILING).subtract(BigDecimal.ONE).add(gridDomainMin);
            }

        }
        
        if (returnLowerLimit.compareTo(gridDomainMin.subtract(BigDecimal.ONE)) == 0) {
            returnLowerLimit = gridDomainMin;
        }
        if (returnUpperLimit.compareTo(gridDomainMin.subtract(BigDecimal.ONE)) == 0) {
            returnUpperLimit = gridDomainMin;
        }

        if (gridDomainMax != null) {
            if (returnLowerLimit.compareTo(gridDomainMax.add(BigDecimal.ONE)) == 0) {
                returnLowerLimit = gridDomainMax;
            }
            if (returnUpperLimit.compareTo(gridDomainMax.add(BigDecimal.ONE)) == 0) {
                returnUpperLimit = gridDomainMax;
            }
        }
        
        return new ParsedSubset(returnLowerLimit.longValue(), returnUpperLimit.longValue());
    }

    /**
     * Translate the  grid subset with grid CRS (i.e: CRS:1) to geo subset
     * e.g: Long:"CRS:1"(0:50) -> Long(0.5:20.5)
     * NOTE: no rounding for geo bounds as they should be not integer values
     * @param numericSubset
     * @param gridDomainMin
     * @param gridDomainMax
     * @param resolution
     * @param geoDomainMin
     * @return 
     */
    public static ParsedSubset<BigDecimal> gridToGeoForRegularAxis(ParsedSubset<BigDecimal> numericSubset, BigDecimal gridDomainMin,
            BigDecimal gridDomainMax, BigDecimal resolution, BigDecimal geoDomainMin) {
        boolean zeroIsMin = resolution.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal returnLowerLimit, returnUpperLimit;
        if (zeroIsMin) {
            // e.g: Long:"CRS:1"(0:50) -> Long(0.5:20.5)
            returnLowerLimit = BigDecimalUtil.multiple(numericSubset.getLowerLimit().subtract(gridDomainMin), resolution)
                               .add(geoDomainMin);
            returnUpperLimit = BigDecimalUtil.multiple(numericSubset.getUpperLimit().add(BigDecimal.ONE).subtract(gridDomainMin), resolution)
                               .add(geoDomainMin);

            // because we use ceil - 1, when values are close (less than 1 resolution dif), the upper will be pushed below the lower
            if (returnUpperLimit.compareTo(returnLowerLimit) < 0) {
                returnUpperLimit = returnLowerLimit;
            }
        } else {
            // Linear negative axis (eg northing of georeferenced images)
            // First coordHi, so that left-hand index is the lower one
            // e.g: Lat:"CRS:"(0:50) -> Lat(0.23:20.23)
            // (input grid - total pixels) / resolution + geoDomain, NOTE: total pixels + 1 (e.g: 0:710 then max is not: 0 but 711)
            returnLowerLimit = BigDecimalUtil.multiple(numericSubset.getUpperLimit().subtract(gridDomainMax), resolution)
                               .add(geoDomainMin);
            returnUpperLimit = BigDecimalUtil.multiple(numericSubset.getLowerLimit().subtract(BigDecimal.ONE).subtract(gridDomainMax), resolution)
                               .add(geoDomainMin);

            if (returnUpperLimit.compareTo(returnLowerLimit) < 0) {
                returnUpperLimit = returnLowerLimit;
            }
        }
        return new ParsedSubset(returnLowerLimit, returnUpperLimit);
    }

    /**
     * Returns the translated subset if the coverage has an irregular axis
     * This needs to be further refactored: the correct coefficients must be added in the WcpsCoverageMetadata object when a subset is done
     * on it, and the min and max coefficients should be passed to this method.
     *
     */
    public ParsedSubset<Long> geoToGridForIrregularAxes(WcpsSubsetDimension subsetDimension,
        ParsedSubset<BigDecimal> numericSubset, BigDecimal scalarResolution, BigDecimal gridDomainMin,
        BigDecimal gridDomainMax, BigDecimal geoDomainMin, IrregularAxis irregularAxis) throws PetascopeException {
        
        BigDecimal lowerLimit = null;
        BigDecimal upperLimit = null;
        String originalLowerBound = null;
        String originalUpperBound = null;

        if (subsetDimension instanceof WcpsTrimSubsetDimension) {
            lowerLimit = numericSubset.getLowerLimit();
            upperLimit = numericSubset.getUpperLimit();
            originalLowerBound = ((WcpsTrimSubsetDimension)subsetDimension).getLowerBound();
            originalUpperBound = ((WcpsTrimSubsetDimension)subsetDimension).getUpperBound();
        } else if (subsetDimension instanceof WcpsSliceSubsetDimension) {
            lowerLimit = numericSubset.getSlicingCoordinate();
            upperLimit = numericSubset.getSlicingCoordinate();
            originalLowerBound = ((WcpsSliceSubsetDimension)subsetDimension).getBound();
            originalUpperBound = ((WcpsSliceSubsetDimension)subsetDimension).getBound();
        } else {
            WcpsSliceTemporalSubsetDimension sliceTemporalSubsetDimension = (WcpsSliceTemporalSubsetDimension)subsetDimension;
            // special time slicing, here it has time granularity as lowerBound:upperBound
            lowerLimit = numericSubset.getLowerLimit();
            upperLimit = numericSubset.getUpperLimit();

            // in datetime format (e.g. "2023-01") bound input by user
            originalLowerBound = ((WcpsSliceTemporalSubsetDimension)subsetDimension).getOriginalBound();
        }


        // e.g: t(148654) in irr_cube_2
        NumericSubset originalGeoBounds = irregularAxis.getOriginalGeoBounds();
        BigDecimal originalGeoDomainMin = originalGeoBounds.getLowerLimit();
        BigDecimal originalGeoDomainMax = originalGeoBounds.getUpperLimit();
        if (originalGeoDomainMin.compareTo(originalGeoDomainMax) > 0) {
            originalGeoDomainMin = originalGeoDomainMax;
        }


        BigDecimal lowerCoefficient = (lowerLimit.subtract(irregularAxis.getCoefficientZeroValueAsNumber())).divide(scalarResolution);
        BigDecimal upperCoefficient = (upperLimit.subtract(irregularAxis.getCoefficientZeroValueAsNumber())).divide(scalarResolution);

        if (irregularAxis.isFlippedCoefficients()) {
            lowerCoefficient = (irregularAxis.getCoefficientZeroValueAsNumber().subtract(lowerLimit)).divide(scalarResolution);
            upperCoefficient = (irregularAxis.getCoefficientZeroValueAsNumber().subtract(upperLimit)).divide(scalarResolution);
        }

        // In case of flip(), lowerbound and upperbound are swapped
        BigDecimal originalLowerBoundNumber = originalGeoBounds.getLowerLimit().compareTo(originalGeoBounds.getUpperLimit()) < 0 
                                                ? originalGeoBounds.getLowerLimit() 
                                                : originalGeoBounds.getUpperLimit();
        BigDecimal originalUpperBoundNumber = originalGeoBounds.getUpperLimit().compareTo(originalGeoBounds.getLowerLimit()) > 0 
                                        ? originalGeoBounds.getUpperLimit()
                                        : originalGeoBounds.getLowerLimit();
        
        if (lowerLimit.compareTo(originalLowerBoundNumber) < 0) {
            String subsetGeoLowerBound = lowerLimit.toPlainString();
            String axisGeoLowerBound = originalLowerBoundNumber.toPlainString();
            
            if (irregularAxis.isTimeAxis()) {
                subsetGeoLowerBound = TimeUtil.valueToISODateTime(BigDecimal.ZERO, lowerLimit, irregularAxis.getCrsDefinition());
                axisGeoLowerBound = TimeUtil.valueToISODateTime(BigDecimal.ZERO, originalLowerBoundNumber, irregularAxis.getCrsDefinition());
            }
            
            throw new PetascopeException(ExceptionCode.InvalidRequest, 
                    "Request lower bound: " + subsetGeoLowerBound + " must be greater than the axis geo lower bound: " + axisGeoLowerBound);
        } else if (upperLimit.compareTo(originalUpperBoundNumber) > 0) {
            String subsetGeoUpperBound = upperLimit.toPlainString();
            String axisGeoUpperBound = originalUpperBoundNumber.toPlainString();
            
            if (irregularAxis.isTimeAxis()) {
                subsetGeoUpperBound = TimeUtil.valueToISODateTime(BigDecimal.ZERO, upperLimit, irregularAxis.getCrsDefinition());
                axisGeoUpperBound = TimeUtil.valueToISODateTime(BigDecimal.ZERO, originalUpperBoundNumber, irregularAxis.getCrsDefinition());
            }
            
            throw new PetascopeException(ExceptionCode.InvalidRequest, 
                    "Request upper bound: " + subsetGeoUpperBound + " must be smaller than the axis geo upper bound: " + axisGeoUpperBound);
        }
        
        // Return the grid indices of the lower and upper coefficients in an irregular axis
        Pair<Long, Long> gridIndicePair = irregularAxis.getGridIndices(lowerCoefficient, upperCoefficient);
        Long gridLowerBound = gridIndicePair.fst;
        Long gridUpperBound = gridIndicePair.snd;

        if (gridLowerBound == null || gridUpperBound == null
            || gridLowerBound > gridUpperBound) {
            if (irregularAxis.isTimeAxis()) {
                originalLowerBound = TimeUtil.valueToISODateTime(BigDecimal.ZERO, lowerLimit, irregularAxis.getCrsDefinition());
                originalUpperBound = TimeUtil.valueToISODateTime(BigDecimal.ZERO, upperLimit, irregularAxis.getCrsDefinition());
            }
            
            throw new IrregularAxisTrimmingCoefficientNotFoundException(irregularAxis.getLabel(), originalLowerBound, originalUpperBound);
        }
        Pair<Long, Long> gridBoundsPair = irregularAxis.calculateGridBoundsByZeroCoefficientIndex(gridIndicePair.fst, gridIndicePair.snd);
        return new ParsedSubset(gridBoundsPair.fst, gridBoundsPair.snd);
    }
    
    /**
     * Translate grid subset to geo subset for irregular axis (e.g: time).
     * 1 grid coordinate is attached to 1 geo coefficient of irregular axis.
     */
    public static ParsedSubset<BigDecimal> gridToGeoForIrregularAxes(ParsedSubset<BigDecimal> numericSubset, IrregularAxis irregularAxis) {
        BigDecimal lowerBoundCoefficient = irregularAxis.getDirectPositions().get(numericSubset.getLowerLimit().intValue());
        BigDecimal geoLowerBound = irregularAxis.getOrigin().add(lowerBoundCoefficient);
        
        BigDecimal upperBoundCoefficient = irregularAxis.getDirectPositions().get(numericSubset.getUpperLimit().intValue());
        BigDecimal geoUpperBound = irregularAxis.getOrigin().add(upperBoundCoefficient);
        
        return new ParsedSubset(geoLowerBound, geoUpperBound);
    }
}
