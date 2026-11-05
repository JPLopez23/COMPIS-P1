package sql.ast;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import sql.SqlType;
import sqlgen.SqlBaseVisitor;
import sqlgen.SqlParser;

import java.util.ArrayList;
import java.util.List;

/** Convierte el árbol sintáctico de ANTLR en el AST de SQL. */
public class AstBuilder extends SqlBaseVisitor<Ast.Node> {

    private <T extends Ast.Node> T at(T node, ParserRuleContext ctx) {
        Token t = ctx.getStart();
        node.line = t.getLine();
        node.col = t.getCharPositionInLine() + 1;
        return node;
    }

    private Ast.Expr expr(SqlParser.ExprContext ctx) { return (Ast.Expr) visit(ctx); }

    private Ast.Select select(SqlParser.SelectStmtContext ctx) { return (Ast.Select) visit(ctx); }

    private static String name(SqlParser.IdentContext ctx) { return ctx.getText(); }

    // ── Programa y DDL ──────────────────────────────────────────────────────
    @Override public Ast.Node visitScript(SqlParser.ScriptContext ctx) {
        Ast.Script s = at(new Ast.Script(), ctx);
        for (SqlParser.StatementContext st : ctx.statement()) s.statements.add((Ast.Stmt) visit(st));
        return s;
    }

    @Override public Ast.Node visitStatement(SqlParser.StatementContext ctx) { return visit(ctx.getChild(0)); }

    @Override public Ast.Node visitCreateTable(SqlParser.CreateTableContext ctx) {
        List<Ast.ColumnDef> cols = new ArrayList<>();
        List<Ast.ForeignKey> fks = new ArrayList<>();
        for (SqlParser.TableElementContext e : ctx.tableElement()) {
            if (e instanceof SqlParser.ColumnElementContext) {
                SqlParser.ColumnDefContext def = ((SqlParser.ColumnElementContext) e).columnDef();
                cols.add((Ast.ColumnDef) visit(def));
                // REFERENCES escrito junto a la columna equivale a un FOREIGN KEY
                for (SqlParser.ColumnConstraintContext c : def.columnConstraint())
                    if (c instanceof SqlParser.ReferencesConstraintContext) {
                        SqlParser.ReferencesConstraintContext r = (SqlParser.ReferencesConstraintContext) c;
                        fks.add(at(new Ast.ForeignKey(name(def.ident()), name(r.ident(0)), name(r.ident(1))), r));
                    }
            } else {
                SqlParser.ForeignKeyElementContext f = (SqlParser.ForeignKeyElementContext) e;
                fks.add(at(new Ast.ForeignKey(name(f.ident(0)), name(f.ident(1)), name(f.ident(2))), f));
            }
        }
        return at(new Ast.CreateTable(name(ctx.ident()), cols, fks), ctx);
    }

    @Override public Ast.Node visitColumnDef(SqlParser.ColumnDefContext ctx) {
        SqlParser.DataTypeContext dt = ctx.dataType();
        SqlType type = SqlType.valueOf(dt.getStart().getText().toUpperCase());
        int size = dt.INTLIT() != null ? parseSize(dt.INTLIT().getText()) : 0;
        boolean pk = false, nn = false;
        for (SqlParser.ColumnConstraintContext c : ctx.columnConstraint()) {
            if (c instanceof SqlParser.PkConstraintContext) pk = true;
            else if (c instanceof SqlParser.NotNullConstraintContext) nn = true;
        }
        return at(new Ast.ColumnDef(name(ctx.ident()), type, size, pk, nn), ctx);
    }

    private static int parseSize(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return Integer.MAX_VALUE; }
    }

    @Override public Ast.Node visitDropTable(SqlParser.DropTableContext ctx) {
        return at(new Ast.DropTable(name(ctx.ident()), ctx.CASCADE() != null), ctx);
    }

    @Override public Ast.Node visitCreateIndex(SqlParser.CreateIndexContext ctx) {
        return at(new Ast.CreateIndex(name(ctx.ident(0)), name(ctx.ident(1)), name(ctx.ident(2))), ctx);
    }

    @Override public Ast.Node visitDropIndex(SqlParser.DropIndexContext ctx) {
        return at(new Ast.DropIndex(name(ctx.ident())), ctx);
    }

    // ── DML ─────────────────────────────────────────────────────────────────
    @Override public Ast.Node visitInsertStmt(SqlParser.InsertStmtContext ctx) {
        List<String> cols = null;
        if (ctx.ident().size() > 1) {
            cols = new ArrayList<>();
            for (int i = 1; i < ctx.ident().size(); i++) cols.add(name(ctx.ident(i)));
        }
        List<Ast.Row> rows = new ArrayList<>();
        for (SqlParser.ValueRowContext r : ctx.valueRow()) {
            List<Ast.Expr> vals = new ArrayList<>();
            for (SqlParser.ExprContext e : r.expr()) vals.add(expr(e));
            rows.add(at(new Ast.Row(vals), r));
        }
        return at(new Ast.Insert(name(ctx.ident(0)), cols, rows), ctx);
    }

    @Override public Ast.Node visitUpdateStmt(SqlParser.UpdateStmtContext ctx) {
        List<Ast.Assignment> as = new ArrayList<>();
        for (SqlParser.AssignmentContext a : ctx.assignment())
            as.add(at(new Ast.Assignment(name(a.ident()), expr(a.expr())), a));
        return at(new Ast.Update(name(ctx.ident()), as, ctx.expr() == null ? null : expr(ctx.expr())), ctx);
    }

    @Override public Ast.Node visitDeleteStmt(SqlParser.DeleteStmtContext ctx) {
        return at(new Ast.Delete(name(ctx.ident()), ctx.expr() == null ? null : expr(ctx.expr())), ctx);
    }

    // ── SELECT ──────────────────────────────────────────────────────────────
    @Override public Ast.Node visitSelectStmt(SqlParser.SelectStmtContext ctx) {
        List<Ast.SelectItem> items = new ArrayList<>();
        for (SqlParser.SelectItemContext i : ctx.selectItem()) items.add((Ast.SelectItem) visit(i));
        Ast.TableRef from = tableRef(ctx.tableRef());
        List<Ast.Join> joins = new ArrayList<>();
        for (SqlParser.JoinClauseContext j : ctx.joinClause())
            joins.add(at(new Ast.Join(tableRef(j.tableRef()), expr(j.expr())), j));
        // WHERE, GROUP BY y HAVING comparten la regla expr: se distinguen por la posición de los tokens
        Ast.Expr where = null, having = null;
        List<Ast.Expr> groupBy = new ArrayList<>();
        for (SqlParser.ExprContext e : ctx.expr()) {
            Ast.Expr built = expr(e);
            int prev = previousKeyword(ctx, e);
            if (prev == SqlParser.WHERE) where = built;
            else if (prev == SqlParser.HAVING) having = built;
            else groupBy.add(built);
        }
        List<Ast.OrderItem> order = new ArrayList<>();
        for (SqlParser.OrderItemContext o : ctx.orderItem())
            order.add(at(new Ast.OrderItem(expr(o.expr()), o.DESC() != null), o));
        return at(new Ast.Select(items, from, joins, where, groupBy, having, order), ctx);
    }

    /** Tipo del último WHERE/HAVING/GROUP/COMMA que precede a la expresión directa del SELECT. */
    private int previousKeyword(SqlParser.SelectStmtContext ctx, SqlParser.ExprContext e) {
        int idx = ctx.children.indexOf(e);
        for (int i = idx - 1; i >= 0; i--) {
            if (!(ctx.children.get(i) instanceof org.antlr.v4.runtime.tree.TerminalNode)) continue;
            int t = ((org.antlr.v4.runtime.tree.TerminalNode) ctx.children.get(i)).getSymbol().getType();
            if (t == SqlParser.WHERE || t == SqlParser.HAVING) return t;
            if (t == SqlParser.BY) return SqlParser.GROUP;
        }
        return SqlParser.GROUP;
    }

    private Ast.TableRef tableRef(SqlParser.TableRefContext ctx) {
        String alias = ctx.ident().size() > 1 ? name(ctx.ident(1)) : null;
        return at(new Ast.TableRef(name(ctx.ident(0)), alias), ctx);
    }

    @Override public Ast.Node visitStarItem(SqlParser.StarItemContext ctx) {
        return at(new Ast.SelectItem(null, null, true, null), ctx);
    }

    @Override public Ast.Node visitTableStarItem(SqlParser.TableStarItemContext ctx) {
        return at(new Ast.SelectItem(null, null, true, name(ctx.ident())), ctx);
    }

    @Override public Ast.Node visitExprItem(SqlParser.ExprItemContext ctx) {
        String alias = ctx.ident() == null ? null : name(ctx.ident());
        return at(new Ast.SelectItem(expr(ctx.expr()), alias, false, null), ctx);
    }

    // ── Expresiones ─────────────────────────────────────────────────────────
    @Override public Ast.Node visitParenExpr(SqlParser.ParenExprContext ctx) { return visit(ctx.expr()); }

    @Override public Ast.Node visitNegExpr(SqlParser.NegExprContext ctx) {
        return at(new Ast.Unary("-", expr(ctx.expr())), ctx);
    }

    @Override public Ast.Node visitNotExpr(SqlParser.NotExprContext ctx) {
        return at(new Ast.Unary("NOT", expr(ctx.expr())), ctx);
    }

    @Override public Ast.Node visitMulExpr(SqlParser.MulExprContext ctx) {
        return at(new Ast.Binary(ctx.op.getText(), expr(ctx.expr(0)), expr(ctx.expr(1))), ctx);
    }

    @Override public Ast.Node visitAddExpr(SqlParser.AddExprContext ctx) {
        return at(new Ast.Binary(ctx.op.getText(), expr(ctx.expr(0)), expr(ctx.expr(1))), ctx);
    }

    @Override public Ast.Node visitCmpExpr(SqlParser.CmpExprContext ctx) {
        String op = ctx.op.getText();
        if (op.equals("!=")) op = "<>";
        return at(new Ast.Binary(op, expr(ctx.expr(0)), expr(ctx.expr(1))), ctx);
    }

    @Override public Ast.Node visitAndExpr(SqlParser.AndExprContext ctx) {
        return at(new Ast.Binary("AND", expr(ctx.expr(0)), expr(ctx.expr(1))), ctx);
    }

    @Override public Ast.Node visitOrExpr(SqlParser.OrExprContext ctx) {
        return at(new Ast.Binary("OR", expr(ctx.expr(0)), expr(ctx.expr(1))), ctx);
    }

    @Override public Ast.Node visitIsNullExpr(SqlParser.IsNullExprContext ctx) {
        return at(new Ast.IsNull(expr(ctx.expr()), ctx.NOT() != null), ctx);
    }

    @Override public Ast.Node visitInListExpr(SqlParser.InListExprContext ctx) {
        List<Ast.Expr> values = new ArrayList<>();
        for (int i = 1; i < ctx.expr().size(); i++) values.add(expr(ctx.expr(i)));
        return at(new Ast.InList(expr(ctx.expr(0)), values, ctx.NOT() != null), ctx);
    }

    @Override public Ast.Node visitInSubqueryExpr(SqlParser.InSubqueryExprContext ctx) {
        return at(new Ast.InSubquery(expr(ctx.expr()), select(ctx.selectStmt()), ctx.NOT() != null), ctx);
    }

    @Override public Ast.Node visitExistsExpr(SqlParser.ExistsExprContext ctx) {
        return at(new Ast.Exists(select(ctx.selectStmt())), ctx);
    }

    @Override public Ast.Node visitScalarSubqueryExpr(SqlParser.ScalarSubqueryExprContext ctx) {
        return at(new Ast.ScalarSubquery(select(ctx.selectStmt())), ctx);
    }

    @Override public Ast.Node visitAggregateExpr(SqlParser.AggregateExprContext ctx) {
        Ast.Expr arg = ctx.expr() == null ? null : expr(ctx.expr());
        return at(new Ast.Aggregate(ctx.fn.getText().toUpperCase(), arg), ctx);
    }

    @Override public Ast.Node visitLiteralExpr(SqlParser.LiteralExprContext ctx) {
        SqlParser.LiteralContext l = ctx.literal();
        String text = l.getText();
        Ast.Literal lit;
        if (l.INTLIT() != null) {
            try { lit = new Ast.Literal(Long.parseLong(text), SqlType.INT, text); }
            catch (NumberFormatException e) { lit = new Ast.Literal(Double.parseDouble(text), SqlType.FLOAT, text); }
        } else if (l.FLOATLIT() != null) {
            lit = new Ast.Literal(Double.parseDouble(text), SqlType.FLOAT, text);
        } else if (l.STRING() != null) {
            lit = new Ast.Literal(text.substring(1, text.length() - 1).replace("''", "'"), SqlType.VARCHAR, text);
        } else if (l.TRUE() != null) {
            lit = new Ast.Literal(Boolean.TRUE, SqlType.BOOLEAN, "TRUE");
        } else if (l.FALSE() != null) {
            lit = new Ast.Literal(Boolean.FALSE, SqlType.BOOLEAN, "FALSE");
        } else {
            lit = new Ast.Literal(null, SqlType.NULL, "NULL");
        }
        return at(lit, ctx);
    }

    @Override public Ast.Node visitColumnExpr(SqlParser.ColumnExprContext ctx) {
        if (ctx.ident().size() == 2) return at(new Ast.ColumnRef(name(ctx.ident(0)), name(ctx.ident(1))), ctx);
        return at(new Ast.ColumnRef(null, name(ctx.ident(0))), ctx);
    }
}
