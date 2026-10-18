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

    // --- Información para generación de código (la rellena codegen.TacGenerator) ---
    /** GLOBAL | LOCAL | PARAM | FIELD | FUNCTION | CLASS | "" (aún sin asignar). */
    public String storage = "";
    /** Desplazamiento en bytes: respecto a gp (GLOBAL), fp (LOCAL/PARAM) o al inicio del objeto (FIELD). -1 si no aplica. */
    public int offset = -1;
    /** Tamaño en bytes del valor almacenado (0 si no aplica). */
    public int size = 0;
    /** Etiqueta en el código intermedio (funciones, métodos, clases). */
    public String label = "";
    /** Nivel de anidamiento del marco que contiene el símbolo (0 = global). */
    public int nestLevel = 0;
    /** Nombre usado en el TAC (único dentro de su marco). */
    public String tacName = "";

    public Symbol(String name, Type type, int line, int column) {
        this.name = name;
        this.type = type;
        this.line = line;
        this.column = column;
    }

    public String describe() { return kind + " " + name + ": " + type; }
}
