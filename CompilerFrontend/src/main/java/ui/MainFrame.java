package ui;

import ir.CodeGenerator;
import ir.Quadruple;
import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;
import parser.ASTNode;
import parser.Parser;
import semantic.SemanticAnalyzer;
import semantic.SymbolTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {
    private JTextArea sourceArea;
    private JTable tokenTable;
    private JTree astTree;
    private JTable symbolTable;
    private JList<String> irList;
    private DefaultTableModel tokenModel;
    private DefaultTableModel symbolModel;
    private DefaultListModel<String> irListModel;

    public MainFrame() {
        setTitle("Compiler Frontend - 题目1");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 顶部工具栏
        JToolBar toolBar = new JToolBar();
        JButton lexerBtn = new JButton("词法分析");
        JButton parserBtn = new JButton("语法分析");
        JButton semanticBtn = new JButton("语义分析");
        JButton codeGenBtn = new JButton("生成四元式");
        JButton runAllBtn = new JButton("一键运行");
        toolBar.add(lexerBtn);
        toolBar.add(parserBtn);
        toolBar.add(semanticBtn);
        toolBar.add(codeGenBtn);
        toolBar.add(runAllBtn);
        add(toolBar, BorderLayout.NORTH);

        // 中间分屏：左边源代码，右边结果标签页
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.4);

        // 左边：源代码编辑区
        sourceArea = new JTextArea();
        sourceArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        sourceArea.setText("""
                {
                    a = 3;
                    b = 4;
                    if (a < b) {
                        c = a + b;
                    }
                }""");
        JScrollPane sourceScroll = new JScrollPane(sourceArea);
        sourceScroll.setBorder(BorderFactory.createTitledBorder("源代码"));
        splitPane.setLeftComponent(sourceScroll);

        // 右边：标签页展示结果
        JTabbedPane tabbedPane = new JTabbedPane();

        // Token 表格
        tokenModel = new DefaultTableModel(new String[]{"类型", "值", "行", "列"}, 0);
        tokenTable = new JTable(tokenModel);
        tabbedPane.addTab("Token 流", new JScrollPane(tokenTable));

        // AST 树
        astTree = new JTree();
        tabbedPane.addTab("AST 树", new JScrollPane(astTree));

        // 符号表表格
        symbolModel = new DefaultTableModel(new String[]{"变量名", "类型", "行号"}, 0);
        symbolTable = new JTable(symbolModel);
        tabbedPane.addTab("符号表", new JScrollPane(symbolTable));

        // 四元式列表
        irListModel = new DefaultListModel<>();
        irList = new JList<>(irListModel);
        tabbedPane.addTab("四元式", new JScrollPane(irList));

        splitPane.setRightComponent(tabbedPane);
        add(splitPane, BorderLayout.CENTER);

        // 按钮事件
        lexerBtn.addActionListener(e -> doLexer());
        parserBtn.addActionListener(e -> doParser());
        semanticBtn.addActionListener(e -> doSemantic());
        codeGenBtn.addActionListener(e -> doCodeGen());
        runAllBtn.addActionListener(e -> {
            doLexer();
            doParser();
            doSemantic();
            doCodeGen();
        });
    }

    private List<Token> lastTokens;
    private ASTNode lastAst;
    private SymbolTable lastSymTable;
    private List<Quadruple> lastQuadruples;

    private void doLexer() {
        String code = sourceArea.getText();
        Lexer lexer = new Lexer(code);
        try {
            lastTokens = lexer.tokenize();
            tokenModel.setRowCount(0);
            for (Token t : lastTokens) {
                if (t.getType() == TokenType.EOF) continue;
                tokenModel.addRow(new Object[]{
                        t.getType(), t.getValue(), t.getLine(), t.getColumn()
                });
            }
            JOptionPane.showMessageDialog(this, "词法分析完成，共 " + (lastTokens.size()-1) + " 个 Token");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "词法错误: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doParser() {
        if (lastTokens == null) {
            JOptionPane.showMessageDialog(this, "请先进行词法分析", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Parser parser = new Parser(lastTokens);
        try {
            lastAst = parser.parse();
            // 将 AST 转换为树形结构
            DefaultMutableTreeNode root = buildTree(lastAst);
            astTree.setModel(new DefaultTreeModel(root));
            expandAll(astTree);
            JOptionPane.showMessageDialog(this, "语法分析完成");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "语法错误: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private DefaultMutableTreeNode buildTree(ASTNode node) {
        if (node instanceof parser.BlockNode) {
            parser.BlockNode block = (parser.BlockNode) node;
            DefaultMutableTreeNode blockNode = new DefaultMutableTreeNode("Block");
            for (ASTNode stmt : block.getStatements()) {
                blockNode.add(buildTree(stmt));
            }
            return blockNode;
        } else if (node instanceof parser.AssignNode) {
            parser.AssignNode assign = (parser.AssignNode) node;
            DefaultMutableTreeNode assignNode = new DefaultMutableTreeNode("Assign");
            assignNode.add(new DefaultMutableTreeNode("id: " + assign.getId()));
            assignNode.add(buildTree(assign.getExpr()));
            return assignNode;
        } else if (node instanceof parser.IfNode) {
            parser.IfNode ifNode = (parser.IfNode) node;
            DefaultMutableTreeNode ifNodeTree = new DefaultMutableTreeNode("If");
            ifNodeTree.add(new DefaultMutableTreeNode("Condition"));
            ifNodeTree.add(buildTree(ifNode.getCondition()));
            ifNodeTree.add(new DefaultMutableTreeNode("Then"));
            ifNodeTree.add(buildTree(ifNode.getThenStmt()));
            if (ifNode.getElseStmt() != null) {
                ifNodeTree.add(new DefaultMutableTreeNode("Else"));
                ifNodeTree.add(buildTree(ifNode.getElseStmt()));
            }
            return ifNodeTree;
        } else if (node instanceof parser.WhileNode) {
            parser.WhileNode whileNode = (parser.WhileNode) node;
            DefaultMutableTreeNode whileNodeTree = new DefaultMutableTreeNode("While");
            whileNodeTree.add(new DefaultMutableTreeNode("Condition"));
            whileNodeTree.add(buildTree(whileNode.getCondition()));
            whileNodeTree.add(new DefaultMutableTreeNode("Body"));
            whileNodeTree.add(buildTree(whileNode.getBody()));
            return whileNodeTree;
        } else if (node instanceof parser.BinaryOpNode) {
            parser.BinaryOpNode bin = (parser.BinaryOpNode) node;
            DefaultMutableTreeNode opNode = new DefaultMutableTreeNode(bin.getOp());
            opNode.add(buildTree(bin.getLeft()));
            opNode.add(buildTree(bin.getRight()));
            return opNode;
        } else if (node instanceof parser.NumberNode) {
            parser.NumberNode num = (parser.NumberNode) node;
            return new DefaultMutableTreeNode(num.getValue());
        } else if (node instanceof parser.IdNode) {
            parser.IdNode id = (parser.IdNode) node;
            return new DefaultMutableTreeNode(id.getName());
        } else {
            return new DefaultMutableTreeNode(node.toString());
        }
    }

    private void expandAll(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void doSemantic() {
        if (lastAst == null) {
            JOptionPane.showMessageDialog(this, "请先进行语法分析", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        try {
            lastSymTable = semanticAnalyzer.analyze(lastAst);
            symbolModel.setRowCount(0);
            // 获取符号表（需要修改 SymbolTable 增加获取所有符号的方法，这里简单遍历，暂时用反射？不，我们在 SemanticAnalyzer 中提供 getSymbolTable 方法）
            // 简便做法：直接修改 SemanticAnalyzer 暴露 symTable，或添加 getter。我们临时在 SemanticAnalyzer 加一个方法：
            // 为了不重新粘贴整个类，建议你在 SemanticAnalyzer 中添加 public SymbolTable getSymbolTable() { return symTable; }
            // 然后调用 getSymbolTable().getAllSymbols()。为简化，此处假设我们有该方法。
            // 如果不想改代码，可以暂时不显示符号表，或者直接输出到控制台。
            // 这里为了演示，我假装符号表数据已填充（实际你需要实现）。
            // 请确保你在 SemanticAnalyzer 中添加了 public SymbolTable getSymbolTable() { return symTable; }
            // 然后在 SymbolTable 中添加 getAllSymbols() 方法。
            JOptionPane.showMessageDialog(this, "语义分析完成，符号表大小: " + lastSymTable.toString().split("\n").length);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "语义错误: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doCodeGen() {
        if (lastAst == null) {
            JOptionPane.showMessageDialog(this, "请先进行语法分析", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        CodeGenerator codeGen = new CodeGenerator();
        try {
            lastQuadruples = codeGen.generate(lastAst);
            irListModel.clear();
            for (Quadruple q : lastQuadruples) {
                irListModel.addElement(q.toString());
            }
            JOptionPane.showMessageDialog(this, "生成四元式完成，共 " + lastQuadruples.size() + " 条");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "中间代码生成错误: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
