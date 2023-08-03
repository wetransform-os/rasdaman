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
package petascope.wcps.handler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.rasdaman.domain.wms.Dimension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.util.CrsUtil;
import petascope.util.StringUtil;
import petascope.wcps.exception.processing.IncompatibleAxesNumberException;
import static petascope.wcps.handler.AbstractOperatorHandler.checkOperandIsCoverage;

import petascope.wcps.metadata.model.Axis;
import petascope.wcps.metadata.model.NumericTrimming;
import petascope.wcps.metadata.model.Subset;
import petascope.wcps.metadata.model.WcpsCoverageMetadata;
import petascope.wcps.metadata.service.RasqlTranslationService;
import petascope.wcps.metadata.service.SubsetParsingService;
import petascope.wcps.metadata.service.WcpsCoverageMetadataGeneralService;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.result.WcpsMetadataResult;
import petascope.wcps.result.WcpsResult;
import petascope.wcps.subset_axis.model.DimensionIntervalList;
import petascope.wcps.subset_axis.model.WcpsSubsetDimension;
import petascope.wcps.subset_axis.model.WcpsTrimSubsetDimension;

/**
 * Translator class for the extend operation in wcps for c in
 * (test_mean_summer_airtemp) return encode( extend(c,
 * {Lat:"http://www.opengis.net/def/crs/EPSG/0/3857"(-4000000:-2500000),
 * Long:"http://www.opengis.net/def/crs/EPSG/0/4326"(120:130)} ), "png",
 * "nodata=0")
 *
 * @author <a href="mailto:alex@flanche.net">Alex Dumitru</a>
 * @author <a href="mailto:vlad@flanche.net">Vlad Merticariu</a>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ExtendExpressionHandler extends Handler {

    @Autowired
    private WcpsCoverageMetadataGeneralService wcpsCoverageMetadataService;
    @Autowired
    private SubsetParsingService subsetParsingService;
    @Autowired
    private RasqlTranslationService rasqlTranslationService;
    
    public static final String OPERATOR = "extend";
    
    public ExtendExpressionHandler() {
        
    }
    
    public ExtendExpressionHandler create(Handler coverageExpressionHandler, Handler dimensionIntervalListHandler) {
        ExtendExpressionHandler result = new ExtendExpressionHandler();
        result.setChildren(Arrays.asList(coverageExpressionHandler, dimensionIntervalListHandler));
        result.wcpsCoverageMetadataService = this.wcpsCoverageMetadataService;
        result.subsetParsingService = this.subsetParsingService;
        result.rasqlTranslationService = rasqlTranslationService;
        
        return result;
    }
    
    public WcpsResult handle(List<Object> serviceRegistries) throws PetascopeException {
        WcpsResult coverageExpression = ((WcpsResult)this.getFirstChild().handle(serviceRegistries));
        VisitorResult secondChildVisitorResult = this.getSecondChild().handle(serviceRegistries);

        WcpsResult result = null;
        if (secondChildVisitorResult instanceof DimensionIntervalList) {
            // This comes from the case when extend(c, { [i(0:30), j(0:30)] })
            result = this.handle(coverageExpression, (DimensionIntervalList) secondChildVisitorResult);
        } else if (secondChildVisitorResult instanceof WcpsResult) {
            // This comes from the case when extend(c, {dst}) and dst is a LET clause variable
            WcpsResult wcpsResult = (WcpsResult) secondChildVisitorResult;

            if (wcpsResult.getDimensionIntervalList() != null) {
                // here LET dst := [i(0:30), j(0:30)]
                result = this.handle(coverageExpression, wcpsResult.getDimensionIntervalList());
            } else {
                // here LET dst := imageCrsdomain(c)
                WcpsMetadataResult wcpsMetadataResult = new WcpsMetadataResult(wcpsResult.getMetadata(), wcpsResult.getRasql());
                result = this.handle(coverageExpression, wcpsMetadataResult);
            }
        } else if (secondChildVisitorResult instanceof WcpsMetadataResult) {
            // This comes from the case when extend(c, {imageCrsdomain()})
            WcpsMetadataResult wcpsMetadataResult = (WcpsMetadataResult) secondChildVisitorResult;
            result = this.handle(coverageExpression, wcpsMetadataResult);
        }

        return result;
    }

    private WcpsResult handle(WcpsResult coverageExpression, DimensionIntervalList dimensionIntervalList) throws PetascopeException {

        checkOperandIsCoverage(coverageExpression, OPERATOR);         

        WcpsCoverageMetadata metadata = coverageExpression.getMetadata();
        // extend(coverageExpression, {domainIntervals})
        List<WcpsSubsetDimension> subsetDimensions = dimensionIntervalList.getIntervals();
        List<Subset> numericSubsets = subsetParsingService.convertToNumericSubsets(subsetDimensions, metadata.getAxes());
        
        if (metadata.getAxes().size() != numericSubsets.size()) {
            throw new IncompatibleAxesNumberException(metadata.getCoverageName(), metadata.getAxes().size(), numericSubsets.size());
        }

        // NOTE: from WCPS 1.0 standard: In this sense the extendExpr is a generalization of the trimExpr; still the trimExpr should be
        // used whenever the application needs to be sure that a proper subsetting has to take place.
        wcpsCoverageMetadataService.applySubsets(false, false, metadata, subsetDimensions, numericSubsets);

        // it will not get all the axis to build the intervals in case of (extend() and scale())
        String domainIntervals = rasqlTranslationService.constructSpecificRasqlDomain(metadata.getSortedAxesByGridOrder(), numericSubsets);
        String rasql = TEMPLATE.replace("$coverage", coverageExpression.getRasql())
                               .replace("$intervalList", domainIntervals);
        
        return new WcpsResult(metadata, rasql);
    }

    private WcpsResult handle(WcpsResult coverageExpression,
                              WcpsMetadataResult wcpsMetadataResult) throws PetascopeException {

        checkOperandIsCoverage(coverageExpression, OPERATOR);

        WcpsCoverageMetadata metadata = coverageExpression.getMetadata();

        List<WcpsSubsetDimension> subsetDimensions = new ArrayList<>();
        List<Subset> numericSubsets = new ArrayList<>();

        List<Axis> axes = metadata.getSortedAxesByGridOrder();
        String dimensionIntervalList = StringUtil.stripParentheses(wcpsMetadataResult.getResult());


        // e.g: imageCrsdomain(c) returns 0:30,0:40,0:60
        String[] values = dimensionIntervalList.split(",");

        if (axes.size() != values.length) {
            throw new IncompatibleAxesNumberException(metadata.getCoverageName(), axes.size(), values.length);
        }

        for (int i = 0; i < axes.size(); i++) {
            String axisLabel = axes.get(i).getLabel();
            String lowerValue = values[i].split(":")[0];
            String upperValue = values[i].split(":")[1];
            NumericTrimming numericTrimming = new NumericTrimming(new BigDecimal(lowerValue), new BigDecimal(upperValue));
            Subset subset = new Subset(numericTrimming, CrsUtil.GRID_CRS, axisLabel);
            numericSubsets.add(subset);

            subsetDimensions.add(new WcpsTrimSubsetDimension(axisLabel, CrsUtil.GRID_CRS, lowerValue, upperValue));
        }

        // NOTE: from WCPS 1.0 standard: In this sense the extendExpr is a generalization of the trimExpr; still the trimExpr should be
        // used whenever the application needs to be sure that a proper subsetting has to take place.
        wcpsCoverageMetadataService.applySubsets(false, false, metadata, subsetDimensions, numericSubsets);

        // it will not get all the axis to build the intervals in case of (extend() and scale())
        String rasql = TEMPLATE.replace("$coverage", coverageExpression.getRasql())
                .replace("$intervalList", dimensionIntervalList);

        return new WcpsResult(metadata, rasql);
    }

    private final String TEMPLATE = "EXTEND($coverage, [$intervalList])";
}
