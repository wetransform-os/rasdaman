// Generated from wcps.g4 by ANTLR 4.13.0
package petascope.wcps.parser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link wcpsParser}.
 */
public interface wcpsListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by the {@code WcpsQueryLabel}
	 * labeled alternative in {@link wcpsParser#wcpsQuery}.
	 * @param ctx the parse tree
	 */
	void enterWcpsQueryLabel(wcpsParser.WcpsQueryLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code WcpsQueryLabel}
	 * labeled alternative in {@link wcpsParser#wcpsQuery}.
	 * @param ctx the parse tree
	 */
	void exitWcpsQueryLabel(wcpsParser.WcpsQueryLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ForClauseListLabel}
	 * labeled alternative in {@link wcpsParser#forClauseList}.
	 * @param ctx the parse tree
	 */
	void enterForClauseListLabel(wcpsParser.ForClauseListLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ForClauseListLabel}
	 * labeled alternative in {@link wcpsParser#forClauseList}.
	 * @param ctx the parse tree
	 */
	void exitForClauseListLabel(wcpsParser.ForClauseListLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#coverageIdForClause}.
	 * @param ctx the parse tree
	 */
	void enterCoverageIdForClause(wcpsParser.CoverageIdForClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#coverageIdForClause}.
	 * @param ctx the parse tree
	 */
	void exitCoverageIdForClause(wcpsParser.CoverageIdForClauseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ForClauseLabel}
	 * labeled alternative in {@link wcpsParser#forClause}.
	 * @param ctx the parse tree
	 */
	void enterForClauseLabel(wcpsParser.ForClauseLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ForClauseLabel}
	 * labeled alternative in {@link wcpsParser#forClause}.
	 * @param ctx the parse tree
	 */
	void exitForClauseLabel(wcpsParser.ForClauseLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LetClauseListLabel}
	 * labeled alternative in {@link wcpsParser#letClauseList}.
	 * @param ctx the parse tree
	 */
	void enterLetClauseListLabel(wcpsParser.LetClauseListLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LetClauseListLabel}
	 * labeled alternative in {@link wcpsParser#letClauseList}.
	 * @param ctx the parse tree
	 */
	void exitLetClauseListLabel(wcpsParser.LetClauseListLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#letClauseDimensionIntervalList}.
	 * @param ctx the parse tree
	 */
	void enterLetClauseDimensionIntervalList(wcpsParser.LetClauseDimensionIntervalListContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#letClauseDimensionIntervalList}.
	 * @param ctx the parse tree
	 */
	void exitLetClauseDimensionIntervalList(wcpsParser.LetClauseDimensionIntervalListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code letClauseDimensionIntervalListLabel}
	 * labeled alternative in {@link wcpsParser#letClause}.
	 * @param ctx the parse tree
	 */
	void enterLetClauseDimensionIntervalListLabel(wcpsParser.LetClauseDimensionIntervalListLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code letClauseDimensionIntervalListLabel}
	 * labeled alternative in {@link wcpsParser#letClause}.
	 * @param ctx the parse tree
	 */
	void exitLetClauseDimensionIntervalListLabel(wcpsParser.LetClauseDimensionIntervalListLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code letClauseCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#letClause}.
	 * @param ctx the parse tree
	 */
	void enterLetClauseCoverageExpressionLabel(wcpsParser.LetClauseCoverageExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code letClauseCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#letClause}.
	 * @param ctx the parse tree
	 */
	void exitLetClauseCoverageExpressionLabel(wcpsParser.LetClauseCoverageExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code WhereClauseLabel}
	 * labeled alternative in {@link wcpsParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void enterWhereClauseLabel(wcpsParser.WhereClauseLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code WhereClauseLabel}
	 * labeled alternative in {@link wcpsParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void exitWhereClauseLabel(wcpsParser.WhereClauseLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ReturnClauseLabel}
	 * labeled alternative in {@link wcpsParser#returnClause}.
	 * @param ctx the parse tree
	 */
	void enterReturnClauseLabel(wcpsParser.ReturnClauseLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ReturnClauseLabel}
	 * labeled alternative in {@link wcpsParser#returnClause}.
	 * @param ctx the parse tree
	 */
	void exitReturnClauseLabel(wcpsParser.ReturnClauseLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#domainPropertyValueExtraction}.
	 * @param ctx the parse tree
	 */
	void enterDomainPropertyValueExtraction(wcpsParser.DomainPropertyValueExtractionContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#domainPropertyValueExtraction}.
	 * @param ctx the parse tree
	 */
	void exitDomainPropertyValueExtraction(wcpsParser.DomainPropertyValueExtractionContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#domainIntervals}.
	 * @param ctx the parse tree
	 */
	void enterDomainIntervals(wcpsParser.DomainIntervalsContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#domainIntervals}.
	 * @param ctx the parse tree
	 */
	void exitDomainIntervals(wcpsParser.DomainIntervalsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code GeoXYAxisLabelAndDomainResolutionLabel}
	 * labeled alternative in {@link wcpsParser#geoXYAxisLabelAndDomainResolution}.
	 * @param ctx the parse tree
	 */
	void enterGeoXYAxisLabelAndDomainResolutionLabel(wcpsParser.GeoXYAxisLabelAndDomainResolutionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code GeoXYAxisLabelAndDomainResolutionLabel}
	 * labeled alternative in {@link wcpsParser#geoXYAxisLabelAndDomainResolution}.
	 * @param ctx the parse tree
	 */
	void exitGeoXYAxisLabelAndDomainResolutionLabel(wcpsParser.GeoXYAxisLabelAndDomainResolutionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageVariableNameLabel}
	 * labeled alternative in {@link wcpsParser#coverageVariableName}.
	 * @param ctx the parse tree
	 */
	void enterCoverageVariableNameLabel(wcpsParser.CoverageVariableNameLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageVariableNameLabel}
	 * labeled alternative in {@link wcpsParser#coverageVariableName}.
	 * @param ctx the parse tree
	 */
	void exitCoverageVariableNameLabel(wcpsParser.CoverageVariableNameLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#processingExpression}.
	 * @param ctx the parse tree
	 */
	void enterProcessingExpression(wcpsParser.ProcessingExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#processingExpression}.
	 * @param ctx the parse tree
	 */
	void exitProcessingExpression(wcpsParser.ProcessingExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code scalarValueCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#scalarValueCoverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterScalarValueCoverageExpressionLabel(wcpsParser.ScalarValueCoverageExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code scalarValueCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#scalarValueCoverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitScalarValueCoverageExpressionLabel(wcpsParser.ScalarValueCoverageExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#scalarExpression}.
	 * @param ctx the parse tree
	 */
	void enterScalarExpression(wcpsParser.ScalarExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#scalarExpression}.
	 * @param ctx the parse tree
	 */
	void exitScalarExpression(wcpsParser.ScalarExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BooleanBinaryScalarLabel}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 */
	void enterBooleanBinaryScalarLabel(wcpsParser.BooleanBinaryScalarLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BooleanBinaryScalarLabel}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 */
	void exitBooleanBinaryScalarLabel(wcpsParser.BooleanBinaryScalarLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BooleanReduceExpression}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 */
	void enterBooleanReduceExpression(wcpsParser.BooleanReduceExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BooleanReduceExpression}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 */
	void exitBooleanReduceExpression(wcpsParser.BooleanReduceExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BooleanUnaryScalarLabel}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 */
	void enterBooleanUnaryScalarLabel(wcpsParser.BooleanUnaryScalarLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BooleanUnaryScalarLabel}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 */
	void exitBooleanUnaryScalarLabel(wcpsParser.BooleanUnaryScalarLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BooleanStringComparisonScalar}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 */
	void enterBooleanStringComparisonScalar(wcpsParser.BooleanStringComparisonScalarContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BooleanStringComparisonScalar}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 */
	void exitBooleanStringComparisonScalar(wcpsParser.BooleanStringComparisonScalarContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BooleanConstantLabel}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 */
	void enterBooleanConstantLabel(wcpsParser.BooleanConstantLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BooleanConstantLabel}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 */
	void exitBooleanConstantLabel(wcpsParser.BooleanConstantLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BooleanNumericalComparisonScalarLabel}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 */
	void enterBooleanNumericalComparisonScalarLabel(wcpsParser.BooleanNumericalComparisonScalarLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BooleanNumericalComparisonScalarLabel}
	 * labeled alternative in {@link wcpsParser#booleanScalarExpression}.
	 * @param ctx the parse tree
	 */
	void exitBooleanNumericalComparisonScalarLabel(wcpsParser.BooleanNumericalComparisonScalarLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#booleanUnaryOperator}.
	 * @param ctx the parse tree
	 */
	void enterBooleanUnaryOperator(wcpsParser.BooleanUnaryOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#booleanUnaryOperator}.
	 * @param ctx the parse tree
	 */
	void exitBooleanUnaryOperator(wcpsParser.BooleanUnaryOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#booleanConstant}.
	 * @param ctx the parse tree
	 */
	void enterBooleanConstant(wcpsParser.BooleanConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#booleanConstant}.
	 * @param ctx the parse tree
	 */
	void exitBooleanConstant(wcpsParser.BooleanConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#booleanOperator}.
	 * @param ctx the parse tree
	 */
	void enterBooleanOperator(wcpsParser.BooleanOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#booleanOperator}.
	 * @param ctx the parse tree
	 */
	void exitBooleanOperator(wcpsParser.BooleanOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#numericalComparissonOperator}.
	 * @param ctx the parse tree
	 */
	void enterNumericalComparissonOperator(wcpsParser.NumericalComparissonOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#numericalComparissonOperator}.
	 * @param ctx the parse tree
	 */
	void exitNumericalComparissonOperator(wcpsParser.NumericalComparissonOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#stringOperator}.
	 * @param ctx the parse tree
	 */
	void enterStringOperator(wcpsParser.StringOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#stringOperator}.
	 * @param ctx the parse tree
	 */
	void exitStringOperator(wcpsParser.StringOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code StringScalarExpressionLabel}
	 * labeled alternative in {@link wcpsParser#stringScalarExpression}.
	 * @param ctx the parse tree
	 */
	void enterStringScalarExpressionLabel(wcpsParser.StringScalarExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code StringScalarExpressionLabel}
	 * labeled alternative in {@link wcpsParser#stringScalarExpression}.
	 * @param ctx the parse tree
	 */
	void exitStringScalarExpressionLabel(wcpsParser.StringScalarExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code StarExpressionLabel}
	 * labeled alternative in {@link wcpsParser#starExpression}.
	 * @param ctx the parse tree
	 */
	void enterStarExpressionLabel(wcpsParser.StarExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code StarExpressionLabel}
	 * labeled alternative in {@link wcpsParser#starExpression}.
	 * @param ctx the parse tree
	 */
	void exitStarExpressionLabel(wcpsParser.StarExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#booleanSwitchCaseCoverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterBooleanSwitchCaseCoverageExpression(wcpsParser.BooleanSwitchCaseCoverageExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#booleanSwitchCaseCoverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitBooleanSwitchCaseCoverageExpression(wcpsParser.BooleanSwitchCaseCoverageExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#booleanSwitchCaseCombinedExpression}.
	 * @param ctx the parse tree
	 */
	void enterBooleanSwitchCaseCombinedExpression(wcpsParser.BooleanSwitchCaseCombinedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#booleanSwitchCaseCombinedExpression}.
	 * @param ctx the parse tree
	 */
	void exitBooleanSwitchCaseCombinedExpression(wcpsParser.BooleanSwitchCaseCombinedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NumericalTrigonometricScalarExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 */
	void enterNumericalTrigonometricScalarExpressionLabel(wcpsParser.NumericalTrigonometricScalarExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NumericalTrigonometricScalarExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 */
	void exitNumericalTrigonometricScalarExpressionLabel(wcpsParser.NumericalTrigonometricScalarExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NumericalNanNumberExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 */
	void enterNumericalNanNumberExpressionLabel(wcpsParser.NumericalNanNumberExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NumericalNanNumberExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 */
	void exitNumericalNanNumberExpressionLabel(wcpsParser.NumericalNanNumberExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NumericalRealNumberExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 */
	void enterNumericalRealNumberExpressionLabel(wcpsParser.NumericalRealNumberExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NumericalRealNumberExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 */
	void exitNumericalRealNumberExpressionLabel(wcpsParser.NumericalRealNumberExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NumericalCondenseExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 */
	void enterNumericalCondenseExpressionLabel(wcpsParser.NumericalCondenseExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NumericalCondenseExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 */
	void exitNumericalCondenseExpressionLabel(wcpsParser.NumericalCondenseExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NumericalUnaryScalarExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 */
	void enterNumericalUnaryScalarExpressionLabel(wcpsParser.NumericalUnaryScalarExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NumericalUnaryScalarExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 */
	void exitNumericalUnaryScalarExpressionLabel(wcpsParser.NumericalUnaryScalarExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NumericalBinaryScalarExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 */
	void enterNumericalBinaryScalarExpressionLabel(wcpsParser.NumericalBinaryScalarExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NumericalBinaryScalarExpressionLabel}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 */
	void exitNumericalBinaryScalarExpressionLabel(wcpsParser.NumericalBinaryScalarExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NumericalComplexNumberConstant}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 */
	void enterNumericalComplexNumberConstant(wcpsParser.NumericalComplexNumberConstantContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NumericalComplexNumberConstant}
	 * labeled alternative in {@link wcpsParser#numericalScalarExpression}.
	 * @param ctx the parse tree
	 */
	void exitNumericalComplexNumberConstant(wcpsParser.NumericalComplexNumberConstantContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ComplexNumberConstantLabel}
	 * labeled alternative in {@link wcpsParser#complexNumberConstant}.
	 * @param ctx the parse tree
	 */
	void enterComplexNumberConstantLabel(wcpsParser.ComplexNumberConstantLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ComplexNumberConstantLabel}
	 * labeled alternative in {@link wcpsParser#complexNumberConstant}.
	 * @param ctx the parse tree
	 */
	void exitComplexNumberConstantLabel(wcpsParser.ComplexNumberConstantLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#numericalOperator}.
	 * @param ctx the parse tree
	 */
	void enterNumericalOperator(wcpsParser.NumericalOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#numericalOperator}.
	 * @param ctx the parse tree
	 */
	void exitNumericalOperator(wcpsParser.NumericalOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#numericalUnaryOperation}.
	 * @param ctx the parse tree
	 */
	void enterNumericalUnaryOperation(wcpsParser.NumericalUnaryOperationContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#numericalUnaryOperation}.
	 * @param ctx the parse tree
	 */
	void exitNumericalUnaryOperation(wcpsParser.NumericalUnaryOperationContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#trigonometricOperator}.
	 * @param ctx the parse tree
	 */
	void enterTrigonometricOperator(wcpsParser.TrigonometricOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#trigonometricOperator}.
	 * @param ctx the parse tree
	 */
	void exitTrigonometricOperator(wcpsParser.TrigonometricOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#getComponentExpression}.
	 * @param ctx the parse tree
	 */
	void enterGetComponentExpression(wcpsParser.GetComponentExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#getComponentExpression}.
	 * @param ctx the parse tree
	 */
	void exitGetComponentExpression(wcpsParser.GetComponentExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageIdentifierExpressionLabel}
	 * labeled alternative in {@link wcpsParser#coverageIdentifierExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageIdentifierExpressionLabel(wcpsParser.CoverageIdentifierExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageIdentifierExpressionLabel}
	 * labeled alternative in {@link wcpsParser#coverageIdentifierExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageIdentifierExpressionLabel(wcpsParser.CoverageIdentifierExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CellCountExpressionLabel}
	 * labeled alternative in {@link wcpsParser#cellCountExpression}.
	 * @param ctx the parse tree
	 */
	void enterCellCountExpressionLabel(wcpsParser.CellCountExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CellCountExpressionLabel}
	 * labeled alternative in {@link wcpsParser#cellCountExpression}.
	 * @param ctx the parse tree
	 */
	void exitCellCountExpressionLabel(wcpsParser.CellCountExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageCrsSetExpressionLabel}
	 * labeled alternative in {@link wcpsParser#coverageCrsSetExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageCrsSetExpressionLabel(wcpsParser.CoverageCrsSetExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageCrsSetExpressionLabel}
	 * labeled alternative in {@link wcpsParser#coverageCrsSetExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageCrsSetExpressionLabel(wcpsParser.CoverageCrsSetExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DomainExpressionLabel}
	 * labeled alternative in {@link wcpsParser#domainExpression}.
	 * @param ctx the parse tree
	 */
	void enterDomainExpressionLabel(wcpsParser.DomainExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DomainExpressionLabel}
	 * labeled alternative in {@link wcpsParser#domainExpression}.
	 * @param ctx the parse tree
	 */
	void exitDomainExpressionLabel(wcpsParser.DomainExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code imageCrsDomainByDimensionExpressionLabel}
	 * labeled alternative in {@link wcpsParser#imageCrsDomainByDimensionExpression}.
	 * @param ctx the parse tree
	 */
	void enterImageCrsDomainByDimensionExpressionLabel(wcpsParser.ImageCrsDomainByDimensionExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code imageCrsDomainByDimensionExpressionLabel}
	 * labeled alternative in {@link wcpsParser#imageCrsDomainByDimensionExpression}.
	 * @param ctx the parse tree
	 */
	void exitImageCrsDomainByDimensionExpressionLabel(wcpsParser.ImageCrsDomainByDimensionExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code imageCrsDomainExpressionLabel}
	 * labeled alternative in {@link wcpsParser#imageCrsDomainExpression}.
	 * @param ctx the parse tree
	 */
	void enterImageCrsDomainExpressionLabel(wcpsParser.ImageCrsDomainExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code imageCrsDomainExpressionLabel}
	 * labeled alternative in {@link wcpsParser#imageCrsDomainExpression}.
	 * @param ctx the parse tree
	 */
	void exitImageCrsDomainExpressionLabel(wcpsParser.ImageCrsDomainExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code imageCrsExpressionLabel}
	 * labeled alternative in {@link wcpsParser#imageCrsExpression}.
	 * @param ctx the parse tree
	 */
	void enterImageCrsExpressionLabel(wcpsParser.ImageCrsExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code imageCrsExpressionLabel}
	 * labeled alternative in {@link wcpsParser#imageCrsExpression}.
	 * @param ctx the parse tree
	 */
	void exitImageCrsExpressionLabel(wcpsParser.ImageCrsExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DescribeCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#describeCoverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterDescribeCoverageExpressionLabel(wcpsParser.DescribeCoverageExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DescribeCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#describeCoverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitDescribeCoverageExpressionLabel(wcpsParser.DescribeCoverageExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#positionalParamater}.
	 * @param ctx the parse tree
	 */
	void enterPositionalParamater(wcpsParser.PositionalParamaterContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#positionalParamater}.
	 * @param ctx the parse tree
	 */
	void exitPositionalParamater(wcpsParser.PositionalParamaterContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#extraParams}.
	 * @param ctx the parse tree
	 */
	void enterExtraParams(wcpsParser.ExtraParamsContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#extraParams}.
	 * @param ctx the parse tree
	 */
	void exitExtraParams(wcpsParser.ExtraParamsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code EncodedCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#encodedCoverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterEncodedCoverageExpressionLabel(wcpsParser.EncodedCoverageExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code EncodedCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#encodedCoverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitEncodedCoverageExpressionLabel(wcpsParser.EncodedCoverageExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DecodedCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#decodeCoverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterDecodedCoverageExpressionLabel(wcpsParser.DecodedCoverageExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DecodedCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#decodeCoverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitDecodedCoverageExpressionLabel(wcpsParser.DecodedCoverageExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionCrsTransformShorthandLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionCrsTransformShorthandLabel(wcpsParser.CoverageExpressionCrsTransformShorthandLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionCrsTransformShorthandLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionCrsTransformShorthandLabel(wcpsParser.CoverageExpressionCrsTransformShorthandLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionModLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionModLabel(wcpsParser.CoverageExpressionModLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionModLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionModLabel(wcpsParser.CoverageExpressionModLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionTrigonometricLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionTrigonometricLabel(wcpsParser.CoverageExpressionTrigonometricLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionTrigonometricLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionTrigonometricLabel(wcpsParser.CoverageExpressionTrigonometricLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionCoverageLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionCoverageLabel(wcpsParser.CoverageExpressionCoverageLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionCoverageLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionCoverageLabel(wcpsParser.CoverageExpressionCoverageLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionConstantLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionConstantLabel(wcpsParser.CoverageExpressionConstantLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionConstantLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionConstantLabel(wcpsParser.CoverageExpressionConstantLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionShorthandSliceLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionShorthandSliceLabel(wcpsParser.CoverageExpressionShorthandSliceLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionShorthandSliceLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionShorthandSliceLabel(wcpsParser.CoverageExpressionShorthandSliceLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code coverageExpressionSortLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionSortLabel(wcpsParser.CoverageExpressionSortLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code coverageExpressionSortLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionSortLabel(wcpsParser.CoverageExpressionSortLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionShorthandSubsetLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionShorthandSubsetLabel(wcpsParser.CoverageExpressionShorthandSubsetLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionShorthandSubsetLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionShorthandSubsetLabel(wcpsParser.CoverageExpressionShorthandSubsetLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionScaleByImageCrsDomainLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionScaleByImageCrsDomainLabel(wcpsParser.CoverageExpressionScaleByImageCrsDomainLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionScaleByImageCrsDomainLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionScaleByImageCrsDomainLabel(wcpsParser.CoverageExpressionScaleByImageCrsDomainLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionScaleByDimensionIntervalsLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionScaleByDimensionIntervalsLabel(wcpsParser.CoverageExpressionScaleByDimensionIntervalsLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionScaleByDimensionIntervalsLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionScaleByDimensionIntervalsLabel(wcpsParser.CoverageExpressionScaleByDimensionIntervalsLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionArithmeticLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionArithmeticLabel(wcpsParser.CoverageExpressionArithmeticLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionArithmeticLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionArithmeticLabel(wcpsParser.CoverageExpressionArithmeticLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionOverlayLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionOverlayLabel(wcpsParser.CoverageExpressionOverlayLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionOverlayLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionOverlayLabel(wcpsParser.CoverageExpressionOverlayLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionExponentialLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionExponentialLabel(wcpsParser.CoverageExpressionExponentialLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionExponentialLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionExponentialLabel(wcpsParser.CoverageExpressionExponentialLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionLogicLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionLogicLabel(wcpsParser.CoverageExpressionLogicLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionLogicLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionLogicLabel(wcpsParser.CoverageExpressionLogicLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionComparissonLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionComparissonLabel(wcpsParser.CoverageExpressionComparissonLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionComparissonLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionComparissonLabel(wcpsParser.CoverageExpressionComparissonLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionSliceLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionSliceLabel(wcpsParser.CoverageExpressionSliceLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionSliceLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionSliceLabel(wcpsParser.CoverageExpressionSliceLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionClipCurtainLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionClipCurtainLabel(wcpsParser.CoverageExpressionClipCurtainLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionClipCurtainLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionClipCurtainLabel(wcpsParser.CoverageExpressionClipCurtainLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code coverageExpressionShortHandSubsetWithLetClauseVariableLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionShortHandSubsetWithLetClauseVariableLabel(wcpsParser.CoverageExpressionShortHandSubsetWithLetClauseVariableLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code coverageExpressionShortHandSubsetWithLetClauseVariableLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionShortHandSubsetWithLetClauseVariableLabel(wcpsParser.CoverageExpressionShortHandSubsetWithLetClauseVariableLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionRangeSubsettingLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionRangeSubsettingLabel(wcpsParser.CoverageExpressionRangeSubsettingLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionRangeSubsettingLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionRangeSubsettingLabel(wcpsParser.CoverageExpressionRangeSubsettingLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionMaxBinaryLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionMaxBinaryLabel(wcpsParser.CoverageExpressionMaxBinaryLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionMaxBinaryLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionMaxBinaryLabel(wcpsParser.CoverageExpressionMaxBinaryLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionDomainIntervalsLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionDomainIntervalsLabel(wcpsParser.CoverageExpressionDomainIntervalsLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionDomainIntervalsLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionDomainIntervalsLabel(wcpsParser.CoverageExpressionDomainIntervalsLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionUnaryBooleanLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionUnaryBooleanLabel(wcpsParser.CoverageExpressionUnaryBooleanLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionUnaryBooleanLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionUnaryBooleanLabel(wcpsParser.CoverageExpressionUnaryBooleanLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionVariableNameLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionVariableNameLabel(wcpsParser.CoverageExpressionVariableNameLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionVariableNameLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionVariableNameLabel(wcpsParser.CoverageExpressionVariableNameLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionScaleByFactorLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionScaleByFactorLabel(wcpsParser.CoverageExpressionScaleByFactorLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionScaleByFactorLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionScaleByFactorLabel(wcpsParser.CoverageExpressionScaleByFactorLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageIsNullExpression}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageIsNullExpression(wcpsParser.CoverageIsNullExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageIsNullExpression}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageIsNullExpression(wcpsParser.CoverageIsNullExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionClipWKTLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionClipWKTLabel(wcpsParser.CoverageExpressionClipWKTLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionClipWKTLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionClipWKTLabel(wcpsParser.CoverageExpressionClipWKTLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionScalarLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionScalarLabel(wcpsParser.CoverageExpressionScalarLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionScalarLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionScalarLabel(wcpsParser.CoverageExpressionScalarLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionGeoXYAxisLabelAndDomainResolution}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionGeoXYAxisLabelAndDomainResolution(wcpsParser.CoverageExpressionGeoXYAxisLabelAndDomainResolutionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionGeoXYAxisLabelAndDomainResolution}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionGeoXYAxisLabelAndDomainResolution(wcpsParser.CoverageExpressionGeoXYAxisLabelAndDomainResolutionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionRangeConstructorLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionRangeConstructorLabel(wcpsParser.CoverageExpressionRangeConstructorLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionRangeConstructorLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionRangeConstructorLabel(wcpsParser.CoverageExpressionRangeConstructorLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionExtendByDomainIntervalsLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionExtendByDomainIntervalsLabel(wcpsParser.CoverageExpressionExtendByDomainIntervalsLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionExtendByDomainIntervalsLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionExtendByDomainIntervalsLabel(wcpsParser.CoverageExpressionExtendByDomainIntervalsLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionTrimCoverageLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionTrimCoverageLabel(wcpsParser.CoverageExpressionTrimCoverageLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionTrimCoverageLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionTrimCoverageLabel(wcpsParser.CoverageExpressionTrimCoverageLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionCastLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionCastLabel(wcpsParser.CoverageExpressionCastLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionCastLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionCastLabel(wcpsParser.CoverageExpressionCastLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionMinBinaryLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionMinBinaryLabel(wcpsParser.CoverageExpressionMinBinaryLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionMinBinaryLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionMinBinaryLabel(wcpsParser.CoverageExpressionMinBinaryLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionPowerLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionPowerLabel(wcpsParser.CoverageExpressionPowerLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionPowerLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionPowerLabel(wcpsParser.CoverageExpressionPowerLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionConstructorLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionConstructorLabel(wcpsParser.CoverageExpressionConstructorLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionConstructorLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionConstructorLabel(wcpsParser.CoverageExpressionConstructorLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionCrsTransformLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionCrsTransformLabel(wcpsParser.CoverageExpressionCrsTransformLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionCrsTransformLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionCrsTransformLabel(wcpsParser.CoverageExpressionCrsTransformLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code coverageExpresisonFlipLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpresisonFlipLabel(wcpsParser.CoverageExpresisonFlipLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code coverageExpresisonFlipLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpresisonFlipLabel(wcpsParser.CoverageExpresisonFlipLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionDecodeLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionDecodeLabel(wcpsParser.CoverageExpressionDecodeLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionDecodeLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionDecodeLabel(wcpsParser.CoverageExpressionDecodeLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionScaleByFactorListLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionScaleByFactorListLabel(wcpsParser.CoverageExpressionScaleByFactorListLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionScaleByFactorListLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionScaleByFactorListLabel(wcpsParser.CoverageExpressionScaleByFactorListLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionExtendLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionExtendLabel(wcpsParser.CoverageExpressionExtendLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionExtendLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionExtendLabel(wcpsParser.CoverageExpressionExtendLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionUnaryArithmeticLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionUnaryArithmeticLabel(wcpsParser.CoverageExpressionUnaryArithmeticLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionUnaryArithmeticLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionUnaryArithmeticLabel(wcpsParser.CoverageExpressionUnaryArithmeticLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionClipCorridorLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionClipCorridorLabel(wcpsParser.CoverageExpressionClipCorridorLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionClipCorridorLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionClipCorridorLabel(wcpsParser.CoverageExpressionClipCorridorLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageExpressionSwitchCaseLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageExpressionSwitchCaseLabel(wcpsParser.CoverageExpressionSwitchCaseLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageExpressionSwitchCaseLabel}
	 * labeled alternative in {@link wcpsParser#coverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageExpressionSwitchCaseLabel(wcpsParser.CoverageExpressionSwitchCaseLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#coverageArithmeticOperator}.
	 * @param ctx the parse tree
	 */
	void enterCoverageArithmeticOperator(wcpsParser.CoverageArithmeticOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#coverageArithmeticOperator}.
	 * @param ctx the parse tree
	 */
	void exitCoverageArithmeticOperator(wcpsParser.CoverageArithmeticOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#unaryArithmeticExpressionOperator}.
	 * @param ctx the parse tree
	 */
	void enterUnaryArithmeticExpressionOperator(wcpsParser.UnaryArithmeticExpressionOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#unaryArithmeticExpressionOperator}.
	 * @param ctx the parse tree
	 */
	void exitUnaryArithmeticExpressionOperator(wcpsParser.UnaryArithmeticExpressionOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code UnaryCoverageArithmeticExpressionLabel}
	 * labeled alternative in {@link wcpsParser#unaryArithmeticExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryCoverageArithmeticExpressionLabel(wcpsParser.UnaryCoverageArithmeticExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code UnaryCoverageArithmeticExpressionLabel}
	 * labeled alternative in {@link wcpsParser#unaryArithmeticExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryCoverageArithmeticExpressionLabel(wcpsParser.UnaryCoverageArithmeticExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TrigonometricExpressionLabel}
	 * labeled alternative in {@link wcpsParser#trigonometricExpression}.
	 * @param ctx the parse tree
	 */
	void enterTrigonometricExpressionLabel(wcpsParser.TrigonometricExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TrigonometricExpressionLabel}
	 * labeled alternative in {@link wcpsParser#trigonometricExpression}.
	 * @param ctx the parse tree
	 */
	void exitTrigonometricExpressionLabel(wcpsParser.TrigonometricExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#exponentialExpressionOperator}.
	 * @param ctx the parse tree
	 */
	void enterExponentialExpressionOperator(wcpsParser.ExponentialExpressionOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#exponentialExpressionOperator}.
	 * @param ctx the parse tree
	 */
	void exitExponentialExpressionOperator(wcpsParser.ExponentialExpressionOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ExponentialExpressionLabel}
	 * labeled alternative in {@link wcpsParser#exponentialExpression}.
	 * @param ctx the parse tree
	 */
	void enterExponentialExpressionLabel(wcpsParser.ExponentialExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ExponentialExpressionLabel}
	 * labeled alternative in {@link wcpsParser#exponentialExpression}.
	 * @param ctx the parse tree
	 */
	void exitExponentialExpressionLabel(wcpsParser.ExponentialExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code UnaryPowerExpressionLabel}
	 * labeled alternative in {@link wcpsParser#unaryPowerExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryPowerExpressionLabel(wcpsParser.UnaryPowerExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code UnaryPowerExpressionLabel}
	 * labeled alternative in {@link wcpsParser#unaryPowerExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryPowerExpressionLabel(wcpsParser.UnaryPowerExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code UnaryModExpressionLabel}
	 * labeled alternative in {@link wcpsParser#unaryModExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryModExpressionLabel(wcpsParser.UnaryModExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code UnaryModExpressionLabel}
	 * labeled alternative in {@link wcpsParser#unaryModExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryModExpressionLabel(wcpsParser.UnaryModExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code minBinaryExpressionLabel}
	 * labeled alternative in {@link wcpsParser#minBinaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterMinBinaryExpressionLabel(wcpsParser.MinBinaryExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code minBinaryExpressionLabel}
	 * labeled alternative in {@link wcpsParser#minBinaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitMinBinaryExpressionLabel(wcpsParser.MinBinaryExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code maxBinaryExpressionLabel}
	 * labeled alternative in {@link wcpsParser#maxBinaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterMaxBinaryExpressionLabel(wcpsParser.MaxBinaryExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code maxBinaryExpressionLabel}
	 * labeled alternative in {@link wcpsParser#maxBinaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitMaxBinaryExpressionLabel(wcpsParser.MaxBinaryExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NotUnaryBooleanExpressionLabel}
	 * labeled alternative in {@link wcpsParser#unaryBooleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterNotUnaryBooleanExpressionLabel(wcpsParser.NotUnaryBooleanExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NotUnaryBooleanExpressionLabel}
	 * labeled alternative in {@link wcpsParser#unaryBooleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitNotUnaryBooleanExpressionLabel(wcpsParser.NotUnaryBooleanExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BitUnaryBooleanExpressionLabel}
	 * labeled alternative in {@link wcpsParser#unaryBooleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterBitUnaryBooleanExpressionLabel(wcpsParser.BitUnaryBooleanExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BitUnaryBooleanExpressionLabel}
	 * labeled alternative in {@link wcpsParser#unaryBooleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitBitUnaryBooleanExpressionLabel(wcpsParser.BitUnaryBooleanExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#rangeType}.
	 * @param ctx the parse tree
	 */
	void enterRangeType(wcpsParser.RangeTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#rangeType}.
	 * @param ctx the parse tree
	 */
	void exitRangeType(wcpsParser.RangeTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CastExpressionLabel}
	 * labeled alternative in {@link wcpsParser#castExpression}.
	 * @param ctx the parse tree
	 */
	void enterCastExpressionLabel(wcpsParser.CastExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CastExpressionLabel}
	 * labeled alternative in {@link wcpsParser#castExpression}.
	 * @param ctx the parse tree
	 */
	void exitCastExpressionLabel(wcpsParser.CastExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#fieldName}.
	 * @param ctx the parse tree
	 */
	void enterFieldName(wcpsParser.FieldNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#fieldName}.
	 * @param ctx the parse tree
	 */
	void exitFieldName(wcpsParser.FieldNameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RangeConstructorExpressionLabel}
	 * labeled alternative in {@link wcpsParser#rangeConstructorExpression}.
	 * @param ctx the parse tree
	 */
	void enterRangeConstructorExpressionLabel(wcpsParser.RangeConstructorExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RangeConstructorExpressionLabel}
	 * labeled alternative in {@link wcpsParser#rangeConstructorExpression}.
	 * @param ctx the parse tree
	 */
	void exitRangeConstructorExpressionLabel(wcpsParser.RangeConstructorExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rangeConstructorElementLabel}
	 * labeled alternative in {@link wcpsParser#rangeConstructorElement}.
	 * @param ctx the parse tree
	 */
	void enterRangeConstructorElementLabel(wcpsParser.RangeConstructorElementLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rangeConstructorElementLabel}
	 * labeled alternative in {@link wcpsParser#rangeConstructorElement}.
	 * @param ctx the parse tree
	 */
	void exitRangeConstructorElementLabel(wcpsParser.RangeConstructorElementLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rangeConstructorElementListLabel}
	 * labeled alternative in {@link wcpsParser#rangeConstructorElementList}.
	 * @param ctx the parse tree
	 */
	void enterRangeConstructorElementListLabel(wcpsParser.RangeConstructorElementListLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rangeConstructorElementListLabel}
	 * labeled alternative in {@link wcpsParser#rangeConstructorElementList}.
	 * @param ctx the parse tree
	 */
	void exitRangeConstructorElementListLabel(wcpsParser.RangeConstructorElementListLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RangeConstructorSwitchCaseExpressionLabel}
	 * labeled alternative in {@link wcpsParser#rangeConstructorSwitchCaseExpression}.
	 * @param ctx the parse tree
	 */
	void enterRangeConstructorSwitchCaseExpressionLabel(wcpsParser.RangeConstructorSwitchCaseExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RangeConstructorSwitchCaseExpressionLabel}
	 * labeled alternative in {@link wcpsParser#rangeConstructorSwitchCaseExpression}.
	 * @param ctx the parse tree
	 */
	void exitRangeConstructorSwitchCaseExpressionLabel(wcpsParser.RangeConstructorSwitchCaseExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DimensionPointListLabel}
	 * labeled alternative in {@link wcpsParser#dimensionPointList}.
	 * @param ctx the parse tree
	 */
	void enterDimensionPointListLabel(wcpsParser.DimensionPointListLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DimensionPointListLabel}
	 * labeled alternative in {@link wcpsParser#dimensionPointList}.
	 * @param ctx the parse tree
	 */
	void exitDimensionPointListLabel(wcpsParser.DimensionPointListLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DimensionPointElementLabel}
	 * labeled alternative in {@link wcpsParser#dimensionPointElement}.
	 * @param ctx the parse tree
	 */
	void enterDimensionPointElementLabel(wcpsParser.DimensionPointElementLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DimensionPointElementLabel}
	 * labeled alternative in {@link wcpsParser#dimensionPointElement}.
	 * @param ctx the parse tree
	 */
	void exitDimensionPointElementLabel(wcpsParser.DimensionPointElementLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DimensionIntervalListLabel}
	 * labeled alternative in {@link wcpsParser#dimensionIntervalList}.
	 * @param ctx the parse tree
	 */
	void enterDimensionIntervalListLabel(wcpsParser.DimensionIntervalListLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DimensionIntervalListLabel}
	 * labeled alternative in {@link wcpsParser#dimensionIntervalList}.
	 * @param ctx the parse tree
	 */
	void exitDimensionIntervalListLabel(wcpsParser.DimensionIntervalListLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SliceScaleDimensionPointElementLabel}
	 * labeled alternative in {@link wcpsParser#scaleDimensionPointElement}.
	 * @param ctx the parse tree
	 */
	void enterSliceScaleDimensionPointElementLabel(wcpsParser.SliceScaleDimensionPointElementLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SliceScaleDimensionPointElementLabel}
	 * labeled alternative in {@link wcpsParser#scaleDimensionPointElement}.
	 * @param ctx the parse tree
	 */
	void exitSliceScaleDimensionPointElementLabel(wcpsParser.SliceScaleDimensionPointElementLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ScaleDimensionPointListLabel}
	 * labeled alternative in {@link wcpsParser#scaleDimensionPointList}.
	 * @param ctx the parse tree
	 */
	void enterScaleDimensionPointListLabel(wcpsParser.ScaleDimensionPointListLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ScaleDimensionPointListLabel}
	 * labeled alternative in {@link wcpsParser#scaleDimensionPointList}.
	 * @param ctx the parse tree
	 */
	void exitScaleDimensionPointListLabel(wcpsParser.ScaleDimensionPointListLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ScaleDimensionIntervalListLabel}
	 * labeled alternative in {@link wcpsParser#scaleDimensionIntervalList}.
	 * @param ctx the parse tree
	 */
	void enterScaleDimensionIntervalListLabel(wcpsParser.ScaleDimensionIntervalListLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ScaleDimensionIntervalListLabel}
	 * labeled alternative in {@link wcpsParser#scaleDimensionIntervalList}.
	 * @param ctx the parse tree
	 */
	void exitScaleDimensionIntervalListLabel(wcpsParser.ScaleDimensionIntervalListLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TrimScaleDimensionIntervalElementLabel}
	 * labeled alternative in {@link wcpsParser#scaleDimensionIntervalElement}.
	 * @param ctx the parse tree
	 */
	void enterTrimScaleDimensionIntervalElementLabel(wcpsParser.TrimScaleDimensionIntervalElementLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TrimScaleDimensionIntervalElementLabel}
	 * labeled alternative in {@link wcpsParser#scaleDimensionIntervalElement}.
	 * @param ctx the parse tree
	 */
	void exitTrimScaleDimensionIntervalElementLabel(wcpsParser.TrimScaleDimensionIntervalElementLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TrimDimensionIntervalElementLabel}
	 * labeled alternative in {@link wcpsParser#dimensionIntervalElement}.
	 * @param ctx the parse tree
	 */
	void enterTrimDimensionIntervalElementLabel(wcpsParser.TrimDimensionIntervalElementLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TrimDimensionIntervalElementLabel}
	 * labeled alternative in {@link wcpsParser#dimensionIntervalElement}.
	 * @param ctx the parse tree
	 */
	void exitTrimDimensionIntervalElementLabel(wcpsParser.TrimDimensionIntervalElementLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TrimDimensionIntervalByImageCrsDomainElementLabel}
	 * labeled alternative in {@link wcpsParser#dimensionIntervalElement}.
	 * @param ctx the parse tree
	 */
	void enterTrimDimensionIntervalByImageCrsDomainElementLabel(wcpsParser.TrimDimensionIntervalByImageCrsDomainElementLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TrimDimensionIntervalByImageCrsDomainElementLabel}
	 * labeled alternative in {@link wcpsParser#dimensionIntervalElement}.
	 * @param ctx the parse tree
	 */
	void exitTrimDimensionIntervalByImageCrsDomainElementLabel(wcpsParser.TrimDimensionIntervalByImageCrsDomainElementLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SliceDimensionIntervalElementLabel}
	 * labeled alternative in {@link wcpsParser#dimensionIntervalElement}.
	 * @param ctx the parse tree
	 */
	void enterSliceDimensionIntervalElementLabel(wcpsParser.SliceDimensionIntervalElementLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SliceDimensionIntervalElementLabel}
	 * labeled alternative in {@link wcpsParser#dimensionIntervalElement}.
	 * @param ctx the parse tree
	 */
	void exitSliceDimensionIntervalElementLabel(wcpsParser.SliceDimensionIntervalElementLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code wktPointsLabel}
	 * labeled alternative in {@link wcpsParser#wktPoints}.
	 * @param ctx the parse tree
	 */
	void enterWktPointsLabel(wcpsParser.WktPointsLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code wktPointsLabel}
	 * labeled alternative in {@link wcpsParser#wktPoints}.
	 * @param ctx the parse tree
	 */
	void exitWktPointsLabel(wcpsParser.WktPointsLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code WKTPointElementListLabel}
	 * labeled alternative in {@link wcpsParser#wktPointElementList}.
	 * @param ctx the parse tree
	 */
	void enterWKTPointElementListLabel(wcpsParser.WKTPointElementListLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code WKTPointElementListLabel}
	 * labeled alternative in {@link wcpsParser#wktPointElementList}.
	 * @param ctx the parse tree
	 */
	void exitWKTPointElementListLabel(wcpsParser.WKTPointElementListLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code WKTLineStringLabel}
	 * labeled alternative in {@link wcpsParser#wktLineString}.
	 * @param ctx the parse tree
	 */
	void enterWKTLineStringLabel(wcpsParser.WKTLineStringLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code WKTLineStringLabel}
	 * labeled alternative in {@link wcpsParser#wktLineString}.
	 * @param ctx the parse tree
	 */
	void exitWKTLineStringLabel(wcpsParser.WKTLineStringLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code WKTPolygonLabel}
	 * labeled alternative in {@link wcpsParser#wktPolygon}.
	 * @param ctx the parse tree
	 */
	void enterWKTPolygonLabel(wcpsParser.WKTPolygonLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code WKTPolygonLabel}
	 * labeled alternative in {@link wcpsParser#wktPolygon}.
	 * @param ctx the parse tree
	 */
	void exitWKTPolygonLabel(wcpsParser.WKTPolygonLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code WKTMultipolygonLabel}
	 * labeled alternative in {@link wcpsParser#wktMultipolygon}.
	 * @param ctx the parse tree
	 */
	void enterWKTMultipolygonLabel(wcpsParser.WKTMultipolygonLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code WKTMultipolygonLabel}
	 * labeled alternative in {@link wcpsParser#wktMultipolygon}.
	 * @param ctx the parse tree
	 */
	void exitWKTMultipolygonLabel(wcpsParser.WKTMultipolygonLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code WKTCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#wktCoverageExpression}.
	 * @param ctx the parse tree
	 */
	void enterWKTCoverageExpressionLabel(wcpsParser.WKTCoverageExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code WKTCoverageExpressionLabel}
	 * labeled alternative in {@link wcpsParser#wktCoverageExpression}.
	 * @param ctx the parse tree
	 */
	void exitWKTCoverageExpressionLabel(wcpsParser.WKTCoverageExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code WKTExpressionLabel}
	 * labeled alternative in {@link wcpsParser#wktExpression}.
	 * @param ctx the parse tree
	 */
	void enterWKTExpressionLabel(wcpsParser.WKTExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code WKTExpressionLabel}
	 * labeled alternative in {@link wcpsParser#wktExpression}.
	 * @param ctx the parse tree
	 */
	void exitWKTExpressionLabel(wcpsParser.WKTExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#curtainProjectionAxisLabel1}.
	 * @param ctx the parse tree
	 */
	void enterCurtainProjectionAxisLabel1(wcpsParser.CurtainProjectionAxisLabel1Context ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#curtainProjectionAxisLabel1}.
	 * @param ctx the parse tree
	 */
	void exitCurtainProjectionAxisLabel1(wcpsParser.CurtainProjectionAxisLabel1Context ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#curtainProjectionAxisLabel2}.
	 * @param ctx the parse tree
	 */
	void enterCurtainProjectionAxisLabel2(wcpsParser.CurtainProjectionAxisLabel2Context ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#curtainProjectionAxisLabel2}.
	 * @param ctx the parse tree
	 */
	void exitCurtainProjectionAxisLabel2(wcpsParser.CurtainProjectionAxisLabel2Context ctx);
	/**
	 * Enter a parse tree produced by the {@code ClipCurtainExpressionLabel}
	 * labeled alternative in {@link wcpsParser#clipCurtainExpression}.
	 * @param ctx the parse tree
	 */
	void enterClipCurtainExpressionLabel(wcpsParser.ClipCurtainExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ClipCurtainExpressionLabel}
	 * labeled alternative in {@link wcpsParser#clipCurtainExpression}.
	 * @param ctx the parse tree
	 */
	void exitClipCurtainExpressionLabel(wcpsParser.ClipCurtainExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#corridorProjectionAxisLabel1}.
	 * @param ctx the parse tree
	 */
	void enterCorridorProjectionAxisLabel1(wcpsParser.CorridorProjectionAxisLabel1Context ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#corridorProjectionAxisLabel1}.
	 * @param ctx the parse tree
	 */
	void exitCorridorProjectionAxisLabel1(wcpsParser.CorridorProjectionAxisLabel1Context ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#corridorProjectionAxisLabel2}.
	 * @param ctx the parse tree
	 */
	void enterCorridorProjectionAxisLabel2(wcpsParser.CorridorProjectionAxisLabel2Context ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#corridorProjectionAxisLabel2}.
	 * @param ctx the parse tree
	 */
	void exitCorridorProjectionAxisLabel2(wcpsParser.CorridorProjectionAxisLabel2Context ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#corridorWKTLabel1}.
	 * @param ctx the parse tree
	 */
	void enterCorridorWKTLabel1(wcpsParser.CorridorWKTLabel1Context ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#corridorWKTLabel1}.
	 * @param ctx the parse tree
	 */
	void exitCorridorWKTLabel1(wcpsParser.CorridorWKTLabel1Context ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#corridorWKTLabel2}.
	 * @param ctx the parse tree
	 */
	void enterCorridorWKTLabel2(wcpsParser.CorridorWKTLabel2Context ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#corridorWKTLabel2}.
	 * @param ctx the parse tree
	 */
	void exitCorridorWKTLabel2(wcpsParser.CorridorWKTLabel2Context ctx);
	/**
	 * Enter a parse tree produced by the {@code ClipCorridorExpressionLabel}
	 * labeled alternative in {@link wcpsParser#clipCorridorExpression}.
	 * @param ctx the parse tree
	 */
	void enterClipCorridorExpressionLabel(wcpsParser.ClipCorridorExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ClipCorridorExpressionLabel}
	 * labeled alternative in {@link wcpsParser#clipCorridorExpression}.
	 * @param ctx the parse tree
	 */
	void exitClipCorridorExpressionLabel(wcpsParser.ClipCorridorExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ClipWKTExpressionLabel}
	 * labeled alternative in {@link wcpsParser#clipWKTExpression}.
	 * @param ctx the parse tree
	 */
	void enterClipWKTExpressionLabel(wcpsParser.ClipWKTExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ClipWKTExpressionLabel}
	 * labeled alternative in {@link wcpsParser#clipWKTExpression}.
	 * @param ctx the parse tree
	 */
	void exitClipWKTExpressionLabel(wcpsParser.ClipWKTExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CrsTransformExpressionLabel}
	 * labeled alternative in {@link wcpsParser#crsTransformExpression}.
	 * @param ctx the parse tree
	 */
	void enterCrsTransformExpressionLabel(wcpsParser.CrsTransformExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CrsTransformExpressionLabel}
	 * labeled alternative in {@link wcpsParser#crsTransformExpression}.
	 * @param ctx the parse tree
	 */
	void exitCrsTransformExpressionLabel(wcpsParser.CrsTransformExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CrsTransformShorthandExpressionLabel}
	 * labeled alternative in {@link wcpsParser#crsTransformShorthandExpression}.
	 * @param ctx the parse tree
	 */
	void enterCrsTransformShorthandExpressionLabel(wcpsParser.CrsTransformShorthandExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CrsTransformShorthandExpressionLabel}
	 * labeled alternative in {@link wcpsParser#crsTransformShorthandExpression}.
	 * @param ctx the parse tree
	 */
	void exitCrsTransformShorthandExpressionLabel(wcpsParser.CrsTransformShorthandExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DimensionCrsListLabel}
	 * labeled alternative in {@link wcpsParser#dimensionCrsList}.
	 * @param ctx the parse tree
	 */
	void enterDimensionCrsListLabel(wcpsParser.DimensionCrsListLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DimensionCrsListLabel}
	 * labeled alternative in {@link wcpsParser#dimensionCrsList}.
	 * @param ctx the parse tree
	 */
	void exitDimensionCrsListLabel(wcpsParser.DimensionCrsListLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DimensionGeoXYResolutionsListLabel}
	 * labeled alternative in {@link wcpsParser#dimensionGeoXYResolutionsList}.
	 * @param ctx the parse tree
	 */
	void enterDimensionGeoXYResolutionsListLabel(wcpsParser.DimensionGeoXYResolutionsListLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DimensionGeoXYResolutionsListLabel}
	 * labeled alternative in {@link wcpsParser#dimensionGeoXYResolutionsList}.
	 * @param ctx the parse tree
	 */
	void exitDimensionGeoXYResolutionsListLabel(wcpsParser.DimensionGeoXYResolutionsListLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#dimensionGeoXYResolution}.
	 * @param ctx the parse tree
	 */
	void enterDimensionGeoXYResolution(wcpsParser.DimensionGeoXYResolutionContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#dimensionGeoXYResolution}.
	 * @param ctx the parse tree
	 */
	void exitDimensionGeoXYResolution(wcpsParser.DimensionGeoXYResolutionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DimensionCrsElementLabel}
	 * labeled alternative in {@link wcpsParser#dimensionCrsElement}.
	 * @param ctx the parse tree
	 */
	void enterDimensionCrsElementLabel(wcpsParser.DimensionCrsElementLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DimensionCrsElementLabel}
	 * labeled alternative in {@link wcpsParser#dimensionCrsElement}.
	 * @param ctx the parse tree
	 */
	void exitDimensionCrsElementLabel(wcpsParser.DimensionCrsElementLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code InterpolationTypeLabel}
	 * labeled alternative in {@link wcpsParser#interpolationType}.
	 * @param ctx the parse tree
	 */
	void enterInterpolationTypeLabel(wcpsParser.InterpolationTypeLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code InterpolationTypeLabel}
	 * labeled alternative in {@link wcpsParser#interpolationType}.
	 * @param ctx the parse tree
	 */
	void exitInterpolationTypeLabel(wcpsParser.InterpolationTypeLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageConstructorExpressionLabel}
	 * labeled alternative in {@link wcpsParser#coverageConstructorExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageConstructorExpressionLabel(wcpsParser.CoverageConstructorExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageConstructorExpressionLabel}
	 * labeled alternative in {@link wcpsParser#coverageConstructorExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageConstructorExpressionLabel(wcpsParser.CoverageConstructorExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AxisIteratorDomainIntervalsLabel}
	 * labeled alternative in {@link wcpsParser#axisIterator}.
	 * @param ctx the parse tree
	 */
	void enterAxisIteratorDomainIntervalsLabel(wcpsParser.AxisIteratorDomainIntervalsLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AxisIteratorDomainIntervalsLabel}
	 * labeled alternative in {@link wcpsParser#axisIterator}.
	 * @param ctx the parse tree
	 */
	void exitAxisIteratorDomainIntervalsLabel(wcpsParser.AxisIteratorDomainIntervalsLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AxisIteratorLabel}
	 * labeled alternative in {@link wcpsParser#axisIterator}.
	 * @param ctx the parse tree
	 */
	void enterAxisIteratorLabel(wcpsParser.AxisIteratorLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AxisIteratorLabel}
	 * labeled alternative in {@link wcpsParser#axisIterator}.
	 * @param ctx the parse tree
	 */
	void exitAxisIteratorLabel(wcpsParser.AxisIteratorLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IntervalExpressionLabel}
	 * labeled alternative in {@link wcpsParser#intervalExpression}.
	 * @param ctx the parse tree
	 */
	void enterIntervalExpressionLabel(wcpsParser.IntervalExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IntervalExpressionLabel}
	 * labeled alternative in {@link wcpsParser#intervalExpression}.
	 * @param ctx the parse tree
	 */
	void exitIntervalExpressionLabel(wcpsParser.IntervalExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoverageConstantExpressionLabel}
	 * labeled alternative in {@link wcpsParser#coverageConstantExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoverageConstantExpressionLabel(wcpsParser.CoverageConstantExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoverageConstantExpressionLabel}
	 * labeled alternative in {@link wcpsParser#coverageConstantExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoverageConstantExpressionLabel(wcpsParser.CoverageConstantExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AxisSpecLabel}
	 * labeled alternative in {@link wcpsParser#axisSpec}.
	 * @param ctx the parse tree
	 */
	void enterAxisSpecLabel(wcpsParser.AxisSpecLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AxisSpecLabel}
	 * labeled alternative in {@link wcpsParser#axisSpec}.
	 * @param ctx the parse tree
	 */
	void exitAxisSpecLabel(wcpsParser.AxisSpecLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#condenseExpression}.
	 * @param ctx the parse tree
	 */
	void enterCondenseExpression(wcpsParser.CondenseExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#condenseExpression}.
	 * @param ctx the parse tree
	 */
	void exitCondenseExpression(wcpsParser.CondenseExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#reduceBooleanExpressionOperator}.
	 * @param ctx the parse tree
	 */
	void enterReduceBooleanExpressionOperator(wcpsParser.ReduceBooleanExpressionOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#reduceBooleanExpressionOperator}.
	 * @param ctx the parse tree
	 */
	void exitReduceBooleanExpressionOperator(wcpsParser.ReduceBooleanExpressionOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#reduceNumericalExpressionOperator}.
	 * @param ctx the parse tree
	 */
	void enterReduceNumericalExpressionOperator(wcpsParser.ReduceNumericalExpressionOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#reduceNumericalExpressionOperator}.
	 * @param ctx the parse tree
	 */
	void exitReduceNumericalExpressionOperator(wcpsParser.ReduceNumericalExpressionOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ReduceBooleanExpressionLabel}
	 * labeled alternative in {@link wcpsParser#reduceBooleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterReduceBooleanExpressionLabel(wcpsParser.ReduceBooleanExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ReduceBooleanExpressionLabel}
	 * labeled alternative in {@link wcpsParser#reduceBooleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitReduceBooleanExpressionLabel(wcpsParser.ReduceBooleanExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ReduceNumericalExpressionLabel}
	 * labeled alternative in {@link wcpsParser#reduceNumericalExpression}.
	 * @param ctx the parse tree
	 */
	void enterReduceNumericalExpressionLabel(wcpsParser.ReduceNumericalExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ReduceNumericalExpressionLabel}
	 * labeled alternative in {@link wcpsParser#reduceNumericalExpression}.
	 * @param ctx the parse tree
	 */
	void exitReduceNumericalExpressionLabel(wcpsParser.ReduceNumericalExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#reduceExpression}.
	 * @param ctx the parse tree
	 */
	void enterReduceExpression(wcpsParser.ReduceExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#reduceExpression}.
	 * @param ctx the parse tree
	 */
	void exitReduceExpression(wcpsParser.ReduceExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#condenseExpressionOperator}.
	 * @param ctx the parse tree
	 */
	void enterCondenseExpressionOperator(wcpsParser.CondenseExpressionOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#condenseExpressionOperator}.
	 * @param ctx the parse tree
	 */
	void exitCondenseExpressionOperator(wcpsParser.CondenseExpressionOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code GeneralCondenseExpressionLabel}
	 * labeled alternative in {@link wcpsParser#generalCondenseExpression}.
	 * @param ctx the parse tree
	 */
	void enterGeneralCondenseExpressionLabel(wcpsParser.GeneralCondenseExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code GeneralCondenseExpressionLabel}
	 * labeled alternative in {@link wcpsParser#generalCondenseExpression}.
	 * @param ctx the parse tree
	 */
	void exitGeneralCondenseExpressionLabel(wcpsParser.GeneralCondenseExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code flipExpressionLabel}
	 * labeled alternative in {@link wcpsParser#flipExpression}.
	 * @param ctx the parse tree
	 */
	void enterFlipExpressionLabel(wcpsParser.FlipExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code flipExpressionLabel}
	 * labeled alternative in {@link wcpsParser#flipExpression}.
	 * @param ctx the parse tree
	 */
	void exitFlipExpressionLabel(wcpsParser.FlipExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sortExpressionLabel}
	 * labeled alternative in {@link wcpsParser#sortExpression}.
	 * @param ctx the parse tree
	 */
	void enterSortExpressionLabel(wcpsParser.SortExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sortExpressionLabel}
	 * labeled alternative in {@link wcpsParser#sortExpression}.
	 * @param ctx the parse tree
	 */
	void exitSortExpressionLabel(wcpsParser.SortExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code switchCaseExpressionLabel}
	 * labeled alternative in {@link wcpsParser#switchCaseExpression}.
	 * @param ctx the parse tree
	 */
	void enterSwitchCaseExpressionLabel(wcpsParser.SwitchCaseExpressionLabelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code switchCaseExpressionLabel}
	 * labeled alternative in {@link wcpsParser#switchCaseExpression}.
	 * @param ctx the parse tree
	 */
	void exitSwitchCaseExpressionLabel(wcpsParser.SwitchCaseExpressionLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#switchCaseElement}.
	 * @param ctx the parse tree
	 */
	void enterSwitchCaseElement(wcpsParser.SwitchCaseElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#switchCaseElement}.
	 * @param ctx the parse tree
	 */
	void exitSwitchCaseElement(wcpsParser.SwitchCaseElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#switchCaseElementList}.
	 * @param ctx the parse tree
	 */
	void enterSwitchCaseElementList(wcpsParser.SwitchCaseElementListContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#switchCaseElementList}.
	 * @param ctx the parse tree
	 */
	void exitSwitchCaseElementList(wcpsParser.SwitchCaseElementListContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#switchCaseDefaultElement}.
	 * @param ctx the parse tree
	 */
	void enterSwitchCaseDefaultElement(wcpsParser.SwitchCaseDefaultElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#switchCaseDefaultElement}.
	 * @param ctx the parse tree
	 */
	void exitSwitchCaseDefaultElement(wcpsParser.SwitchCaseDefaultElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#crsName}.
	 * @param ctx the parse tree
	 */
	void enterCrsName(wcpsParser.CrsNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#crsName}.
	 * @param ctx the parse tree
	 */
	void exitCrsName(wcpsParser.CrsNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#axisName}.
	 * @param ctx the parse tree
	 */
	void enterAxisName(wcpsParser.AxisNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#axisName}.
	 * @param ctx the parse tree
	 */
	void exitAxisName(wcpsParser.AxisNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#number}.
	 * @param ctx the parse tree
	 */
	void enterNumber(wcpsParser.NumberContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#number}.
	 * @param ctx the parse tree
	 */
	void exitNumber(wcpsParser.NumberContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterConstant(wcpsParser.ConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitConstant(wcpsParser.ConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link wcpsParser#sortingOrder}.
	 * @param ctx the parse tree
	 */
	void enterSortingOrder(wcpsParser.SortingOrderContext ctx);
	/**
	 * Exit a parse tree produced by {@link wcpsParser#sortingOrder}.
	 * @param ctx the parse tree
	 */
	void exitSortingOrder(wcpsParser.SortingOrderContext ctx);
}