package semantic;

public final class ArrayType extends Type {
    public final Type elem;

    public ArrayType(Type elem) { this.elem = elem; }

    @Override public String toString() { return elem.toString() + "[]"; }

    @Override public boolean equals(Object o) {
        return o instanceof ArrayType && ((ArrayType) o).elem.equals(elem);
    }

    @Override public int hashCode() { return 31 * elem.hashCode() + 7; }
}
