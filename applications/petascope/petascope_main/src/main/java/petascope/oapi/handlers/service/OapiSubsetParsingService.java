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
package petascope.oapi.handlers.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import petascope.core.BoundingBox;
import petascope.core.KVPSymbols;
import petascope.core.Pair;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.util.CrsUtil;
import petascope.util.StringUtil;
import petascope.util.TimeUtil;
import petascope.wcps.metadata.model.Axis;
import petascope.wcps.metadata.model.WcpsCoverageMetadata;
import petascope.wcps.metadata.service.WcpsCoverageMetadataTranslator;
import petascope.wms.exception.WMSInvalidBoundingBoxException;
import petascope.wms.handlers.kvp.KVPWMSGetMapHandler;

/**
 * Class to parse subset for Oapi GetCoverage request
 * 
 * @author Vlad Merticariu <v.merticariu@jacobs-university.de>
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
public class OapiSubsetParsingService {
    
    private static org.slf4j.Logger log = LoggerFactory.getLogger(OapiSubsetParsingService.class);
    
    private static final String COMMA = ",";
    private static final String COLON = ":";
    private static final String RIGHT_PARENTHESIS = ")";
    private static final String TIME_QUOTE = "\"";

    @Autowired
    private WcpsCoverageMetadataTranslator wcpsCoverageMetadataTranslator;

    /**
     * Parse the GetCoverage request subsets as list of strings
     * e.g: subset=Lat(0,10) -- WCS style
     *      subset=Lat(0:10) -- WCPS style
     *      subset=ansi("2020-01-01:00"),Lat(0:1),Long(0:2) -- Combination of multiple subsets
     */
    public String[] parseGetCoverageSubsets(String coverageId, String bbox, String bboxCrs, String datetime, String[] inputSubsets) throws PetascopeException {
        List<String> results = new ArrayList<>();

        if (bbox != null || datetime != null) {
            WcpsCoverageMetadata metadata = this.wcpsCoverageMetadataTranslator.translate(coverageId);

            if (bbox != null) {
                Pair<String, String> xySubsetsPair = this.parseBBox(metadata, bbox, bboxCrs);
                results.add(xySubsetsPair.fst);
                results.add(xySubsetsPair.snd);
            }

            if (datetime != null) {
                String timeSubset = this.parseDateTime(metadata, datetime);
                results.add(timeSubset);
            }
        }
        
        if (inputSubsets != null) {        
            for (String inputSubset : inputSubsets) {
                inputSubset = inputSubset.trim();
                int indexOfComma = inputSubset.indexOf(COMMA);
                if (indexOfComma < 0) {
                    // subset contains single value, e.g: subset=Lat(0:20) or subset=Lat(0,10) or subset=ansi("2015-01-01")
                    String value = replaceColonByComma(inputSubset);
                    results.add(value);
                } else {
                    // subset contains multiple values, e.g: subset=Lat(0:20),Lat(0,10),ansi("2015-01-01T20:10:40":"2015-02-03T10:10:05")
                    String[] subsets = inputSubset.split(Pattern.quote(RIGHT_PARENTHESIS + COMMA));
                    for (String subset : subsets) {
                        String value = replaceColonByComma(subset + RIGHT_PARENTHESIS);
                        results.add(value);
                    }
                }
            }
        }
        
        return results.toArray(new String[results.size()]);
    }

    /**
     * Parse bboxValue and bboxValue-crs to a pair of spatial subsets on XY axes
     */
    private Pair<String, String> parseBBox(WcpsCoverageMetadata metadata, String bboxValue, String bboxCRS) throws PetascopeException {
        if (!metadata.hasXYAxes()) {
            throw new PetascopeException(ExceptionCode.InvalidRequest,
                    "Coverage: " + metadata.getCoverageName() + " does not have XY axes to subset by: " + KVPSymbols.KEY_OAPI_BBOX + " parameter.");
        }

        BoundingBox bbox = BoundingBox.parse(bboxValue);

        if (bboxCRS != null) {
            // Default bboxCRS is WGS84 (long,lat) order
            boolean xyAxesOrder = CrsUtil.isXYAxesOrder(bboxCRS);
            if (!xyAxesOrder) {
                // e.g. EPSG:4326 with latMin,longMin,latMax,longMax
                // then convert to XY axes order
                bbox.swapXYOrder();
            }
        }

        List<Axis> xyAxes = metadata.getXYAxes();
        Axis axisX = xyAxes.get(0);
        Axis axisY = xyAxes.get(1);

        Pair<BigDecimal, BigDecimal> subsetIntersectionOnAxisX = bbox.getIntersectionByXAxis(axisX.getGeoBounds().getLowerLimit(),
                                                                                                axisX.getGeoBounds().getUpperLimit());
        Pair<BigDecimal, BigDecimal> subsetIntersectionOnAxisY = bbox.getIntersectionByYAxis(axisY.getGeoBounds().getLowerLimit(),
                                                                                                axisY.getGeoBounds().getUpperLimit());

        String subsetAxisX = axisX.getLabel() + "(" + subsetIntersectionOnAxisX.fst + "," + subsetIntersectionOnAxisX.snd + ")";
        String subsetAxisY = axisY.getLabel() + "(" + subsetIntersectionOnAxisY.fst + "," + subsetIntersectionOnAxisY.snd + ")";

        Pair<String, String> pair = new Pair<>(subsetAxisX, subsetAxisY);
        return pair;
    }

    /**
     * Parse datetime value to a temporal subset
     *
     * interval-closed     = date-time "/" date-time
     * interval-open-start = [".."] "/" date-time
     * interval-open-end   = date-time "/" [".."]
     * interval            = interval-closed / interval-open-start / interval-open-end
     * datetime            = date-time / interval
     *
     * "2015-01-01"/"2015-02-03" or ../"2015-02-03" or/"2015-02-03" or / or ../ or ../.. will be valid for datetime value
     */
    private String parseDateTime(WcpsCoverageMetadata metadata, String datetime) throws PetascopeException {
        Axis timeAxis = metadata.getTimeAxis();
        if (timeAxis == null) {
            throw new PetascopeException(ExceptionCode.InvalidRequest,
                    "Coverage: " + metadata.getCoverageName() + " does not have any temporal axis for subsetting by: " + KVPSymbols.KEY_OAPI_DATETIME  + " parameter.");
        }

        if (datetime.trim().equals("/")) {
            datetime = "../..";
        }

        String[] parts = datetime.split("/");

        // input values from datetime param
        String requestLowerBoundPart = this.convertDateTimeBound(timeAxis, parts[0], true);
        String requestUpperBoundPart = this.convertDateTimeBound(timeAxis, parts[1], false);

        // temporal axis' extents
        DateTime timeAxisLowerBoundDateTime = TimeUtil.parseDateTimeByPattern(timeAxis.getLowerGeoBoundRepresentation());
        DateTime timeAxisUpperBoundDateTime = TimeUtil.parseDateTimeByPattern(timeAxis.getUpperGeoBoundRepresentation());

        // shifted temporal bounds if valid
        String resultLowerBoundPart = null, resultUpperBoundPart = null;

        if (TimeUtil.parseDateTimeByPattern(requestLowerBoundPart).compareTo(timeAxisLowerBoundDateTime) <= 0) {
            resultLowerBoundPart = timeAxisLowerBoundDateTime.toString();
        }
        if (TimeUtil.parseDateTimeByPattern(requestUpperBoundPart).compareTo(timeAxisUpperBoundDateTime) >= 0) {
            resultUpperBoundPart = timeAxisUpperBoundDateTime.toString();
        }

        // Validate if the lower / upper bound of requesting times are valid
        if (TimeUtil.parseDateTimeByPattern(requestLowerBoundPart).compareTo(timeAxisLowerBoundDateTime) >= 0
           && TimeUtil.parseDateTimeByPattern(requestLowerBoundPart).compareTo(timeAxisUpperBoundDateTime) <= 0) {
            resultLowerBoundPart = requestLowerBoundPart;
        }
        if (TimeUtil.parseDateTimeByPattern(requestUpperBoundPart).compareTo(timeAxisLowerBoundDateTime) >= 0
            && TimeUtil.parseDateTimeByPattern(requestUpperBoundPart).compareTo(timeAxisUpperBoundDateTime) <= 0) {
            resultUpperBoundPart = requestUpperBoundPart;
        }

        if (resultLowerBoundPart == null || resultUpperBoundPart == null) {
            throw new PetascopeException(ExceptionCode.InvalidRequest,
                    "Subset on temporal axis with bounds: " + StringUtil.quote(requestLowerBoundPart) + ":" + StringUtil.quote(requestUpperBoundPart)
                            + " does not intersect with the temporal axis' extent: " + timeAxis.getLowerGeoBoundRepresentation() + ":" + timeAxis.getUpperGeoBoundRepresentation());
        }

        String result = timeAxis.getLabel() + "(" + StringUtil.quote(resultLowerBoundPart) + "," + StringUtil.quote(resultUpperBoundPart) + ")";
        return result;
    }

    /**
     * Convert .. or emptry string to lower / upper bound of a temporal axis' bounds
     * @param requestDatetimeBound: lower / upper bound value of datetime request parameter
     * @return
     */
    private String convertDateTimeBound(Axis timeAxis, String requestDatetimeBound, boolean isLowerBound) throws PetascopeException {
        String timeAxisLowerBound = timeAxis.getLowerGeoBoundRepresentation();
        String timeAxisUpperBound = timeAxis.getUpperGeoBoundRepresentation();

        String result = requestDatetimeBound;

        if (requestDatetimeBound.equals("..") || requestDatetimeBound.equals("")) {
            if (isLowerBound) {
                result = timeAxisLowerBound;
            } else {
                result = timeAxisUpperBound;
            }
        }

        // it can be null in case the request time doesn't belong to time axis' extent
        return StringUtil.stripQuotes(result);
    }
    
    /**
     * Change interval with WCPS style, separated by ":" to WCS style, separated by ","
     */
    private String replaceColonByComma(String subset) {
        String result;
        
        if (!subset.contains(TIME_QUOTE)) {
            // Lat(0:20) -> Lat(0,10)
            result = subset.replace(COLON, COMMA);
        } else {
            // ansi("2015-01-01T10:00:02":"2015-03-02T10:30:20") -> ansi("2015-01-01T10:00:02","2015-03-02T10:30:20")
            result = subset.replace(COLON + TIME_QUOTE, COMMA + TIME_QUOTE);
        }
        
        return result;
    }

}
