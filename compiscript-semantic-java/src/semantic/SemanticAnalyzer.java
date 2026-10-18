package semantic;

import generated.CompiscriptBaseVisitor;
import generated.CompiscriptParser;
import generated.CompiscriptParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Analizador semántico de Compiscript: recorre el ParseTree de ANTLR con un Visitor. */
public class SemanticAnalyzer extends CompiscriptBaseVisitor<Void> {

    public final ScopeStack stack = new ScopeStack();
    public final List<SemanticError> errors = new ArrayList<>();
    public final List<SemanticError> warnings = new ArrayList<>();

    /** Scope creado para cada nodo del árbol (bloque, función, clase, for, foreach, case...). Lo usa el generador de TAC. */
    public final Map<ParserRuleContext, Scope> scopeOf = new java.util.IdentityHashMap<>();
    /** Tipo estático de átomos y sufijos de leftHandSide. Lo usa el generador de TAC. */
    public final Map<ParseTree, Type> nodeTypes = new java.util.IdentityHashMap<>();

    private int loopDepth = 0;
    private int switchDepth = 0;
    private final Deque<FunctionSymbol> functionStack = new ArrayDeque<>();
    private final Deque<ClassSymbol> classStack = new ArrayDeque<>();
    private final Map<String, ClassSymbol> classes = new LinkedHashMap<>();

    // -------------------- utilidades --------------------

    private static int[] pos(ParserRuleContext ctx) {
        return new int[]{ctx.start.getLine(), ctx.start.getCharPositionInLine() + 1};
    }

    private void err(String cat, String msg, ParserRuleContext ctx) {
        int[] p = pos(ctx);
        errors.add(new SemanticError(cat, msg, p[0], p[1]));
    }

    private void warn(String cat, String msg, ParserRuleContext ctx) {
        int[] p = pos(ctx);
        warnings.add(new SemanticError(cat, msg, p[0], p[1]));
    }

    private Type resolveType(TypeContext ctx) {
        if (ctx == null) return PrimitiveType.VOID;
        String name = ctx.baseType().getText();
        String text = ctx.getText();
        int depth = 0;
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) == '[' && text.charAt(i + 1) == ']') depth++;
        }
        Type base;
        switch (name) {
            case "integer": base = PrimitiveType.INTEGER; break;
            case "string": base = PrimitiveType.STRING; break;
            case "boolean": base = PrimitiveType.BOOLEAN; break;
            default:
                ClassSymbol cls = classes.get(name);
                base = (cls != null) ? cls.classType : new ClassType(name);
        }
        Type t = base;
        for (int i = 0; i < depth; i++) t = new ArrayType(t);
        return t;
    }

    // -------------------- programa --------------------

    @Override
    public Void visitProgram(ProgramContext ctx) {
        for (StatementContext st : ctx.statement()) preregister(st);
        for (StatementContext st : ctx.statement()) analyzeStmt(st);
        return null;
    }

    private void preregister(StatementContext st) {
        if (st.classDeclaration() != null) preregisterClass(st.classDeclaration());
        else if (st.functionDeclaration() != null) preregisterFunction(st.functionDeclaration());
    }

    private void preregisterClass(ClassDeclarationContext ctx) {
        List<org.antlr.v4.runtime.tree.TerminalNode> idents = ctx.Identifier();
        String name = idents.get(0).getText();
        ClassType parent = null;
        if (idents.size() > 1) {
            ClassSymbol p = classes.get(idents.get(1).getText());
            if (p != null) parent = p.classType;
        }
        ClassType ct = new ClassType(name, parent);
        int[] p = pos(ctx);
        ClassSymbol sym = new ClassSymbol(name, ct, p[0], p[1]);
        if (!stack.declare(sym)) {
            err("SCOPE", "redeclaración de '" + name + "' en el ámbito global", ctx);
            return;
        }
        classes.put(name, sym);
    }

    private void preregisterFunction(FunctionDeclarationContext ctx) {
        String name = ctx.Identifier().getText();
        List<String> pNames = new ArrayList<>();
        List<Type> pTypes = new ArrayList<>();
        if (ctx.parameters() != null) {
            for (ParameterContext pc : ctx.parameters().parameter()) {
                pNames.add(pc.Identifier().getText());
                pTypes.add(pc.type() != null ? resolveType(pc.type()) : ErrorType.INSTANCE);
            }
        }
        Type ret = ctx.type() != null ? resolveType(ctx.type()) : PrimitiveType.VOID;
        FunctionType fnType = new FunctionType(new ArrayList<>(pTypes), ret);
        int[] p = pos(ctx);
        FunctionSymbol sym = new FunctionSymbol(name, fnType, p[0], p[1]);
        sym.paramNames = pNames;
        sym.paramTypes = pTypes;
        sym.returnType = ret;
        if (!stack.declare(sym)) err("FUNCTION", "redeclaración de función '" + name + "'", ctx);
    }

    // -------------------- statements --------------------

    /** Devuelve true si el statement termina el flujo (return/break/continue). */
    private boolean analyzeStmt(StatementContext ctx) {
        if (ctx == null) return false;
        if (ctx.variableDeclaration() != null) return stmtVariableDeclaration(ctx.variableDeclaration());
        if (ctx.constantDeclaration() != null) return stmtConstantDeclaration(ctx.constantDeclaration());
        if (ctx.assignment() != null) return stmtAssignment(ctx.assignment());
        if (ctx.functionDeclaration() != null) return stmtFunctionDeclaration(ctx.functionDeclaration());
        if (ctx.classDeclaration() != null) return stmtClassDeclaration(ctx.classDeclaration());
        if (ctx.expressionStatement() != null) return stmtExpressionStatement(ctx.expressionStatement());
        if (ctx.printStatement() != null) return stmtPrintStatement(ctx.printStatement());
        if (ctx.block() != null) return analyzeBlock(ctx.block(), true, "block");
        if (ctx.ifStatement() != null) return stmtIfStatement(ctx.ifStatement());
        if (ctx.whileStatement() != null) return stmtWhileStatement(ctx.whileStatement());
        if (ctx.doWhileStatement() != null) return stmtDoWhileStatement(ctx.doWhileStatement());
        if (ctx.forStatement() != null) return stmtForStatement(ctx.forStatement());
        if (ctx.foreachStatement() != null) return stmtForeachStatement(ctx.foreachStatement());
        if (ctx.tryCatchStatement() != null) return stmtTryCatchStatement(ctx.tryCatchStatement());
        if (ctx.switchStatement() != null) return stmtSwitchStatement(ctx.switchStatement());
        if (ctx.breakStatement() != null) return stmtBreakStatement(ctx.breakStatement());
        if (ctx.continueStatement() != null) return stmtContinueStatement(ctx.continueStatement());
        if (ctx.returnStatement() != null) return stmtReturnStatement(ctx.returnStatement());
        return false;
    }

    private boolean analyzeBlock(BlockContext ctx, boolean pushScope, String label) {
        if (pushScope) scopeOf.put(ctx, stack.push("block", label));
        boolean terminates = false;
        boolean deadReported = false;
        for (StatementContext st : ctx.statement()) {
            if (terminates && !deadReported) {
                warn("GENERAL", "código muerto (statement inalcanzable)", st);
                deadReported = true;
            }
            if (analyzeStmt(st)) terminates = true;
        }
        if (pushScope) stack.pop();
        return terminates;
    }

    private boolean stmtVariableDeclaration(VariableDeclarationContext ctx) {
        String name = ctx.Identifier().getText();
        Type declType = ctx.typeAnnotation() != null ? resolveType(ctx.typeAnnotation().type()) : null;
        Type initType = null;
        if (ctx.initializer() != null) initType = typeOfExpression(ctx.initializer().expression());
        Type varType;
        if (declType == null && initType == null) {
            err("TYPE", "variable '" + name + "' sin tipo ni inicializador", ctx);
            varType = ErrorType.INSTANCE;
        } else if (declType == null) {
            varType = initType;
        } else {
            varType = declType;
            if (initType != null && !TypeUtils.isAssignable(declType, initType))
                err("TYPE", "no se puede inicializar '" + name + "': " + declType + " con " + initType, ctx);
        }
        int[] p = pos(ctx);
        VariableSymbol sym = new VariableSymbol(name, varType, p[0], p[1]);
        sym.initialized = initType != null;
        if (!stack.declare(sym)) err("SCOPE", "redeclaración de '" + name + "' en el ámbito actual", ctx);
        return false;
    }

    private boolean stmtConstantDeclaration(ConstantDeclarationContext ctx) {
        String name = ctx.Identifier().getText();
        Type declType = ctx.typeAnnotation() != null ? resolveType(ctx.typeAnnotation().type()) : null;
        Type initType = typeOfExpression(ctx.expression());
        if (declType != null && !TypeUtils.isAssignable(declType, initType))
            err("TYPE", "no se puede inicializar const '" + name + "': " + declType + " con " + initType, ctx);
        int[] p = pos(ctx);
        ConstantSymbol sym = new ConstantSymbol(name, declType != null ? declType : initType, p[0], p[1]);
        if (!stack.declare(sym)) err("SCOPE", "redeclaración de '" + name + "' en el ámbito actual", ctx);
        return false;
    }

    private boolean stmtAssignment(AssignmentContext ctx) {
        List<ExpressionContext> exprs = ctx.expression();
        if (ctx.Identifier() != null && exprs.size() == 1) {
            String name = ctx.Identifier().getText();
            Symbol sym = stack.resolve(name);
            Type rhsT = typeOfExpression(exprs.get(0));
            if (sym == null) {
                err("SCOPE", "variable '" + name + "' no declarada", ctx);
                return false;
            }
            if (!sym.mutable) {
                err("TYPE", "'" + name + "' es constante, no se puede reasignar", ctx);
                return false;
            }
            if (!TypeUtils.isAssignable(sym.type, rhsT))
                err("TYPE", "no se puede asignar " + rhsT + " a '" + name + "' de tipo " + sym.type, ctx);
            sym.initialized = true;
        } else if (exprs.size() == 2) {
            Type objT = typeOfExpression(exprs.get(0));
            Type rhsT = typeOfExpression(exprs.get(1));
            String prop = ctx.Identifier().getText();
            if (objT instanceof ClassType) {
                Symbol m = ((ClassType) objT).lookup(prop);
                if (m == null) err("CLASS", "la clase '" + ((ClassType) objT).name + "' no tiene el miembro '" + prop + "'", ctx);
                else if (!TypeUtils.isAssignable(m.type, rhsT))
                    err("TYPE", "no se puede asignar " + rhsT + " a " + ((ClassType) objT).name + "." + prop + " (" + m.type + ")", ctx);
            } else if (!objT.isError()) {
                err("TYPE", "acceso a propiedad sobre no-objeto (" + objT + ")", ctx);
            }
        }
        return false;
    }

    private boolean stmtExpressionStatement(ExpressionStatementContext ctx) {
        typeOfExpression(ctx.expression());
        return false;
    }

    private boolean stmtPrintStatement(PrintStatementContext ctx) {
        typeOfExpression(ctx.expression());
        return false;
    }

    private boolean stmtIfStatement(IfStatementContext ctx) {
        Type condT = typeOfExpression(ctx.expression());
        if (!condT.equals(PrimitiveType.BOOLEAN) && !condT.isError())
            err("FLOW", "la condición del 'if' debe ser boolean, es " + condT, ctx);
        List<BlockContext> blocks = ctx.block();
        boolean termThen = analyzeBlock(blocks.get(0), true, "if");
        boolean termElse = false;
        if (blocks.size() > 1) termElse = analyzeBlock(blocks.get(1), true, "else");
        return termThen && termElse;
    }

    private boolean stmtWhileStatement(WhileStatementContext ctx) {
        Type condT = typeOfExpression(ctx.expression());
        if (!condT.equals(PrimitiveType.BOOLEAN) && !condT.isError())
            err("FLOW", "la condición de 'while' debe ser boolean, es " + condT, ctx);
        loopDepth++;
        analyzeBlock(ctx.block(), true, "while");
        loopDepth--;
        return false;
    }

    private boolean stmtDoWhileStatement(DoWhileStatementContext ctx) {
        loopDepth++;
        analyzeBlock(ctx.block(), true, "do-while");
        loopDepth--;
        Type condT = typeOfExpression(ctx.expression());
        if (!condT.equals(PrimitiveType.BOOLEAN) && !condT.isError())
            err("FLOW", "la condición de 'do-while' debe ser boolean, es " + condT, ctx);
        return false;
    }

    private boolean stmtForStatement(ForStatementContext ctx) {
        scopeOf.put(ctx, stack.push("block", "for"));
        if (ctx.variableDeclaration() != null) stmtVariableDeclaration(ctx.variableDeclaration());
        else if (ctx.assignment() != null) stmtAssignment(ctx.assignment());
        List<ExpressionContext> exprs = ctx.expression();
        if (exprs.size() > 0) {
            Type condT = typeOfExpression(exprs.get(0));
            if (!condT.equals(PrimitiveType.BOOLEAN) && !condT.isError())
                err("FLOW", "la condición de 'for' debe ser boolean, es " + condT, ctx);
        }
        if (exprs.size() > 1) typeOfExpression(exprs.get(1));
        loopDepth++;
        analyzeBlock(ctx.block(), false, "for-body");
        loopDepth--;
        stack.pop();
        return false;
    }

    private boolean stmtForeachStatement(ForeachStatementContext ctx) {
        String name = ctx.Identifier().getText();
        Type itT = typeOfExpression(ctx.expression());
        Type elem;
        if (itT instanceof ArrayType) elem = ((ArrayType) itT).elem;
        else if (itT.isError()) elem = ErrorType.INSTANCE;
        else {
            err("LIST", "'foreach' requiere un iterable (array), se obtuvo " + itT, ctx);
            elem = ErrorType.INSTANCE;
        }
        scopeOf.put(ctx, stack.push("block", "foreach"));
        int[] p = pos(ctx);
        VariableSymbol sym = new VariableSymbol(name, elem, p[0], p[1]);
        sym.initialized = true;
        stack.declare(sym);
        loopDepth++;
        analyzeBlock(ctx.block(), false, "foreach-body");
        loopDepth--;
        stack.pop();
        return false;
    }

    private boolean stmtTryCatchStatement(TryCatchStatementContext ctx) {
        List<BlockContext> blocks = ctx.block();
        analyzeBlock(blocks.get(0), true, "try");
        scopeOf.put(ctx, stack.push("block", "catch"));
        String name = ctx.Identifier().getText();
        int[] p = pos(ctx);
        VariableSymbol sym = new VariableSymbol(name, PrimitiveType.STRING, p[0], p[1]);
        sym.initialized = true;
        stack.declare(sym);
        analyzeBlock(blocks.get(1), false, "catch-body");
        stack.pop();
        return false;
    }

    private boolean stmtSwitchStatement(SwitchStatementContext ctx) {
        Type swT = typeOfExpression(ctx.expression());
        switchDepth++;
        for (SwitchCaseContext c : ctx.switchCase()) {
            Type caseT = typeOfExpression(c.expression());
            if (!TypeUtils.typesComparable(swT, caseT))
                err("TYPE", "tipo de 'case' (" + caseT + ") incompatible con switch (" + swT + ")", c);
            scopeOf.put(c, stack.push("block", "case"));
            for (StatementContext st : c.statement()) analyzeStmt(st);
            stack.pop();
        }
        DefaultCaseContext def = ctx.defaultCase();
        if (def != null) {
            scopeOf.put(def, stack.push("block", "default"));
            for (StatementContext st : def.statement()) analyzeStmt(st);
            stack.pop();
        }
        switchDepth--;
        return false;
    }

    private boolean stmtBreakStatement(BreakStatementContext ctx) {
        if (loopDepth == 0 && switchDepth == 0) err("FLOW", "'break' fuera de bucle o switch", ctx);
        return true;
    }

    private boolean stmtContinueStatement(ContinueStatementContext ctx) {
        if (loopDepth == 0) err("FLOW", "'continue' fuera de bucle", ctx);
        return true;
    }

    private boolean stmtReturnStatement(ReturnStatementContext ctx) {
        if (functionStack.isEmpty()) {
            err("FLOW", "'return' fuera de función", ctx);
            return true;
        }
        FunctionSymbol fn = functionStack.peek();
        Type expected = fn.returnType != null ? fn.returnType : PrimitiveType.VOID;
        ExpressionContext expr = ctx.expression();
        if (expr == null) {
            if (!expected.equals(PrimitiveType.VOID))
                err("FUNCTION", "'return' sin valor en función con tipo " + expected, ctx);
        } else {
            Type t = typeOfExpression(expr);
            if (expected.equals(PrimitiveType.VOID)) {
                if (!t.isError()) err("FUNCTION", "función void no debe retornar un valor", ctx);
            } else if (!TypeUtils.isAssignable(expected, t)) {
                err("FUNCTION", "tipo de retorno " + t + " incompatible con declarado " + expected, ctx);
            }
        }
        return true;
    }

    private boolean stmtFunctionDeclaration(FunctionDeclarationContext ctx) {
        String name = ctx.Identifier().getText();
        Symbol existing = stack.current().resolveLocal(name);
        FunctionSymbol sym;
        if (existing instanceof FunctionSymbol) {
            sym = (FunctionSymbol) existing;
        } else {
            List<String> pNames = new ArrayList<>();
            List<Type> pTypes = new ArrayList<>();
            if (ctx.parameters() != null) {
                for (ParameterContext pc : ctx.parameters().parameter()) {
                    pNames.add(pc.Identifier().getText());
                    pTypes.add(pc.type() != null ? resolveType(pc.type()) : ErrorType.INSTANCE);
                }
            }
            Type ret = ctx.type() != null ? resolveType(ctx.type()) : PrimitiveType.VOID;
            int[] p = pos(ctx);
            sym = new FunctionSymbol(name, new FunctionType(new ArrayList<>(pTypes), ret), p[0], p[1]);
            sym.paramNames = pNames;
            sym.paramTypes = pTypes;
            sym.returnType = ret;
            sym.isMethod = !classStack.isEmpty();
            if (!classStack.isEmpty()) sym.ownerClass = classStack.peek().classType;
            if (!stack.declare(sym)) err("FUNCTION", "redeclaración de función '" + name + "'", ctx);
        }
        scopeOf.put(ctx, stack.push("function", sym, null, name));
        if (!classStack.isEmpty()) {
            ClassSymbol cls = classStack.peek();
            int[] p = pos(ctx);
            VariableSymbol thisSym = new VariableSymbol("this", cls.classType, p[0], p[1]);
            thisSym.initialized = true;
            stack.declare(thisSym);
        }
        for (int i = 0; i < sym.paramNames.size(); i++) {
            int[] p = pos(ctx);
            stack.declare(new ParameterSymbol(sym.paramNames.get(i), sym.paramTypes.get(i), p[0], p[1]));
        }
        functionStack.push(sym);
        analyzeBlock(ctx.block(), false, "fn-body:" + name);
        functionStack.pop();
        stack.pop();
        return false;
    }

    private boolean stmtClassDeclaration(ClassDeclarationContext ctx) {
        List<org.antlr.v4.runtime.tree.TerminalNode> idents = ctx.Identifier();
        String name = idents.get(0).getText();
        Symbol existing = stack.current().resolveLocal(name);
        ClassSymbol sym;
        if (existing instanceof ClassSymbol) {
            sym = (ClassSymbol) existing;
        } else {
            ClassType parent = null;
            if (idents.size() > 1) {
                ClassSymbol p = classes.get(idents.get(1).getText());
                if (p != null) parent = p.classType;
            }
            ClassType ct = new ClassType(name, parent);
            int[] p = pos(ctx);
            sym = new ClassSymbol(name, ct, p[0], p[1]);
            if (!stack.declare(sym)) {
                err("SCOPE", "redeclaración de clase '" + name + "'", ctx);
                return false;
            }
            classes.put(name, sym);
        }
        scopeOf.put(ctx, stack.push("class", null, sym, name));
        classStack.push(sym);
        for (ClassMemberContext m : ctx.classMember()) {
            if (m.variableDeclaration() != null) analyzeMemberVar(m.variableDeclaration(), sym);
            else if (m.constantDeclaration() != null) analyzeMemberConst(m.constantDeclaration(), sym);
            else if (m.functionDeclaration() != null) {
                FunctionDeclarationContext fdecl = m.functionDeclaration();
                String fname = fdecl.Identifier().getText();
                List<String> pNames = new ArrayList<>();
                List<Type> pTypes = new ArrayList<>();
                if (fdecl.parameters() != null) {
                    for (ParameterContext pc : fdecl.parameters().parameter()) {
                        pNames.add(pc.Identifier().getText());
                        pTypes.add(pc.type() != null ? resolveType(pc.type()) : ErrorType.INSTANCE);
                    }
                }
                Type ret = fdecl.type() != null ? resolveType(fdecl.type()) : PrimitiveType.VOID;
                int[] p = pos(fdecl);
                FunctionSymbol msym = new FunctionSymbol(fname, new FunctionType(new ArrayList<>(pTypes), ret), p[0], p[1]);
                msym.paramNames = pNames;
                msym.paramTypes = pTypes;
                msym.returnType = ret;
                msym.isMethod = true;
                msym.ownerClass = sym.classType;
                if (!stack.declare(msym)) err("FUNCTION", "redeclaración de método '" + fname + "'", fdecl);
                sym.classType.members.put(fname, msym);
                stmtFunctionDeclaration(fdecl);
            }
        }
        classStack.pop();
        stack.pop();
        return false;
    }

    private void analyzeMemberVar(VariableDeclarationContext ctx, ClassSymbol clsSym) {
        String name = ctx.Identifier().getText();
        Type declType = ctx.typeAnnotation() != null ? resolveType(ctx.typeAnnotation().type()) : null;
        Type initType = ctx.initializer() != null ? typeOfExpression(ctx.initializer().expression()) : null;
        Type varType = declType != null ? declType : (initType != null ? initType : ErrorType.INSTANCE);
        if (declType != null && initType != null && !TypeUtils.isAssignable(declType, initType))
            err("TYPE", "tipo incompatible en atributo '" + name + "': " + declType + " vs " + initType, ctx);
        int[] p = pos(ctx);
        VariableSymbol sym = new VariableSymbol(name, varType, p[0], p[1]);
        sym.initialized = initType != null;
        if (!stack.declare(sym)) err("SCOPE", "redeclaración de atributo '" + name + "'", ctx);
        clsSym.classType.members.put(name, sym);
    }

    private void analyzeMemberConst(ConstantDeclarationContext ctx, ClassSymbol clsSym) {
        String name = ctx.Identifier().getText();
        Type declType = ctx.typeAnnotation() != null ? resolveType(ctx.typeAnnotation().type()) : null;
        Type initType = typeOfExpression(ctx.expression());
        if (declType != null && !TypeUtils.isAssignable(declType, initType))
            err("TYPE", "tipo incompatible en const '" + name + "': " + declType + " vs " + initType, ctx);
        int[] p = pos(ctx);
        ConstantSymbol sym = new ConstantSymbol(name, declType != null ? declType : initType, p[0], p[1]);
        if (!stack.declare(sym)) err("SCOPE", "redeclaración de const '" + name + "'", ctx);
        clsSym.classType.members.put(name, sym);
    }

    // -------------------- expresiones (typeOf) --------------------

    private Type typeOfExpression(ExpressionContext ctx) {
        if (ctx == null) return ErrorType.INSTANCE;
        return typeOfAssignment(ctx.assignmentExpr());
    }

    private Type typeOfAssignment(AssignmentExprContext ctx) {
        if (ctx instanceof AssignExprContext) {
            AssignExprContext c = (AssignExprContext) ctx;
            Type lhsT = typeOfLeftHandSide(c.lhs);
            Type rhsT = typeOfAssignment(c.assignmentExpr());
            if (!TypeUtils.isAssignable(lhsT, rhsT))
                err("TYPE", "asignación incompatible: " + lhsT + " = " + rhsT, ctx);
            return lhsT;
        }
        if (ctx instanceof PropertyAssignExprContext) {
            PropertyAssignExprContext c = (PropertyAssignExprContext) ctx;
            Type objT = typeOfLeftHandSide(c.lhs);
            Type rhsT = typeOfAssignment(c.assignmentExpr());
            String prop = c.Identifier().getText();
            if (objT instanceof ClassType) {
                Symbol m = ((ClassType) objT).lookup(prop);
                if (m == null) {
                    err("CLASS", "la clase '" + ((ClassType) objT).name + "' no tiene el miembro '" + prop + "'", ctx);
                    return ErrorType.INSTANCE;
                }
                if (!TypeUtils.isAssignable(m.type, rhsT))
                    err("TYPE", "asignación incompatible: " + ((ClassType) objT).name + "." + prop + " : " + m.type + " = " + rhsT, ctx);
                return m.type;
            }
            if (!objT.isError()) err("TYPE", "acceso a propiedad sobre no-objeto (" + objT + ")", ctx);
            return ErrorType.INSTANCE;
        }
        if (ctx instanceof ExprNoAssignContext) {
            return typeOfConditional(((ExprNoAssignContext) ctx).conditionalExpr());
        }
        return ErrorType.INSTANCE;
    }

    private Type typeOfConditional(ConditionalExprContext raw) {
        // única alternativa de conditionalExpr está etiquetada como #TernaryExpr
        TernaryExprContext ctx = (TernaryExprContext) raw;
        Type orT = typeOfLogicalOr(ctx.logicalOrExpr());
        List<ExpressionContext> exprs = ctx.expression();
        if (exprs.isEmpty()) return orT;
        if (!orT.equals(PrimitiveType.BOOLEAN) && !orT.isError())
            err("TYPE", "la condición del ternario debe ser boolean, es " + orT, ctx);
        Type t1 = typeOfExpression(exprs.get(0));
        Type t2 = typeOfExpression(exprs.get(1));
        if (t1.equals(t2)) return t1;
        if (TypeUtils.isAssignable(t1, t2)) return t1;
        if (TypeUtils.isAssignable(t2, t1)) return t2;
        if (!t1.isError() && !t2.isError())
            err("TYPE", "ramas del ternario incompatibles: " + t1 + " vs " + t2, ctx);
        return ErrorType.INSTANCE;
    }

    private Type typeOfLogicalOr(LogicalOrExprContext ctx) {
        List<LogicalAndExprContext> ands = ctx.logicalAndExpr();
        if (ands.size() == 1) return typeOfLogicalAnd(ands.get(0));
        for (LogicalAndExprContext a : ands) {
            Type t = typeOfLogicalAnd(a);
            if (!t.equals(PrimitiveType.BOOLEAN) && !t.isError())
                err("TYPE", "operando de '||' no es boolean: " + t, a);
        }
        return PrimitiveType.BOOLEAN;
    }

    private Type typeOfLogicalAnd(LogicalAndExprContext ctx) {
        List<EqualityExprContext> eqs = ctx.equalityExpr();
        if (eqs.size() == 1) return typeOfEquality(eqs.get(0));
        for (EqualityExprContext e : eqs) {
            Type t = typeOfEquality(e);
            if (!t.equals(PrimitiveType.BOOLEAN) && !t.isError())
                err("TYPE", "operando de '&&' no es boolean: " + t, e);
        }
        return PrimitiveType.BOOLEAN;
    }

    private Type typeOfEquality(EqualityExprContext ctx) {
        List<RelationalExprContext> rels = ctx.relationalExpr();
        if (rels.size() == 1) return typeOfRelational(rels.get(0));
        List<Type> types = new ArrayList<>();
        for (RelationalExprContext r : rels) types.add(typeOfRelational(r));
        for (int i = 1; i < types.size(); i++) {
            if (!TypeUtils.typesComparable(types.get(i - 1), types.get(i)))
                err("TYPE", "comparación entre tipos incompatibles: " + types.get(i - 1) + " vs " + types.get(i), ctx);
        }
        return PrimitiveType.BOOLEAN;
    }

    private Type typeOfRelational(RelationalExprContext ctx) {
        List<AdditiveExprContext> adds = ctx.additiveExpr();
        if (adds.size() == 1) return typeOfAdditive(adds.get(0));
        List<Type> types = new ArrayList<>();
        for (AdditiveExprContext a : adds) types.add(typeOfAdditive(a));
        for (int i = 1; i < types.size(); i++) {
            Type a = types.get(i - 1), b = types.get(i);
            if (!(TypeUtils.isNumeric(a) && TypeUtils.isNumeric(b)) && !a.isError() && !b.isError())
                err("TYPE", "comparación relacional requiere numéricos: " + a + " vs " + b, ctx);
        }
        return PrimitiveType.BOOLEAN;
    }

    private Type typeOfAdditive(AdditiveExprContext ctx) {
        List<MultiplicativeExprContext> muls = ctx.multiplicativeExpr();
        if (muls.size() == 1) return typeOfMultiplicative(muls.get(0));
        Type curType = typeOfMultiplicative(muls.get(0));
        for (int i = 1; i < muls.size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            Type nextType = typeOfMultiplicative(muls.get(i));
            if (op.equals("+")) {
                if (curType.equals(PrimitiveType.STRING) && nextType.equals(PrimitiveType.STRING)) {
                    curType = PrimitiveType.STRING;
                } else if (TypeUtils.isNumeric(curType) && TypeUtils.isNumeric(nextType)) {
                    curType = TypeUtils.unifyNumeric(curType, nextType);
                } else if (curType.isError() || nextType.isError()) {
                    curType = ErrorType.INSTANCE;
                } else {
                    err("TYPE", "'+' no aplica a " + curType + " y " + nextType, ctx);
                    curType = ErrorType.INSTANCE;
                }
            } else {
                if (TypeUtils.isNumeric(curType) && TypeUtils.isNumeric(nextType)) {
                    curType = TypeUtils.unifyNumeric(curType, nextType);
                } else if (curType.isError() || nextType.isError()) {
                    curType = ErrorType.INSTANCE;
                } else {
                    err("TYPE", "'" + op + "' requiere numéricos, se obtuvo " + curType + " y " + nextType, ctx);
                    curType = ErrorType.INSTANCE;
                }
            }
        }
        return curType;
    }

    private Type typeOfMultiplicative(MultiplicativeExprContext ctx) {
        List<UnaryExprContext> us = ctx.unaryExpr();
        if (us.size() == 1) return typeOfUnary(us.get(0));
        Type curType = typeOfUnary(us.get(0));
        for (int i = 1; i < us.size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            Type nxt = typeOfUnary(us.get(i));
            if (TypeUtils.isNumeric(curType) && TypeUtils.isNumeric(nxt)) {
                curType = TypeUtils.unifyNumeric(curType, nxt);
            } else if (curType.isError() || nxt.isError()) {
                curType = ErrorType.INSTANCE;
            } else {
                err("TYPE", "'" + op + "' requiere numéricos, se obtuvo " + curType + " y " + nxt, ctx);
                curType = ErrorType.INSTANCE;
            }
        }
        return curType;
    }

    private Type typeOfUnary(UnaryExprContext ctx) {
        if (ctx.getChildCount() == 2) {
            String op = ctx.getChild(0).getText();
            Type inner = typeOfUnary(ctx.unaryExpr());
            if (op.equals("-")) {
                if (TypeUtils.isNumeric(inner) || inner.isError()) return inner;
                err("TYPE", "'-' unario requiere numérico, es " + inner, ctx);
                return ErrorType.INSTANCE;
            }
            if (op.equals("!")) {
                if (inner.equals(PrimitiveType.BOOLEAN) || inner.isError()) return PrimitiveType.BOOLEAN;
                err("TYPE", "'!' requiere boolean, es " + inner, ctx);
                return ErrorType.INSTANCE;
            }
            return ErrorType.INSTANCE;
        }
        return typeOfPrimary(ctx.primaryExpr());
    }

    private Type typeOfPrimary(PrimaryExprContext ctx) {
        if (ctx.literalExpr() != null) return typeOfLiteral(ctx.literalExpr());
        if (ctx.leftHandSide() != null) return typeOfLeftHandSide(ctx.leftHandSide());
        return typeOfExpression(ctx.expression());
    }

    private Type typeOfLiteral(LiteralExprContext ctx) {
        if (ctx.arrayLiteral() != null) return typeOfArrayLiteral(ctx.arrayLiteral());
        String text = ctx.getText();
        if (text.equals("null")) return PrimitiveType.NULL;
        if (text.equals("true") || text.equals("false")) return PrimitiveType.BOOLEAN;
        if (ctx.Literal() != null) {
            String tok = ctx.Literal().getText();
            if (tok.startsWith("\"")) return PrimitiveType.STRING;
            return PrimitiveType.INTEGER;
        }
        return ErrorType.INSTANCE;
    }

    private Type typeOfArrayLiteral(ArrayLiteralContext ctx) {
        List<ExpressionContext> exprs = ctx.expression();
        if (exprs.isEmpty()) return new ArrayType(ErrorType.INSTANCE);
        List<Type> types = new ArrayList<>();
        for (ExpressionContext e : exprs) types.add(typeOfExpression(e));
        Type base = types.get(0);
        for (int i = 1; i < types.size(); i++) {
            Type t = types.get(i);
            if (t.equals(base)) continue;
            if (TypeUtils.isAssignable(base, t)) continue;
            if (TypeUtils.isAssignable(t, base)) { base = t; continue; }
            err("LIST", "lista heterogénea: " + base + " vs " + t, ctx);
            return new ArrayType(ErrorType.INSTANCE);
        }
        return new ArrayType(base);
    }

    private Type typeOfLeftHandSide(LeftHandSideContext ctx) {
        Type atomT = typeOfPrimaryAtom(ctx.primaryAtom());
        nodeTypes.put(ctx.primaryAtom(), atomT);
        for (SuffixOpContext suf : ctx.suffixOp()) {
            atomT = applySuffix(atomT, suf);
            nodeTypes.put(suf, atomT);
        }
        return atomT;
    }

    private Type typeOfPrimaryAtom(PrimaryAtomContext ctx) {
        if (ctx instanceof IdentifierExprContext) {
            String name = ((IdentifierExprContext) ctx).Identifier().getText();
            Symbol sym = stack.resolve(name);
            if (sym == null) {
                err("SCOPE", "identificador no declarado: '" + name + "'", ctx);
                return ErrorType.INSTANCE;
            }
            if (!functionStack.isEmpty() &&
                    (sym instanceof VariableSymbol || sym instanceof ConstantSymbol || sym instanceof ParameterSymbol)) {
                FunctionSymbol fn = functionStack.peek();
                if (!fn.captured.contains(name) && !fn.paramNames.contains(name)) {
                    if (stack.current().resolveLocal(name) == null) fn.captured.add(name);
                }
            }
            return sym.type;
        }
        if (ctx instanceof NewExprContext) {
            NewExprContext c = (NewExprContext) ctx;
            String cname = c.Identifier().getText();
            ClassSymbol cls = classes.get(cname);
            if (cls == null) {
                err("CLASS", "clase '" + cname + "' no declarada", ctx);
                return ErrorType.INSTANCE;
            }
            ArgumentsContext argsCtx = c.arguments();
            Symbol ctor = cls.classType != null ? cls.classType.lookup("constructor") : null;
            if (ctor instanceof FunctionSymbol) {
                FunctionSymbol ctorFn = (FunctionSymbol) ctor;
                List<Type> argTypes = new ArrayList<>();
                if (argsCtx != null) for (ExpressionContext e : argsCtx.expression()) argTypes.add(typeOfExpression(e));
                if (argTypes.size() != ctorFn.paramTypes.size()) {
                    err("FUNCTION", "constructor de '" + cname + "' espera " + ctorFn.paramTypes.size() +
                            " args, recibió " + argTypes.size(), ctx);
                } else {
                    for (int i = 0; i < argTypes.size(); i++) {
                        if (!TypeUtils.isAssignable(ctorFn.paramTypes.get(i), argTypes.get(i)))
                            err("FUNCTION", "arg " + (i + 1) + " del constructor '" + cname + "': " +
                                    ctorFn.paramTypes.get(i) + " vs " + argTypes.get(i), ctx);
                    }
                }
            }
            return cls.classType;
        }
        if (ctx instanceof ThisExprContext) {
            if (classStack.isEmpty()) {
                err("CLASS", "'this' fuera de método de clase", ctx);
                return ErrorType.INSTANCE;
            }
            return classStack.peek().classType;
        }
        return ErrorType.INSTANCE;
    }

    private Type applySuffix(Type baseT, SuffixOpContext suf) {
        if (suf instanceof CallExprContext) {
            ArgumentsContext argsCtx = ((CallExprContext) suf).arguments();
            List<Type> argTypes = new ArrayList<>();
            if (argsCtx != null) for (ExpressionContext e : argsCtx.expression()) argTypes.add(typeOfExpression(e));
            if (baseT instanceof FunctionType) {
                FunctionType ft = (FunctionType) baseT;
                if (argTypes.size() != ft.params.size()) {
                    err("FUNCTION", "aridad incorrecta: esperados " + ft.params.size() + ", recibidos " + argTypes.size(), suf);
                } else {
                    for (int i = 0; i < argTypes.size(); i++) {
                        if (!TypeUtils.isAssignable(ft.params.get(i), argTypes.get(i)))
                            err("FUNCTION", "arg " + (i + 1) + ": se esperaba " + ft.params.get(i) +
                                    ", se recibió " + argTypes.get(i), suf);
                    }
                }
                return ft.ret;
            }
            if (baseT.isError()) return ErrorType.INSTANCE;
            err("FUNCTION", "llamada sobre no-función (" + baseT + ")", suf);
            return ErrorType.INSTANCE;
        }
        if (suf instanceof IndexExprContext) {
            Type idxT = typeOfExpression(((IndexExprContext) suf).expression());
            if (!idxT.equals(PrimitiveType.INTEGER) && !idxT.isError())
                err("LIST", "índice de array debe ser integer, es " + idxT, suf);
            if (baseT instanceof ArrayType) return ((ArrayType) baseT).elem;
            if (baseT.isError()) return ErrorType.INSTANCE;
            err("LIST", "indexación sobre no-array (" + baseT + ")", suf);
            return ErrorType.INSTANCE;
        }
        if (suf instanceof PropertyAccessExprContext) {
            String name = ((PropertyAccessExprContext) suf).Identifier().getText();
            if (baseT instanceof ClassType) {
                Symbol m = ((ClassType) baseT).lookup(name);
                if (m == null) {
                    err("CLASS", "la clase '" + ((ClassType) baseT).name + "' no tiene el miembro '" + name + "'", suf);
                    return ErrorType.INSTANCE;
                }
                return m.type;
            }
            if (baseT.isError()) return ErrorType.INSTANCE;
            err("CLASS", "acceso a propiedad sobre no-objeto (" + baseT + ")", suf);
            return ErrorType.INSTANCE;
        }
        return ErrorType.INSTANCE;
    }
}
