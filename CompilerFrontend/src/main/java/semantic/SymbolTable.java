package semantic;

import java.util.*;

public class SymbolTable {
    private Stack<Map<String, Symbol>> scopes = new Stack<>();

    public SymbolTable() {
        enterScope();
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        scopes.pop();
    }

    public void declare(String name, String type, int line) {
        Map<String, Symbol> current = scopes.peek();
        if (current.containsKey(name)) {
            throw new RuntimeException("变量 '" + name + "' 已在第 " + current.get(name).getLine() + " 行声明");
        }
        current.put(name, new Symbol(name, type, line));
    }

    public Symbol lookup(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Symbol sym = scopes.get(i).get(name);
            if (sym != null) return sym;
        }
        return null;
    }

    public void checkDeclared(String name, int line) {
        if (lookup(name) == null) {
            throw new RuntimeException("变量 '" + name + "' 在第 " + line + " 行未声明");
        }
    }

    public List<Symbol> getAllSymbols() {
        List<Symbol> all = new ArrayList<>();
        for (Map<String, Symbol> scope : scopes) {
            all.addAll(scope.values());
        }
        return all;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Symbol sym : getAllSymbols()) {
            sb.append(sym).append("\n");
        }
        return sb.toString();
    }
}