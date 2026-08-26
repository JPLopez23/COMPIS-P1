package semantic;

public class ClassSymbol extends Symbol {
    public ClassType classType;

    public ClassSymbol(String name, ClassType classType, int line, int column) {
        super(name, classType, line, column);
        this.kind = "class";
        this.classType = classType;
    }
}
