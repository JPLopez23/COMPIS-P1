package codegen;

import generated.CompiscriptParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import semantic.*;

import java.util.*;

/** Traducción de accesos: variables, lvalues, sufijos (índice, propiedad, llamada), new y this. */
public abstract class AccessGenerator extends TacBase {

    protected AccessGenerator(SemanticAnalyzer sa) { super(sa); }

    // ======================================================================
    // Acceso a variables
    // ======================================================================

    protected static final class LValue {
        String kind;   // var | index | field
        String base;   // operando del objeto / arreglo
        String index;  // índice (index)
        String name;   // nombre de campo (field) o de variable (var)
    }

    protected String envPrefix(int d) { return d <= 0 ? "" : "env(" + d + ")."; }

    protected String varOperand(Symbol sym) {
        switch (sym.storage) {
            case "GLOBAL": return "G." + sym.tacName;
            case "LOCAL":
            case "PARAM": return envPrefix(cur.level - sym.nestLevel) + sym.tacName;
            default: return sym.name;
        }
    }

    protected String thisOperand() {
        int d = 0;
        Frame f = cur;
        while (f != null && !f.hasThis) { f = f.parent; d++; }
        return d == 0 ? "this" : "env(" + d + ").this";
    }

    protected LValue lvalueOfSymbol(Symbol sym) {
        LValue lv = new LValue();
        if (sym != null && sym.storage.equals("FIELD")) {
            lv.kind = "field";
            lv.base = thisOperand();
            lv.name = sym.name;
        } else {
            lv.kind = "var";
            lv.name = sym == null ? "?" : varOperand(sym);
        }
        return lv;
    }

    /** Almacena v en el lvalue. Devuelve el operando que contiene el valor. */
    protected String store(LValue lv, String v) {
        switch (lv.kind) {
            case "var":
                return assignTo(lv.name, v);
            case "index":
                emit(lv.base + "[" + lv.index + "] = " + v);
                free(lv.index);
                free(lv.base);
                return v;
            default:
                emit(lv.base + "." + lv.name + " = " + v);
                free(lv.base);
                return v;
        }
    }

    // ======================================================================
    // leftHandSide: identificadores, new, this, llamadas, índices, propiedades
    // ======================================================================

    protected LValue lvalueOfLhs(LeftHandSideContext lhs) {
        List<SuffixOpContext> sufs = lhs.suffixOp();
        if (sufs.isEmpty()) {
            PrimaryAtomContext a = lhs.primaryAtom();
            if (a instanceof IdentifierExprContext)
                return lvalueOfSymbol(curScope.resolve(((IdentifierExprContext) a).Identifier().getText()));
            LValue lv = new LValue();
            lv.kind = "var";
            lv.name = genLhs(lhs, 0);
            return lv;
        }
        SuffixOpContext last = sufs.get(sufs.size() - 1);
        LValue lv = new LValue();
        lv.base = genLhs(lhs, sufs.size() - 1);
        if (last instanceof IndexExprContext) {
            lv.kind = "index";
            lv.index = genExpr(((IndexExprContext) last).expression());
        } else if (last instanceof PropertyAccessExprContext) {
            lv.kind = "field";
            lv.name = ((PropertyAccessExprContext) last).Identifier().getText();
        } else {
            throw new IllegalStateException("Destino de asignación inválido");
        }
        return lv;
    }

    /** Evalúa el átomo y los primeros n sufijos; devuelve un operando (null si la llamada es void). */
    protected String genLhs(LeftHandSideContext ctx, int n) {
        PrimaryAtomContext atom = ctx.primaryAtom();
        List<SuffixOpContext> sufs = ctx.suffixOp();
        String curOp = null;
        int i = 0;
        ParseTree lastNode = atom;

        if (atom instanceof IdentifierExprContext) {
            String name = ((IdentifierExprContext) atom).Identifier().getText();
            Symbol sym = curScope.resolve(name);
            if (sym instanceof FunctionSymbol) {
                FunctionSymbol fs = (FunctionSymbol) sym;
                if (n > 0 && sufs.get(0) instanceof CallExprContext) {
                    List<String> args = new ArrayList<>();
                    if (fs.isMethod) args.add(thisOperand());
                    args.addAll(genArgs(((CallExprContext) sufs.get(0)).arguments()));
                    curOp = emitCall(fs.label, false, args, !PrimitiveType.VOID.equals(fs.returnType),
                            "enlace=env(" + linkDepth(fs) + ")");
                    lastNode = sufs.get(0);
                    i = 1;
                } else {
                    String t = newTemp();
                    emit(t + " = closure " + fs.label + ", env(" + linkDepth(fs) + ")");
                    curOp = t;
                }
            } else if (sym != null && sym.storage.equals("FIELD")) {
                String t = newTemp();
                emit(t + " = " + thisOperand() + "." + sym.name);
                curOp = t;
            } else if (sym != null) {
                curOp = varOperand(sym);
            } else {
                curOp = name;
            }
        } else if (atom instanceof NewExprContext) {
            curOp = genNew((NewExprContext) atom);
        } else {
            curOp = thisOperand();
        }

        while (i < n) {
            SuffixOpContext suf = sufs.get(i);
            Type recvType = lastNode == null ? null : sa.nodeTypes.get(lastNode);
            if (suf instanceof IndexExprContext) {
                String idx = genExpr(((IndexExprContext) suf).expression());
                free(curOp);
                free(idx);
                String t = newTemp();
                emit(t + " = " + curOp + "[" + idx + "]");
                curOp = t;
                lastNode = suf;
                i++;
            } else if (suf instanceof PropertyAccessExprContext) {
                String name = ((PropertyAccessExprContext) suf).Identifier().getText();
                Symbol m = recvType instanceof ClassType ? ((ClassType) recvType).lookup(name) : null;
                if (m instanceof FunctionSymbol && i + 1 < n && sufs.get(i + 1) instanceof CallExprContext) {
                    FunctionSymbol fs = (FunctionSymbol) m;
                    List<String> args = new ArrayList<>();
                    args.add(curOp);
                    args.addAll(genArgs(((CallExprContext) sufs.get(i + 1)).arguments()));
                    curOp = emitCall(fs.label, false, args, !PrimitiveType.VOID.equals(fs.returnType),
                            "método de " + (fs.ownerClass != null ? fs.ownerClass.name : "?"));
                    lastNode = sufs.get(i + 1);
                    i += 2;
                } else {
                    free(curOp);
                    String t = newTemp();
                    emit(t + " = " + curOp + "." + name);
                    curOp = t;
                    lastNode = suf;
                    i++;
                }
            } else { // llamada sobre un valor (función de primera clase / closure)
                Type ft = sa.nodeTypes.get(lastNode);
                boolean ret = !(ft instanceof FunctionType) || !PrimitiveType.VOID.equals(((FunctionType) ft).ret);
                List<String> args = genArgs(((CallExprContext) suf).arguments());
                args.add(0, curOp);
                curOp = emitCall(null, true, args, ret, "llamada indirecta");
                lastNode = suf;
                i++;
            }
        }
        return curOp;
    }

    protected List<String> genArgs(ArgumentsContext a) {
        List<String> args = new ArrayList<>();
        if (a != null) for (ExpressionContext e : a.expression()) args.add(genExpr(e));
        return args;
    }

    /** k = cuántos enlaces de acceso hay que seguir desde el marco actual hasta el marco que define a fs. */
    protected int linkDepth(FunctionSymbol fs) {
        return Math.max(0, cur.level - (fs.nestLevel - 1));
    }

    /**
     * Emite param/call. Para llamadas indirectas args[0] es el valor de la closure.
     * Devuelve el temporal con el resultado o null si la función es void.
     */
    protected String emitCall(String target, boolean indirect, List<String> args, boolean returnsValue, String note) {
        List<String> real = indirect ? args.subList(1, args.size()) : args;
        for (String a : real) emit("param " + a);
        String callee = indirect ? args.get(0) : target;
        for (String a : args) free(a);
        String instr = (indirect ? "callind " : "call ") + callee + ", " + real.size();
        String res = null;
        if (returnsValue) {
            res = newTemp();
            emit(res + " = " + instr + "   ; " + note);
        } else {
            emit(instr + "   ; " + note);
        }
        return res;
    }

    protected String genNew(NewExprContext ne) {
        String cname = ne.Identifier().getText();
        ClassSymbol cs = classByName.get(cname);
        String t = newTemp();
        emit(t + " = new " + cname + ", " + (cs != null ? cs.instanceSize : 0));
        // inicializadores de atributos: de la clase raíz hacia la clase instanciada
        Deque<String> chain = new ArrayDeque<>();
        for (ClassType ct = cs != null ? cs.classType : null; ct != null; ct = ct.parent) chain.push(ct.name);
        for (String c : chain) {
            String il = initLabel.get(c);
            if (il != null) {
                emit("param " + t);
                emit("call " + il + ", 1   ; inicializa atributos de " + c);
            }
        }
        Symbol ctor = cs != null ? cs.classType.lookup("constructor") : null;
        List<String> args = genArgs(ne.arguments());
        if (ctor instanceof FunctionSymbol) {
            emit("param " + t);
            for (String a : args) emit("param " + a);
            emit("call " + ((FunctionSymbol) ctor).label + ", " + (args.size() + 1) + "   ; constructor de " + cname);
        }
        for (String a : args) free(a);
        return t;
    }
}
