package codegen;

import generated.CompiscriptParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import semantic.*;

import java.util.*;

/**
 * Base del generador de TAC: estado compartido, pre-pasada (etiquetas, niveles y disposición de clases),
 * utilidades de emisión y declaración de símbolos en el marco actual.
 */
public abstract class TacBase {

    protected final SemanticAnalyzer sa;

    public final List<Frame> frames = new ArrayList<>();
    public final List<ClassSymbol> classList = new ArrayList<>();
    public int globalSize = 0;

    protected Frame cur;
    protected Scope curScope;
    protected int labelCount = 0;
    protected int hiddenCount = 0;
    protected int globalNext = 0;
    protected final Set<String> globalNames = new HashSet<>();
    protected final Set<String> usedLabels = new HashSet<>(Arrays.asList("main"));
    protected final Map<String, ClassSymbol> classByName = new LinkedHashMap<>();
    protected final Map<String, String> initLabel = new HashMap<>(); // clase -> etiqueta de su inicializador
    protected Deque<String> breakTargets = new ArrayDeque<>();
    protected Deque<String> continueTargets = new ArrayDeque<>();

    protected TacBase(SemanticAnalyzer sa) { this.sa = sa; }

    /** Genera el operando con el valor de la expresión (null si es una llamada void). Lo implementa ExprGenerator. */
    protected abstract String genExpr(ParseTree n);

    // ======================================================================
    // Pre-pasada: etiquetas de funciones, niveles y disposición de clases
    // ======================================================================

    protected void prepass() {
        for (Scope sc : sa.stack.allScopes) {
            if (sc.ownerClass != null) {
                ClassSymbol cs = sc.ownerClass;
                classByName.put(cs.name, cs);
                classList.add(cs);
                cs.storage = "CLASS";
                cs.label = cs.name;
            }
        }
        for (Scope sc : sa.stack.allScopes) {
            if (!sc.kind.equals("function") || sc.ownerFn == null) continue;
            FunctionSymbol fs = sc.ownerFn;
            List<String> parts = new ArrayList<>();
            int level = 0;
            for (Scope p = sc; p != null; p = p.parent) {
                if (p.kind.equals("function") && p.ownerFn != null) { parts.add(0, p.ownerFn.name); level++; }
                else if (p.kind.equals("class") && p.ownerClass != null) parts.add(0, p.ownerClass.name);
            }
            fs.storage = "FUNCTION";
            fs.nestLevel = level;
            fs.label = uniqLabel(String.join("_", parts));
        }
        for (ClassSymbol cs : classList) layout(cs, new HashSet<>());
        for (Map.Entry<ParserRuleContext, Scope> e : sa.scopeOf.entrySet()) {
            if (!(e.getKey() instanceof ClassDeclarationContext)) continue;
            ClassDeclarationContext cd = (ClassDeclarationContext) e.getKey();
            boolean any = false;
            for (ClassMemberContext m : cd.classMember()) {
                if ((m.variableDeclaration() != null && m.variableDeclaration().initializer() != null)
                        || m.constantDeclaration() != null) any = true;
            }
            if (any) initLabel.put(cd.Identifier(0).getText(), uniqLabel(cd.Identifier(0).getText() + "_$init"));
        }
    }

    protected void layout(ClassSymbol cs, Set<ClassSymbol> visiting) {
        if (cs.instanceSize > 0 || !cs.fieldOffsets.isEmpty() || !visiting.add(cs)) return;
        int off = 0;
        ClassType parent = cs.classType.parent;
        if (parent != null) {
            ClassSymbol ps = classByName.get(parent.name);
            if (ps != null) {
                layout(ps, visiting);
                cs.fieldOffsets.putAll(ps.fieldOffsets);
                off = ps.instanceSize;
            }
        }
        for (Symbol m : cs.classType.members.values()) {
            if (m instanceof FunctionSymbol) continue;
            m.storage = "FIELD";
            m.size = sizeOf(m.type);
            m.tacName = m.name;
            Integer existing = cs.fieldOffsets.get(m.name);
            if (existing != null) { m.offset = existing; continue; }
            m.offset = off;
            cs.fieldOffsets.put(m.name, off);
            off += Frame.align4(m.size);
        }
        cs.instanceSize = off;
        cs.size = off;
    }

    protected String uniqLabel(String base) {
        if (base.matches("L\\d+")) base += "_f";
        String c = base;
        int k = 2;
        while (!usedLabels.add(c)) c = base + "_" + k++;
        return c;
    }

    protected static int sizeOf(Type t) {
        return PrimitiveType.BOOLEAN.equals(t) ? 1 : 4;
    }

    // ======================================================================
    // Utilidades de emisión
    // ======================================================================

    protected void emit(String s) { cur.code.add("    " + s); }
    protected void emitLabel(String l) { cur.code.add(l + ":"); }
    protected String newLabel() { return "L" + (++labelCount); }
    protected String newTemp() { return cur.newTemp(); }
    protected void free(String op) { cur.free(op); }

    /** Retarget: si src es un temporal definido por la última instrucción, la redirige a dest. */
    protected String assignTo(String dest, String src) {
        if (dest.equals(src)) return dest;
        if (Frame.isTemp(src) && !cur.code.isEmpty()) {
            int last = cur.code.size() - 1;
            String l = cur.code.get(last);
            String prefix = "    " + src + " = ";
            if (l.startsWith(prefix)) {
                cur.code.set(last, "    " + dest + " = " + l.substring(prefix.length()));
                free(src);
                return dest;
            }
        }
        emit(dest + " = " + src);
        free(src);
        return dest;
    }

    protected void finishFrame(Frame f) {
        List<String> opt = Peephole.optimize(f.code);
        f.code.clear();
        f.code.addAll(opt);
    }

    // ======================================================================
    // Declaración de símbolos
    // ======================================================================

    protected String uniq(Set<String> used, String n) {
        String base = n;
        if (base.matches("t\\d+|L\\d+") || base.equals("G") || base.equals("env") || base.equals("this")) base += "_v";
        String c = base;
        int k = 2;
        while (!used.add(c)) c = base + "_" + k++;
        return c;
    }

    protected void declare(Symbol s, boolean isParam) {
        if (s == null) return;
        int sz = sizeOf(s.type);
        s.size = sz;
        if (!isParam && curScope.kind.equals("global")) {
            s.storage = "GLOBAL";
            s.nestLevel = 0;
            s.offset = globalNext;
            globalNext += Frame.align4(sz);
            s.tacName = uniq(globalNames, s.name);
        } else {
            s.storage = isParam ? "PARAM" : "LOCAL";
            s.nestLevel = cur.level;
            s.tacName = uniq(cur.usedNames, s.name);
            s.offset = cur.alloc(s.tacName, isParam ? "param" : "local", sz);
        }
    }

    protected String hiddenLocal(String base) {
        String name = "$" + base + (++hiddenCount);
        cur.alloc(name, "oculta", 4);
        return name;
    }
}
