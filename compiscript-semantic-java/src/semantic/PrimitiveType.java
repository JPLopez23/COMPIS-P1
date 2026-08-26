package semantic;

public final class PrimitiveType extends Type {
    public static final PrimitiveType INTEGER = new PrimitiveType("integer");
    public static final PrimitiveType FLOAT   = new PrimitiveType("float");
    public static final PrimitiveType STRING  = new PrimitiveType("string");
    public static final PrimitiveType BOOLEAN = new PrimitiveType("boolean");
    public static final PrimitiveType NULL    = new PrimitiveType("null");
    public static final PrimitiveType VOID    = new PrimitiveType("void");

    public final String name;

    private PrimitiveType(String name) { this.name = name; }

    @Override public String toString() { return name; }

    @Override public boolean equals(Object o) {
        return o instanceof PrimitiveType && ((PrimitiveType) o).name.equals(name);
    }

    @Override public int hashCode() { return ("prim:" + name).hashCode(); }
}
