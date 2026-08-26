package semantic;

/** Símbolo base que vive en la tabla de símbolos. */
public class Symbol {
    public final String name;
    public Type type;
    public final int line;
    public final int column;
    public String kind = "symbol"; // variable | constant | parameter | function | class
    public boolean mutable = true;
    public boolean initialized = false;

    public Symbol(String name, Type type, int line, int column) {
        this.name = name;
        this.type = type;
        this.line = line;
        this.column = column;
    }

    public String describe() { return kind + " " + name + ": " + type; }
}
