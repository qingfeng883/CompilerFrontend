package parser;

public class BinaryOpNode extends ASTNode {
    private String op;
    private ASTNode left;
    private ASTNode right;

    public BinaryOpNode(String op, ASTNode left, ASTNode right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    public String getOp() { return op; }
    public ASTNode getLeft() { return left; }
    public ASTNode getRight() { return right; }

    @Override
    public String toString() {
        return "(" + left + " " + op + " " + right + ")";
    }
}
