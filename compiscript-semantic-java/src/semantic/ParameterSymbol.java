package semantic;

public class ParameterSymbol extends Symbol {
    public ParameterSymbol(String name, Type type, int line, int column) {
        super(name, type, line, column);
        this.kind = "parameter";
        this.initialized = true;
    }
}
