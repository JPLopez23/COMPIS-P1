package codegen;

import generated.CompiscriptParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import semantic.*;

import java.util.*;

/** Traducción de expresiones (aritméticas, lógicas, comparaciones, ternario) y código de saltos con cortocircuito. */
public abstract class ExprGenerator extends AccessGenerator {

    protected ExprGenerator(SemanticAnalyzer sa) { super(sa); }

    // ======================================================================
    // Expresiones (valor)
    // ======================================================================

    protected String genExpr(ParseTree n) {
        if (n instanceof ExpressionContext) return genExpr(((ExpressionContext) n).assignmentExpr());
        if (n instanceof AssignExprContext) {
            AssignExprContext c = (AssignExprContext) n;
            LValue lv = lvalueOfLhs(c.lhs);
            String v = genExpr(c.assignmentExpr());
            return store(lv, v);
        }
        if (n instanceof PropertyAssignExprContext) {
            PropertyAssignExprContext c = (PropertyAssignExprContext) n;
            LValue lv = new LValue();
            lv.kind = "field";
            lv.base = genLhs(c.lhs, c.lhs.suffixOp().size());
            lv.name = c.Identifier().getText();
            String v = genExpr(c.assignmentExpr());
            return store(lv, v);
        }
        if (n instanceof ExprNoAssignContext) return genExpr(((ExprNoAssignContext) n).conditionalExpr());
        if (n instanceof TernaryExprContext) return genTernary((TernaryExprContext) n);
        if (n instanceof LogicalOrExprContext) {
            LogicalOrExprContext c = (LogicalOrExprContext) n;
            if (c.logicalAndExpr().size() == 1) return genExpr(c.logicalAndExpr(0));
            return materializeBool(n);
        }
        if (n instanceof LogicalAndExprContext) {
            LogicalAndExprContext c = (LogicalAndExprContext) n;
            if (c.equalityExpr().size() == 1) return genExpr(c.equalityExpr(0));
            return materializeBool(n);
        }
        if (n instanceof EqualityExprContext) {
            EqualityExprContext c = (EqualityExprContext) n;
            return genBinaryChain(c, c.relationalExpr().size(), 0);
        }
        if (n instanceof RelationalExprContext) {
            RelationalExprContext c = (RelationalExprContext) n;
            return genBinaryChain(c, c.additiveExpr().size(), 0);
        }
        if (n instanceof AdditiveExprContext) {
            AdditiveExprContext c = (AdditiveExprContext) n;
            return genBinaryChain(c, c.multiplicativeExpr().size(), 0);
        }
        if (n instanceof MultiplicativeExprContext) {
            MultiplicativeExprContext c = (MultiplicativeExprContext) n;
            return genBinaryChain(c, c.unaryExpr().size(), 0);
        }
        if (n instanceof UnaryExprContext) return genUnary((UnaryExprContext) n);
        if (n instanceof PrimaryExprContext) {
            PrimaryExprContext c = (PrimaryExprContext) n;
            if (c.literalExpr() != null) return genLiteral(c.literalExpr());
            if (c.leftHandSide() != null) return genLhs(c.leftHandSide(), c.leftHandSide().suffixOp().size());
            return genExpr(c.expression());
        }
        throw new IllegalStateException("Expresión no soportada: " + n.getClass().getSimpleName());
    }

    /** Operandos hijos (no terminales) de un nodo binario encadenado, en orden. */
    protected List<ParseTree> operandsOf(ParseTree chain) {
        List<ParseTree> ops = new ArrayList<>();
        for (int i = 0; i < chain.getChildCount(); i += 2) ops.add(chain.getChild(i));
        return ops;
    }

    protected String genBinaryChain(ParseTree chain, int count, int unused) {
        List<ParseTree> ops = operandsOf(chain);
        if (ops.size() == 1) return genExpr(ops.get(0));
        String acc = genExpr(ops.get(0));
        for (int i = 1; i < ops.size(); i++) {
            String op = chain.getChild(2 * i - 1).getText();
            String rhs = genExpr(ops.get(i));
            free(acc);
            free(rhs);
            String t = newTemp();
            emit(t + " = " + acc + " " + op + " " + rhs);
            acc = t;
        }
        return acc;
    }

    protected String genUnary(UnaryExprContext c) {
        if (c.getChildCount() == 1 || c.primaryExpr() != null) return genExpr(c.primaryExpr());
        String op = c.getChild(0).getText();
        String v = genExpr(c.unaryExpr());
        if (op.equals("-") && v.matches("\\d+")) return "-" + v;
        free(v);
        String t = newTemp();
        emit(t + " = " + (op.equals("-") ? "- " : "! ") + v);
        return t;
    }

    protected String genLiteral(LiteralExprContext c) {
        if (c.arrayLiteral() != null) {
            List<ExpressionContext> es = c.arrayLiteral().expression();
            String t = newTemp();
            emit(t + " = newarray " + es.size());
            for (int i = 0; i < es.size(); i++) {
                String v = genExpr(es.get(i));
                emit(t + "[" + i + "] = " + v);
                free(v);
            }
            return t;
        }
        return c.getText();
    }

    protected String genTernary(TernaryExprContext c) {
        if (c.expression().isEmpty()) return genExpr(c.logicalOrExpr());
        String r = newTemp();
        String lt = newLabel(), lf = newLabel(), le = newLabel();
        cond(c.logicalOrExpr(), lt, lf);
        emitLabel(lt);
        String a = genExpr(c.expression(0));
        assignTo(r, a);
        emit("goto " + le);
        emitLabel(lf);
        String b = genExpr(c.expression(1));
        assignTo(r, b);
        emitLabel(le);
        return r;
    }

    /** Evalúa una expresión lógica compuesta (&&, ||) a un valor booleano usando saltos con cortocircuito. */
    protected String materializeBool(ParseTree n) {
        String r = newTemp();
        String lt = newLabel(), lf = newLabel(), le = newLabel();
        cond(n, lt, lf);
        emitLabel(lt);
        emit(r + " = true");
        emit("goto " + le);
        emitLabel(lf);
        emit(r + " = false");
        emitLabel(le);
        return r;
    }

    // ======================================================================
    // Condiciones (código de saltos con cortocircuito)
    // ======================================================================

    protected void cond(ParseTree n, String lt, String lf) {
        if (n instanceof ExpressionContext) { cond(((ExpressionContext) n).assignmentExpr(), lt, lf); return; }
        if (n instanceof ExprNoAssignContext) { cond(((ExprNoAssignContext) n).conditionalExpr(), lt, lf); return; }
        if (n instanceof TernaryExprContext && ((TernaryExprContext) n).expression().isEmpty()) {
            cond(((TernaryExprContext) n).logicalOrExpr(), lt, lf);
            return;
        }
        if (n instanceof LogicalOrExprContext) {
            List<LogicalAndExprContext> l = ((LogicalOrExprContext) n).logicalAndExpr();
            for (int i = 0; i < l.size() - 1; i++) {
                String next = newLabel();
                cond(l.get(i), lt, next);
                emitLabel(next);
            }
            cond(l.get(l.size() - 1), lt, lf);
            return;
        }
        if (n instanceof LogicalAndExprContext) {
            List<EqualityExprContext> l = ((LogicalAndExprContext) n).equalityExpr();
            for (int i = 0; i < l.size() - 1; i++) {
                String next = newLabel();
                cond(l.get(i), next, lf);
                emitLabel(next);
            }
            cond(l.get(l.size() - 1), lt, lf);
            return;
        }
        if (n instanceof EqualityExprContext || n instanceof RelationalExprContext) {
            List<ParseTree> ops = operandsOf(n);
            if (ops.size() == 1) { cond(ops.get(0), lt, lf); return; }
            if (ops.size() == 2) {
                String a = genExpr(ops.get(0));
                String b = genExpr(ops.get(1));
                emit("if " + a + " " + n.getChild(1).getText() + " " + b + " goto " + lt);
                free(a);
                free(b);
                emit("goto " + lf);
                return;
            }
        }
        if (n instanceof AdditiveExprContext || n instanceof MultiplicativeExprContext) {
            List<ParseTree> ops = operandsOf(n);
            if (ops.size() == 1) { cond(ops.get(0), lt, lf); return; }
        }
        if (n instanceof UnaryExprContext) {
            UnaryExprContext u = (UnaryExprContext) n;
            if (u.primaryExpr() != null) { cond(u.primaryExpr(), lt, lf); return; }
            if (u.getChild(0).getText().equals("!")) { cond(u.unaryExpr(), lf, lt); return; }
        }
        if (n instanceof PrimaryExprContext) {
            PrimaryExprContext p = (PrimaryExprContext) n;
            if (p.expression() != null) { cond(p.expression(), lt, lf); return; }
            if (p.literalExpr() != null) {
                String t = p.literalExpr().getText();
                if (t.equals("true")) { emit("goto " + lt); return; }
                if (t.equals("false")) { emit("goto " + lf); return; }
            }
        }
        String v = genExpr(n);
        emit("if " + v + " goto " + lt);
        free(v);
        emit("goto " + lf);
    }
}
