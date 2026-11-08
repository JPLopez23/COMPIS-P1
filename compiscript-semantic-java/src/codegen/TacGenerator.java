package codegen;

import generated.CompiscriptParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import semantic.*;

import java.util.*;

/**
 * Generador de código intermedio (TAC) de Compiscript.
 *
 * Recorre el árbol sintáctico después del análisis semántico y reutiliza su tabla de símbolos
 * (scopes, símbolos y tipos de sufijos) a la que le agrega direcciones, desplazamientos, tamaños,
 * etiquetas y la información de los registros de activación.
 */
public class TacGenerator extends ExprGenerator {

    public TacGenerator(SemanticAnalyzer sa) { super(sa); }

    // ======================================================================
    // Punto de entrada
    // ======================================================================

    /** Genera el TAC del programa completo y lo devuelve como texto. */
    public String generate(ProgramContext prog) {
        prepass();

        Frame main = new Frame("main", 0, null);
        frames.add(main);
        cur = main;
        curScope = sa.stack.globalScope;
        for (StatementContext st : prog.statement()) genStatement(st);
        finishFrame(main);
        globalSize = globalNext;

        StringBuilder sb = new StringBuilder();
        sb.append("; ===== Código intermedio (TAC) de Compiscript =====\n");
        sb.append("; área global: ").append(globalSize).append(" bytes\n\n");
        for (Frame f : frames) {
            sb.append("function ").append(f.label).append(":   ; nivel=").append(f.level)
              .append(" marco=").append(f.frameSize()).append("B temporales=").append(f.maxTemps).append('\n');
            for (String line : f.code) sb.append(line).append('\n');
            sb.append("end ").append(f.label).append("\n\n");
        }
        return sb.toString().replaceAll("\\s+$", "") + "\n";
    }

    // ======================================================================
    // Sentencias
    // ======================================================================

    protected void genStatement(StatementContext st) {
        if (st.variableDeclaration() != null) genVarDecl(st.variableDeclaration());
        else if (st.constantDeclaration() != null) genConstDecl(st.constantDeclaration());
        else if (st.assignment() != null) genAssignment(st.assignment());
        else if (st.functionDeclaration() != null) genFunction(st.functionDeclaration());
        else if (st.classDeclaration() != null) genClass(st.classDeclaration());
        else if (st.expressionStatement() != null) {
            String v = genExpr(st.expressionStatement().expression());
            free(v);
        } else if (st.printStatement() != null) {
            String v = genExpr(st.printStatement().expression());
            Type pt = sa.exprTypes.get(st.printStatement().expression());
            if (isString(pt)) emit("prints " + v);
            else if (PrimitiveType.BOOLEAN.equals(pt)) emit("printb " + v);
            else if (PrimitiveType.NULL.equals(pt)) emit("prints \"null\"");
            else emit("print " + v);
            free(v);
        } else if (st.block() != null) genBlock(st.block(), true);
        else if (st.ifStatement() != null) genIf(st.ifStatement());
        else if (st.whileStatement() != null) genWhile(st.whileStatement());
        else if (st.doWhileStatement() != null) genDoWhile(st.doWhileStatement());
        else if (st.forStatement() != null) genFor(st.forStatement());
        else if (st.foreachStatement() != null) genForeach(st.foreachStatement());
        else if (st.tryCatchStatement() != null) genTry(st.tryCatchStatement());
        else if (st.switchStatement() != null) genSwitch(st.switchStatement());
        else if (st.breakStatement() != null) {
            if (!breakTargets.isEmpty()) emit("goto " + breakTargets.peek());
        } else if (st.continueStatement() != null) {
            if (!continueTargets.isEmpty()) emit("goto " + continueTargets.peek());
        } else if (st.returnStatement() != null) {
            ExpressionContext e = st.returnStatement().expression();
            if (e == null) emit("return");
            else {
                String v = genExpr(e);
                emit("return " + v);
                free(v);
            }
        }
        cur.freeAll();
    }

    protected void genBlock(BlockContext b, boolean newScope) {
        Scope saved = curScope;
        if (newScope) {
            Scope s = sa.scopeOf.get(b);
            if (s != null) curScope = s;
        }
        for (StatementContext st : b.statement()) genStatement(st);
        curScope = saved;
    }

    protected void genVarDecl(VariableDeclarationContext ctx) {
        Symbol sym = curScope.resolveLocal(ctx.Identifier().getText());
        declare(sym, false);
        if (ctx.initializer() != null && sym != null) {
            String v = genExpr(ctx.initializer().expression());
            assignTo(varOperand(sym), v);
        }
    }

    protected void genConstDecl(ConstantDeclarationContext ctx) {
        Symbol sym = curScope.resolveLocal(ctx.Identifier().getText());
        declare(sym, false);
        if (sym != null) {
            String v = genExpr(ctx.expression());
            assignTo(varOperand(sym), v);
        }
    }

    protected void genAssignment(AssignmentContext ctx) {
        List<ExpressionContext> exprs = ctx.expression();
        if (exprs.size() == 1) {
            Symbol sym = curScope.resolve(ctx.Identifier().getText());
            LValue lv = lvalueOfSymbol(sym);
            String v = genExpr(exprs.get(0));
            store(lv, v);
            free(v);
        } else {
            String obj = genExpr(exprs.get(0));
            LValue lv = new LValue();
            lv.kind = "field";
            lv.base = obj;
            String fname = ctx.Identifier().getText();
            lv.name = fname + "@" + fieldOffset(sa.exprTypes.get(exprs.get(0)), fname);
            String v = genExpr(exprs.get(1));
            store(lv, v);
            free(v);
            free(obj);
        }
    }

    protected void genIf(IfStatementContext ctx) {
        String lt = newLabel(), lf = newLabel();
        List<BlockContext> bl = ctx.block();
        cond(ctx.expression(), lt, lf);
        emitLabel(lt);
        genBlock(bl.get(0), true);
        if (bl.size() > 1) {
            String le = newLabel();
            emit("goto " + le);
            emitLabel(lf);
            genBlock(bl.get(1), true);
            emitLabel(le);
        } else {
            emitLabel(lf);
        }
    }

    protected void genWhile(WhileStatementContext ctx) {
        String top = newLabel(), body = newLabel(), end = newLabel();
        emitLabel(top);
        cond(ctx.expression(), body, end);
        emitLabel(body);
        breakTargets.push(end);
        continueTargets.push(top);
        genBlock(ctx.block(), true);
        breakTargets.pop();
        continueTargets.pop();
        emit("goto " + top);
        emitLabel(end);
    }

    protected void genDoWhile(DoWhileStatementContext ctx) {
        String body = newLabel(), cont = newLabel(), end = newLabel();
        emitLabel(body);
        breakTargets.push(end);
        continueTargets.push(cont);
        genBlock(ctx.block(), true);
        breakTargets.pop();
        continueTargets.pop();
        emitLabel(cont);
        cond(ctx.expression(), body, end);
        emitLabel(end);
    }

    protected void genFor(ForStatementContext ctx) {
        Scope saved = curScope;
        Scope s = sa.scopeOf.get(ctx);
        if (s != null) curScope = s;

        // Se separa init / condición / paso mirando los hijos directos.
        ExpressionContext condE = null, stepE = null;
        int semis = 0;
        for (int i = 2; i < ctx.getChildCount(); i++) {
            ParseTree ch = ctx.getChild(i);
            if (ch instanceof TerminalNode) {
                if (ch.getText().equals(";")) semis++;
                if (ch.getText().equals(")")) break;
            } else if (ch instanceof VariableDeclarationContext || ch instanceof AssignmentContext) {
                semis = 1;
            } else if (ch instanceof ExpressionContext) {
                if (semis <= 1) condE = (ExpressionContext) ch; else stepE = (ExpressionContext) ch;
            }
        }
        if (ctx.variableDeclaration() != null) genVarDecl(ctx.variableDeclaration());
        else if (ctx.assignment() != null) genAssignment(ctx.assignment());
        cur.freeAll();

        String top = newLabel(), body = newLabel(), cont = newLabel(), end = newLabel();
        emitLabel(top);
        if (condE != null) {
            cond(condE, body, end);
            emitLabel(body);
        }
        breakTargets.push(end);
        continueTargets.push(cont);
        genBlock(ctx.block(), false);
        breakTargets.pop();
        continueTargets.pop();
        emitLabel(cont);
        if (stepE != null) {
            String v = genExpr(stepE);
            free(v);
            cur.freeAll();
        }
        emit("goto " + top);
        emitLabel(end);
        curScope = saved;
    }

    protected void genForeach(ForeachStatementContext ctx) {
        Scope saved = curScope;
        Scope s = sa.scopeOf.get(ctx);
        String arr = hiddenLocal("arr"), idx = hiddenLocal("i");
        String src = genExpr(ctx.expression());
        assignTo(arr, src);
        emit(idx + " = 0");
        cur.freeAll();
        if (s != null) curScope = s;
        Symbol it = curScope.resolveLocal(ctx.Identifier().getText());
        declare(it, false);

        String top = newLabel(), body = newLabel(), cont = newLabel(), end = newLabel();
        emitLabel(top);
        String len = newTemp();
        emit(len + " = len " + arr);
        emit("if " + idx + " < " + len + " goto " + body);
        free(len);
        emit("goto " + end);
        emitLabel(body);
        emit(varOperand(it) + " = " + arr + "[" + idx + "]");
        breakTargets.push(end);
        continueTargets.push(cont);
        genBlock(ctx.block(), false);
        breakTargets.pop();
        continueTargets.pop();
        emitLabel(cont);
        emit(idx + " = " + idx + " + 1");
        emit("goto " + top);
        emitLabel(end);
        curScope = saved;
    }

    protected void genTry(TryCatchStatementContext ctx) {
        List<BlockContext> bl = ctx.block();
        String lc = newLabel(), le = newLabel();
        emit("pushhandler " + lc);
        genBlock(bl.get(0), true);
        emit("pophandler");
        emit("goto " + le);
        emitLabel(lc);
        Scope saved = curScope;
        Scope s = sa.scopeOf.get(ctx);
        if (s != null) curScope = s;
        Symbol ex = curScope.resolveLocal(ctx.Identifier().getText());
        declare(ex, false);
        if (ex != null) emit(varOperand(ex) + " = exception");
        genBlock(bl.get(1), false);
        curScope = saved;
        emitLabel(le);
    }

    protected void genSwitch(SwitchStatementContext ctx) {
        String sw = hiddenLocal("sw");
        String v = genExpr(ctx.expression());
        assignTo(sw, v);
        cur.freeAll();
        String end = newLabel();
        List<SwitchCaseContext> cases = ctx.switchCase();
        List<String> labels = new ArrayList<>();
        for (SwitchCaseContext c : cases) {
            String lc = newLabel();
            labels.add(lc);
            String cv = genExpr(c.expression());
            emit("if " + sw + (isString(sa.exprTypes.get(ctx.expression())) ? " ==s " : " == ") + cv + " goto " + lc);
            free(cv);
        }
        DefaultCaseContext def = ctx.defaultCase();
        String ld = def != null ? newLabel() : end;
        emit("goto " + ld);
        cur.freeAll();

        breakTargets.push(end);
        Scope saved = curScope;
        for (int i = 0; i < cases.size(); i++) {
            emitLabel(labels.get(i));
            Scope s = sa.scopeOf.get(cases.get(i));
            curScope = s != null ? s : saved;
            for (StatementContext st : cases.get(i).statement()) genStatement(st);
        }
        if (def != null) {
            emitLabel(ld);
            Scope s = sa.scopeOf.get(def);
            curScope = s != null ? s : saved;
            for (StatementContext st : def.statement()) genStatement(st);
        }
        curScope = saved;
        breakTargets.pop();
        emitLabel(end);
    }

    // ======================================================================
    // Funciones y clases
    // ======================================================================

    protected void genFunction(FunctionDeclarationContext ctx) {
        Scope sc = sa.scopeOf.get(ctx);
        if (sc == null || sc.ownerFn == null) return;
        FunctionSymbol fs = sc.ownerFn;

        Frame f = new Frame(fs.label, fs.nestLevel, cur);
        frames.add(f);
        Frame savedFrame = cur;
        Scope savedScope = curScope;
        Deque<String> sb = breakTargets, sc2 = continueTargets;
        breakTargets = new ArrayDeque<>();
        continueTargets = new ArrayDeque<>();
        cur = f;
        curScope = sc;

        if (trueMethods.contains(fs)) {
            f.hasThis = true;
            f.usedNames.add("this");
            Symbol th = sc.resolveLocal("this");
            if (th != null) {
                th.storage = "PARAM";
                th.size = 4;
                th.nestLevel = f.level;
                th.tacName = "this";
                th.offset = f.alloc("this", "param", 4);
            } else {
                f.alloc("this", "param", 4);
            }
        }
        for (String pn : fs.paramNames) declare(sc.resolveLocal(pn), true);

        genBlock(ctx.block(), false);
        if (!endsWithReturn(f)) emit("return");

        fs.paramsSize = f.paramsSize;
        fs.localsSize = f.localsSize;
        fs.tempCount = f.maxTemps;
        finishFrame(f);
        fs.frameSize = f.frameSize();

        cur = savedFrame;
        curScope = savedScope;
        breakTargets = sb;
        continueTargets = sc2;
    }

    protected static boolean endsWithReturn(Frame f) {
        return !f.code.isEmpty() && f.code.get(f.code.size() - 1).startsWith("    return");
    }

    protected void genClass(ClassDeclarationContext ctx) {
        Scope sc = sa.scopeOf.get(ctx);
        if (sc == null || sc.ownerClass == null) return;
        ClassSymbol cs = sc.ownerClass;
        String il = initLabel.get(cs.name);
        if (il != null) {
            Frame f = new Frame(il, cur.level + 1, cur);
            f.hasThis = true;
            frames.add(f);
            Frame savedFrame = cur;
            Scope savedScope = curScope;
            cur = f;
            curScope = sc;
            f.alloc("this", "param", 4);
            for (ClassMemberContext m : ctx.classMember()) {
                if (m.variableDeclaration() != null && m.variableDeclaration().initializer() != null) {
                    String v = genExpr(m.variableDeclaration().initializer().expression());
                    String fn = m.variableDeclaration().Identifier().getText();
                    emit("this." + fn + "@" + cs.fieldOffsets.get(fn) + " = " + v);
                    free(v);
                } else if (m.constantDeclaration() != null) {
                    String v = genExpr(m.constantDeclaration().expression());
                    String fn = m.constantDeclaration().Identifier().getText();
                    emit("this." + fn + "@" + cs.fieldOffsets.get(fn) + " = " + v);
                    free(v);
                }
                cur.freeAll();
            }
            emit("return");
            finishFrame(f);
            cur = savedFrame;
            curScope = savedScope;
        }
        for (ClassMemberContext m : ctx.classMember()) {
            if (m.functionDeclaration() != null) genFunction(m.functionDeclaration());
        }
    }
}
