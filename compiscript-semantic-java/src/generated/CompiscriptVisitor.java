// Generated from grammar/Compiscript.g4 by ANTLR 4.13.2
package generated;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link CompiscriptParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface CompiscriptVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(CompiscriptParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(CompiscriptParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(CompiscriptParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#variableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclaration(CompiscriptParser.VariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#constantDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantDeclaration(CompiscriptParser.ConstantDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#typeAnnotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeAnnotation(CompiscriptParser.TypeAnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializer(CompiscriptParser.InitializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(CompiscriptParser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#expressionStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionStatement(CompiscriptParser.ExpressionStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#printStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrintStatement(CompiscriptParser.PrintStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#ifStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatement(CompiscriptParser.IfStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#whileStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStatement(CompiscriptParser.WhileStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#doWhileStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoWhileStatement(CompiscriptParser.DoWhileStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#forStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForStatement(CompiscriptParser.ForStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#foreachStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeachStatement(CompiscriptParser.ForeachStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#breakStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreakStatement(CompiscriptParser.BreakStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#continueStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinueStatement(CompiscriptParser.ContinueStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#returnStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnStatement(CompiscriptParser.ReturnStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#tryCatchStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTryCatchStatement(CompiscriptParser.TryCatchStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#switchStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchStatement(CompiscriptParser.SwitchStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#switchCase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchCase(CompiscriptParser.SwitchCaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#defaultCase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultCase(CompiscriptParser.DefaultCaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#functionDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDeclaration(CompiscriptParser.FunctionDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameters(CompiscriptParser.ParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter(CompiscriptParser.ParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#classDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassDeclaration(CompiscriptParser.ClassDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#classMember}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassMember(CompiscriptParser.ClassMemberContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(CompiscriptParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AssignExpr}
	 * labeled alternative in {@link CompiscriptParser#assignmentExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignExpr(CompiscriptParser.AssignExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PropertyAssignExpr}
	 * labeled alternative in {@link CompiscriptParser#assignmentExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyAssignExpr(CompiscriptParser.PropertyAssignExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ExprNoAssign}
	 * labeled alternative in {@link CompiscriptParser#assignmentExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprNoAssign(CompiscriptParser.ExprNoAssignContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TernaryExpr}
	 * labeled alternative in {@link CompiscriptParser#conditionalExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTernaryExpr(CompiscriptParser.TernaryExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#logicalOrExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalOrExpr(CompiscriptParser.LogicalOrExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#logicalAndExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalAndExpr(CompiscriptParser.LogicalAndExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#equalityExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityExpr(CompiscriptParser.EqualityExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#relationalExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalExpr(CompiscriptParser.RelationalExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#additiveExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveExpr(CompiscriptParser.AdditiveExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#multiplicativeExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeExpr(CompiscriptParser.MultiplicativeExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#unaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpr(CompiscriptParser.UnaryExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExpr(CompiscriptParser.PrimaryExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#literalExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralExpr(CompiscriptParser.LiteralExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#leftHandSide}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLeftHandSide(CompiscriptParser.LeftHandSideContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IdentifierExpr}
	 * labeled alternative in {@link CompiscriptParser#primaryAtom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierExpr(CompiscriptParser.IdentifierExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NewExpr}
	 * labeled alternative in {@link CompiscriptParser#primaryAtom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNewExpr(CompiscriptParser.NewExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ThisExpr}
	 * labeled alternative in {@link CompiscriptParser#primaryAtom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThisExpr(CompiscriptParser.ThisExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CallExpr}
	 * labeled alternative in {@link CompiscriptParser#suffixOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallExpr(CompiscriptParser.CallExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IndexExpr}
	 * labeled alternative in {@link CompiscriptParser#suffixOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndexExpr(CompiscriptParser.IndexExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PropertyAccessExpr}
	 * labeled alternative in {@link CompiscriptParser#suffixOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyAccessExpr(CompiscriptParser.PropertyAccessExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(CompiscriptParser.ArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#arrayLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayLiteral(CompiscriptParser.ArrayLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(CompiscriptParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CompiscriptParser#baseType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBaseType(CompiscriptParser.BaseTypeContext ctx);
}