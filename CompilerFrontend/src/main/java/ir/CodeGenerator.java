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
        // 添加 end 四元式表示程序结束
        quadruples.add(new Quadruple("end", "", "", ""));
        // 设置每条四元式的行号（从1开始）
        for (int i = 0; i < quadruples.size(); i++) {
            quadruples.get(i).setLineNumber(i + 1);
        }
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

            // 生成条件表达式
            String condResult = generateCond(ifNode.getCondition());

            // 记录条件跳转的位置
            int falseJumpIndex = quadruples.size();
            quadruples.add(new Quadruple("if_false", condResult, "", "0"));

            // 生成 then 分支代码
            generateNode(ifNode.getThenStmt());

            // 记录无条件跳转的位置
            int gotoIndex = quadruples.size();
            quadruples.add(new Quadruple("goto", "", "", "0"));

            // 确定 else 分支开始的行号
            int elseTarget = quadruples.size() + 1;

            // 生成 else 分支代码
            if (ifNode.getElseStmt() != null) {
                generateNode(ifNode.getElseStmt());
            }

            // 确定 end 的行号
            int endTarget = quadruples.size() + 1;

            // 回填条件跳转
            if (ifNode.getElseStmt() != null) {
                Quadruple jumpQuad = quadruples.get(falseJumpIndex);
                quadruples.set(falseJumpIndex,
                        new Quadruple("if_false", jumpQuad.getArg1(), "", String.valueOf(elseTarget)));
            } else {
                Quadruple jumpQuad = quadruples.get(falseJumpIndex);
                quadruples.set(falseJumpIndex,
                        new Quadruple("if_false", jumpQuad.getArg1(), "", String.valueOf(endTarget)));
            }

            // 回填无条件跳转
            Quadruple gotoQuad = quadruples.get(gotoIndex);
            quadruples.set(gotoIndex,
                    new Quadruple("goto", "", "", String.valueOf(endTarget)));

        } else if (node instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) node;

            // 记录循环开始位置（显示行号）
            int startLine = quadruples.size() + 1;

            // 生成条件表达式
            String condResult = generateCond(whileNode.getCondition());

            // 记录条件跳转的位置
            int falseJumpIndex = quadruples.size();
            quadruples.add(new Quadruple("if_false", condResult, "", "0"));

            // 生成循环体
            generateNode(whileNode.getBody());

            // 无条件跳转到循环开始
            quadruples.add(new Quadruple("goto", "", "", String.valueOf(startLine)));

            // 回填条件跳转（条件为假时跳出循环）
            // end 在 goto 之后，即当前列表大小 + 1
            int endTarget = quadruples.size() + 1;
            Quadruple jumpQuad = quadruples.get(falseJumpIndex);
            quadruples.set(falseJumpIndex,
                    new Quadruple("if_false", jumpQuad.getArg1(), "", String.valueOf(endTarget)));

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

            int value = ((NumberNode) node).getIntValue() ;

            return String.valueOf(value);
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
            if (op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=")
                    || op.equals("==") || op.equals("!=")) {
                String left = generateExpr(bin.getLeft());
                String right = generateExpr(bin.getRight());
                String temp = newTemp();
                quadruples.add(new Quadruple(op, left, right, temp));
                return temp;
            }
        }
        return generateExpr(node);
    }
}