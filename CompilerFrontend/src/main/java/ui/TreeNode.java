package ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    private String label;
    private List<TreeNode> children;
    private Rectangle bounds;
    private int x, y;

    public TreeNode(String label) {
        this.label = label;
        this.children = new ArrayList<>();
    }

    public void addChild(TreeNode child) {
        if (child != null) {
            children.add(child);
            System.out.println("  添加子节点: " + label + " -> " + child.getLabel());
        }
    }

    public String getLabel() {
        return label;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}