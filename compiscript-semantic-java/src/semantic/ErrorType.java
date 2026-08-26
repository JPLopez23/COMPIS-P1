package semantic;

/** Tipo comodín: compatible con cualquier otro para evitar cascadas de errores. */
public final class ErrorType extends Type {
    public static final ErrorType INSTANCE = new ErrorType();

    private ErrorType() {}

    @Override public boolean isError() { return true; }
    @Override public String toString() { return "<error>"; }
    @Override public boolean equals(Object o) { return o instanceof ErrorType; }
    @Override public int hashCode() { return "error".hashCode(); }
}
