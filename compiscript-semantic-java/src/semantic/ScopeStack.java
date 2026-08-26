package semantic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ScopeStack {
    public final Scope globalScope;
    private final Deque<Scope> stack = new ArrayDeque<>();
    public final List<Scope> allScopes = new ArrayList<>();

    public ScopeStack() {
        Scope.resetIds();
        globalScope = new Scope("global", null, null, null, "global");
        stack.push(globalScope);
        allScopes.add(globalScope);
    }

    public Scope current() { return stack.peek(); }

    public Scope push(String kind, FunctionSymbol ownerFn, ClassSymbol ownerClass, String label) {
        Scope s = new Scope(kind, current(), ownerFn, ownerClass, label);
        stack.push(s);
        allScopes.add(s);
        return s;
    }

    public Scope push(String kind, String label) { return push(kind, null, null, label); }

    public Scope pop() { return stack.pop(); }

    public boolean declare(Symbol sym) { return current().declare(sym); }

    public Symbol resolve(String name) { return current().resolve(name); }

    public Symbol resolveLocal(String name) { return current().resolveLocal(name); }
}
