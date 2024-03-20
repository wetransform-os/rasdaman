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
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.core.Pair;
import petascope.core.XMLSymbols;
import petascope.exceptions.PetascopeException;
import petascope.util.*;

import static petascope.wcps.handler.CoverageConstantHandler.updateAxisNamesFromAxisIterators;

import petascope.wcps.metadata.model.*;
import petascope.wcps.metadata.service.*;
import petascope.wcps.result.WcpsResult;
import petascope.wcps.subset_axis.model.AxisIterator;
import petascope.wcps.subset_axis.model.WcpsSubsetDimension;

import petascope.wcps.subset_axis.model.WcpsTrimSubsetDimension;

/**
 * Translation node from wcps coverage list to rasql for the general condenser
 * Example:  <code>
 * CONDENSE +
 * OVER x x(0:100)
 * WHERE true
 * USING 2
 * </code> translates to  <code>
 * CONDENSE +
 * OVER x in [0:100]
 * WHERE true
 * USING 2
 * </code>
 *
 * @author <a href="mailto:alex@flanche.net">Alex Dumitru</a>
 * @author <a href="mailto:vlad@flanche.net">Vlad Merticariu</a>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CoverageGeneralCondenserHandler extends Handler {

    @Autowired
    private WcpsCoverageMetadataGeneralService wcpsCoverageMetadataService;
    @Autowired
    private SubsetParsingService subsetParsingService;
    @Autowired
    private RasqlTranslationService rasqlTranslationService;
    @Autowired
    private AxisIteratorAliasRegistry axisIteratorAliasRegistry;
    @Autowired
    private UsingCondenseRegistry usingCondenseRegistry;
    @Autowired
    private CoverageConstructorHandler coverageConstructorHandler;
    
    public CoverageGeneralCondenserHandler() {
        
    }
    
    public CoverageGeneralCondenserHandler create(Handler operatorHandler, List<Handler> axisIteratorHandlers,
                                                  Handler whereClauseHandler, Handler usingClauseHandler) {
        CoverageGeneralCondenserHandler result = new CoverageGeneralCondenserHandler();
        List<Handler> childHandlers = new ArrayList<>();
        childHandlers.add(operatorHandler);
        childHandlers.addAll(axisIteratorHandlers);
        childHandlers.add(whereClauseHandler);
        childHandlers.add(usingClauseHandler);
        
        result.setChildren(childHandlers);
        
        result.wcpsCoverageMetadataService = this.wcpsCoverageMetadataService;
        result.subsetParsingService = this.subsetParsingService;
        result.rasqlTranslationService = this.rasqlTranslationService;
        result.axisIteratorAliasRegistry = this.axisIteratorAliasRegistry;
        result.usingCondenseRegistry = this.usingCondenseRegistry;
        result.coverageConstructorHandler = this.coverageConstructorHandler;
        
        return result;
    }

    @Override
    public WcpsResult handle(List<Object> serviceRegistries) throws PetascopeException {
        String operator = ((WcpsResult)this.getFirstChild().handle(serviceRegistries)).getRasql();
        // NOTE: this is important to handle general condenser for virtual coverage properly 
        // e.g. over $pt t (imageCrsdomain(c[unix("2011-01-01":"2012-01-01")], unix)) 
        //      using scale(c[unix($pt)] , {Lat:"CRS:1"(0:5)})
        this.usingCondenseRegistry.setOperator(operator);
        
        List<Handler> axisIteratorHandlers = this.getChildren().subList(1, this.getChildren().size() - 2);
        
        String rasqlAliasName = "";
        String aliasName = "";
        int count = 0;

        List<AxisIterator> temporalAxisIterators = new ArrayList<>();
        
        List<AxisIterator> axisIterators = new ArrayList<>();
        for (Handler axisIteratorHandler : axisIteratorHandlers) {
            AxisIterator axisIterator = (AxisIterator)axisIteratorHandler.handle(serviceRegistries);
            aliasName = axisIterator.getAliasName();
            axisIteratorAliasRegistry.addAxisIteratorAliasMapping(aliasName, axisIterator);
            
            if (rasqlAliasName.isEmpty()) {
                rasqlAliasName = StringUtil.stripDollarSign(aliasName);
            }
            
            axisIterator.setRasqlAliasName(rasqlAliasName);
            axisIterator.setAxisIteratorOrder(count);            
            
            axisIterators.add(axisIterator);
            count++;

            if (axisIterator.isTemporal()) {
                temporalAxisIterators.add(axisIterator);
            }
        }
        
        Handler whereClauseHandler = this.getChildren().get(this.getChildren().size() - 2);
        WcpsResult whereClauseResult = null;
        if (whereClauseHandler != null) {
            whereClauseResult = (WcpsResult) whereClauseHandler.handle(serviceRegistries);
        }

        Handler usingClauseHandler = this.getChildren().get(this.getChildren().size() - 1);

        WcpsResult result = null;
        
        if (temporalAxisIterators.isEmpty()) {
            // it doesn't have temporal axis iterator
            WcpsResult usingExpressionResult = (WcpsResult) usingClauseHandler.handle(serviceRegistries);
            if (usingExpressionResult.getMetadata() != null) {
                usingExpressionResult.getMetadata().setCondenserResult(true);
            }

            result = this.handle(operator, axisIterators, whereClauseResult, usingExpressionResult);
        } else {
            // it has temporal axis iterator
            for (AxisIterator temporalAxisIterator : temporalAxisIterators) {
                Pair<List<WcpsResult>, List<BigDecimal>> pair = this.coverageConstructorHandler.handleTemporalAxisIterator(serviceRegistries,
                                                                                                temporalAxisIterator,
                                                                                                usingClauseHandler, false);
                List<WcpsResult> segmentedUsingExpressionResults = pair.fst;
                List<BigDecimal> dateTimeCoefficients = pair.snd;

                result = this.handleSegmentedWcpsResultsForTemporalAxisIterator(operator,
                                                                                temporalAxisIterator, axisIterators,
                                                                                whereClauseResult,
                                                                                segmentedUsingExpressionResults, dateTimeCoefficients);
            }
        }

        return result;
    }

    /**
     * From the segmented slicing datetimeIntervals, then create a final wcpsResult with combined rasql query and Wcps coverage metadata
     */
    private WcpsResult handleSegmentedWcpsResultsForTemporalAxisIterator(String operator,
                                                                         AxisIterator temporalAxisIterator,
                                                                         List<AxisIterator> axisIterators,
                                                                         WcpsResult whereClauseResult,
                                                                         List<WcpsResult> segmentedUsingExpressionResults,
                                                                         List<BigDecimal> dateTimeCoefficients) throws PetascopeException {

        String temporalAxisIteratorName = temporalAxisIterator.getAxisName();

        List<String> subRasqlQueries = new ArrayList<>();

        int totalGridPixels = 0;

        WcpsCoverageMetadata metadataResult = null;

        for (WcpsResult segmentedUsingExpressionResult : segmentedUsingExpressionResults) {
            if (metadataResult == null) {
                metadataResult = (WcpsCoverageMetadata) JSONUtil.clone(segmentedUsingExpressionResult.getMetadata());
            }

            Axis temporalAxis = segmentedUsingExpressionResult.getMetadata().getAxisByName(temporalAxisIteratorName);
            String gridLowerBound = String.valueOf(temporalAxis.getGridBounds().getLowerLimit().longValue());
            String gridUpperBound = String.valueOf(temporalAxis.getGridBounds().getUpperLimit().longValue());

            totalGridPixels += temporalAxis.getGridBounds().getUpperLimit().longValue() - temporalAxis.getGridBounds().getLowerLimit().longValue();
            WcpsTrimSubsetDimension trimSubsetDimension = new WcpsTrimSubsetDimension(temporalAxisIteratorName, CrsUtil.GRID_CRS, gridLowerBound, gridUpperBound);
            temporalAxisIterator.setSubsetDimension(trimSubsetDimension);

            WcpsResult segmentedCoverageConstructorResult = this.handle(operator, axisIterators, whereClauseResult, segmentedUsingExpressionResult);
            String subRaqlQuery = segmentedCoverageConstructorResult.getRasql();
            subRasqlQueries.add(" ( " + subRaqlQuery + " ) ");
        }

        // Then, create a WcpsResult object for the axis iterator as an irregular axis
        String concatenatedRasqlQueriesStr = "";
        if (subRasqlQueries.size() == 1) {
            concatenatedRasqlQueriesStr = subRasqlQueries.get(0);
        } else {
            // e.g. MARRAY pt in [0:3] VALUES c[pt[0],0:35,0:17]) WITH (c[0:0,0:35,0:17]) WITH (c[0:0,0:35,0:17])
            concatenatedRasqlQueriesStr = ListUtil.join(subRasqlQueries, " " + operator);
        }

        WcpsResult result = this.coverageConstructorHandler.handleFinalTemporalAxisIteratorResult(temporalAxisIterator, metadataResult,
                                                                                    totalGridPixels, dateTimeCoefficients, concatenatedRasqlQueriesStr, true);
        return result;
    }

    // -------- all axis iterators handler


    private WcpsResult handle(String operator, List<AxisIterator> axisIterators, WcpsResult whereClauseExpression,
                                WcpsResult usingCoverageExpression) throws PetascopeException {
        
        // contains subset dimension without "$"
        List<WcpsSubsetDimension> pureSubsetDimensions = new ArrayList<>();
        // contains subset dimension with "$"
        List<WcpsSubsetDimension> axisIteratorSubsetDimensions = new ArrayList<>();

        // All of the axis iterators uses the same rasql alias name (e.g: px)
        String rasqlAliasName = "";
        
        List<Axis> axes = new ArrayList<>();

        for (AxisIterator axisIterator : axisIterators) {
            String alias = axisIterator.getAliasName();
            WcpsSubsetDimension subsetDimension = axisIterator.getSubsetDimension();

            if (rasqlAliasName.isEmpty()) {
                rasqlAliasName = alias.replace(WcpsSubsetDimension.AXIS_ITERATOR_DOLLAR_SIGN, "");
            }
            // Check if axis iterator's subset dimension which has the "$"
            
            String bounds = axisIterator.getSubsetDimension().getStringBounds();
            if (bounds.contains(WcpsSubsetDimension.AXIS_ITERATOR_DOLLAR_SIGN) || bounds.contains("[")) {
                // e.g. axis iterator in rasql: pt[0] is used as lower / upper bound
                axisIteratorSubsetDimensions.add(subsetDimension);
                axes.add(new RegularAxis(subsetDimension.getAxisName(), null, null, null, null, null, null, null, 0, BigDecimal.ZERO, BigDecimal.ONE, null));
            } else {
                pureSubsetDimensions.add(subsetDimension);
                axes.add(axisIterator.getAxis());
            }
        }

        //create a coverage with the domain expressed in the condenser
        List<Subset> numericSubsets = subsetParsingService.convertToRawNumericSubsets(pureSubsetDimensions, axes);
        WcpsCoverageMetadata metadata = wcpsCoverageMetadataService.createCoverage(CONDENSER_TEMP_NAME, usingCoverageExpression.getMetadata(), numericSubsets, axes);
        
        updateAxisNamesFromAxisIterators(metadata, axisIterators);

        String rasqlAxisIteratorGridDomain = rasqlTranslationService.constructRasqlDomain(metadata.getSortedAxesByGridOrder(),
                                                                                        axisIteratorSubsetDimensions, false);
        String template = TEMPLATE.replace("$operation", operator)
                                  .replace("$iter", rasqlAliasName)
                                  .replace("$intervals", rasqlAxisIteratorGridDomain)
                                  .replace("$using", usingCoverageExpression.getRasql());
        

        if (whereClauseExpression != null) {
            template = template.replace("$whereClause", whereClauseExpression.getRasql());
        } else {
            template = template.replace("$whereClause", "");
        }

        return new WcpsResult(usingCoverageExpression.getMetadata(), template);
    }

    public static final String USING = "USING";
    private final String CONDENSER_TEMP_NAME = "CONDENSE_TEMP";
    private final String TEMPLATE = "CONDENSE $operation OVER $iter in [$intervals] $whereClause " + USING + " $using";
}
