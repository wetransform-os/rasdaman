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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.core.Pair;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.WCPSException;
import petascope.util.BigDecimalUtil;
import petascope.util.CrsUtil;
import petascope.util.JSONUtil;

import static petascope.util.CrsUtil.GRID_CRS;
import static petascope.wcps.handler.AbstractOperatorHandler.checkOperandIsCoverage;

import petascope.util.StringUtil;
import petascope.wcps.exception.processing.Coverage0DMetadataNullException;
import petascope.wcps.exception.processing.IncompatibleAxesNumberException;
import petascope.wcps.exception.processing.InvalidScaleExtentException;
import petascope.wcps.handler.service.ScaleHandlerService;
import petascope.wcps.metadata.model.*;
import petascope.wcps.metadata.service.*;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.result.WcpsMetadataResult;
import petascope.wcps.result.WcpsResult;
import petascope.wcps.subset_axis.model.*;
import petascope.wcps.xml.handler.DimensionInterval;

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

    public enum ScaleType {
        DEFAULT_TYPE,
        SCALE_BY_FACTORS,
        SCALE_BY_AXES,
        SCALE_BY_EXTENTS,
        SCALE_BY_SIZES
    }

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
    @Autowired
    private ScaleHandlerService scaleHandlerService;

    private ScaleType scaleType;

    public static final String OPERATOR = "scale";
    
    public ScaleExpressionByDimensionIntervalsHandler() {
        
    }

    public ScaleExpressionByDimensionIntervalsHandler create(Handler coverageExpressionHandler, Handler dimensionIntervalListHandler) {
        ScaleExpressionByDimensionIntervalsHandler result = this.create(coverageExpressionHandler, dimensionIntervalListHandler, this.scaleType);
        return result;
    }

    public ScaleExpressionByDimensionIntervalsHandler create(Handler coverageExpressionHandler, Handler dimensionIntervalListHandler,
                                                             ScaleType scaleType) {
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
        result.scaleHandlerService = scaleHandlerService;


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
                coordinateTranslationService,
                scaleHandlerService

        );
        result.injectedServicesRegistries = injectedServicesRegistries;

        result.scaleType = scaleType;

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
            } else if (secondChildHandlerResult instanceof DimensionIntervalList) {
                dimensionIntervalList = (DimensionIntervalList) secondChildHandlerResult;
            } else {
                dimensionIntervalList = this.createDimensionalIntervalListByScalingType(coverageExpressionResult.getMetadata(),
                                                                                       secondChildHandlerResult);
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
     * Iterate all CoverageVariableName child handlers of this SCALE() node and update to use the selected pyramid member coverage
     * instead of base coverage.
     *
     */
    private void updateCoverageVariableNameChildHandlers(String pyramidMemberCoverageId, String pyramidMemberRasdamanCollectionName) throws PetascopeException {
        if (this.getChildren() == null || this.getChildren().isEmpty()) {
            return;
        }
        
        Queue<Handler> queue = new ArrayDeque<>();
        queue.add(this.getFirstChild());

        while (!queue.isEmpty()) {
            Handler currentHandler = queue.remove();
            if (currentHandler.getChildren() != null) {

                if (currentHandler instanceof CoverageVariableNameHandler) {
                    StringScalarHandler stringScalarHandler = ((StringScalarHandler)currentHandler.getFirstChild());
                    // e.g. co -> baseCoverage
                    String currentCoverageAlias = stringScalarHandler.getValue();
                    if (!this.axisIteratorAliasRegistry.exists(currentCoverageAlias)) {
                        // e.g. co_0 -> pyramidMemberCoverage
                        String newlyCoverageAlias = this.coverageAliasRegistry.getNextDownscaledCoverageAlias(currentCoverageAlias);
                        stringScalarHandler.setValue(newlyCoverageAlias);

                        this.coverageAliasRegistry.addCoverageToForClauseListMapping(newlyCoverageAlias, pyramidMemberCoverageId, pyramidMemberRasdamanCollectionName);
                    }
                }

                for (Handler childHandler : currentHandler.getChildren()) {
                    if (childHandler != null) {
                        queue.add(childHandler);
                    }
                }
            }
        }
    }

    /**
     * Depending on the type of scale, then adjust the dimensionIntervalList accordingly.
     */
    private DimensionIntervalList createDimensionalIntervalListByScalingType(WcpsCoverageMetadata metadata, VisitorResult scaleAxesDimensionList) throws PetascopeException {
        DimensionIntervalList result = new DimensionIntervalList();

        if (this.scaleType == ScaleType.SCALE_BY_EXTENTS) {
            result = this.scaleHandlerService.createDimensionIntervalListByScalingExtends(metadata, scaleAxesDimensionList);
        } else if (this.scaleType == ScaleType.SCALE_BY_FACTORS
                || this.scaleType == ScaleType.SCALE_BY_AXES) {
            // NOTE: WCS scaleAxes = WCPS scale factor for each axis
            result = this.scaleHandlerService.createDimensionalIntervalListByScalingFactors(metadata, scaleAxesDimensionList);
        } else if (this.scaleType == ScaleType.SCALE_BY_SIZES) {
            result = this.scaleHandlerService.createDimensionalIntervalListByScalingSizes(metadata, scaleAxesDimensionList);
        }

        return result;
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

    public WcpsResult handle (WcpsResult coverageExpression, DimensionIntervalList dimensionIntervalList, boolean implicitScaleByXorYAxis, Handler firstChildHandler,
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
            this.scaleHandlerService.validateAxisLabelExist(metadata, subset.getAxisName());
        }
        List<Subset> numericSubsets = subsetParsingService.convertToNumericSubsets(subsetDimensions, metadata.getAxes());

        if (this.scaleHandlerService.processXOrYAxisImplicitly(metadata, numericSubsets)) {
            this.scaleHandlerService.handleScaleWithOnlyXorYAxis(coverageExpression, numericSubsets, implicitScaleByXorYAxis);
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
        String selectedRasdamanCollectionName = selectedCoverage.getRasdamanCollectionName();
        
        if (!selectedCoverageId.equals(metadata.getCoverageName())) {

            // NOTE: here it needs to recalculate coverageExpression based on the new pyramid member
            // a pyramid member is selected, this scale expression needs to rerun for this selected pyramid member
            this.updateCoverageVariableNameChildHandlers(selectedCoverageId, selectedRasdamanCollectionName);

            Handler firstChildHandlerTmp = firstChildHandler;
            if (firstChildHandlerTmp == null) {
                firstChildHandlerTmp = this.getFirstChild();
            }

            WcpsResult coverageExpressionResult = (WcpsResult)firstChildHandlerTmp.handle(serviceRegistries);
            coverageExpression = coverageExpressionResult;
            metadata = coverageExpressionResult.getMetadata();

        }
        
        
        // NOTE: from WCPS 1.0 standard, C2 = scale(C1, {x(lo1:hi1), y(lo2:hi2),...}        
        // for all a ∈ dimensionList(C2), c ∈ crsSet(C2, a):
        //          imageCrsDomain(C2 , a ) = (lo:hi) - it means: ***axis's grid domain will be set*** to corresponding lo:hi!
        //          domain(C2,a,c) = domain(C1,a,c) - it means: ***axis's geo domain will not change***!
        wcpsCoverageMetadataService.applySubsets(false, false, metadata, subsetDimensions, numericSubsets);
        
        this.scaleHandlerService.addImplicitScaleGridIntervals(metadata, numericSubsets);
        
        // it will not get all the axis to build the intervals in case of (extend() and scale())
        String domainIntervals = rasqlTranslationService.constructSpecificRasqlDomain(metadata.getSortedAxesByGridOrder(), numericSubsets);

        String rasql = TEMPLATE.replace("$coverage", coverageExpression.getRasql())
                               .replace("$intervalList", domainIntervals);

        this.scaleHandlerService.revertAfterScale(metadata, geoBoundAxes, directPositionsMap);
        this.scaleHandlerService.applyScaleOnIrregularAxes(metadata, gridBoundAxes);
        
        return new WcpsResult(metadata, rasql);
    }



    //in case we will need to handle scale with a factor, use a method such as below
    //public  WcpsResult handle(WcpsResult coverageExpression, BigDecimal scaleFactor)
    private final String TEMPLATE = "SCALE($coverage, [$intervalList])";
}
