package parser;

public class NumberNode extends ASTNode {
    private int value;

    public NumberNode(int value) {
        this.value = value;
    }

    public int getValue() { return value; }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
