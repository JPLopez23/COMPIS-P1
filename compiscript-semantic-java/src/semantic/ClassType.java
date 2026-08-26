package semantic;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ClassType extends Type {
    public final String name;
    public ClassType parent;
    public final Map<String, Symbol> members = new LinkedHashMap<>();

    public ClassType(String name) { this(name, null); }

    public ClassType(String name, ClassType parent) {
        this.name = name;
        this.parent = parent;
    }

    /** Busca un miembro subiendo por la cadena de herencia. */
    public Symbol lookup(String member) {
        Symbol s = members.get(member);
        if (s != null) return s;
        if (parent != null) return parent.lookup(member);
        return null;
    }

    public boolean isSubclassOf(ClassType other) {
        ClassType cur = this.parent;
        while (cur != null) {
            if (cur.equals(other)) return true;
            cur = cur.parent;
        }
        return false;
    }

    @Override public String toString() { return name; }

    @Override public boolean equals(Object o) {
        return o instanceof ClassType && ((ClassType) o).name.equals(name);
    }

    @Override public int hashCode() { return ("class:" + name).hashCode(); }
}
