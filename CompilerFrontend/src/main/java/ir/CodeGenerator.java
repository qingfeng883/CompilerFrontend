package ir;

import parser.*;

import java.util.ArrayList;
import java.util.List;

public class CodeGenerator {
    private List<Quadruple> quadruples = new ArrayList<>();
    private int tempCounter = 0;

    public List<Quadruple> generate(ASTNode root) {
        quadruples.clear();
        tempCounter = 0;
        generateNode(root);
        return quadruples;
    }

    private String newTemp() {
        return "t" + (tempCounter++);
    }

    private void generateNode(ASTNode node) {
        if (node instanceof BlockNode) {
            for (ASTNode stmt : ((BlockNode) node).getStatements()) {
                generateNode(stmt);
            }
        } else if (node instanceof AssignNode) {
            AssignNode assign = (AssignNode) node;
            String exprResult = generateExpr(assign.getExpr());
            quadruples.add(new Quadruple("=", exprResult, "", assign.getId()));
        } else if (node instanceof IfNode) {
            IfNode ifNode = (IfNode) node;
            String condResult = generateCond(ifNode.getCondition());
            String elseLabel = "L" + tempCounter++;
            String endLabel = "L" + tempCounter++;
            quadruples.add(new Quadruple("if", condResult, "goto", elseLabel));
            generateNode(ifNode.getThenStmt());
            quadruples.add(new Quadruple("goto", "", "", endLabel));
            quadruples.add(new Quadruple("label", "", "", elseLabel));
            if (ifNode.getElseStmt() != null) {
                generateNode(ifNode.getElseStmt());
            }
            quadruples.add(new Quadruple("label", "", "", endLabel));
        } else if (node instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) node;
            String startLabel = "L" + tempCounter++;
            String endLabel = "L" + tempCounter++;
            quadruples.add(new Quadruple("label", "", "", startLabel));
            String condResult = generateCond(whileNode.getCondition());
            quadruples.add(new Quadruple("if", condResult, "goto", endLabel));
            generateNode(whileNode.getBody());
            quadruples.add(new Quadruple("goto", "", "", startLabel));
            quadruples.add(new Quadruple("label", "", "", endLabel));
        } else if (node instanceof BinaryOpNode) {
            // 表达式节点在 generateExpr 中处理
        } else if (node instanceof NumberNode || node instanceof IdNode) {
            // 原子节点不单独生成四元式
        } else {
            throw new RuntimeException("未知节点类型: " + node.getClass());
        }
    }

    private String generateExpr(ASTNode node) {
        if (node instanceof BinaryOpNode) {
            BinaryOpNode bin = (BinaryOpNode) node;
            String left = generateExpr(bin.getLeft());
            String right = generateExpr(bin.getRight());
            String temp = newTemp();
            quadruples.add(new Quadruple(bin.getOp(), left, right, temp));
            return temp;
        } else if (node instanceof NumberNode) {
            return ((NumberNode) node).getValue() + "";
        } else if (node instanceof IdNode) {
            return ((IdNode) node).getName();
        } else {
            throw new RuntimeException("无效表达式节点: " + node.getClass());
        }
    }

    private String generateCond(ASTNode node) {
        if (node instanceof BinaryOpNode) {
            BinaryOpNode bin = (BinaryOpNode) node;
            String op = bin.getOp();
            if (op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=")) {
                String left = generateExpr(bin.getLeft());
                String right = generateExpr(bin.getRight());
                String temp = newTemp();
                quadruples.add(new Quadruple(op, left, right, temp));
                return temp;
            }
        }
        // 普通表达式作为条件（非零为真）
        return generateExpr(node);
    }
}