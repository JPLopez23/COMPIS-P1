package semantic;

import java.util.List;
import java.util.stream.Collectors;

public final class FunctionType extends Type {
    public final List<Type> params;
    public final Type ret;

    public FunctionType(List<Type> params, Type ret) {
        this.params = params;
        this.ret = ret;
    }

    @Override public String toString() {
        String p = params.stream().map(Type::toString).collect(Collectors.joining(", "));
        return "(" + p + ") -> " + ret;
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof FunctionType)) return false;
        FunctionType f = (FunctionType) o;
        return params.equals(f.params) && ret.equals(f.ret);
    }

    @Override public int hashCode() { return 31 * params.hashCode() + ret.hashCode(); }
}
