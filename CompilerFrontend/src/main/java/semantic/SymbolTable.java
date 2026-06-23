package semantic;

import java.util.*;

public class SymbolTable {
    private Map<String, Symbol> symbols;

    public SymbolTable() {
        this.symbols = new LinkedHashMap<>();
    }

    /**
     * 声明或更新变量
     */
    public void put(String name, Object value) {
        if (symbols.containsKey(name)) {
            symbols.get(name).setValue(value);
        } else {
            symbols.put(name, new Symbol(name, value));
        }
    }

    /**
     * 声明或更新变量（带类型）
     */
    public void put(String name, String type, Object value) {
        if (symbols.containsKey(name)) {
            Symbol sym = symbols.get(name);
            sym.setType(type);
            sym.setValue(value);
        } else {
            symbols.put(name, new Symbol(name, type, value));
        }
    }

    /**
     * 查找变量
     */
    public Symbol get(String name) {
        return symbols.get(name);
    }

    /**
     * 判断变量是否存在
     */
    public boolean contains(String name) {
        return symbols.containsKey(name);
    }

    /**
     * 获取所有符号
     */
    public List<Symbol> getAllSymbols() {
        return new ArrayList<>(symbols.values());
    }

    /**
     * 获取变量值
     */
    public Object getValue(String name) {
        Symbol sym = symbols.get(name);
        return sym != null ? sym.getValue() : null;
    }

    /**
     * 设置变量值
     */
    public void setValue(String name, Object value) {
        if (symbols.containsKey(name)) {
            symbols.get(name).setValue(value);
        } else {
            symbols.put(name, new Symbol(name, value));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== 符号表 ==========\n");
        for (Symbol sym : symbols.values()) {
            sb.append("  ").append(sym).append("\n");
        }
        if (symbols.isEmpty()) {
            sb.append("  (空)\n");
        }
        sb.append("=============================");
        return sb.toString();
    }

    /**
     * 导出为表格数据（用于UI显示）- 包含类型列
     */
    public String[][] toTableData() {
        List<Symbol> all = getAllSymbols();
        String[][] data = new String[all.size()][3];  // 3列: 变量名, 类型, 值
        for (int i = 0; i < all.size(); i++) {
            Symbol sym = all.get(i);
            data[i][0] = sym.getName();
            data[i][1] = sym.getType() != null ? sym.getType() : "int";
            Object val = sym.getValue();
            data[i][2] = val != null ? val.toString() : "";
        }
        return data;
    }
}