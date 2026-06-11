package semantic;

import parser.*;

import java.util.List;

public class SemanticAnalyzer {
    private SymbolTable symTable;
    private int currentLine;

    public SemanticAnalyzer() {
        this.symTable = new SymbolTable();
        this.currentLine = 1;
    }

    public SymbolTable analyze(ASTNode root) {
        visit(root);
        return symTable;
    }

    public SymbolTable getSymbolTable() {
        return symTable;
    }

    private void visit(ASTNode node) {
        if (node instanceof BlockNode) {
            visitBlock((BlockNode) node);
        } else if (node instanceof DeclarationNode) {
            visitDeclaration((DeclarationNode) node);
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
        }
    }

    private void visitBlock(BlockNode block) {
        symTable.enterScope();
        for (ASTNode stmt : block.getStatements()) {
            visit(stmt);
        }
        symTable.exitScope();
    }

    private void visitDeclaration(DeclarationNode decl) {
        String varName = decl.getId();
        String varType = decl.getType();

        // 声明变量（无行号信息，传 -1）
        symTable.declare(varName, varType);
        System.out.println("[语义分析] 声明变量: " + varName + " : " + varType);

        // 如果有初始化表达式，检查表达式
        if (decl.hasInit()) {
            visit(decl.getInitExpr());
        }
    }

    private void visitAssign(AssignNode assign) {
        String varName = assign.getId();
        Symbol existing = symTable.lookup(varName);

        if (existing == null) {
            // 未声明则自动声明为 int 类型
            System.out.println("[语义分析] 警告: 变量 '" + varName + "' 未声明，已自动声明为 int 类型");
            symTable.declare(varName, "int");
        }

        // 检查表达式
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
        Symbol sym = symTable.lookup(id.getName());
        if (sym == null) {
            throw new RuntimeException("变量 '" + id.getName() + "' 在使用前未声明");
        }
    }
}