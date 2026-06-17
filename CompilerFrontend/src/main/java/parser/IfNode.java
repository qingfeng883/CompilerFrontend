package parser;

public class IfNode extends ASTNode {
    private ASTNode condition;
    private ASTNode thenStmt;
    private ASTNode elseStmt;

    public IfNode(ASTNode condition, ASTNode thenStmt, ASTNode elseStmt) {
        this.condition = condition;
        this.thenStmt = thenStmt;
        this.elseStmt = elseStmt;
    }

    public IfNode(ASTNode condition, ASTNode thenStmt, ASTNode elseStmt, int line, int column) {
        super(line, column);
        this.condition = condition;
        this.thenStmt = thenStmt;
        this.elseStmt = elseStmt;
    }

    public ASTNode getCondition() { return condition; }
    public ASTNode getThenStmt() { return thenStmt; }
    public ASTNode getElseStmt() { return elseStmt; }

    @Override
    public String toString() {
        String s = "if(" + condition + ")" + thenStmt;
        if (elseStmt != null) s += " else " + elseStmt;
        return s;
    }
}