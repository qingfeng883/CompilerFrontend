package parser;

public class AssignNode extends ASTNode {
    private String id;
    private ASTNode expr;

    public AssignNode(String id, ASTNode expr) {
        this.id = id;
        this.expr = expr;
    }

    public String getId() { return id; }
    public ASTNode getExpr() { return expr; }

    @Override
    public String toString() {
        return id + "=" + expr;
    }
}