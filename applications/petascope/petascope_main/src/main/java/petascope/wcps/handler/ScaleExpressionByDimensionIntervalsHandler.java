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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import liquibase.pro.packaged.D;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.core.Pair;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.PetascopeRuntimeException;
import petascope.exceptions.WCPSException;
import petascope.util.BigDecimalUtil;
import petascope.util.CrsUtil;
import petascope.util.JSONUtil;
import static petascope.wcps.handler.AbstractOperatorHandler.checkOperandIsCoverage;

import petascope.util.StringUtil;
import petascope.wcps.exception.processing.Coverage0DMetadataNullException;
import petascope.wcps.exception.processing.IncompatibleAxesNumberException;
import petascope.wcps.metadata.model.Axis;
import petascope.wcps.metadata.model.IrregularAxis;
import petascope.wcps.metadata.model.NumericTrimming;
import petascope.wcps.metadata.model.Subset;
import petascope.wcps.metadata.model.WcpsCoverageMetadata;
import petascope.wcps.metadata.service.*;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.result.WcpsMetadataResult;
import petascope.wcps.result.WcpsResult;
import petascope.wcps.subset_axis.model.DimensionIntervalList;
import petascope.wcps.subset_axis.model.WcpsSubsetDimension;
import petascope.wcps.subset_axis.model.WcpsTrimSubsetDimension;

import javax.servlet.http.HttpServletRequest;

/**
 * Class to translate a scale wcps expression into rasql  <code>
 *    SCALE($coverageExpression, [$dimensionIntervalList])
 * </code>
 *
 * @author <a href="mailto:alex@flanche.net">Alex Dumitru</a>
 * @author <a href="mailto:vlad@flanche.net">Vlad Merticariu</a>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ScaleExpressionByDimensionIntervalsHandler extends Handler {

    @Autowired
    private WcpsCoverageMetadataGeneralService wcpsCoverageMetadataService;
    @Autowired
    private SubsetParsingService subsetParsingService;
    @Autowired
    private SubsetExpressionHandler subsetExpressionHandler;
    @Autowired
    private RasqlTranslationService rasqlTranslationService;
    @Autowired
    private WcpsCoverageMetadataTranslator wcpsCoverageMetadataTranslatorService;
    
    @Autowired
    private CoverageAliasRegistry coverageAliasRegistry;
    @Autowired
    private AxisIteratorAliasRegistry axisIteratorAliasRegistry;
    @Autowired
    private StringScalarHandler stringScalarHandler;
    @Autowired
    private LetClauseAliasRegistry letClauseAliasRegistry;
    @Autowired
    private LetClauseAliasCoverageExpressionHandlerRegistry letClauseAliasCoverageExpressionHandlerRegistry;
    @Autowired
    // proxied by Spring framework
    private HttpServletRequest httpServletRequest;
    @Autowired
    private CoordinateTranslationService coordinateTranslationService;


    @Autowired
    private WcpsCoverageMetadataTranslator wcpsCoverageMetadataTranslator;

    public static final String OPERATOR = "scale";
    
    public ScaleExpressionByDimensionIntervalsHandler() {
        
    }
    
    public ScaleExpressionByDimensionIntervalsHandler create(Handler coverageExpressionHandler, Handler dimensionIntervalListHandler) {
        ScaleExpressionByDimensionIntervalsHandler result = new ScaleExpressionByDimensionIntervalsHandler();
        
        result.setChildren(Arrays.asList(coverageExpressionHandler, dimensionIntervalListHandler));
        
        result.wcpsCoverageMetadataService = this.wcpsCoverageMetadataService;
        result.subsetParsingService = this.subsetParsingService;
        result.subsetExpressionHandler = this.subsetExpressionHandler;
        result.rasqlTranslationService = this.rasqlTranslationService;
        result.wcpsCoverageMetadataTranslatorService = this.wcpsCoverageMetadataTranslatorService;
        
        result.coverageAliasRegistry = coverageAliasRegistry;
        result.axisIteratorAliasRegistry = axisIteratorAliasRegistry;
        result.stringScalarHandler = stringScalarHandler;

        result.letClauseAliasRegistry = letClauseAliasRegistry;
        result.letClauseAliasCoverageExpressionHandlerRegistry = letClauseAliasCoverageExpressionHandlerRegistry;

        result.wcpsCoverageMetadataTranslator = wcpsCoverageMetadataTranslator;
        result.httpServletRequest = httpServletRequest;
        result.coordinateTranslationService = coordinateTranslationService;


        this.injectedServicesRegistries = Arrays.asList(
                wcpsCoverageMetadataService,
                subsetParsingService,
                subsetExpressionHandler,
                rasqlTranslationService,
                wcpsCoverageMetadataTranslatorService,
                coverageAliasRegistry,
                axisIteratorAliasRegistry,
                stringScalarHandler,
                letClauseAliasRegistry,
                letClauseAliasCoverageExpressionHandlerRegistry,
                wcpsCoverageMetadataTranslator,
                httpServletRequest,
                coordinateTranslationService

        );
        result.injectedServicesRegistries = injectedServicesRegistries;
        
        return result;
        
    }
    
    public WcpsResult handle(List<Object> serviceRegistries) throws PetascopeException {
        // e.g. scale( (c0 + c1)[ansi("2016-07-25"), {...} )
        // then, first handle (c0 + c1)[ansi("2016-07-25")] to convert it to: (c0[ansi("2016-07-25")] + c1[ansi("2016-07-25")])
        // and after that, update query tree with current scale node to:
        // scale( c0[ansi("2016-07-25")], {...} ) + scale( c1[ansi("2016-07-25")], {...} )
        boolean isCurrentNodeRemoved = false;

        if (!this.isQueryTreeUpdated) {
            // NOTE: this is required in case, e.g. scale( (c + d)[i:"CRS:1"(0:10)], { ... } )
            // then, firstChild is ShorthandSubsetHandler will change to (c[i:"CRS:1"(0:10)] + d[i:"CRS:1"(0:10)])
            // then, scale can be created to each operand: ( scale(c[i:"CRS:1"(0:10)], {...}) + scale(d[i:"CRS:1"(0:10)], {...}) )
            this.getFirstChild().handle(serviceRegistries);

            if (this.letClauseAliasCoverageExpressionHandlerRegistry.getMap().size() > 0) {
                this.fillQueryTreeByLetClauseExpressions();
            }

            if (!(this.getParent() instanceof LetClauseHandler) && (this.getChildren().size() > 0)) {
                this.updateQueryTree(this.getFirstChild(), this.getSecondChild());
            }

            if (this.isQueryTreeUpdated) {
                // remove the current node from the tree
                int currentNodeIndex = this.getChildIndexInParentsList();

                Handler parentHandler = this.getParent();
                Handler firstChildFromScaleHandler = this.getFirstChild();

                parentHandler.getChildren().set(currentNodeIndex, firstChildFromScaleHandler);
                firstChildFromScaleHandler.setParent(parentHandler);

                isCurrentNodeRemoved = true;
            }

            this.isQueryTreeUpdated = true;
        }

        WcpsResult coverageExpressionResult = (WcpsResult) this.getFirstChild().handle(serviceRegistries);

        if (!isCurrentNodeRemoved) {
            // this node is not updated in the query tree
            VisitorResult secondChildHandlerResult = this.getSecondChild().handle(serviceRegistries);
            DimensionIntervalList dimensionIntervalList = null;

            if (secondChildHandlerResult instanceof WcpsMetadataResult) {
                // In case scale(..., { imageCrsdomain() })
                WcpsMetadataResult metadataResult = (WcpsMetadataResult)secondChildHandlerResult;
                dimensionIntervalList = this.convertFromImageCrsDomain(coverageExpressionResult, metadataResult);
            } else {
                dimensionIntervalList = (DimensionIntervalList) secondChildHandlerResult;
            }

            coverageExpressionResult = this.handle(coverageExpressionResult, dimensionIntervalList, true, null,
                                                    serviceRegistries);
        }

        return coverageExpressionResult;

    }

    private DimensionIntervalList convertFromImageCrsDomain(WcpsResult coverageExpression, WcpsMetadataResult domainIntervalsResult) {
        checkOperandIsCoverage(coverageExpression, OPERATOR);

        String dimensionIntervalList = StringUtil.stripParentheses(domainIntervalsResult.getResult());

        WcpsCoverageMetadata metadata = coverageExpression.getMetadata();

        // e.g: imageCrsdomain(c) returns 0:30,0:40,0:60 in grid order (e.g. Long Lat, not EPSG:4326 Lat Long geo order)
        String[] values = dimensionIntervalList.split(",");
        List<Axis> sortedAxesByGridOrder = metadata.getSortedAxesByGridOrder();

        if (sortedAxesByGridOrder.size() != values.length) {
            throw new IncompatibleAxesNumberException(metadata.getCoverageName(), sortedAxesByGridOrder.size(), values.length);
        }

        List<WcpsSubsetDimension> subsetDimensions = new ArrayList<>();

        for (int i = 0; i < sortedAxesByGridOrder.size(); i++) {
            Axis axis = sortedAxesByGridOrder.get(i);
            String[] gridBounds = values[i].split(":");
            String lowerValue = gridBounds[0];
            String upperValue = gridBounds[1];

            WcpsSubsetDimension trimSubsetDimension = new WcpsTrimSubsetDimension(axis.getLabel(), CrsUtil.GRID_CRS, lowerValue, upperValue);
            subsetDimensions.add(trimSubsetDimension);
        }

        DimensionIntervalList result = new DimensionIntervalList(subsetDimensions);
        return result;
    }

    /**
     * e.g. LET $sub := [ansi("2016-06-07")],
     *          $band := $c[ $sub ]
     * return encode( SCALE( $band, {...} ) , "png")
     * then, change the query tree to:
     * return encode( SCALE ( $c[ansi("2016-06-07")], {...} ), "png")
     */
    private void fillQueryTreeByLetClauseExpressions() throws PetascopeException {
        Handler parentHandler = this.getParent();
        while (!(parentHandler instanceof EncodeCoverageHandler)) {
            if (parentHandler.getParent() != null) {
                parentHandler = parentHandler.getParent();
            } else {
                break;
            }
        }

        // Integer points to the index of a child Handler in its parent's children handlers list
        Queue<Pair<Handler, Integer>> queue = new ArrayDeque<>();
        for (int i = 0; i < parentHandler.getChildren().size(); i++) {
            queue.add(new Pair<>(parentHandler.getChildren().get(i), i));
        }

        while (!queue.isEmpty()) {
            Pair<Handler, Integer> pair = queue.remove();
            Handler currentHandler = pair.fst;
            int indexInParentChildrenHandlersList = pair.snd;

            if (currentHandler instanceof CoverageVariableNameHandler) {
                // e.g. $band
                String alias = ((StringScalarHandler)currentHandler.getFirstChild()).getValue();
                Handler letClauseExpressionHandler = this.letClauseAliasCoverageExpressionHandlerRegistry.get(alias);
                if (letClauseExpressionHandler != null) {
                    Handler tmp = (Handler) JSONUtil.clone(letClauseExpressionHandler);
                    tmp.injectedServicesRegistries = this.injectedServicesRegistries;

                    Queue<Handler> childQueue = new ArrayDeque<>();
                    if (tmp.getChildren() != null) {
                        childQueue.add(tmp);
                        for (Handler handlerTmp : tmp.getChildren()) {
                            if (handlerTmp != null) {
                                childQueue.add(handlerTmp);
                            }
                        }
                    }

                    while (!childQueue.isEmpty()) {
                        Handler childHandlerTmp = childQueue.remove();
                        List<Field> fields = FieldUtils.getFieldsListWithAnnotation(childHandlerTmp.getClass(), Autowired.class);
                        for (Field field : fields) {
                            field.setAccessible(true);
                            Object service = this.getServiceRegistry(field.getType().getName());
                            try {
                                field.set(childHandlerTmp, service);
                            } catch (Exception ex) {
                                throw new PetascopeException(ExceptionCode.InternalComponentError,
                                            "Cannot set injected service for field: " + field.getName()
                                            + " of class: " + field.getType().getName() + ". Reason: " + ex.getMessage(), ex);
                            }
                        }
                        childHandlerTmp.injectedServicesRegistries = this.injectedServicesRegistries;

                        if (childHandlerTmp.getChildren() != null) {
                            for (Handler handlerTmp : childHandlerTmp.getChildren()) {
                                if (handlerTmp != null) {
                                    childQueue.add(handlerTmp);
                                }
                            }
                        }
                    }

                    Handler correctHandlerToFill = tmp;
                    // replace $band with  $c[ $sub ] from LET clause
                    Handler parentHandlerTmp = currentHandler.getParent();
                    parentHandlerTmp.getChildren().set(indexInParentChildrenHandlersList, correctHandlerToFill);
                    correctHandlerToFill.setParent(parentHandlerTmp);

                    currentHandler = correctHandlerToFill;
                    queue.add(new Pair<>(currentHandler, indexInParentChildrenHandlersList));
                }
            }

            if (currentHandler != null && currentHandler.getChildren() != null) {
                for (int i = 0; i < currentHandler.getChildren().size(); i++) {
                    Handler childHandlerTmp = currentHandler.getChildren().get(i);
                    if (!(childHandlerTmp instanceof StringScalarHandler
                        || childHandlerTmp instanceof RealNumberConstantHandler
                        || childHandlerTmp instanceof DimensionIntervalListHandler)) {

                        // e.g. $sub from $c [ $sub ] and $sub defined in LET clause
                        queue.add(new Pair<>(childHandlerTmp, i));
                    }
                }
            }
        }
    }
    

    /**
     * For example, this query:
     * for $c in (base_cov)
     * LET $subset := [ansi("2017-06-06")],
     *     $bands := $c[ $subset ]
     * return encode( SCALE($bands, {X:"CRS:1"(0:20), Y:"CRS:1"(0:30)}), "png")
     *
     * After SCALE, $c should use **pyramid_cov** instead of base_cov,
     * hence, this function will traverse up to LET clause list expression and change $bands to $bands := $c_0[ $subset ]
     * with $c_0 points to **pyramid_cov** and this expression needs to be recalculated corresponding to **pyramid_cov**.
     */

    public WcpsResult handle(WcpsResult coverageExpression, DimensionIntervalList dimensionIntervalList, boolean implicitScaleByXorYAxis, Handler firstChildHandler,
                             List<Object> serviceRegistries
                ) throws PetascopeException {
        // SCALE LEFT_PARENTHESIS
        //          coverageExpression COMMA LEFT_BRACE dimensionIntervalList RIGHT_BRACE (COMMA fieldInterpolationList)*
        //       RIGHT_PARENTHESIS
        // scale($c, {intervalList})
        // e.g: scale(c[t(0)], {Lat:"CRS:1"(0:200), Long:"CRS:1"(0:300)}

        if (coverageExpression.getMetadata() == null) {
            throw new Coverage0DMetadataNullException(OPERATOR);
        }

        checkOperandIsCoverage(coverageExpression, OPERATOR);

        WcpsCoverageMetadata metadata = coverageExpression.getMetadata();
        // scale(coverageExpression, {domainIntervals})
        List<WcpsSubsetDimension> subsetDimensions = dimensionIntervalList.getIntervals();
        for (WcpsSubsetDimension subset : subsetDimensions) {
            this.validateAxisLabelExist(metadata, subset.getAxisName());
        }
        List<Subset> numericSubsets = subsetParsingService.convertToNumericSubsets(subsetDimensions, metadata.getAxes());

        if (this.processXOrYAxisImplicitly(metadata, numericSubsets)) {
            this.handleScaleWithOnlyXorYAxis(coverageExpression, numericSubsets, implicitScaleByXorYAxis);
        }

        // Then, check if any non-XY axes from coverage which are not specified from the scale() interval
        // will be added implitcily to the scale domains interval
        List<Axis> nonXYAxes = metadata.getNonXYAxes();
        for (Axis axis : nonXYAxes) {
            boolean exists = false;
            for (Subset inputSubset : numericSubsets) {
                if (CrsUtil.axisLabelsMatch(axis.getLabel(), inputSubset.getAxisName())) {
                    exists = true;
                    break;
                }
            }
            
            // Axis is not specified in the domain intervals for scale()
            if (!exists) {
                BigDecimal geoLowerBound = axis.getGeoBounds().getLowerLimit();
                BigDecimal geoUpperBound = axis.getGeoBounds().getUpperLimit();
                
                numericSubsets.add(new Subset(new NumericTrimming(geoLowerBound, geoUpperBound), axis.getNativeCrsUri(), axis.getLabel()));
                subsetDimensions.add(new WcpsTrimSubsetDimension(axis.getLabel(), axis.getNativeCrsUri(), 
                                                                 geoLowerBound.toPlainString(),
                                                                 geoUpperBound.toPlainString()));
            }
        }
        
        for (Axis axis : metadata.getAxes()) {
            boolean exists = false;
            for (WcpsSubsetDimension subsetDimension : subsetDimensions) {
                if (CrsUtil.axisLabelsMatch(subsetDimension.getAxisName(), axis.getLabel())) {
                    exists = true;
                    break;
                }
            }
            
            if (!exists) {
                String lowerBound = axis.getLowerGeoBoundRepresentation();
                String upperBound = axis.getUpperGeoBoundRepresentation();
                for (Subset subset : numericSubsets) {
                    if (subset.getAxisName().equals(axis.getLabel())) {
                        lowerBound = subset.getNumericSubset().getLowerLimit().toPlainString();
                        upperBound = subset.getNumericSubset().getUpperLimit().toPlainString();
                        break;
                    }                    
                }
                
                subsetDimensions.add(new WcpsTrimSubsetDimension(axis.getLabel(), axis.getNativeCrsUri(), lowerBound, upperBound));
            }
        }

        List<Pair> geoBoundAxes = new ArrayList();
        List<Pair> gridBoundAxes = new ArrayList();
        Map<String, List<BigDecimal>> directPositionsMap = new HashMap<>();
        for (Axis axis : metadata.getAxes()) {
            NumericTrimming numericTrimming = new NumericTrimming(axis.getGeoBounds().getLowerLimit(), axis.getGeoBounds().getUpperLimit());
            Pair<String, NumericTrimming> pair = new Pair(axis.getLabel(), numericTrimming);
            geoBoundAxes.add(pair);
            
            NumericTrimming gridNumericTrimming = new NumericTrimming(new BigDecimal(axis.getGridBounds().getLowerLimit().toPlainString()),
                                                                      new BigDecimal(axis.getGridBounds().getUpperLimit().toPlainString()));
            Pair<String, NumericTrimming> gridPair = new Pair(axis.getLabel(), gridNumericTrimming);
            gridBoundAxes.add(gridPair);
            
            if (axis instanceof IrregularAxis) {
                List<BigDecimal> currentDirectPositions = ((IrregularAxis) axis).getDirectPositions();
                List<BigDecimal> tmpDirectPositions = new ArrayList<>();
                for (BigDecimal value : currentDirectPositions) {
                    tmpDirectPositions.add(new BigDecimal(value.toPlainString()));
                }
                
                directPositionsMap.put(axis.getLabel(), tmpDirectPositions);
            }
        }
        
        // Only for 2D XY coverage imported with downscaled collections
        WcpsCoverageMetadata selectedCoverage = this.wcpsCoverageMetadataTranslatorService.applyDownscaledLevelOnXYGridAxesForScale(coverageExpression, metadata, numericSubsets);
        String selectedCoverageId = selectedCoverage.getCoverageName();
        
        if (!selectedCoverageId.equals(metadata.getCoverageName())) {
            // NOTE: here it needs to recalculate coverageExpression based on the new pyramid member
            // a pyramid member is selected, this scale expression needs to rerun for this selected pyramid member
            String selectedRasdamanCollectionName = selectedCoverage.getRasdamanCollectionName();
            
            this.coverageAliasRegistry.addDownscaledCoverageAliasId(selectedCoverageId, selectedRasdamanCollectionName);
            // e.g. $c -> points to base_cov
            String baseCoverageId = metadata.getCoverageName();
            String baseRasdamanCollectionName = metadata.getRasdamanCollectionName();

            String baseCoverageAlias = this.coverageAliasRegistry.getAliasByCoverageName(baseCoverageId);
            // e.g. $c_0 -> points to pyramid_member_cov
            String pyramidMemberCoverageAlias = this.coverageAliasRegistry.retrieveDownscaledCoverageAliasByCoverageId(selectedCoverageId);
            if (!this.coverageAliasRegistry.existInFinalCoverageAliasMappings(pyramidMemberCoverageAlias)) {
                this.coverageAliasRegistry.addToFinalCoverageAliasMappings(pyramidMemberCoverageAlias, selectedCoverageId, selectedRasdamanCollectionName);
            }
            
            Handler firstChildHandlerTmp = firstChildHandler;
            if (firstChildHandlerTmp == null) {
                firstChildHandlerTmp = this.getFirstChild();
            }

            // e.g. $c and $c_0 (pyramid member coverage alias)
            List<String> baseCoverageAliases = this.coverageAliasRegistry.getAliasesByCoverageName(baseCoverageId);
            for (String baseAliasTmp : baseCoverageAliases) {
                coverageAliasRegistry.addCoverageAliasAndDownscaledAlias(baseAliasTmp, pyramidMemberCoverageAlias);
            }

            // Then, temporarily update coverage aliases to point to selected pyramid member instead of base coverage
            coverageAliasRegistry.replaceBaseCoverageByDownscaledCoverage(baseCoverageId,
                                                                        selectedCoverageId,
                                                                        selectedRasdamanCollectionName);

            WcpsResult coverageExpressionResult = (WcpsResult)firstChildHandlerTmp.handle(serviceRegistries);
            coverageExpression = coverageExpressionResult;
            metadata = coverageExpressionResult.getMetadata();

            // NOTE:after scaling, revert downscaled alias back to base alias for other expression handlers
            // e.g. scale($c, {...}) + avg($c) then inside scale() $c can be a pyramid member, but inside avg() $c must be a base coverage
            coverageAliasRegistry.replaceBaseCoverageByDownscaledCoverage(selectedCoverageId,
                                                                        baseCoverageId,
                                                                        baseRasdamanCollectionName);
            for (String baseAliasTmp : baseCoverageAliases) {
                coverageAliasRegistry.removeCoverageAliasAndDownscaledAlias(baseAliasTmp);
            }

        }
        
        
        // NOTE: from WCPS 1.0 standard, C2 = scale(C1, {x(lo1:hi1), y(lo2:hi2),...}        
        // for all a ∈ dimensionList(C2), c ∈ crsSet(C2, a):
        //          imageCrsDomain(C2 , a ) = (lo:hi) - it means: ***axis's grid domain will be set*** to corresponding lo:hi!
        //          domain(C2,a,c) = domain(C1,a,c) - it means: ***axis's geo domain will not change***!
        wcpsCoverageMetadataService.applySubsets(false, false, metadata, subsetDimensions, numericSubsets);
        
        this.addImplicitScaleGridIntervals(metadata, numericSubsets);
        
        // it will not get all the axis to build the intervals in case of (extend() and scale())
        String domainIntervals = rasqlTranslationService.constructSpecificRasqlDomain(metadata.getSortedAxesByGridOrder(), numericSubsets);

        String rasql = coverageExpression.getRasql();
        rasql = TEMPLATE.replace("$coverage", coverageExpression.getRasql())
                         .replace("$intervalList", domainIntervals);

        
        this.revertAfterScale(metadata, geoBoundAxes, directPositionsMap);
        this.applyScaleOnIrregularAxes(metadata, gridBoundAxes);
        
        return new WcpsResult(metadata, rasql);
    }


    /**
     * Check if scaling axis exists in the input coverage
     */
    private void validateAxisLabelExist(WcpsCoverageMetadata metadata, String scaleAxisLabel) {
        List<Axis> coverageAxes = metadata.getAxes();
        
        boolean result = false;
        for (Axis axis : coverageAxes) {
            if (CrsUtil.axisLabelsMatch(axis.getLabel(), scaleAxisLabel)) {
                result = true;
                break;
            }
        }
        
        if (!result) {
            throw new WCPSException(ExceptionCode.InvalidAxisLabel, "Scaling axis label '" + scaleAxisLabel + "' does not exist in coverage '" + metadata.getCoverageName() + "'.");
        }
    }
    
    /**
     * Special case, only 1 X or Y axis specified, find the grid domain for another axis implicitly from the specified axis
     */
    private void handleScaleWithOnlyXorYAxis(WcpsResult coverageExpression, List<Subset> subsets, boolean implicitScaleByXorYAxis) {
        // e.g: for c in (test_mean_summer_airtemp) return encode(scale( c, { Long:"CRS:1"(0:10)} ), "png")
        Subset subset1 = subsets.get(0);
        BigDecimal lowerLimit1 = subset1.getNumericSubset().getLowerLimit();
        BigDecimal upperLimit1 = subset1.getNumericSubset().getUpperLimit();            

        // e.g: Long axis has grid bounds: 30:50
        Axis axis1 = coverageExpression.getMetadata().getAxisByName(subset1.getAxisName());
        List<Axis> xyAxes = coverageExpression.getMetadata().getXYAxes();
        Axis axis2 = null;
        for (Axis axis : xyAxes) {
            if (!CrsUtil.axisLabelsMatch(axis.getLabel(), subset1.getAxisName())) {
                axis2 = axis;
                break;
            }
        }
        
        if (!implicitScaleByXorYAxis) {
            // NOTE: for example scaleextent() of WCS scale extension, it doesn't have this auto implicitly scale ratio by X or Y axis
            NumericTrimming numericTrimming = new NumericTrimming(axis2.getGridBounds().getLowerLimit(), axis2.getGridBounds().getUpperLimit());
            Subset subset2 = new Subset(numericTrimming, axis2.getNativeCrsUri(), axis2.getLabel());
            subsets.add(subset2);
            return;
        }        
        
        BigDecimal gridDistance1 = axis1.getGridBounds().getUpperLimit().subtract(axis1.getGridBounds().getLowerLimit());
        // scale ratio is: (10 - 0) / (50 - 30) = 10 / 20 = 0.5 (downscale)
        BigDecimal scaleRatio = BigDecimalUtil.divide(upperLimit1.subtract(lowerLimit1), gridDistance1);

        // Lat axis has grid bounds: 60:70
        // -> scale on Lat axis: 0:(70 - 60) * 0.5 = 0:5
        BigDecimal gridDistance2 = axis2.getGridBounds().getUpperLimit().subtract(axis2.getGridBounds().getLowerLimit());
        BigDecimal lowerLimit2 = BigDecimal.ZERO;
        BigDecimal upperLimit2 = gridDistance2.multiply(scaleRatio);
        NumericTrimming numericTrimming = new NumericTrimming(lowerLimit2, upperLimit2);

        Subset subset2 = new Subset(numericTrimming, subset1.getCrs(), axis2.getLabel());
        subsets.add(subset2);
    }    
    
    
    /**
     * Add each axis's grid domains which is not decleared in the scale's interval explicitly
     */
    private void addImplicitScaleGridIntervals(WcpsCoverageMetadata metadata, List<Subset> gridNumericSubsets) {
        for (Axis axis : metadata.getAxes()) {
            boolean exists = false;
            for (Subset subset : gridNumericSubsets) {
                if (CrsUtil.axisLabelsMatch(axis.getLabel(), subset.getAxisName())) {
                    exists = true;
                    break;
                }
            }
            
            if (!exists) {
                NumericTrimming numericTrimming = new NumericTrimming(axis.getGridBounds().getLowerLimit(), axis.getGeoBounds().getUpperLimit());
                Subset subset = new Subset(numericTrimming, axis.getNativeCrsUri(), axis.getLabel());
                gridNumericSubsets.add(subset);
            }
        }
    }
    
    /**
     * Revert some values after applying subset from scale's intervals
     */
    private void revertAfterScale(WcpsCoverageMetadata metadata, List<Pair> geoBoundAxes, Map<String, List<BigDecimal>> directPositionsMap) {
        // Revert the changed axes' geo bounds as before applying scale subsets.
        // e.g: scale(c, {Lat:"CRS:1"(0:20), Long:"CRS:1"(0:20)} and before scale, 
        // coverage has geo domains: Lat(-40, 40), Long(-30, 30), grid domains: Lat":CRS:1"(0:300), Long:"CRS:1"(0:200)
        // After scale, the geo domains are kept and grid domain will be: Lat":CRS1:"(0:20), Long:"CRS:1"(0:20)
        for (Axis axis : metadata.getAxes()) {
            for (Pair<String, NumericTrimming> pair : geoBoundAxes) {
                if (CrsUtil.axisLabelsMatch(axis.getLabel(), pair.fst)) {
                    axis.getGeoBounds().setLowerLimit(pair.snd.getLowerLimit());
                    axis.getGeoBounds().setUpperLimit(pair.snd.getUpperLimit());
                    
                    this.wcpsCoverageMetadataService.updateGeoResolutionByGridBound(axis);
                }
            }
        }
        
        // Revert the direct positions for irregular axes to the ones before applying scaling intervals
        for (Axis axis : metadata.getAxes()) {
            if (axis instanceof IrregularAxis) {
                IrregularAxis irregularAxis = ((IrregularAxis)axis);
                List<BigDecimal> directPositions = directPositionsMap.get(axis.getLabel());
                irregularAxis.setDirectPositions(directPositions);
            }
        }
    }
    
    /**
     * For irregular axes, when scaling, the coefficients must be filtered (scale down, typical case) or added (scale up).
     * 
     */
    public void applyScaleOnIrregularAxes(WcpsCoverageMetadata metadata, List<Pair> gridBoundAxes) {
        for (Axis axis : metadata.getAxes()) {
            for (Pair<String, NumericTrimming> pair : gridBoundAxes) {
                String axisLabel = pair.fst;
                if (axis instanceof IrregularAxis && CrsUtil.axisLabelsMatch(axis.getLabel(), axisLabel)) {
                    // e.g: [0:10]
                    NumericTrimming scaleGridTrimming = pair.snd;
                    long sourceGridLowerBound = scaleGridTrimming.getLowerLimit().longValue();
                    long sourceGridUpperBound = scaleGridTrimming.getUpperLimit().longValue();
                    long sourceGridPoints = sourceGridUpperBound - sourceGridLowerBound + 1;
                    
                    // e.g: [0:4]
                    long destGridLowerBound = axis.getGridBounds().getLowerLimit().longValue();
                    long destGridUpperBound = axis.getGridBounds().getUpperLimit().longValue();
                    long destGridPoints = destGridUpperBound - destGridLowerBound + 1;                    
                    
                    if (sourceGridPoints >= destGridPoints) {
                        // scale down [0:11] -> [0:3]
                        this.applyScaleDownOnIrregularAxis((IrregularAxis)axis, scaleGridTrimming);
                    } else {
                        // scale up [0:11] -> [0:300]                        
                        // e.g: before scale time("2001":"2010") has 6 coefficients: 2001, 2002, 2005, 2007, 2008, 2009, 2010 with grid [0:5]
                        //      after scale  time("2001":"2010") has 301 coefficients: 2001, ... 2010 with grid [0:300]
                        // @TODO: how to calculate the newly added coefficients in the middle of irregular axis?                        
                        if (CrsUtil.isGridCrs(axis.getNativeCrsUri())) {
                            throw new WCPSException(ExceptionCode.NoApplicableCode, 
                                    "Cannot scale up on irregular axis '" + axisLabel + "', only scale down is supported.");
                        } else {
                            this.applyScaleUpOnIrregularAxisWithGridCRS((IrregularAxis) axis);
                        }
                    }
                                        
                }
            }            
        }
    }
    
    /**
     * e.g: irregular time axis has 11 coefficients (time slices) with grid bounds [0:10] and scaling's grid interval is [0:3]
     * then after scaling, only 4 coefficients are left on time axis
     */
    private void applyScaleDownOnIrregularAxis(IrregularAxis axis, NumericTrimming sourceGridTrimming) {
        // e.g: [0:11]
        long sourceLowerBound = sourceGridTrimming.getLowerLimit().longValue();
        long sourceUpperBound = sourceGridTrimming.getUpperLimit().longValue();
        long sourceGridPoints = sourceUpperBound - sourceLowerBound;
        sourceLowerBound = 0;
        sourceUpperBound = sourceGridPoints;
        
        // e.g: scale to [0:3]
        long destLowerBound = axis.getGridBounds().getLowerLimit().longValue();        
        long destUpperBound = axis.getGridBounds().getUpperLimit().longValue();
        long destGridPoints = destUpperBound - destLowerBound;
        destLowerBound = 0;
        destUpperBound = destGridPoints;
        
        BigDecimal scaleRatio = BigDecimalUtil.divide(new BigDecimal(sourceUpperBound - sourceLowerBound + 1), new BigDecimal(destUpperBound - destLowerBound + 1));
        BigDecimal realIndex = BigDecimal.ZERO;
        int intIndex = 0;
        List<BigDecimal> selectedCoefficients = new ArrayList<>();
        selectedCoefficients.add(axis.getDirectPositions().get(0));
        
        while (intIndex <= sourceUpperBound) {
            realIndex = realIndex.add(scaleRatio); 
           intIndex = realIndex.intValue();
            
            if (intIndex <= sourceUpperBound) {
                BigDecimal coefficient = axis.getDirectPositions().get(intIndex);
                selectedCoefficients.add(coefficient);
            }
        }
        
        axis.setDirectPositions(selectedCoefficients);        
    }
    
    /**
     * e.g: irregular time axis has 11 coefficients (time slices) with grid bounds [0:10] and scaling's grid interval is [0:3]
     * then after scaling, only 4 coefficients are left on time axis
     */
    private void applyScaleUpOnIrregularAxisWithGridCRS(IrregularAxis axis) {
        // e.g: scale grid domain to [0:10]
        long destLowerBound = axis.getGridBounds().getLowerLimit().longValue();        
        long destUpperBound = axis.getGridBounds().getUpperLimit().longValue();
        long destGridPoints = destUpperBound - destLowerBound;
        axis.setDirectPositions(new ArrayList<>());
        
        for (int i = 0; i <= destGridPoints; i++) {
            axis.getDirectPositions().add(new BigDecimal(i));
        }
    }
    
    /**
     * Check if the coverage contains X and Y axes, but one only specifies
     * X or Y axis for scale()
     */
    private boolean processXOrYAxisImplicitly(WcpsCoverageMetadata metadata, List<Subset> numericSubsets) {
        if (metadata.hasXYAxes()) {
            // NOTE: in case 
            Axis axisX = metadata.getXYAxes().get(0);
            Axis axisY = metadata.getXYAxes().get(1);
            List<Boolean> hasXYAxes = new ArrayList<>();
            
            for (Subset subset : numericSubsets) {
                if (CrsUtil.axisLabelsMatch(subset.getAxisName(), axisX.getLabel())
                    || CrsUtil.axisLabelsMatch(subset.getAxisName(), axisY.getLabel())) {
                    hasXYAxes.add(true);
                }
                
                if (hasXYAxes.size() == 2) {
                    break;
                }
            }
            
            // Coverage has X and Y axes, but user only specifies one of X or Y for the scale(), then the domain for the other axis
            // will be determined from the specified X/Y axis.
            if (hasXYAxes.size() == 1) {
                return true;
            }
        }
        
        return false;
    }

    //in case we will need to handle scale with a factor, use a method such as below
    //public  WcpsResult handle(WcpsResult coverageExpression, BigDecimal scaleFactor)
    private final String TEMPLATE = "SCALE($coverage, [$intervalList])";
}
