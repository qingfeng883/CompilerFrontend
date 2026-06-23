package semantic;

import parser.*;

import java.util.ArrayList;
import java.util.List;

public class SemanticAnalyzer {
    private SymbolTable symTable;
    private List<String> errors;
    private boolean hasError;

    public SemanticAnalyzer() {
        this.symTable = new SymbolTable();
        this.errors = new ArrayList<>();
        this.hasError = false;
    }

    public SymbolTable analyze(ASTNode root) {
        System.out.println("\n========== 开始语义分析 ==========");
        visit(root);
        System.out.println("========== 语义分析完成 ==========");
        System.out.println(symTable);
        return symTable;
    }

    public List<Symbol> getAllSymbols() {
        return symTable.getAllSymbols();
    }

    public SymbolTable getSymbolTable() {
        return symTable;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return hasError || !errors.isEmpty();
    }

    private void visit(ASTNode node) {
        if (node == null) return;

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
            visitNumber((NumberNode) node);
        } else if (node instanceof IdNode) {
            visitId((IdNode) node);
        }
    }

    private void visitBlock(BlockNode block) {
        for (ASTNode stmt : block.getStatements()) {
            visit(stmt);
        }
    }

    private void visitAssign(AssignNode assign) {
        String varName = assign.getId();
        System.out.println("[语义分析] 赋值: " + varName + " = ...");

        if (!symTable.contains(varName)) {
            symTable.put(varName, null);
        }

        Object result = evaluate(assign.getExpr());

        if (result != null) {
            symTable.put(varName, result);
        }

        System.out.println("[语义分析] " + varName + " = " + (result != null ? result : "无法计算/未赋值"));
    }

    private void visitIf(IfNode ifNode) {
        System.out.println("[语义分析] if 语句");

        Object condResult = evaluate(ifNode.getCondition());
        System.out.println("[语义分析] 条件值: " + condResult);

        System.out.println("[语义分析] 分析 then 分支");
        if (ifNode.getThenStmt() != null) {
            visit(ifNode.getThenStmt());
        }

        if (ifNode.getElseStmt() != null) {
            System.out.println("[语义分析] 分析 else 分支");
            visit(ifNode.getElseStmt());
        }
    }

    private void visitWhile(WhileNode whileNode) {
        System.out.println("[语义分析] while 循环");

        int maxIterations = 100;
        int count = 0;

        while (count < maxIterations) {
            Object condResult = evaluate(whileNode.getCondition());
            System.out.println("[语义分析] while 条件值: " + condResult + " (第" + (count + 1) + "次)");

            if (!isTrue(condResult)) {
                System.out.println("[语义分析] while 条件为假，退出循环");
                break;
            }

            System.out.println("[语义分析] while 条件为真，执行循环体");
            visit(whileNode.getBody());
            count++;
        }

        if (count >= maxIterations) {
            System.out.println("[语义分析] 警告: while 循环执行超过 " + maxIterations + " 次，强制退出");
        }
    }

    private void visitBinaryOp(BinaryOpNode binOp) {
        evaluate(binOp);
    }

    private void visitNumber(NumberNode num) {
        // 不处理
    }

    private void visitId(IdNode id) {
        String varName = id.getName();
        System.out.println("[语义分析] 使用变量: " + varName);

        if (!symTable.contains(varName)) {
            symTable.put(varName, null);
            addError("变量 '" + varName + "' 在使用前未声明");
        }
    }

    private boolean isTrue(Object value) {
        if (value == null) return false;
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }

    private Object evaluate(ASTNode node) {
        if (node == null) return null;

        if (node instanceof NumberNode) {
            NumberNode num = (NumberNode) node;
            return (int) num.getValue();

        } else if (node instanceof IdNode) {
            IdNode id = (IdNode) node;
            String varName = id.getName();

            if (!symTable.contains(varName)) {
                symTable.put(varName, null);
                addError("变量 '" + varName + "' 未声明");
                return null;
            }

            Symbol sym = symTable.get(varName);
            if (sym == null || sym.getValue() == null) {
                return null;
            }
            return sym.getValue();

        } else if (node instanceof BinaryOpNode) {
            BinaryOpNode bin = (BinaryOpNode) node;
            Object left = evaluate(bin.getLeft());
            Object right = evaluate(bin.getRight());

            if (left != null && right != null) {
                try {
                    int l = ((Number) left).intValue();
                    int r = ((Number) right).intValue();
                    // 传入 bin 节点以获取位置信息
                    return calculate(l, r, bin.getOp(), bin);
                } catch (ClassCastException e) {
                    return null;
                }
            }
            return null;
        }

        return null;
    }

    /**
     * 计算算术和比较运算结果，包含除零检查
     */
    private int calculate(int left, int right, String op, ASTNode node) {
        switch (op) {
            case "+":
                return left + right;
            case "-":
                return left - right;
            case "*":
                return left * right;
            case "/":
                // 除零检查
                if (right == 0) {
                    // 获取错误位置信息
                    int line = node != null ? node.getLine() : -1;
                    int col = node != null ? node.getColumn() : -1;
                    String location = (line > 0) ? " at line " + line + ":" + col : "";
                    addError("除零错误: " + left + " / 0" + location);
                    return 0;  // 返回0继续分析
                }
                return left / right;
            case "<":
                return left < right ? 1 : 0;
            case "<=":
                return left <= right ? 1 : 0;
            case ">":
                return left > right ? 1 : 0;
            case ">=":
                return left >= right ? 1 : 0;
            case "==":
                return left == right ? 1 : 0;
            case "!=":
                return left != right ? 1 : 0;
            default:
                return 0;
        }
    }

    private void addError(String message) {
        errors.add(message);
        hasError = true;
        System.err.println("[语义分析] ERROR: " + message);
    }
}