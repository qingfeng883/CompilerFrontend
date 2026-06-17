package parser;

public abstract class ASTNode {
    protected int line;
    protected int column;

    public ASTNode() {
        this.line = -1;
        this.column = -1;
    }

    public ASTNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public int getLine() { return line; }
    public int getColumn() { return column; }
    public void setLine(int line) { this.line = line; }
    public void setColumn(int column) { this.column = column; }
}