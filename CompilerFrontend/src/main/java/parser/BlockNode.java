package parser;

import java.util.List;

public class BlockNode extends ASTNode {
    private List<ASTNode> statements;

    public BlockNode(List<ASTNode> statements) {
        this.statements = statements;
    }

    public List<ASTNode> getStatements() {
        return statements;
    }

    @Override
    public String toString() {
        return "Block{" + statements + "}";
    }
}
