package ui;

import parser.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.List;

public class TreePanel extends JPanel {
    private TreeNode root;
    private int nodeWidth = 60;
    private int nodeHeight = 32;
    private int levelHeight = 65;
    private int minX = 0;
    private int maxX = 0;

    public TreePanel() {
        setBackground(new Color(250, 250, 250));
        setPreferredSize(new Dimension(1200, 700));
    }

    public void setAST(ASTNode ast) {
        System.out.println("\n=== 开始构建语法树 ===");
        root = buildTree(ast);
        if (root != null) {
            printTree(root, 0);
            calculatePositions();
        }
        repaint();
    }

    private void printTree(TreeNode node, int depth) {
        if (node == null) return;
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) indent.append("  ");
        System.out.println(indent + node.getLabel());
        for (TreeNode child : node.getChildren()) {
            printTree(child, depth + 1);
        }
    }

    private TreeNode buildTree(ASTNode node) {
        if (node == null) return null;

        // Block 节点
        if (node instanceof BlockNode) {
            BlockNode block = (BlockNode) node;
            List<ASTNode> statements = block.getStatements();

            if (statements.isEmpty()) {
                return new TreeNode("空");
            } else if (statements.size() == 1) {
                return buildTree(statements.get(0));
            } else {
                TreeNode programNode = new TreeNode("Program");
                for (ASTNode stmt : statements) {
                    TreeNode stmtNode = buildTree(stmt);
                    if (stmtNode != null) {
                        programNode.addChild(stmtNode);
                    }
                }
                return programNode;
            }
        }

        // Declaration 节点
        else if (node instanceof DeclarationNode) {
            DeclarationNode decl = (DeclarationNode) node;
            TreeNode declNode = new TreeNode(decl.getType());
            declNode.addChild(new TreeNode(decl.getId()));
            if (decl.hasInit()) {
                TreeNode initNode = new TreeNode("=");
                initNode.addChild(buildTree(decl.getInitExpr()));
                declNode.addChild(initNode);
            }
            return declNode;
        }

        // Assign 节点
        else if (node instanceof AssignNode) {
            AssignNode assign = (AssignNode) node;
            TreeNode assignNode = new TreeNode("=");
            assignNode.addChild(new TreeNode(assign.getId()));
            assignNode.addChild(buildTree(assign.getExpr()));
            return assignNode;
        }

        // If 节点
        else if (node instanceof IfNode) {
            IfNode ifNode = (IfNode) node;
            TreeNode ifNodeTree = new TreeNode("if");

            TreeNode condNode = new TreeNode("C");
            condNode.addChild(buildTree(ifNode.getCondition()));
            ifNodeTree.addChild(condNode);

            TreeNode thenNode = new TreeNode("then");
            thenNode.addChild(buildTree(ifNode.getThenStmt()));
            ifNodeTree.addChild(thenNode);

            if (ifNode.getElseStmt() != null) {
                TreeNode elseNode = new TreeNode("else");
                elseNode.addChild(buildTree(ifNode.getElseStmt()));
                ifNodeTree.addChild(elseNode);
            }

            return ifNodeTree;
        }

        // While 节点
        else if (node instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) node;
            TreeNode whileNodeTree = new TreeNode("while");

            TreeNode condNode = new TreeNode("C");
            condNode.addChild(buildTree(whileNode.getCondition()));
            whileNodeTree.addChild(condNode);

            TreeNode bodyNode = new TreeNode("Body");
            bodyNode.addChild(buildTree(whileNode.getBody()));
            whileNodeTree.addChild(bodyNode);

            return whileNodeTree;
        }

        // 二元运算节点
        else if (node instanceof BinaryOpNode) {
            BinaryOpNode bin = (BinaryOpNode) node;
            TreeNode opNode = new TreeNode(bin.getOp());
            opNode.addChild(buildTree(bin.getLeft()));
            opNode.addChild(buildTree(bin.getRight()));
            return opNode;
        }

        // 数字节点
        else if (node instanceof NumberNode) {
            NumberNode num = (NumberNode) node;
            return new TreeNode(num.toString());
        }

        // 标识符节点
        else if (node instanceof IdNode) {
            IdNode id = (IdNode) node;
            return new TreeNode(id.getName());
        }

        return new TreeNode(node.getClass().getSimpleName());
    }

    private void calculatePositions() {
        if (root == null) return;
        minX = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;

        int startX = 400;
        int startY = 40;
        calculateNodePositions(root, startX, startY, 180);

        if (minX < 50) {
            int offset = 50 - minX;
            shiftNodes(root, offset);
        }
    }

    private void calculateNodePositions(TreeNode node, int x, int y, int xOffset) {
        if (node == null) return;

        node.setX(x);
        node.setY(y);
        node.setBounds(new Rectangle(x - nodeWidth/2, y - nodeHeight/2, nodeWidth, nodeHeight));

        if (x < minX) minX = x;
        if (x > maxX) maxX = x;

        List<TreeNode> children = node.getChildren();
        int childCount = children.size();
        if (childCount == 0) return;

        int[] childX = new int[childCount];
        int totalWidth = 0;
        for (int i = 0; i < childCount; i++) {
            totalWidth += getTreeWidth(children.get(i));
        }

        int currentX = x - totalWidth / 2;
        for (int i = 0; i < childCount; i++) {
            int childTreeWidth = getTreeWidth(children.get(i));
            childX[i] = currentX + childTreeWidth / 2;
            currentX += childTreeWidth;
        }

        for (int i = 0; i < childCount; i++) {
            calculateNodePositions(children.get(i), childX[i], y + levelHeight, xOffset / 2);
        }
    }

    private int getTreeWidth(TreeNode node) {
        if (node == null) return 0;
        List<TreeNode> children = node.getChildren();
        if (children.isEmpty()) return nodeWidth;

        int totalWidth = 0;
        for (TreeNode child : children) {
            totalWidth += getTreeWidth(child);
        }
        return Math.max(totalWidth, nodeWidth);
    }

    private void shiftNodes(TreeNode node, int offset) {
        if (node == null) return;
        node.setX(node.getX() + offset);
        for (TreeNode child : node.getChildren()) {
            shiftNodes(child, offset);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        if (root != null) {
            drawLines(g2d, root);
            drawNodes(g2d, root);
        } else {
            g2d.setColor(Color.GRAY);
            g2d.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            String msg = "请先进行语法分析生成 AST 树";
            FontMetrics fm = g2d.getFontMetrics();
            int msgX = (getWidth() - fm.stringWidth(msg)) / 2;
            g2d.drawString(msg, msgX, getHeight() / 2);
        }
    }

    private void drawLines(Graphics2D g, TreeNode node) {
        if (node == null) return;

        g.setColor(new Color(100, 100, 100));
        g.setStroke(new BasicStroke(1.5f));

        int startX = node.getX();
        int startY = node.getY() + nodeHeight / 2;

        for (TreeNode child : node.getChildren()) {
            int endX = child.getX();
            int endY = child.getY() - nodeHeight / 2;
            g.draw(new Line2D.Double(startX, startY, endX, endY));
            drawLines(g, child);
        }
    }

    private void drawNodes(Graphics2D g, TreeNode node) {
        if (node == null) return;

        String label = node.getLabel();

        Color bgColor;
        Color borderColor;

        if (label.equals("if") || label.equals("while")) {
            bgColor = new Color(255, 220, 220);
            borderColor = new Color(200, 60, 60);
        } else if (label.equals("C") || label.equals("then") || label.equals("else") || label.equals("Body")) {
            bgColor = new Color(220, 255, 220);
            borderColor = new Color(60, 160, 60);
        } else if (label.equals("=") || label.equals("+") || label.equals("-") ||
                label.equals("*") || label.equals("/") || label.equals("<") ||
                label.equals("<=") || label.equals(">") || label.equals(">=")) {
            bgColor = new Color(220, 220, 255);
            borderColor = new Color(60, 60, 200);
        } else if (label.equals("Program")) {
            bgColor = new Color(255, 255, 200);
            borderColor = new Color(160, 160, 60);
        } else if (label.equals("int") || label.equals("float") || label.equals("string") || label.equals("bool")) {
            bgColor = new Color(255, 200, 200);
            borderColor = new Color(200, 60, 100);
        } else {
            bgColor = Color.WHITE;
            borderColor = new Color(80, 80, 80);
        }

        g.setColor(bgColor);
        g.fillRoundRect(node.getX() - nodeWidth/2, node.getY() - nodeHeight/2,
                nodeWidth, nodeHeight, 10, 10);

        g.setColor(borderColor);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(node.getX() - nodeWidth/2, node.getY() - nodeHeight/2,
                nodeWidth, nodeHeight, 10, 10);

        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int textX = node.getX() - textWidth / 2;
        int textY = node.getY() + fm.getAscent() / 2 - 1;
        g.drawString(label, textX, textY);

        for (TreeNode child : node.getChildren()) {
            drawNodes(g, child);
        }
    }
}