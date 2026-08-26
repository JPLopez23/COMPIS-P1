package semantic;

/** Errores semánticos (y sintácticos, categoría SYNTAX) acumulados durante el análisis. */
public class SemanticError {
    public final String category; // TYPE | SCOPE | FUNCTION | FLOW | CLASS | LIST | GENERAL | SYNTAX
    public final String message;
    public final int line;
    public final int column;

    public SemanticError(String category, String message, int line, int column) {
        this.category = category;
        this.message = message;
        this.line = line;
        this.column = column;
    }

    public String format() {
        return "[" + line + ":" + column + "] " + category + ": " + message;
    }

    @Override public String toString() { return format(); }
}
