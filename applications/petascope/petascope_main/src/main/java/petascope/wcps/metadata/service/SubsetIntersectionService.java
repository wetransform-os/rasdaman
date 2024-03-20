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

package petascope.wcps.metadata.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import petascope.core.Pair;
import petascope.exceptions.PetascopeException;
import petascope.util.BigDecimalUtil;
import petascope.wcps.metadata.model.*;
import petascope.wcps.metadata.service.SubsetParsingService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Check the intersection types of the list of subsets with coverage's axes' extents
 */
@Service
public class SubsetIntersectionService {

    @Autowired
    private SubsetParsingService subsetParsingService;

    public enum SubsetIntersectionType {
        WITHIN_BOUNDS, // normal case
        PARTIAL_OUT_OF_BOUNDS, // need extend()
        CONTAINS_AXIS_BOUNDS, // need extend()
        COMPLETE_OUT_OF_BOUNDS // return 204 http code with all null values
    }

    /**
     * Check the intersection type of each subset with the corresponding axis
     */
    public SubsetIntersectionType getSubsetsIntersectionType(WcpsCoverageMetadata wcpsCoverageMetadata,
                                                            List<Subset> numericSubsets) {


        SubsetIntersectionType result = SubsetIntersectionType.WITHIN_BOUNDS;

        for (Subset numericSubset : numericSubsets) {
            String subsetAxisLabel = numericSubset.getAxisName();
            Axis axis = wcpsCoverageMetadata.getAxisByName(subsetAxisLabel);

            if (axis instanceof RegularAxis) {
                BigDecimal geoLowerBoundAxis = axis.getOriginalGeoBounds().getLowerLimit();
                BigDecimal geoUpperBoundAxis = axis.getOriginalGeoBounds().getUpperLimit();

                BigDecimal geoLowerBoundSubset = null;
                BigDecimal geoUpperBoundSubset = null;

                if (numericSubset.getNumericSubset() instanceof NumericTrimming) {
                    NumericTrimming numericTrimming = (NumericTrimming)numericSubset.getNumericSubset();

                    geoLowerBoundSubset = numericTrimming.getLowerLimit();
                    geoUpperBoundSubset = numericTrimming.getUpperLimit();

                } else if (numericSubset.getNumericSubset() instanceof NumericSlicing) {
                    NumericSlicing numericSlicing = (NumericSlicing)numericSubset.getNumericSubset();

                    geoLowerBoundSubset = numericSlicing.getBound();
                    geoUpperBoundSubset = geoLowerBoundSubset;
                } else if (numericSubset.getNumericSubset() instanceof NumericTemporalSlicing) {
                    NumericTemporalSlicing numericTemporalSlicing = (NumericTemporalSlicing)numericSubset.getNumericSubset();

                    geoLowerBoundSubset = numericTemporalSlicing.getLowerLimit();
                    geoUpperBoundSubset = geoLowerBoundSubset;
                }

                // e.g. axis' extent is: [0:10], and subset is [0:15] or [5:15] or [10:15]
                boolean caseA = (geoLowerBoundSubset.compareTo(geoLowerBoundAxis) >= 0)
                            && (geoLowerBoundSubset.compareTo(geoUpperBoundAxis) <= 0)
                            && (geoUpperBoundSubset.compareTo(geoLowerBoundAxis) > 0) ;

                // e.g. axis' extent is: [0:10], and subset is [-10:5] or [-5:10]
                boolean caseB = (geoLowerBoundSubset.compareTo(geoLowerBoundAxis) < 0)
                            && (geoUpperBoundSubset.compareTo(geoLowerBoundAxis) >= 0)
                            && (geoUpperBoundSubset.compareTo(geoUpperBoundAxis) <= 0);
                if (caseA || caseB) {
                    result = SubsetIntersectionType.PARTIAL_OUT_OF_BOUNDS;
                } else if (geoLowerBoundSubset.compareTo(geoLowerBoundAxis) < 0 && geoUpperBoundSubset.compareTo(geoUpperBoundAxis) > 0) {
                    // e.g. axis' extent is [0:10] and subset is [-5:15]
                    result = SubsetIntersectionType.CONTAINS_AXIS_BOUNDS;
                } else if ((geoUpperBoundSubset.compareTo(geoLowerBoundAxis) < 0)
                           || (geoLowerBoundSubset.compareTo(geoUpperBoundAxis) > 0)) {
                    // e.g. axis' extent is [0:10] and subset is [-10:-5] or [13:18]
                    result = SubsetIntersectionType.COMPLETE_OUT_OF_BOUNDS;
                    return result;
                }

            }
        }

        return result;

    }
}
