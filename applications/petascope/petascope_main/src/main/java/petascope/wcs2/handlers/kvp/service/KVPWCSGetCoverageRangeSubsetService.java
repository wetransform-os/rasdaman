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
package petascope.wcs2.handlers.kvp.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.WCSException;
import petascope.util.BigDecimalUtil;
import petascope.util.ListUtil;
import petascope.wcps.metadata.model.RangeField;
import petascope.wcps.metadata.model.WcpsCoverageMetadata;
import static petascope.wcs2.handlers.kvp.KVPWCSGetCoverageHandler.RANGE_NAME;

/**
 * Service class for Range Subset handler of GetCoverageKVP class
 *
 @author <a href="mailto:b.phamhuu@jacobs-university.de">Bang Pham Huu</a>
 */
@Service
public class KVPWCSGetCoverageRangeSubsetService {

    public KVPWCSGetCoverageRangeSubsetService() {

    }

    /**
     * Get the requested range subsets and apply the range subsets in
     * WcpsCoverageMetadata object e.g: coverage with 10 bands, and the request
     * is: rangesubset=b1:b3,b5,b7,b9:b10
     *
     * @param wcpsCoverageMetadata
     * @param rangeSubsets
     */
    public void handleRangeSubsets(WcpsCoverageMetadata wcpsCoverageMetadata, String[] rangeSubsets) throws WCSException {

        // The ranges of persisted coverage
        List<RangeField> originalRangeFields = wcpsCoverageMetadata.getRangeFields();
        // The range of translated coverage which can contain with less or more range fields than the persisted ranges.
        List<RangeField> translatedRangeFields = new ArrayList<>();

        int totalBands = originalRangeFields.size();

        for (String rangeSubsetValue : rangeSubsets) {
            // It can be: b1, b3 or b2:b3,b5:b7 or OAPI style with 0,1,*
            if (rangeSubsetValue.contains(":")) {
                // range subset is interval with lowerRange:upperRange
                String lowerRangeName = rangeSubsetValue.split(":")[0].trim();
                String upperRangeName = rangeSubsetValue.split(":")[1].trim();

                // validate the range sequence
                int lowerRangeIndex = this.getRangeFieldIndex(originalRangeFields, lowerRangeName);
                int upperRangeIndex = this.getRangeFieldIndex(originalRangeFields, upperRangeName);

                // e.g: coverage has red, green, blue ranges and request is green:red
                if (lowerRangeIndex > upperRangeIndex) {
                    throw new WCSException(ExceptionCode.IllegalFieldSequence, "Lower limit is above the upper limit in the range field interval, received: ." + rangeSubsetValue);
                }

                // add all the ranges from lowerIndex:upperIndex as requested ranges
                for (int i = lowerRangeIndex; i <= upperRangeIndex; i++) {
                    translatedRangeFields.add(originalRangeFields.get(i));
                }
            } else {
                // range subset is not an interval

                 if (rangeSubsetValue.equalsIgnoreCase("*")) {
                     // e.g. *
                     if (translatedRangeFields.isEmpty()) {
                         translatedRangeFields.addAll(originalRangeFields);
                     } else {
                         // add subsequent rangefields starting from the last added rangefield
                         RangeField lastAddedRangeField = translatedRangeFields.get(translatedRangeFields.size() - 1);
                         // e.g. 1 and coverage has 5 bands, then add subsequent bands after band 1, so it has: 2,3,4
                         int lastAddedRangedFieldIndex = this.getRangeFieldIndex(originalRangeFields, lastAddedRangeField.getName());
                         for (int i = lastAddedRangedFieldIndex + 1; i < totalBands; i++) {
                             RangeField newAddedRangeField = originalRangeFields.get(i);
                             translatedRangeFields.add(newAddedRangeField);
                         }
                     }
                 } else {
                     // e.g. Red or 1 (rangeIndex)
                     int rangeIndex = 0;
                     if (BigDecimalUtil.isNumber(rangeSubsetValue)) {
                         // e.g. 2
                         rangeIndex = Integer.parseInt(rangeSubsetValue);
                     } else {
                         // e.g Red
                         rangeIndex = this.getRangeFieldIndex(originalRangeFields, rangeSubsetValue);
                     }

                     // add this range as a requested range
                     translatedRangeFields.add(originalRangeFields.get(rangeIndex));
                 }
            }
        }

        // Now, the translated request ranges override the original ranges
        wcpsCoverageMetadata.setRangeFields(translatedRangeFields);
    }

    /**
     * Generate the range constructor from the requested ranges e.g: origin
     * coverage has 3 ranges and requested with range subset: red,green { red:
     * c[i(0:10)]; green: c[i(0:10)]}
     *
     * @param wcpsCoverageMetadata
     * @param generatedCoverageExpression
     * @param rangeSubsets
     * @return
     */
    public String generateRangeConstructorWCPS(WcpsCoverageMetadata wcpsCoverageMetadata,
            String generatedCoverageExpression, String rangeSubsets,int totalOriginalRangeFields) {
        if (rangeSubsets == null) {
            // If no range is requested, just use all the ranges of coverage
            return generatedCoverageExpression.replace(RANGE_NAME, "");
        } else {
            // Create the range constructor for all the requested ranges
            String rangeConstructorExpression = "{ ";

            List<String> rangeExpressions = new ArrayList<>();
            // Red: c[i(0:10]]
            int i = 0;
            for (RangeField rangeField : wcpsCoverageMetadata.getRangeFields()) {
                // replace the rangeName with the current range name: e.g c[i(0:20)].rangeName -> c[i(0:20)].Red
                String fieldName = rangeField.getName();

                if (i >= totalOriginalRangeFields) {
                    // NOTE: in case, WCS rangeSubset creates more bands than total number of bands of the requested coverage
                    // then it add the suffix to the band index, e.g. red,green,blue,red0,red1,...
                    fieldName += (i - totalOriginalRangeFields);
                }
                rangeExpressions.add(fieldName + ": "
                        + generatedCoverageExpression.replace(RANGE_NAME, "." + rangeField.getName()));

                i += 1;
            }

            rangeConstructorExpression += ListUtil.join(rangeExpressions, "; ") + " }";
            return rangeConstructorExpression;
        }
    }

    /**
     * Check if range name exist in list of ranges
     *
     * @param rangeName
     * @return
     */
    private int getRangeFieldIndex(List<RangeField> rangeFields, String rangeName) throws WCSException {
        int i = 0;
        for (RangeField rangeField : rangeFields) {
            if (rangeField.getName().equals(rangeName)) {
                return i;
            }
            i++;
        }
        // Cannot find the range, throw exception
        throw new WCSException(ExceptionCode.InvalidRequest, "Cannot find the rangeName: " + rangeName + " from the list of ranges.");
    }
}
