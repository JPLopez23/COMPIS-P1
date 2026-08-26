package semantic;

public class VariableSymbol extends Symbol {
    public VariableSymbol(String name, Type type, int line, int column) {
        super(name, type, line, column);
        this.kind = "variable";
    }
}
