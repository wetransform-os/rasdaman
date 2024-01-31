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
package petascope.wcps.metadata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import petascope.core.CrsDefinition;
import petascope.exceptions.PetascopeException;
import static petascope.core.AxisTypes.T_AXIS;

import petascope.util.ListUtil;
import petascope.core.Pair;
import petascope.exceptions.ExceptionCode;
import petascope.util.BigDecimalUtil;
import petascope.util.BigDecimalUtil.BigDecimalComparator;
import petascope.util.TimeUtil;
import petascope.wcps.exception.processing.IrregularAxisTrimmingCoefficientNotFoundException;

/**
 * @author <a href="merticariu@rasdaman.com">Vlad Merticariu</a>
 */
public class IrregularAxis extends Axis {

    // list of coefficients for irregular axis

    private List<BigDecimal> directPositions;
    private List<BigDecimal> originalDirectPositions;

    private List<BigDecimal> directPositionsAreaOfValidityStarts;
    private List<BigDecimal> originalDirectPositionsAreaOfValidityStarts;

    private List<BigDecimal> directPositionsAreaOfValidityEnds;
    private List<BigDecimal> originalDirectPositionsAreaOfValidityEnds;

    public IrregularAxis() {
        
    }

    public IrregularAxis(String label, NumericSubset geoBounds, NumericSubset originalGridBounds, NumericSubset gridBounds,
             String crsUri, CrsDefinition crsDefinition,
             String axisType, String axisUoM,
             int rasdamanOrder, BigDecimal origin, BigDecimal resolution,
             List<BigDecimal> directPositions,
             List<BigDecimal> directPositionsAreaOfValidityStarts,
             List<BigDecimal> directPositionsAreaOfValidityEnds,
             NumericSubset originalGeoBounds) {
        super(label, geoBounds, originalGridBounds, gridBounds, crsUri, crsDefinition, axisType, axisUoM, rasdamanOrder, origin, resolution, originalGeoBounds);
        this.directPositions = directPositions;

        this.directPositionsAreaOfValidityStarts = directPositionsAreaOfValidityStarts;
        this.directPositionsAreaOfValidityEnds = directPositionsAreaOfValidityEnds;

        this.setOriginalDirectPositions();
        this.setOriginalDirectPositionsAreaOfValidityStarts();
        this.setOriginalDirectPositionsAreaOfValidityEnds();
    }

    // ---------------- direct positions

    public List<BigDecimal> getDirectPositions() {
        return directPositions;
    }

    public void setDirectPositions(List<BigDecimal> directPositions) {
        this.directPositions = directPositions;
    }
    
    public List<BigDecimal> getOriginalDirectPositions() {
        return this.originalDirectPositions;
    }

    public void setOriginalDirectPositions() {
        this.originalDirectPositions = new ArrayList<>();

        for (BigDecimal value : this.directPositions) {
            this.originalDirectPositions.add(value);
        }

    }

    // --------------- direct positions area of validity start
    

    public List<BigDecimal> getDirectPositionsAreaOfValidityStarts() {
        return this.directPositionsAreaOfValidityStarts;
    }

    public void setDirectPositionsAreaOfValidityStarts(List<BigDecimal> directPositionsAreaOfValidityStarts) {
        this.directPositionsAreaOfValidityStarts = directPositionsAreaOfValidityStarts;
    }

    public List<BigDecimal> getOriginalDirectPositionsAreaOfValidityStarts() {
        return this.originalDirectPositionsAreaOfValidityStarts;
    }

    public void setOriginalDirectPositionsAreaOfValidityStarts() {
        this.originalDirectPositionsAreaOfValidityStarts = new ArrayList<>();

        if (this.directPositionsAreaOfValidityStarts != null) {
            for (BigDecimal value : this.directPositionsAreaOfValidityStarts) {
                this.originalDirectPositionsAreaOfValidityStarts.add(value);
            }
        }
    }


    // --------------- direct positions area of validity end


    public List<BigDecimal> getDirectPositionsAreaOfValidityEnds() {
        return this.directPositionsAreaOfValidityEnds;
    }

    public void setDirectPositionsAreaOfValidityEnds(List<BigDecimal> directPositionsAreaOfValidityEnds) {
        this.directPositionsAreaOfValidityEnds = directPositionsAreaOfValidityEnds;
    }

    public List<BigDecimal> getOriginalDirectPositionsAreaOfValidityEnds() {
        return this.originalDirectPositionsAreaOfValidityEnds;
    }

    public void setOriginalDirectPositionsAreaOfValidityEnds() {
        this.originalDirectPositionsAreaOfValidityEnds = new ArrayList<>();

        if (directPositionsAreaOfValidityEnds != null) {
            for (BigDecimal value : this.directPositionsAreaOfValidityEnds) {
                this.originalDirectPositionsAreaOfValidityEnds.add(value);
            }
        }
    }

    // ---------------------- Start / End Coefficients calculate main functions


    /**
     * Get the fixed first slice (0) imported coefficient's index from list of directPositions
     */
    @JsonIgnore
    public int getIndexOfCoefficientZero() {
        int i = Collections.binarySearch(this.directPositions, BigDecimal.ZERO);
        return i;
    }

    /**
     * Get the index of a given coefficient from the list of original coefficients
     */
    @JsonIgnore
    public int getIndexOfCoefficient(BigDecimal inputCoefficient) {
        int i = Collections.binarySearch(this.directPositions, inputCoefficient);
        
        if (i < 0) {
            for (int j = 0; j < this.directPositions.size(); j++) {
                BigDecimal coefficient = this.directPositions.get(j);
                
                if (BigDecimalUtil.approximateEquals(coefficient, inputCoefficient)) {
                    i = j;
                    break;
                }
            }
        }
        return i;
    }
    
    /**
     * Get the fixed first slice (0) imported coefficient's index from list of originalDirectPositions
     */
    @JsonIgnore
    public int getIndexOfCoefficientZeroFromOriginalDirectPositions() {
        int i = Collections.binarySearch(this.originalDirectPositions, BigDecimal.ZERO);
        return i;
    }
    
    /**
     * Return the index of input coefficient in list of directions
     */
    @JsonIgnore
    public int getIndexOfCoefficientFromOriginalDirectPositions(BigDecimal coefficient) throws PetascopeException {
        int i = Collections.binarySearch(this.originalDirectPositions, coefficient, new BigDecimalComparator());
        
        return i;
    }
    
    /**
     * Get element in list of coefficients which has the lowest coefficient value
     */
    @JsonIgnore
    public BigDecimal getLowestCoefficientValue() {
        if (this.originalDirectPositions == null) {
            this.setOriginalDirectPositions();
        }
        
        BigDecimal firstCoefficient = this.originalDirectPositions.get(0);
        BigDecimal lastCoefficient = this.originalDirectPositions.get(this.originalDirectPositions.size() - 1);
        
        BigDecimal result = firstCoefficient.compareTo(lastCoefficient) < 0 ? firstCoefficient : lastCoefficient;
        return result;            
    }

    /**
    // NOTE: very important to know the coefficient zero number
    // (e.g. "2001-01-01" not the adjusted lower bound "2000-01-01" from the areas of validity start)
     */
    public BigDecimal getCoefficientZeroValueAsNumber() {
        BigDecimal lowestCoefficient = this.getLowestCoefficientValue();
        if (this.originalDirectPositionsAreaOfValidityStarts != null && !this.originalDirectPositionsAreaOfValidityStarts.isEmpty()) {
            lowestCoefficient = this.originalDirectPositionsAreaOfValidityStarts.get(0);
        }

        BigDecimal coefficientZeroBoundNumber = this.getOriginalGeoBounds().getLowerLimit().add(lowestCoefficient.abs());
        return coefficientZeroBoundNumber;
    }

    /**
     * From the index of grid bound in list of coefficients, find out the correspond
     * grid bound in rasdaman grid axis. e.g: a list of coefficients: 
     * -30 -20 -10 0 10 20
     * grid axis domain is: [-1:4], zero coefficient index is 3 in list of coefficients,
     * then grid bound of index -3 (normalized by zero coefficient) will return grid value -1.
     */
    public Pair<Long, Long> calculateGridBoundsByZeroCoefficientIndex(Long indexOfGridLowerBound, Long indexOfGridUpperBound) throws PetascopeException {
        int coefficientZeroIndex = this.getIndexOfCoefficientZero();

        BigDecimal firstCoefficient = this.directPositions.get(0);
        BigDecimal lastCoefficient = this.directPositions.get(this.directPositions.size() - 1);
        
        if (firstCoefficient.compareTo(lastCoefficient) > 0) {
            firstCoefficient = lastCoefficient;
        }
        
        int gridZeroCoefficientDistance = this.getIndexOfCoefficientFromOriginalDirectPositions(firstCoefficient) - this.getIndexOfCoefficientZeroFromOriginalDirectPositions();
        
        if (coefficientZeroIndex != -1) {
            indexOfGridLowerBound = indexOfGridLowerBound - coefficientZeroIndex; 
        } else {
            // e.g. $c[Lat(55.2:85), Lon(0.1:9.9), unix("2015-09-01":"2015-12-01")][unix("2015-12-01")]
            indexOfGridLowerBound = indexOfGridLowerBound + gridZeroCoefficientDistance;
        }
        if (indexOfGridUpperBound != null) {
            if (coefficientZeroIndex != -1) {
                indexOfGridUpperBound = indexOfGridUpperBound - coefficientZeroIndex;
            } else {
                indexOfGridUpperBound = indexOfGridUpperBound + gridZeroCoefficientDistance;
            }
        }

        long gridLowerBound = Math.abs(indexOfGridLowerBound);
        long gridUpperBound = Math.abs(indexOfGridUpperBound);
        
        if (this.getGridBounds().getLowerLimit().compareTo(BigDecimal.ZERO) >= 0) {
            // In case of irregular axis with reversed geo bounds from input file (e.g: 100 85 70 50)
            // coefficients are -50 -15 -30 0 and grid bounds [0:3]
            if (gridUpperBound < gridLowerBound) {
                long tmp = gridLowerBound;
                gridLowerBound = gridUpperBound;
                gridUpperBound = tmp;
            }
        } else {
            Long normalizedCurrentGridLowerBound = -1L * coefficientZeroIndex;
            Long currentGridLowerBound = this.getGridBounds().getLowerLimit().longValue();

            Long distance = 0L;

            if (normalizedCurrentGridLowerBound.compareTo(indexOfGridLowerBound) == 0) {
                if (indexOfGridUpperBound == null) {
                    distance = 1L;
                }                  
            } else {
                distance = normalizedCurrentGridLowerBound - indexOfGridLowerBound;
            }

            gridLowerBound = currentGridLowerBound - distance;
            gridUpperBound = gridLowerBound;
            if (indexOfGridUpperBound != null) {
                distance = -(indexOfGridUpperBound - indexOfGridLowerBound);
                gridUpperBound = gridLowerBound - distance;
            }
        }
        
        return new Pair<>(gridLowerBound, gridUpperBound);
    }


    /**
     * In this case element 0 is larger than other elements,
     * this is only the case when the normal direct positions are flipped
     */
    public boolean isFlippedCoefficients() {
        boolean result = this.getDirectPositions().get(0).compareTo(this.getDirectPositions().get(this.directPositions.size() - 1)) > 0;
        return result;
    }

    /**
     *
     * Return the grid indices of input geo min and geo max values for irregular
     * axis e.g: 0 10 20 50 70 and input: min is 30, max is 60 then the first
     * value which is selected is 50 (and grid index is: 3) and the second value
     * which is selected is 70 (and grid index is: 4)
     *
     * return [3, 4]
     *
     * @return
     */
    @JsonIgnore
    public Pair<Long, Long> getGridIndices(BigDecimal minInput, BigDecimal maxInput) throws PetascopeException {

        boolean needToSwapBounds = this.isFlippedCoefficients();
        
        BigDecimal smallestCoefficientValueTmp = this.originalDirectPositions.get(0);
        BigDecimal biggestCoefficientValueTmp = this.originalDirectPositions.get(this.originalDirectPositions.size() - 1);

        if (!this.directPositionsAreaOfValidityStarts.isEmpty()) {
            smallestCoefficientValueTmp = this.directPositionsAreaOfValidityStarts.get(0);
        }
        if (!this.directPositionsAreaOfValidityEnds.isEmpty()) {
            biggestCoefficientValueTmp = this.directPositionsAreaOfValidityEnds.get(this.directPositionsAreaOfValidityEnds.size() - 1);
        }

        BigDecimal smallestCoefficientValue = smallestCoefficientValueTmp;
        BigDecimal biggestCoefficientValue = biggestCoefficientValueTmp.subtract(smallestCoefficientValueTmp);

        if (minInput.compareTo(smallestCoefficientValue) < 0) {
            throw new PetascopeException(ExceptionCode.InternalComponentError, "Input coefficient lower bound '" + minInput
                    + "' is lower than the direct positions' lower bound '" + smallestCoefficientValue.toPlainString()
                    + "' of irregular axis '" + this.getLabel() + "'.");
        } else if (maxInput.compareTo(biggestCoefficientValue) > 0) {
            throw new PetascopeException(ExceptionCode.InternalComponentError, "Input upper bound '" + maxInput
                    + "' is greater than the direct positions' upper bound '" + biggestCoefficientValue
                    + "' of irregular axis '" + this.getLabel() + "'.");
        }

        if (needToSwapBounds) {
            smallestCoefficientValue = biggestCoefficientValue;
        }

        Long minIndex = null;
        Long maxIndex = null;
        boolean foundMinIndex = false;  
        
        
        minIndex = BigDecimalUtil.getExactCoefficientIndex(directPositions, minInput);
        maxIndex = BigDecimalUtil.getExactCoefficientIndex(directPositions, maxInput);
        
        if (minIndex != null && maxIndex != null) {        
            return new Pair<> (minIndex, maxIndex);
        }

        if (!needToSwapBounds) {
            // normal list of coefficients
            for (long i = 0; i < directPositions.size(); i++) {
                BigDecimal coefficient = directPositions.get((int) i);
                BigDecimal coefficientStart = coefficient;
                BigDecimal coefficientEnd = coefficient;

                if (!this.directPositionsAreaOfValidityStarts.isEmpty()) {
                    coefficientStart = directPositionsAreaOfValidityStarts.get((int) i);
                    coefficientEnd = directPositionsAreaOfValidityEnds.get((int) i);
                }

                if (!this.directPositionsAreaOfValidityStarts.isEmpty()) {
                    // coefficients has areasOfValidity
                    if (!foundMinIndex && BigDecimalUtil.smallerThanOrEqual(coefficientStart, minInput) &&
                            BigDecimalUtil.greaterThanOrEqual(coefficientEnd, minInput)) {
                        foundMinIndex = true;
                        minIndex = i;
                    } else if (BigDecimalUtil.greaterThanOrEqual(minInput, coefficientStart)) {
                        // NOTE: In case min input does not touch current coefficient's areas of validity
                        // but it is greater than the current coefficient's start value -> next grid point is selected
                        minIndex = i + 1;
                    }

                    if (BigDecimalUtil.smallerThanOrEqual(coefficientStart, maxInput) &&
                            BigDecimalUtil.greaterThanOrEqual(coefficientEnd, maxInput)) {
                        maxIndex = i;
                    } else if (BigDecimalUtil.smallerThanOrEqual(maxInput, coefficientStart)) {
                        // NOTE: In case max input does not touch current coefficient's areas of validity
                        // but it is smaller than the current coefficient's start value -> previous grid point is selected
                        maxIndex = i - 1;
                    }
                } else {
                    // coefficient has no areasOfValidity
                    // find the min number which >= minInput
                    if (!foundMinIndex
                            && BigDecimalUtil.greaterThanOrEqual(coefficient, minInput)
                    ) {
                        minIndex = i;
                        foundMinIndex = true;
                    }


                    // find the max number which <= maxInput (as it is ascending list, so don't stop until coefficient > maxInput
                    if (BigDecimalUtil.smallerThanOrEqual(coefficient, maxInput)) {
                        maxIndex = i;
                    }

                }

                // stop as it should find the minIndex and maxIndex already
                if (coefficientEnd.compareTo(maxInput) >= 0) {
                    break;
                }
            }
        } else {
            // flipped list of coefficients
            for (long i = directPositions.size() - 1; i >= 0; i--) {
                BigDecimal coefficient = directPositions.get((int) i);
                // find the min number which >= minInput
                if (!foundMinIndex && BigDecimalUtil.greaterThanOrEqual(coefficient, minInput)) {
                    minIndex = i;
                    foundMinIndex = true;
                }
                // find the max number which <= maxInput (as it is ascending list, so don't stop until coefficient > maxInput
                if (BigDecimalUtil.smallerThanOrEqual(coefficient, maxInput)) {
                    maxIndex = i;
                }
                // stop as it should find the minIndex and maxIndex already
                if (coefficient.compareTo(maxInput) >= 0) {
                    break;
                }
            }
        }

        Pair<Long, Long> gridBoundsPair = new Pair<>(minIndex, maxIndex);
        return gridBoundsPair;
    }

    /**
     * Get all the coefficients from the list of directPositions which greater
     * than minInput and less than maxInput
     *
     */
    @JsonIgnore
    private List<BigDecimal> getAllCoefficientsInInterval(BigDecimal minInput, BigDecimal maxInput) throws PetascopeException {
        
        // Find the min and max grid indices in the List of directPositions
        Pair<Long, Long> gridIndices = this.getGridIndices(minInput, maxInput);
        if (gridIndices.fst.compareTo(gridIndices.snd) > 0) {
            throw new IrregularAxisTrimmingCoefficientNotFoundException(this.getLabel(), minInput.toPlainString(), maxInput.toPlainString());
        }
        
        List<BigDecimal> coefficients = new ArrayList<>();
        
        for (Long i = gridIndices.fst; i <= gridIndices.snd; i++) {
            BigDecimal coefficient = this.directPositions.get((int)(i.intValue()));
            coefficients.add(coefficient);
        }

        return coefficients;
    }

    /**
     * Collect only coefficients from directPositions which minInput <= coefficients <= maxInput
     * and coefficient starts / coefficient ends from areas of validity
     */
    @JsonIgnore
    public void setCoefficientsAndAreasOfValidityBetweenInterval(BigDecimal minInput, BigDecimal maxInput) throws PetascopeException {
        // Find the min and max grid indices in the List of directPositions
        Pair<Long, Long> gridIndices = this.getGridIndices(minInput, maxInput);
        if (gridIndices.fst.compareTo(gridIndices.snd) > 0) {
            throw new IrregularAxisTrimmingCoefficientNotFoundException(this.getLabel(), minInput.toPlainString(), maxInput.toPlainString());
        }

        Long firstIndex = gridIndices.fst;
        Long secondIndex = gridIndices.snd;

        List<BigDecimal> collectedCoefficientsTmp = new ArrayList<>();
        List<BigDecimal> collectedDirectPositionsAreaOfValidityStartsTmp = new ArrayList<>();
        List<BigDecimal> collectedDirectPositionsAreaOfValidityEndsTmp = new ArrayList<>();

        for (Long i = firstIndex; i <= secondIndex; i++) {
            BigDecimal coefficient = this.directPositions.get((int)(i.intValue()));
            collectedCoefficientsTmp.add(coefficient);

            if (this.directPositionsAreaOfValidityStarts != null && !this.directPositionsAreaOfValidityStarts.isEmpty()) {
                collectedDirectPositionsAreaOfValidityStartsTmp.add(this.directPositionsAreaOfValidityStarts.get(i.intValue()));
                collectedDirectPositionsAreaOfValidityEndsTmp.add(this.directPositionsAreaOfValidityEnds.get(i.intValue()));
            }
        }

        this.directPositions = collectedCoefficientsTmp;
        this.directPositionsAreaOfValidityStarts = collectedDirectPositionsAreaOfValidityStartsTmp;
        this.directPositionsAreaOfValidityEnds = collectedDirectPositionsAreaOfValidityEndsTmp;
    }


    /**
     * In case of irregular axis is imported with reversed values (e.g: 10000 7000 50000 0)
     * then, it the coefficient values should be shown by these values as well.
     */
    private List<String> adjustCoefficientsForPresentation(List<BigDecimal> coefficients) throws PetascopeException {
        List<String> results = new ArrayList<>();

        for (int i = 0; i < coefficients.size(); i++) {
            String numberStr = BigDecimalUtil.stripDecimalZeros(this.getCoefficientZeroValueAsNumber().add(coefficients.get(i))).toPlainString();
            results.add(numberStr);
        }

        return results;
    }

    /**
     * Return the concatenated string of a list of translated coefficients (DateTime axis) or raw
     * coefficients (non-datetime axis)
     */
    public String getRepresentationCoefficients(String delimiterCharacter) throws PetascopeException {
        String coefficientsStr = ListUtil.join(this.getRepresentationCoefficientsList(), delimiterCharacter);
        return coefficientsStr;
    }

    /**
     * Return the list of translated coefficients (DateTime axis) or raw
     * coefficients (non-datetime axis)
     */
    public List<String> getRepresentationCoefficientsList() throws PetascopeException  {
        List<String> results = new ArrayList<>();

        BigDecimal lowerLimit = this.getGeoBounds().getLowerLimit();

        BigDecimal smallestCoefficient = this.getDirectPositions().get(0);
        if (this.directPositionsAreaOfValidityStarts != null && !this.directPositionsAreaOfValidityStarts.isEmpty()) {
            BigDecimal smallestCoefficientStart = this.directPositionsAreaOfValidityStarts.get(0);
            BigDecimal distance = smallestCoefficient.subtract(smallestCoefficientStart);
            lowerLimit = lowerLimit.add(distance);
        }

        // date time axis, need to translate from raw coefficients to datetime format based on CRS origin
        if (this.getAxisType().equals(T_AXIS)) {
            results = TimeUtil.listValuesToISODateTime(lowerLimit,
                    directPositions.get(0),
                    directPositions, this.getCrsDefinition());
        } else {
            // non date time axis
            results = this.adjustCoefficientsForPresentation(directPositions);
        }

        return results;

    }

    @Override
    public IrregularAxis clone() {
        return new IrregularAxis(getLabel(), getGeoBounds(), getOriginalGridBounds(), getGridBounds(),
                getNativeCrsUri(), getCrsDefinition(), getAxisType(), getAxisUoM(),
                getRasdamanOrder(), getOriginalOrigin(), getResolution(), getDirectPositions(),
                getDirectPositionsAreaOfValidityStarts(),
                getDirectPositionsAreaOfValidityEnds(),
                getOriginalGeoBounds());
    }

}
