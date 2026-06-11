package parser;

public class NumberNode extends ASTNode {
    private double value;
    private boolean isInt;

    public NumberNode(int value) {
        this.value = value;
        this.isInt = true;
    }

    public NumberNode(float value) {
        this.value = value;
        this.isInt = false;
    }

    public NumberNode(double value) {
        this.value = value;
        this.isInt = false;
    }

    public double getValue() { return value; }
    public int getIntValue() { return (int) value; }
    public boolean isInt() { return isInt; }

    @Override
    public String toString() {
        if (isInt) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }
}