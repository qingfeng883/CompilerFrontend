package parser;

public class DeclarationNode extends ASTNode {
    private String type;      // int, float, string, bool
    private String id;
    private ASTNode initExpr;  // 可为 null（无初始化）

    public DeclarationNode(String type, String id, ASTNode initExpr) {
        this.type = type;
        this.id = id;
        this.initExpr = initExpr;
    }

    public String getType() { return type; }
    public String getId() { return id; }
    public ASTNode getInitExpr() { return initExpr; }
    public boolean hasInit() { return initExpr != null; }

    @Override
    public String toString() {
        if (initExpr != null) {
            return type + " " + id + " = " + initExpr;
        }
        return type + " " + id;
    }
}