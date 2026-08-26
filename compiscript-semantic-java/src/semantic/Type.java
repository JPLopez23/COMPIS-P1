package semantic;

/** Sistema de tipos de Compiscript. */
public abstract class Type {

    public boolean isError() { return false; }

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object other);

    @Override
    public abstract int hashCode();
}
