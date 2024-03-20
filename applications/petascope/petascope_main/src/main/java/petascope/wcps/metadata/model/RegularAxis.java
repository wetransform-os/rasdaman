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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import petascope.core.CrsDefinition;

/**
 * @author <a href="merticariu@rasdaman.com">Vlad Merticariu</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegularAxis extends Axis {
    
    public RegularAxis() {
        
    }
    
    public RegularAxis(String label, NumericSubset geoBounds, NumericSubset originalGridBounds, NumericSubset gridBounds,
                       String crsUri, CrsDefinition crsDefinition, String axisType, String axisUoM,
                       int rasdamanOrder, BigDecimal origin, BigDecimal resolution) {
        super(label, geoBounds, originalGridBounds, gridBounds, crsUri, crsDefinition, axisType, axisUoM, rasdamanOrder, origin, resolution, null);
    }

    public RegularAxis(String label, NumericSubset geoBounds, NumericSubset originalGridBounds, NumericSubset gridBounds,
                       String crsUri, CrsDefinition crsDefinition, String axisType, String axisUoM,
                       int rasdamanOrder, BigDecimal origin, BigDecimal resolution, NumericSubset originalGeoBounds) {
        super(label, geoBounds, originalGridBounds, gridBounds, crsUri, crsDefinition, axisType, axisUoM, rasdamanOrder, origin, resolution, originalGeoBounds);
    }
    
    public RegularAxis(Axis inputAxis, NumericSubset geoBounds, NumericSubset originalGridBounds, NumericSubset gridBounds) {
        super(inputAxis, geoBounds, originalGridBounds, gridBounds);
    }
    
    @Override
    public RegularAxis clone() {
        return new RegularAxis(getLabel(), getGeoBounds(), getOriginalGridBounds(), getGridBounds(),
                getNativeCrsUri(), getCrsDefinition(), getAxisType(), getAxisUoM(),
                getRasdamanOrder(), getOriginalOrigin(), getResolution(), getOriginalGeoBounds());
    }
}
