// Generated from wcps.g4 by ANTLR 4.13.0
package petascope.wcps.parser;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link wcpsParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface wcpsVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by the {@code WcpsQueryLabel}
	 * labeled alternative in {@link wcpsParser#wcpsQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWcpsQueryLabel(wcpsParser.WcpsQueryLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ForClauseListLabel}
	 * labeled alternative in {@link wcpsParser#forClauseList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForClauseListLabel(wcpsParser.ForClauseListLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#coverageIdForClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageIdForClause(wcpsParser.CoverageIdForClauseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ForClauseLabel}
	 * labeled alternative in {@link wcpsParser#forClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForClauseLabel(wcpsParser.ForClauseLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LetClauseListLabel}
	 * labeled alternative in {@link wcpsParser#letClauseList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLetClauseListLabel(wcpsParser.LetClauseListLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#letClauseDimensionIntervalList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLetClauseDimensionIntervalList(wcpsParser.LetClauseDimensionIntervalListContext ctx);
	/**
	 * Visit a parse tree produced by the {@code letClauseDimensionIntervalListLabel}
	 * labeled alternative in {@link wcpsParser#letClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLetClauseDimensionIntervalListLabel(wcpsParser.LetClauseDimensionIntervalListLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code letClauseCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#letClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLetClauseCoverageExpressionLabel(wcpsParser.LetClauseCoverageExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code WhereClauseLabel}
	 * labeled alternative in {@link wcpsParser#whereClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhereClauseLabel(wcpsParser.WhereClauseLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ReturnClauseLabel}
	 * labeled alternative in {@link wcpsParser#returnClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnClauseLabel(wcpsParser.ReturnClauseLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#domainPropertyValueExtraction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDomainPropertyValueExtraction(wcpsParser.DomainPropertyValueExtractionContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#domainIntervals}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDomainIntervals(wcpsParser.DomainIntervalsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code GeoXYAxisLabelAndDomainResolutionLabel}
	 * labeled alternative in {@link wcpsParser#geoXYAxisLabelAndDomainResolution}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeoXYAxisLabelAndDomainResolutionLabel(wcpsParser.GeoXYAxisLabelAndDomainResolutionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageVariableNameLabel}
	 * labeled alternative in {@link wcpsParser#coverageVariableName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageVariableNameLabel(wcpsParser.CoverageVariableNameLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#processingExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcessingExpression(wcpsParser.ProcessingExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code scalarValueCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#scalarValueCoverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScalarValueCoverageExpressionLabel(wcpsParser.ScalarValueCoverageExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#scalarExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScalarExpression(wcpsParser.ScalarExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BooleanBinaryScalarLabel}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanBinaryScalarLabel(wcpsParser.BooleanBinaryScalarLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BooleanReduceExpression}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanReduceExpression(wcpsParser.BooleanReduceExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BooleanUnaryScalarLabel}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanUnaryScalarLabel(wcpsParser.BooleanUnaryScalarLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BooleanStringComparisonScalar}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanStringComparisonScalar(wcpsParser.BooleanStringComparisonScalarContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BooleanConstantLabel}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanConstantLabel(wcpsParser.BooleanConstantLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BooleanNumericalComparisonScalarLabel}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanNumericalComparisonScalarLabel(wcpsParser.BooleanNumericalComparisonScalarLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#booleanUnaryOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanUnaryOperator(wcpsParser.BooleanUnaryOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#booleanConstant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanConstant(wcpsParser.BooleanConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#booleanOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanOperator(wcpsParser.BooleanOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#numericalComparissonOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericalComparissonOperator(wcpsParser.NumericalComparissonOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#stringOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringOperator(wcpsParser.StringOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code StringScalarExpressionLabel}
	 * labeled alternative in {@link wcpsParser#stringScalarExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringScalarExpressionLabel(wcpsParser.StringScalarExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code StarExpressionLabel}
	 * labeled alternative in {@link wcpsParser#starExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStarExpressionLabel(wcpsParser.StarExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#booleanSwitchCaseCoverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanSwitchCaseCoverageExpression(wcpsParser.BooleanSwitchCaseCoverageExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#booleanSwitchCaseCombinedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanSwitchCaseCombinedExpression(wcpsParser.BooleanSwitchCaseCombinedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NumericalTrigonometricScalarExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericalTrigonometricScalarExpressionLabel(wcpsParser.NumericalTrigonometricScalarExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NumericalNanNumberExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericalNanNumberExpressionLabel(wcpsParser.NumericalNanNumberExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NumericalRealNumberExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericalRealNumberExpressionLabel(wcpsParser.NumericalRealNumberExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NumericalCondenseExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericalCondenseExpressionLabel(wcpsParser.NumericalCondenseExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NumericalUnaryScalarExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericalUnaryScalarExpressionLabel(wcpsParser.NumericalUnaryScalarExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NumericalBinaryScalarExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericalBinaryScalarExpressionLabel(wcpsParser.NumericalBinaryScalarExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NumericalComplexNumberConstant}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericalComplexNumberConstant(wcpsParser.NumericalComplexNumberConstantContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ComplexNumberConstantLabel}
	 * labeled alternative in {@link wcpsParser#complexNumberConstant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComplexNumberConstantLabel(wcpsParser.ComplexNumberConstantLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#numericalOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericalOperator(wcpsParser.NumericalOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#numericalUnaryOperation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericalUnaryOperation(wcpsParser.NumericalUnaryOperationContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#trigonometricOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrigonometricOperator(wcpsParser.TrigonometricOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#getComponentExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetComponentExpression(wcpsParser.GetComponentExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageIdentifierExpressionLabel}
	 * labeled alternative in {@link wcpsParser#coverageIdentifierExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageIdentifierExpressionLabel(wcpsParser.CoverageIdentifierExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CellCountExpressionLabel}
	 * labeled alternative in {@link wcpsParser#cellCountExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCellCountExpressionLabel(wcpsParser.CellCountExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageCrsSetExpressionLabel}
	 * labeled alternative in {@link wcpsParser#coverageCrsSetExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageCrsSetExpressionLabel(wcpsParser.CoverageCrsSetExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DomainExpressionLabel}
	 * labeled alternative in {@link wcpsParser#domainExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDomainExpressionLabel(wcpsParser.DomainExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code imageCrsDomainByDimensionExpressionLabel}
	 * labeled alternative in {@link wcpsParser#imageCrsDomainByDimensionExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImageCrsDomainByDimensionExpressionLabel(wcpsParser.ImageCrsDomainByDimensionExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code imageCrsDomainExpressionLabel}
	 * labeled alternative in {@link wcpsParser#imageCrsDomainExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImageCrsDomainExpressionLabel(wcpsParser.ImageCrsDomainExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code imageCrsExpressionLabel}
	 * labeled alternative in {@link wcpsParser#imageCrsExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImageCrsExpressionLabel(wcpsParser.ImageCrsExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DescribeCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#describeCoverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeCoverageExpressionLabel(wcpsParser.DescribeCoverageExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#positionalParamater}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPositionalParamater(wcpsParser.PositionalParamaterContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#extraParams}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtraParams(wcpsParser.ExtraParamsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code EncodedCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#encodedCoverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEncodedCoverageExpressionLabel(wcpsParser.EncodedCoverageExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DecodedCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#decodeCoverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecodedCoverageExpressionLabel(wcpsParser.DecodedCoverageExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionCrsTransformShorthandLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionCrsTransformShorthandLabel(wcpsParser.CoverageExpressionCrsTransformShorthandLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionModLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionModLabel(wcpsParser.CoverageExpressionModLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionTrigonometricLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionTrigonometricLabel(wcpsParser.CoverageExpressionTrigonometricLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionCoverageLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionCoverageLabel(wcpsParser.CoverageExpressionCoverageLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionConstantLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionConstantLabel(wcpsParser.CoverageExpressionConstantLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionShorthandSliceLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionShorthandSliceLabel(wcpsParser.CoverageExpressionShorthandSliceLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code coverageExpressionSortLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionSortLabel(wcpsParser.CoverageExpressionSortLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionShorthandSubsetLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionShorthandSubsetLabel(wcpsParser.CoverageExpressionShorthandSubsetLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionScaleByImageCrsDomainLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionScaleByImageCrsDomainLabel(wcpsParser.CoverageExpressionScaleByImageCrsDomainLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionScaleByDimensionIntervalsLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionScaleByDimensionIntervalsLabel(wcpsParser.CoverageExpressionScaleByDimensionIntervalsLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionArithmeticLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionArithmeticLabel(wcpsParser.CoverageExpressionArithmeticLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionOverlayLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionOverlayLabel(wcpsParser.CoverageExpressionOverlayLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionExponentialLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionExponentialLabel(wcpsParser.CoverageExpressionExponentialLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionLogicLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionLogicLabel(wcpsParser.CoverageExpressionLogicLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionComparissonLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionComparissonLabel(wcpsParser.CoverageExpressionComparissonLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionSliceLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionSliceLabel(wcpsParser.CoverageExpressionSliceLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionClipCurtainLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionClipCurtainLabel(wcpsParser.CoverageExpressionClipCurtainLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code coverageExpressionShortHandSubsetWithLetClauseVariableLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionShortHandSubsetWithLetClauseVariableLabel(wcpsParser.CoverageExpressionShortHandSubsetWithLetClauseVariableLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionRangeSubsettingLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionRangeSubsettingLabel(wcpsParser.CoverageExpressionRangeSubsettingLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionMaxBinaryLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionMaxBinaryLabel(wcpsParser.CoverageExpressionMaxBinaryLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionDomainIntervalsLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionDomainIntervalsLabel(wcpsParser.CoverageExpressionDomainIntervalsLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionUnaryBooleanLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionUnaryBooleanLabel(wcpsParser.CoverageExpressionUnaryBooleanLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionVariableNameLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionVariableNameLabel(wcpsParser.CoverageExpressionVariableNameLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionScaleByFactorLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionScaleByFactorLabel(wcpsParser.CoverageExpressionScaleByFactorLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageIsNullExpression}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageIsNullExpression(wcpsParser.CoverageIsNullExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionClipWKTLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionClipWKTLabel(wcpsParser.CoverageExpressionClipWKTLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionScalarLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionScalarLabel(wcpsParser.CoverageExpressionScalarLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionGeoXYAxisLabelAndDomainResolution}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionGeoXYAxisLabelAndDomainResolution(wcpsParser.CoverageExpressionGeoXYAxisLabelAndDomainResolutionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionRangeConstructorLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionRangeConstructorLabel(wcpsParser.CoverageExpressionRangeConstructorLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionExtendByDomainIntervalsLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionExtendByDomainIntervalsLabel(wcpsParser.CoverageExpressionExtendByDomainIntervalsLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionTrimCoverageLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionTrimCoverageLabel(wcpsParser.CoverageExpressionTrimCoverageLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionCastLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionCastLabel(wcpsParser.CoverageExpressionCastLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionMinBinaryLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionMinBinaryLabel(wcpsParser.CoverageExpressionMinBinaryLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionPowerLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionPowerLabel(wcpsParser.CoverageExpressionPowerLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionConstructorLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionConstructorLabel(wcpsParser.CoverageExpressionConstructorLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionCrsTransformLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionCrsTransformLabel(wcpsParser.CoverageExpressionCrsTransformLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code coverageExpresisonFlipLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpresisonFlipLabel(wcpsParser.CoverageExpresisonFlipLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionDecodeLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionDecodeLabel(wcpsParser.CoverageExpressionDecodeLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionScaleByFactorListLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionScaleByFactorListLabel(wcpsParser.CoverageExpressionScaleByFactorListLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionExtendLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionExtendLabel(wcpsParser.CoverageExpressionExtendLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionUnaryArithmeticLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionUnaryArithmeticLabel(wcpsParser.CoverageExpressionUnaryArithmeticLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionClipCorridorLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionClipCorridorLabel(wcpsParser.CoverageExpressionClipCorridorLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageExpressionSwitchCaseLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageExpressionSwitchCaseLabel(wcpsParser.CoverageExpressionSwitchCaseLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#coverageArithmeticOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageArithmeticOperator(wcpsParser.CoverageArithmeticOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#unaryArithmeticExpressionOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryArithmeticExpressionOperator(wcpsParser.UnaryArithmeticExpressionOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code UnaryCoverageArithmeticExpressionLabel}
	 * labeled alternative in {@link wcpsParser#unaryArithmeticExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryCoverageArithmeticExpressionLabel(wcpsParser.UnaryCoverageArithmeticExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TrigonometricExpressionLabel}
	 * labeled alternative in {@link wcpsParser#trigonometricExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrigonometricExpressionLabel(wcpsParser.TrigonometricExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#exponentialExpressionOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExponentialExpressionOperator(wcpsParser.ExponentialExpressionOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ExponentialExpressionLabel}
	 * labeled alternative in {@link wcpsParser#exponentialExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExponentialExpressionLabel(wcpsParser.ExponentialExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code UnaryPowerExpressionLabel}
	 * labeled alternative in {@link wcpsParser#unaryPowerExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryPowerExpressionLabel(wcpsParser.UnaryPowerExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code UnaryModExpressionLabel}
	 * labeled alternative in {@link wcpsParser#unaryModExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryModExpressionLabel(wcpsParser.UnaryModExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code minBinaryExpressionLabel}
	 * labeled alternative in {@link wcpsParser#minBinaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMinBinaryExpressionLabel(wcpsParser.MinBinaryExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code maxBinaryExpressionLabel}
	 * labeled alternative in {@link wcpsParser#maxBinaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMaxBinaryExpressionLabel(wcpsParser.MaxBinaryExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotUnaryBooleanExpressionLabel}
	 * labeled alternative in {@link wcpsParser#unaryBooleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotUnaryBooleanExpressionLabel(wcpsParser.NotUnaryBooleanExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BitUnaryBooleanExpressionLabel}
	 * labeled alternative in {@link wcpsParser#unaryBooleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitUnaryBooleanExpressionLabel(wcpsParser.BitUnaryBooleanExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#rangeType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRangeType(wcpsParser.RangeTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CastExpressionLabel}
	 * labeled alternative in {@link wcpsParser#castExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCastExpressionLabel(wcpsParser.CastExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#fieldName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldName(wcpsParser.FieldNameContext ctx);
	/**
	 * Visit a parse tree produced by the {@code RangeConstructorExpressionLabel}
	 * labeled alternative in {@link wcpsParser#rangeConstructorExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRangeConstructorExpressionLabel(wcpsParser.RangeConstructorExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rangeConstructorElementLabel}
	 * labeled alternative in {@link wcpsParser#rangeConstructorElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRangeConstructorElementLabel(wcpsParser.RangeConstructorElementLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rangeConstructorElementListLabel}
	 * labeled alternative in {@link wcpsParser#rangeConstructorElementList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRangeConstructorElementListLabel(wcpsParser.RangeConstructorElementListLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code RangeConstructorSwitchCaseExpressionLabel}
	 * labeled alternative in {@link wcpsParser#rangeConstructorSwitchCaseExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRangeConstructorSwitchCaseExpressionLabel(wcpsParser.RangeConstructorSwitchCaseExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DimensionPointListLabel}
	 * labeled alternative in {@link wcpsParser#dimensionPointList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDimensionPointListLabel(wcpsParser.DimensionPointListLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DimensionPointElementLabel}
	 * labeled alternative in {@link wcpsParser#dimensionPointElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDimensionPointElementLabel(wcpsParser.DimensionPointElementLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DimensionIntervalListLabel}
	 * labeled alternative in {@link wcpsParser#dimensionIntervalList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDimensionIntervalListLabel(wcpsParser.DimensionIntervalListLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SliceScaleDimensionPointElementLabel}
	 * labeled alternative in {@link wcpsParser#scaleDimensionPointElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSliceScaleDimensionPointElementLabel(wcpsParser.SliceScaleDimensionPointElementLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ScaleDimensionPointListLabel}
	 * labeled alternative in {@link wcpsParser#scaleDimensionPointList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScaleDimensionPointListLabel(wcpsParser.ScaleDimensionPointListLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ScaleDimensionIntervalListLabel}
	 * labeled alternative in {@link wcpsParser#scaleDimensionIntervalList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScaleDimensionIntervalListLabel(wcpsParser.ScaleDimensionIntervalListLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TrimScaleDimensionIntervalElementLabel}
	 * labeled alternative in {@link wcpsParser#scaleDimensionIntervalElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrimScaleDimensionIntervalElementLabel(wcpsParser.TrimScaleDimensionIntervalElementLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TrimDimensionIntervalElementLabel}
	 * labeled alternative in {@link wcpsParser#dimensionIntervalElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrimDimensionIntervalElementLabel(wcpsParser.TrimDimensionIntervalElementLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TrimDimensionIntervalByImageCrsDomainElementLabel}
	 * labeled alternative in {@link wcpsParser#dimensionIntervalElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrimDimensionIntervalByImageCrsDomainElementLabel(wcpsParser.TrimDimensionIntervalByImageCrsDomainElementLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SliceDimensionIntervalElementLabel}
	 * labeled alternative in {@link wcpsParser#dimensionIntervalElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSliceDimensionIntervalElementLabel(wcpsParser.SliceDimensionIntervalElementLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code wktPointsLabel}
	 * labeled alternative in {@link wcpsParser#wktPoints}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWktPointsLabel(wcpsParser.WktPointsLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code WKTPointElementListLabel}
	 * labeled alternative in {@link wcpsParser#wktPointElementList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWKTPointElementListLabel(wcpsParser.WKTPointElementListLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code WKTLineStringLabel}
	 * labeled alternative in {@link wcpsParser#wktLineString}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWKTLineStringLabel(wcpsParser.WKTLineStringLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code WKTPolygonLabel}
	 * labeled alternative in {@link wcpsParser#wktPolygon}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWKTPolygonLabel(wcpsParser.WKTPolygonLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code WKTMultipolygonLabel}
	 * labeled alternative in {@link wcpsParser#wktMultipolygon}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWKTMultipolygonLabel(wcpsParser.WKTMultipolygonLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code WKTCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#wktCoverageExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWKTCoverageExpressionLabel(wcpsParser.WKTCoverageExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code WKTExpressionLabel}
	 * labeled alternative in {@link wcpsParser#wktExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWKTExpressionLabel(wcpsParser.WKTExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#curtainProjectionAxisLabel1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCurtainProjectionAxisLabel1(wcpsParser.CurtainProjectionAxisLabel1Context ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#curtainProjectionAxisLabel2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCurtainProjectionAxisLabel2(wcpsParser.CurtainProjectionAxisLabel2Context ctx);
	/**
	 * Visit a parse tree produced by the {@code ClipCurtainExpressionLabel}
	 * labeled alternative in {@link wcpsParser#clipCurtainExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClipCurtainExpressionLabel(wcpsParser.ClipCurtainExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#corridorProjectionAxisLabel1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCorridorProjectionAxisLabel1(wcpsParser.CorridorProjectionAxisLabel1Context ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#corridorProjectionAxisLabel2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCorridorProjectionAxisLabel2(wcpsParser.CorridorProjectionAxisLabel2Context ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#corridorWKTLabel1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCorridorWKTLabel1(wcpsParser.CorridorWKTLabel1Context ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#corridorWKTLabel2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCorridorWKTLabel2(wcpsParser.CorridorWKTLabel2Context ctx);
	/**
	 * Visit a parse tree produced by the {@code ClipCorridorExpressionLabel}
	 * labeled alternative in {@link wcpsParser#clipCorridorExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClipCorridorExpressionLabel(wcpsParser.ClipCorridorExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ClipWKTExpressionLabel}
	 * labeled alternative in {@link wcpsParser#clipWKTExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClipWKTExpressionLabel(wcpsParser.ClipWKTExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CrsTransformExpressionLabel}
	 * labeled alternative in {@link wcpsParser#crsTransformExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCrsTransformExpressionLabel(wcpsParser.CrsTransformExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CrsTransformShorthandExpressionLabel}
	 * labeled alternative in {@link wcpsParser#crsTransformShorthandExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCrsTransformShorthandExpressionLabel(wcpsParser.CrsTransformShorthandExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DimensionCrsListLabel}
	 * labeled alternative in {@link wcpsParser#dimensionCrsList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDimensionCrsListLabel(wcpsParser.DimensionCrsListLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DimensionGeoXYResolutionsListLabel}
	 * labeled alternative in {@link wcpsParser#dimensionGeoXYResolutionsList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDimensionGeoXYResolutionsListLabel(wcpsParser.DimensionGeoXYResolutionsListLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#dimensionGeoXYResolution}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDimensionGeoXYResolution(wcpsParser.DimensionGeoXYResolutionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DimensionCrsElementLabel}
	 * labeled alternative in {@link wcpsParser#dimensionCrsElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDimensionCrsElementLabel(wcpsParser.DimensionCrsElementLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code InterpolationTypeLabel}
	 * labeled alternative in {@link wcpsParser#interpolationType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterpolationTypeLabel(wcpsParser.InterpolationTypeLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageConstructorExpressionLabel}
	 * labeled alternative in {@link wcpsParser#coverageConstructorExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageConstructorExpressionLabel(wcpsParser.CoverageConstructorExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AxisIteratorDomainIntervalsLabel}
	 * labeled alternative in {@link wcpsParser#axisIterator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAxisIteratorDomainIntervalsLabel(wcpsParser.AxisIteratorDomainIntervalsLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AxisIteratorLabel}
	 * labeled alternative in {@link wcpsParser#axisIterator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAxisIteratorLabel(wcpsParser.AxisIteratorLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IntervalExpressionLabel}
	 * labeled alternative in {@link wcpsParser#intervalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntervalExpressionLabel(wcpsParser.IntervalExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoverageConstantExpressionLabel}
	 * labeled alternative in {@link wcpsParser#coverageConstantExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoverageConstantExpressionLabel(wcpsParser.CoverageConstantExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AxisSpecLabel}
	 * labeled alternative in {@link wcpsParser#axisSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAxisSpecLabel(wcpsParser.AxisSpecLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#condenseExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCondenseExpression(wcpsParser.CondenseExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#reduceBooleanExpressionOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReduceBooleanExpressionOperator(wcpsParser.ReduceBooleanExpressionOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#reduceNumericalExpressionOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReduceNumericalExpressionOperator(wcpsParser.ReduceNumericalExpressionOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ReduceBooleanExpressionLabel}
	 * labeled alternative in {@link wcpsParser#reduceBooleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReduceBooleanExpressionLabel(wcpsParser.ReduceBooleanExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ReduceNumericalExpressionLabel}
	 * labeled alternative in {@link wcpsParser#reduceNumericalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReduceNumericalExpressionLabel(wcpsParser.ReduceNumericalExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#reduceExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReduceExpression(wcpsParser.ReduceExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#condenseExpressionOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCondenseExpressionOperator(wcpsParser.CondenseExpressionOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code GeneralCondenseExpressionLabel}
	 * labeled alternative in {@link wcpsParser#generalCondenseExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneralCondenseExpressionLabel(wcpsParser.GeneralCondenseExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code flipExpressionLabel}
	 * labeled alternative in {@link wcpsParser#flipExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlipExpressionLabel(wcpsParser.FlipExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sortExpressionLabel}
	 * labeled alternative in {@link wcpsParser#sortExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortExpressionLabel(wcpsParser.SortExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code switchCaseExpressionLabel}
	 * labeled alternative in {@link wcpsParser#switchCaseExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchCaseExpressionLabel(wcpsParser.SwitchCaseExpressionLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#switchCaseElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchCaseElement(wcpsParser.SwitchCaseElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#switchCaseElementList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchCaseElementList(wcpsParser.SwitchCaseElementListContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#switchCaseDefaultElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchCaseDefaultElement(wcpsParser.SwitchCaseDefaultElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#crsName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCrsName(wcpsParser.CrsNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#axisName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAxisName(wcpsParser.AxisNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(wcpsParser.NumberContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant(wcpsParser.ConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link wcpsParser#sortingOrder}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortingOrder(wcpsParser.SortingOrderContext ctx);
}