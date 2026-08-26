package semantic;

public class ConstantSymbol extends Symbol {
    public ConstantSymbol(String name, Type type, int line, int column) {
        super(name, type, line, column);
        this.kind = "constant";
        this.mutable = false;
        this.initialized = true;
    }
}
