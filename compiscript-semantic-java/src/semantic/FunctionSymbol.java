package semantic;

import java.util.ArrayList;
import java.util.List;

public class FunctionSymbol extends Symbol {
    public List<String> paramNames = new ArrayList<>();
    public List<Type> paramTypes = new ArrayList<>();
    public Type returnType = PrimitiveType.VOID;
    public boolean isMethod = false;
    public ClassType ownerClass;
    public final List<String> captured = new ArrayList<>();

    public FunctionSymbol(String name, Type type, int line, int column) {
        super(name, type, line, column);
        this.kind = "function";
    }

    public FunctionType signature() {
        return new FunctionType(new ArrayList<>(paramTypes), returnType);
    }
}
