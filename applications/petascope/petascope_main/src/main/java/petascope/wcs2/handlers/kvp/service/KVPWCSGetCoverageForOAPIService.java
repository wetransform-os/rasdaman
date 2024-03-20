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
package petascope.wcs2.handlers.kvp.service;

import com.rasdaman.accesscontrol.service.AuthenticationService;
import org.rasdaman.domain.cis.NilValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import petascope.core.Pair;
import petascope.core.response.Response;
import petascope.exceptions.PetascopeException;
import petascope.util.BigDecimalUtil;
import petascope.util.CrsUtil;
import petascope.util.ListUtil;
import petascope.util.ras.RasUtil;
import petascope.wcps.metadata.model.*;
import petascope.wcps.metadata.service.WcpsCoverageMetadataGeneralService;
import petascope.wcps.subset_axis.model.WcpsSubsetDimension;
import petascope.wcps.subset_axis.model.WcpsTrimSubsetDimension;
import petascope.wcs2.parsers.subsets.AbstractSubsetDimension;
import petascope.wcs2.parsers.subsets.TrimmingSubsetDimension;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * NOTE: OAPI requests allow to request subset which intersects partially or completely out of axis' extent
 */
@Service
public class KVPWCSGetCoverageForOAPIService {

    @Autowired
    WcpsCoverageMetadataGeneralService wcpsCoverageMetadataGeneralService;
    @Autowired
    private HttpServletRequest httpServletRequest;



    /**
     *  NOTE: for OAPI, if a subset doesn't touch corresponding axis' extent -> it returns HTTP code 204 and
     *  null values for the grid domains, not throw exception.
     */
    public Response getResponseForOutOfBoundSubset(WcpsCoverageMetadata wcpsCoverageMetadata, List<AbstractSubsetDimension> subsetDimensions, String outputFormat, String subsettingCrs, String outputCrs) throws PetascopeException {
        boolean isAllSlicing = true;
        for (AbstractSubsetDimension subsetDimension : subsetDimensions) {
            // If all subsets are slicing
            if (subsetDimension instanceof TrimmingSubsetDimension) {
                isAllSlicing = false;
                break;
            }
        }

        String nullValuesRepresentation = "0";

        if (wcpsCoverageMetadata.getRangeFields().size() == 1) {
            // Coverage has 1 band
            nullValuesRepresentation = wcpsCoverageMetadata.getNodata().get(0).getValue();
        } else {
            // Coverage has multibands
            List<String> nullValuesTmp = new ArrayList<>();
            for (List<NilValue> nilValues  : wcpsCoverageMetadata.getNilValues()) {
                // e.g. 0:30 -> 0
                String firstNilValue = nilValues.get(0).getValue().split(":")[0];
                nullValuesTmp.add(firstNilValue);
            }

            nullValuesRepresentation = "{ " + ListUtil.join(nullValuesTmp, ", " + " }");
        }

        String query = "";

        if (isAllSlicing) {
            // e.g. select encode({99c, 99c, 99c}, "csv")
            query = "SELECT encode(" + nullValuesRepresentation + ", \"" + outputFormat + "\")";
        } else {
            // at least one trimming

            if (subsettingCrs != null) {
                List<Axis> xyAxes = wcpsCoverageMetadata.getXYAxes();
                Axis axisX = xyAxes.get(0);
                Axis axisY = xyAxes.get(1);

                Subset subsetX = new Subset(new NumericTrimming(axisX.getGeoBounds().getLowerLimit(),
                                                                axisX.getGeoBounds().getUpperLimit()),
                                                                axisX.getNativeCrsUri(), axisX.getLabel());
                Subset subsetY = new Subset(new NumericTrimming(axisY.getGeoBounds().getLowerLimit(),
                                                                axisY.getGeoBounds().getUpperLimit()),
                                                                axisY.getNativeCrsUri(), axisY.getLabel());

                TrimmingSubsetDimension trimmingSubsetDimensionX = null;
                TrimmingSubsetDimension trimmingSubsetDimensionY = null;

                for (AbstractSubsetDimension subsetDimension : subsetDimensions) {
                    if (subsetDimension instanceof TrimmingSubsetDimension) {
                        TrimmingSubsetDimension trimmingSubsetDimension = ((TrimmingSubsetDimension) subsetDimension);

                        if (CrsUtil.axisLabelsMatch(subsetDimension.getDimensionName(), axisX.getLabel())) {
                            trimmingSubsetDimensionX = trimmingSubsetDimension;

                            subsetX = new Subset(new NumericTrimming(BigDecimalUtil.parseDecimalFromString(trimmingSubsetDimension.getLowerBound()),
                                                BigDecimalUtil.parseDecimalFromString(trimmingSubsetDimension.getUpperBound())),
                                                axisX.getNativeCrsUri(), axisX.getLabel());
                        } else if (CrsUtil.axisLabelsMatch(subsetDimension.getDimensionName(), axisY.getLabel())) {
                            trimmingSubsetDimensionY = trimmingSubsetDimension;

                            subsetY = new Subset(new NumericTrimming(BigDecimalUtil.parseDecimalFromString(trimmingSubsetDimension.getLowerBound()),
                                    BigDecimalUtil.parseDecimalFromString(trimmingSubsetDimension.getUpperBound())),
                                    axisX.getNativeCrsUri(), axisX.getLabel());
                        }
                    }
                }

                List<Subset> xySubsets = new ArrayList<>();
                xySubsets.add(subsetX);
                xySubsets.add(subsetY);

                // Transform the subset from subsettingCRS to XY axes' CRS
                this.wcpsCoverageMetadataGeneralService.transformSubsettingCrsXYSubsets(wcpsCoverageMetadata, xySubsets);

                trimmingSubsetDimensionX.setLowerBound(subsetX.getNumericSubset().getLowerLimit().toPlainString());
                trimmingSubsetDimensionX.setUpperBound(subsetX.getNumericSubset().getUpperLimit().toPlainString());

                trimmingSubsetDimensionY.setLowerBound(subsetY.getNumericSubset().getLowerLimit().toPlainString());
                trimmingSubsetDimensionY.setUpperBound(subsetY.getNumericSubset().getUpperLimit().toPlainString());
            }


            List<String> gridDomainsTmp = new ArrayList<>();
            for (AbstractSubsetDimension subsetDimension : subsetDimensions) {
                // Only use trimming subsets on regular axis to create the target grid domain for rasql scale()
                Axis axis = wcpsCoverageMetadata.getAxisByName(subsetDimension.getDimensionName());

                if (axis instanceof RegularAxis && subsetDimension instanceof TrimmingSubsetDimension) {
                    TrimmingSubsetDimension trimmingSubsetDimension = ((TrimmingSubsetDimension) subsetDimension);
                    ParsedSubset<BigDecimal> parsedSubset = new ParsedSubset<>(BigDecimalUtil.parseDecimalFromString(trimmingSubsetDimension.getLowerBound()),
                                                                               BigDecimalUtil.parseDecimalFromString(trimmingSubsetDimension.getUpperBound()));

                    WcpsSubsetDimension wcpsSubsetDimension = new WcpsTrimSubsetDimension(axis.getLabel(), axis.getNativeCrsUri(),
                                                                                            ((TrimmingSubsetDimension) subsetDimension).getLowerBound(), ((TrimmingSubsetDimension) subsetDimension).getUpperBound());
                    ParsedSubset<Long> gridSubset = this.wcpsCoverageMetadataGeneralService.translateGeoToGridCoordinates(wcpsSubsetDimension, parsedSubset, axis, axis.getGeoBounds().getLowerLimit(), axis.getGeoBounds().getUpperLimit(), axis.getGridBounds().getLowerLimit(), axis.getGridBounds().getUpperLimit());
                    gridDomainsTmp.add(gridSubset.getLowerLimit() + ":" + gridSubset.getLowerLimit());
                }
            }

            List<String> sourceGridDomains = new ArrayList<>();
            for (int i = 0; i <gridDomainsTmp.size(); i++) {
                sourceGridDomains.add("0:0");
            }

            // e.g. select encode(scale( <[0:0,0:0] {99c,99c,99c}>, [0:30] ) , "csv")
            String coreGridDomains = "SCALE( <[" + ListUtil.join(sourceGridDomains, ",") + "] "
                                    + nullValuesRepresentation + ">, " + ListUtil.join(gridDomainsTmp, ",");

            if (subsettingCrs != null || outputCrs != null) {
                // In case of crsTransform, it needs projection
                String sourceEPSGCode = CrsUtil.getCode(wcpsCoverageMetadata.getXYCrs());
                String targetEPSGCode = null;

                if (subsettingCrs != null) {
                    targetEPSGCode = CrsUtil.getCode(subsettingCrs);
                } else if (outputCrs != null) {
                    targetEPSGCode = CrsUtil.getCode(outputCrs);
                }

                coreGridDomains = "project( " + coreGridDomains + ", " + sourceEPSGCode + ", " + targetEPSGCode + ") ";
            }

            query = "SELECT encode(" + coreGridDomains +  ", \"" + outputFormat + "\")";
        }

        Pair<String, String> userPair = AuthenticationService.getBasicAuthCredentialsOrRasguest(httpServletRequest);
        byte[] bytes = RasUtil.getRasqlResultAsBytes(query, userPair.fst, userPair.snd);

        Response response = new Response(Arrays.asList(bytes), outputFormat, wcpsCoverageMetadata.getCoverageName());
        return response;
    }

}
