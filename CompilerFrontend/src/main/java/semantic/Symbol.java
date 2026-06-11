package semantic;

public class Symbol {
    private String name;
    private String type;   // int, float, string, bool
    private int line;
    private Object value;

    public Symbol(String name, String type, int line) {
        this.name = name;
        this.type = type;
        this.line = line;
    }

    public Symbol(String name, String type) {
        this(name, type, -1);
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public int getLine() { return line; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }

    @Override
    public String toString() {
        if (line == -1) {
            return name + " : " + type + " (自动声明)";
        }
        return name + " : " + type + " (line " + line + ")";
    }
}