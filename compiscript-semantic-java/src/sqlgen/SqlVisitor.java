// Generated from grammar/Sql.g4 by ANTLR 4.13.2
package sqlgen;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SqlParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SqlVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SqlParser#script}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScript(SqlParser.ScriptContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(SqlParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#createTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTable(SqlParser.CreateTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#columnDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnDef(SqlParser.ColumnDefContext ctx);
	/**
	 * Visit a parse tree produced by the {@code pkConstraint}
	 * labeled alternative in {@link SqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPkConstraint(SqlParser.PkConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code notNullConstraint}
	 * labeled alternative in {@link SqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotNullConstraint(SqlParser.NotNullConstraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#dataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataType(SqlParser.DataTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#dropTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropTable(SqlParser.DropTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#createIndex}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateIndex(SqlParser.CreateIndexContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#dropIndex}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropIndex(SqlParser.DropIndexContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#insertStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertStmt(SqlParser.InsertStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#valueRow}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueRow(SqlParser.ValueRowContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#updateStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdateStmt(SqlParser.UpdateStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(SqlParser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#deleteStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeleteStmt(SqlParser.DeleteStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#selectStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectStmt(SqlParser.SelectStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code starItem}
	 * labeled alternative in {@link SqlParser#selectItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStarItem(SqlParser.StarItemContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tableStarItem}
	 * labeled alternative in {@link SqlParser#selectItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableStarItem(SqlParser.TableStarItemContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprItem}
	 * labeled alternative in {@link SqlParser#selectItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprItem(SqlParser.ExprItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#tableRef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableRef(SqlParser.TableRefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#joinClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinClause(SqlParser.JoinClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#orderItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderItem(SqlParser.OrderItemContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inSubqueryExpr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInSubqueryExpr(SqlParser.InSubqueryExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code orExpr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrExpr(SqlParser.OrExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code existsExpr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExistsExpr(SqlParser.ExistsExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenExpr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenExpr(SqlParser.ParenExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code cmpExpr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCmpExpr(SqlParser.CmpExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code columnExpr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnExpr(SqlParser.ColumnExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code notExpr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotExpr(SqlParser.NotExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code addExpr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddExpr(SqlParser.AddExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code negExpr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNegExpr(SqlParser.NegExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code isNullExpr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIsNullExpr(SqlParser.IsNullExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code literalExpr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralExpr(SqlParser.LiteralExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inListExpr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInListExpr(SqlParser.InListExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code mulExpr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulExpr(SqlParser.MulExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code scalarSubqueryExpr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScalarSubqueryExpr(SqlParser.ScalarSubqueryExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aggregateExpr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggregateExpr(SqlParser.AggregateExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code andExpr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndExpr(SqlParser.AndExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(SqlParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#ident}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdent(SqlParser.IdentContext ctx);
}