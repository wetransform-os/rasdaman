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
package petascope.wcps.subset_axis.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import petascope.util.PetascopeDateTime;
import petascope.util.StringUtil;

/**
 * Used only for slicing on a temporal axis
 */
public class WcpsSliceTemporalSubsetDimension extends WcpsSubsetDimension {

    // input subset by user
    private String originalBound;
    // Calculated by petascope
    private PetascopeDateTime petascopeDateTime;

    private String axisIteratorLabel = null;


    public WcpsSliceTemporalSubsetDimension() {

    }

    public WcpsSliceTemporalSubsetDimension(String axisName, String crs, String originalBound, PetascopeDateTime petascopeDateTime, String axisIteratorLabel) {
        super(axisName, crs);
        this.originalBound = StringUtil.quoteIfNotAlready(originalBound);
        this.petascopeDateTime = petascopeDateTime;
        this.axisIteratorLabel = axisIteratorLabel;
    }

    public String getOriginalBound() {
        return this.originalBound;
    }

    public void setPetascopeDateTime(PetascopeDateTime petascopeDateTime) {
        this.petascopeDateTime = petascopeDateTime;
    }

    public PetascopeDateTime getPetascopeDateTime() {
        return this.petascopeDateTime;
    }

    @Override
    @JsonIgnore
    public String getStringBounds() {
        return this.petascopeDateTime.getDateTimeMinISOFormatStr() + ":" + this.petascopeDateTime.getDateTimeMaxISOFormatStr();
    }

    @Override
    public String toString() {
        String result = getAxisName();
        if(getCrs() != null && !getCrs().isEmpty()){
            result += ":\"" + getCrs() + "\"";
        }
        result += "(" + this.getStringBounds() + ")";
        return result;
    }

    @Override
    @JsonIgnore
    public String toStringWithoutCRS() {
        return getAxisName() + "(" + this.getStringBounds() + ")";
    }

    @JsonIgnore
    public boolean isSlicedByAxisIterator() {
        return this.axisIteratorLabel != null;
    }
}
