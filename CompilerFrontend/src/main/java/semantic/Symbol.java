package semantic;

public class Symbol {
    private String name;
    private String type;  // 新增类型字段
    private Object value;

    public Symbol(String name) {
        this.name = name;
        this.type = "int";  // 默认类型为int
        this.value = null;
    }

    public Symbol(String name, Object value) {
        this.name = name;
        this.type = "int";  // 默认类型为int
        this.value = value;
    }

    public Symbol(String name, String type, Object value) {
        this.name = name;
        this.type = type != null ? type : "int";
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return name + " : " + type + " = " + (value != null ? value : "NULL");
    }
}