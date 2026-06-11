package semantic;

public class Symbol {
    private String name;
    private String type;   // 目前只有 "int"
    private int line;

    public Symbol(String name, String type, int line) {
        this.name = name;
        this.type = type;
        this.line = line;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public int getLine() { return line; }

    @Override
    public String toString() {
        return name + " : " + type + " (line " + line + ")";
    }
}