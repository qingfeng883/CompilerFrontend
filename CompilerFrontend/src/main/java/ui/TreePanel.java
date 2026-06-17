package ui;

import parser.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.util.List;

public class TreePanel extends JPanel {
    private TreeNode root;
    private int nodeWidth = 60;
    private int nodeHeight = 32;
    private int levelHeight = 65;
    private double scale = 1.0;
    private int offsetX = 0;
    private int offsetY = 0;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private boolean isDragging = false;

    private static final double MIN_SCALE = 0.05;
    private static final double MAX_SCALE = 2.0;
    private static final double SCALE_STEP = 0.1;

    public TreePanel() {
        setBackground(new Color(250, 250, 250));
        setPreferredSize(new Dimension(1200, 700));

        addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                double newScale = scale + (e.getWheelRotation() > 0 ? -SCALE_STEP : SCALE_STEP);
                scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartX = e.getX();
                dragStartY = e.getY();
                dragOffsetX = offsetX;
                dragOffsetY = offsetY;
                isDragging = true;
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
                setCursor(Cursor.getDefaultCursor());
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    offsetX = dragOffsetX + (e.getX() - dragStartX);
                    offsetY = dragOffsetY + (e.getY() - dragStartY);
                    repaint();
                }
            }
        });

        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ADD:
                    case KeyEvent.VK_EQUALS:
                        if (e.isControlDown()) {
                            scale = Math.min(MAX_SCALE, scale + SCALE_STEP);
                            repaint();
                        }
                        break;
                    case KeyEvent.VK_SUBTRACT:
                    case KeyEvent.VK_MINUS:
                        if (e.isControlDown()) {
                            scale = Math.max(MIN_SCALE, scale - SCALE_STEP);
                            repaint();
                        }
                        break;
                    case KeyEvent.VK_0:
                        if (e.isControlDown()) {
                            resetView();
                        }
                        break;
                }
            }
        });

        // 组件大小变化时重新计算位置
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (root != null) {
                    autoFitTree();
                }
            }
        });
    }

    public void resetView() {
        scale = 1.0;
        if (root != null) {
            autoFitTree();
        } else {
            offsetX = 0;
            offsetY = 0;
        }
        repaint();
    }

    public void zoomIn() {
        scale = Math.min(MAX_SCALE, scale + SCALE_STEP);
        repaint();
    }

    public void zoomOut() {
        scale = Math.max(MIN_SCALE, scale - SCALE_STEP);
        repaint();
    }

    public void setAST(ASTNode ast) {
        System.out.println("\n=== 开始构建语法树 ===");
        root = buildTree(ast);
        if (root != null) {
            printTree(root, 0);
            calculatePositions();
            autoFitTree();
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
        } else if (node instanceof AssignNode) {
            AssignNode assign = (AssignNode) node;
            TreeNode assignNode = new TreeNode("=");
            assignNode.addChild(new TreeNode(assign.getId()));
            assignNode.addChild(buildTree(assign.getExpr()));
            return assignNode;
        } else if (node instanceof IfNode) {
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
        } else if (node instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) node;
            TreeNode whileNodeTree = new TreeNode("while");

            TreeNode condNode = new TreeNode("C");
            condNode.addChild(buildTree(whileNode.getCondition()));
            whileNodeTree.addChild(condNode);

            TreeNode bodyNode = new TreeNode("Body");
            bodyNode.addChild(buildTree(whileNode.getBody()));
            whileNodeTree.addChild(bodyNode);

            return whileNodeTree;
        } else if (node instanceof BinaryOpNode) {
            BinaryOpNode bin = (BinaryOpNode) node;
            TreeNode opNode = new TreeNode(bin.getOp());
            opNode.addChild(buildTree(bin.getLeft()));
            opNode.addChild(buildTree(bin.getRight()));
            return opNode;
        } else if (node instanceof NumberNode) {
            NumberNode num = (NumberNode) node;
            return new TreeNode(num.toString());
        } else if (node instanceof IdNode) {
            IdNode id = (IdNode) node;
            return new TreeNode(id.getName());
        }

        return new TreeNode(node.getClass().getSimpleName());
    }

    private void calculatePositions() {
        if (root == null) return;

        int startX = 400;
        int startY = 40;
        calculateNodePositions(root, startX, startY, 180);
    }

    /**
     * 自动适应树的大小，确保完整显示
     */
    private void autoFitTree() {
        if (root == null) return;

        // 计算树的边界
        int minX = getMinX(root, Integer.MAX_VALUE);
        int maxX = getMaxX(root, Integer.MIN_VALUE);
        int minY = getMinY(root, Integer.MAX_VALUE);
        int maxY = getMaxY(root, Integer.MIN_VALUE);

        int treeWidth = maxX - minX + nodeWidth;
        int treeHeight = maxY - minY + nodeHeight;

        int panelWidth = getWidth();
        int panelHeight = getHeight();

        if (panelWidth <= 0) panelWidth = 1200;
        if (panelHeight <= 0) panelHeight = 700;

        // 计算合适的缩放比例
        double scaleX = (double) (panelWidth - 60) / treeWidth;
        double scaleY = (double) (panelHeight - 80) / treeHeight;
        double newScale = Math.min(1.0, Math.min(scaleX, scaleY));
        newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));

        // 如果树比面板大，缩小；否则保持1.0
        if (newScale < 1.0) {
            scale = newScale;
        } else {
            scale = 1.0;
        }

        // 计算居中偏移
        int scaledWidth = (int) (treeWidth * scale);
        int scaledHeight = (int) (treeHeight * scale);

        // 计算树的中心位置
        int treeCenterX = (minX + maxX) / 2;
        int treeCenterY = (minY + maxY) / 2;

        // 计算偏移使树居中
        offsetX = panelWidth / 2 - (int) (treeCenterX * scale);
        offsetY = panelHeight / 2 - (int) (treeCenterY * scale);

        // 如果树较小，稍微上移一点
        if (scaledHeight < panelHeight - 100) {
            offsetY += 20;
        }

        repaint();
    }

    private int getMinX(TreeNode node, int min) {
        if (node == null) return min;
        if (node.getX() < min) min = node.getX();
        for (TreeNode child : node.getChildren()) {
            min = getMinX(child, min);
        }
        return min;
    }

    private int getMaxX(TreeNode node, int max) {
        if (node == null) return max;
        if (node.getX() > max) max = node.getX();
        for (TreeNode child : node.getChildren()) {
            max = getMaxX(child, max);
        }
        return max;
    }

    private int getMinY(TreeNode node, int min) {
        if (node == null) return min;
        if (node.getY() < min) min = node.getY();
        for (TreeNode child : node.getChildren()) {
            min = getMinY(child, min);
        }
        return min;
    }

    private int getMaxY(TreeNode node, int max) {
        if (node == null) return max;
        if (node.getY() > max) max = node.getY();
        for (TreeNode child : node.getChildren()) {
            max = getMaxY(child, max);
        }
        return max;
    }

    private void calculateNodePositions(TreeNode node, int x, int y, int xOffset) {
        if (node == null) return;

        node.setX(x);
        node.setY(y);
        node.setBounds(new Rectangle(x - nodeWidth / 2, y - nodeHeight / 2, nodeWidth, nodeHeight));

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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (root != null) {
            // 应用缩放和平移
            g2d.translate(offsetX, offsetY);
            g2d.scale(scale, scale);

            g2d.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            drawLines(g2d, root);
            drawNodes(g2d, root);

            // 重置变换
            g2d.setTransform(new AffineTransform());
        } else {
            g2d.setColor(Color.GRAY);
            g2d.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            String msg = "请先进行语法分析生成 AST 树";
            FontMetrics fm = g2d.getFontMetrics();
            int msgX = (getWidth() - fm.stringWidth(msg)) / 2;
            g2d.drawString(msg, msgX, getHeight() / 2);
        }

        // 绘制缩放信息
        g2d.setColor(new Color(100, 100, 100, 200));
        g2d.fillRoundRect(10, getHeight() - 40, 160, 30, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        String info = String.format("缩放: %.0f%%", scale * 100);
        g2d.drawString(info, 20, getHeight() - 20);

        // 提示信息
        g2d.setColor(new Color(150, 150, 150, 180));
        g2d.setFont(new Font("微软雅黑", Font.PLAIN, 10));
        String tip = "Ctrl+滚轮缩放 | 拖拽平移 | Ctrl+0重置";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(tip, getWidth() - fm.stringWidth(tip) - 20, getHeight() - 20);
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
        } else {
            bgColor = Color.WHITE;
            borderColor = new Color(80, 80, 80);
        }

        // 根据缩放调整节点大小
        int scaledNodeWidth = (int) (nodeWidth * Math.min(1.0, Math.max(0.5, scale)));
        int scaledNodeHeight = (int) (nodeHeight * Math.min(1.0, Math.max(0.5, scale)));
        if (scaledNodeWidth < 30) scaledNodeWidth = 30;
        if (scaledNodeHeight < 18) scaledNodeHeight = 18;

        g.setColor(bgColor);
        g.fillRoundRect(node.getX() - scaledNodeWidth / 2, node.getY() - scaledNodeHeight / 2,
                scaledNodeWidth, scaledNodeHeight, 8, 8);

        g.setColor(borderColor);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(node.getX() - scaledNodeWidth / 2, node.getY() - scaledNodeHeight / 2,
                scaledNodeWidth, scaledNodeHeight, 8, 8);

        g.setColor(Color.BLACK);
        int fontSize = (int) (12 * Math.min(1.0, Math.max(0.6, scale)));
        if (fontSize < 9) fontSize = 9;
        g.setFont(new Font("微软雅黑", Font.PLAIN, fontSize));

        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        if (textWidth > scaledNodeWidth - 8) {
            // 如果文字太长，缩小字体
            int newSize = fontSize - 2;
            if (newSize >= 8) {
                g.setFont(new Font("微软雅黑", Font.PLAIN, newSize));
                fm = g.getFontMetrics();
                textWidth = fm.stringWidth(label);
            }
        }
        int textX = node.getX() - textWidth / 2;
        int textY = node.getY() + fm.getAscent() / 2 - 1;
        g.drawString(label, textX, textY);

        for (TreeNode child : node.getChildren()) {
            drawNodes(g, child);
        }
    }
}