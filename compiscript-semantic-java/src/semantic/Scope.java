package semantic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Un ámbito (scope): global, función, clase o bloque. */
public class Scope {
    private static int nextId = 0;

    public final String kind; // global | function | class | block
    public final Scope parent;
    public final Map<String, Symbol> symbols = new LinkedHashMap<>();
    public FunctionSymbol ownerFn;
    public ClassSymbol ownerClass;
    public final String label;
    public final int id;
    public final List<Scope> children = new ArrayList<>();

    public Scope(String kind, Scope parent, FunctionSymbol ownerFn, ClassSymbol ownerClass, String label) {
        this.kind = kind;
        this.parent = parent;
        this.ownerFn = ownerFn;
        this.ownerClass = ownerClass;
        this.label = label;
        this.id = nextId++;
        if (parent != null) parent.children.add(this);
    }

    static void resetIds() { nextId = 0; }

    public boolean declare(Symbol sym) {
        if (symbols.containsKey(sym.name)) return false;
        symbols.put(sym.name, sym);
        return true;
    }

    public Symbol resolveLocal(String name) { return symbols.get(name); }

    public Symbol resolve(String name) {
        Symbol s = symbols.get(name);
        if (s != null) return s;
        if (parent != null) return parent.resolve(name);
        return null;
    }

    public FunctionSymbol enclosingFunction() {
        Scope cur = this;
        while (cur != null) {
            if (cur.ownerFn != null) return cur.ownerFn;
            cur = cur.parent;
        }
        return null;
    }

    public ClassSymbol enclosingClass() {
        Scope cur = this;
        while (cur != null) {
            if (cur.ownerClass != null) return cur.ownerClass;
            cur = cur.parent;
        }
        return null;
    }

    public String fullLabel() {
        String base = kind + "#" + id;
        if (label != null && !label.isEmpty()) base += " [" + label + "]";
        return base;
    }
}
