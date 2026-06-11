package semantic;

import parser.*;

import java.util.List;

public class SemanticAnalyzer {
    private SymbolTable symTable;

    public SemanticAnalyzer() {
        this.symTable = new SymbolTable();
    }

    public SymbolTable analyze(ASTNode root) {
        visit(root);
        return symTable;
    }

    private void visit(ASTNode node) {
        if (node instanceof BlockNode) {
            visitBlock((BlockNode) node);
        } else if (node instanceof AssignNode) {
            visitAssign((AssignNode) node);
        } else if (node instanceof IfNode) {
            visitIf((IfNode) node);
        } else if (node instanceof WhileNode) {
            visitWhile((WhileNode) node);
        } else if (node instanceof BinaryOpNode) {
            visitBinaryOp((BinaryOpNode) node);
        } else if (node instanceof NumberNode) {
            // 无需处理
        } else if (node instanceof IdNode) {
            visitId((IdNode) node);
        } else {
            throw new RuntimeException("未知 AST 节点: " + node.getClass());
        }
    }

    private void visitBlock(BlockNode block) {
        symTable.enterScope();
        for (ASTNode stmt : block.getStatements()) {
            visit(stmt);
        }
        symTable.exitScope();
    }

    private void visitAssign(AssignNode assign) {
        // 左侧标识符：若未声明则自动声明（int类型）并给出警告
        String varName = assign.getId();
        Symbol existing = symTable.lookup(varName);
        if (existing == null) {
            System.err.println("警告：变量 '" + varName + "' 未声明，已自动声明为 int 类型");
            symTable.declare(varName, "int", -1); // 行号未知，可后续改进
        }
        // 检查表达式（确保其中的变量已声明）
        visit(assign.getExpr());
    }

    private void visitIf(IfNode ifNode) {
        visit(ifNode.getCondition());
        visit(ifNode.getThenStmt());
        if (ifNode.getElseStmt() != null) {
            visit(ifNode.getElseStmt());
        }
    }

    private void visitWhile(WhileNode whileNode) {
        visit(whileNode.getCondition());
        visit(whileNode.getBody());
    }

    private void visitBinaryOp(BinaryOpNode binOp) {
        visit(binOp.getLeft());
        visit(binOp.getRight());
    }

    private void visitId(IdNode id) {
        // 检查变量是否已声明（未声明则报错）
        // 注意：这里不自动声明，只在赋值时允许自动声明
        Symbol sym = symTable.lookup(id.getName());
        if (sym == null) {
            throw new RuntimeException("变量 '" + id.getName() + "' 在使用前未声明");
        }
    }

    public SymbolTable getSymbolTable() {
        return symTable;
    }
}