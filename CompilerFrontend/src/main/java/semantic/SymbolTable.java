package semantic;

import java.util.*;

public class SymbolTable {
    private Stack<Map<String, Symbol>> scopes = new Stack<>();

    public SymbolTable() {
        enterScope();
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
        System.out.println("[符号表] 进入新作用域，当前作用域深度: " + scopes.size());
    }

    public void exitScope() {
        Map<String, Symbol> exiting = scopes.pop();
        System.out.println("[符号表] 退出作用域，移除变量: " + exiting.keySet());
    }

    // 完整参数版本
    public void declare(String name, String type, int line) {
        Map<String, Symbol> current = scopes.peek();
        if (current.containsKey(name)) {
            throw new RuntimeException("变量 '" + name + "' 已在当前作用域声明");
        }
        Symbol sym = new Symbol(name, type, line);
        current.put(name, sym);
        System.out.println("[符号表] 声明变量: " + name + " : " + type);
    }

    // 简化版本（无行号）
    public void declare(String name, String type) {
        declare(name, type, -1);
    }

    public Symbol lookup(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Symbol sym = scopes.get(i).get(name);
            if (sym != null) {
                System.out.println("[符号表] 查找变量: " + name + " -> 找到");
                return sym;
            }
        }
        System.out.println("[符号表] 查找变量: " + name + " -> 未找到");
        return null;
    }

    public void checkDeclared(String name, int line) {
        if (lookup(name) == null) {
            throw new RuntimeException("变量 '" + name + "' 在第 " + line + " 行未声明");
        }
    }

    public List<Symbol> getAllSymbols() {
        List<Symbol> all = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (Map<String, Symbol> scope : scopes) {
            for (Symbol sym : scope.values()) {
                if (!added.contains(sym.getName())) {
                    added.add(sym.getName());
                    all.add(sym);
                }
            }
        }
        return all;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Symbol sym : getAllSymbols()) {
            sb.append(sym).append("\n");
        }
        if (sb.length() == 0) {
            sb.append("(空符号表)");
        }
        return sb.toString();
    }
}