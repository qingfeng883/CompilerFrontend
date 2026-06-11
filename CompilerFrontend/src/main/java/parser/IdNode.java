package parser;


public class IdNode extends ASTNode {
    private String name;

    public IdNode(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}
