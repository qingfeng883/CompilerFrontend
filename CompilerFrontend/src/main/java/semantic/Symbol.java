package semantic;

public class Symbol {
    private String name;
    private Object value;

    public Symbol(String name) {
        this.name = name;
        this.value = null;
    }

    public Symbol(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return name + " = " + (value != null ? value : "NULL");
    }
}