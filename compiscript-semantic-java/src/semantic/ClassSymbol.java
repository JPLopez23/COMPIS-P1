package semantic;

public class ClassSymbol extends Symbol {
    public ClassType classType;

    // --- Disposición del objeto (la rellena codegen.TacGenerator) ---
    public int instanceSize = 0;
    public final java.util.Map<String, Integer> fieldOffsets = new java.util.LinkedHashMap<>();

    public ClassSymbol(String name, ClassType classType, int line, int column) {
        super(name, classType, line, column);
        this.kind = "class";
        this.classType = classType;
    }
}
