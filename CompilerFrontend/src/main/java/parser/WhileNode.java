package parser;

public class WhileNode extends ASTNode {
    private ASTNode condition;
    private ASTNode body;

    public WhileNode(ASTNode condition, ASTNode body) {
        this.condition = condition;
        this.body = body;
    }

    public ASTNode getCondition() { return condition; }
    public ASTNode getBody() { return body; }

    @Override
    public String toString() {
        return "while(" + condition + ")" + body;
    }
}
