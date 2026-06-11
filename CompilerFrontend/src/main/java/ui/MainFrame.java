package ui;

import ir.CodeGenerator;
import ir.Quadruple;
import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;
import parser.ASTNode;
import parser.Parser;
import semantic.SemanticAnalyzer;
import semantic.Symbol;
import semantic.SymbolTable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {
    private JTextArea sourceArea;
    private JTable tokenTable;
    private TreePanel treePanel;
    private JTable symbolTable;
    private JList<String> irList;
    private DefaultTableModel tokenModel;
    private DefaultTableModel symbolModel;
    private DefaultListModel<String> irListModel;

    private List<Token> lastTokens;
    private ASTNode lastAst;
    private SymbolTable lastSymTable;
    private List<Quadruple> lastQuadruples;

    public MainFrame() {
        setTitle("编译器前端系统 - 语法树图形化显示");
        setSize(1400, 900);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 顶部工具栏
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(new Color(240, 240, 240));

        JButton lexerBtn = createStyledButton("词法分析", new Color(70, 130, 180));
        JButton parserBtn = createStyledButton("语法分析", new Color(60, 120, 100));
        JButton semanticBtn = createStyledButton("语义分析", new Color(180, 120, 50));
        JButton codeGenBtn = createStyledButton("生成四元式", new Color(120, 80, 150));
        JButton runAllBtn = createStyledButton("一键运行", new Color(180, 60, 80));

        toolBar.add(lexerBtn);
        toolBar.add(parserBtn);
        toolBar.add(semanticBtn);
        toolBar.add(codeGenBtn);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(runAllBtn);
        add(toolBar, BorderLayout.NORTH);

        // 中间分屏
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.35);
        splitPane.setDividerSize(5);

        // 左边：源代码编辑区
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 150), 1),
                "源代码",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 12)
        ));

        sourceArea = new JTextArea();
        sourceArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        sourceArea.setBackground(new Color(252, 252, 252));
        sourceArea.setText(
                "{\n" +
                        "    x = 0;\n" +
                        "    if (x < 3) {\n" +
                        "        if (x > 0) {\n" +
                        "            x = 5;\n" +
                        "        } else {\n" +
                        "            x = 6;\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n"
        );
        JScrollPane sourceScroll = new JScrollPane(sourceArea);
        sourceScroll.setBorder(null);
        leftPanel.add(sourceScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);

        // 右边：标签页
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        // Token 表格
        tokenModel = new DefaultTableModel(new String[]{"类型", "值", "行", "列"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tokenTable = new JTable(tokenModel);
        tokenTable.setFont(new Font("Consolas", Font.PLAIN, 12));
        tokenTable.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 12));
        tokenTable.setRowHeight(25);
        JScrollPane tokenScroll = new JScrollPane(tokenTable);
        tabbedPane.addTab("Token 流", tokenScroll);

        // AST 树 - 图形化面板
        treePanel = new TreePanel();
        JScrollPane treeScroll = new JScrollPane(treePanel);
        treeScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        treeScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        treeScroll.getViewport().setBackground(Color.WHITE);
        tabbedPane.addTab("AST 树", treeScroll);

        // 符号表表格
        symbolModel = new DefaultTableModel(new String[]{"变量名", "类型", "行号"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        symbolTable = new JTable(symbolModel);
        symbolTable.setFont(new Font("Consolas", Font.PLAIN, 12));
        symbolTable.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 12));
        symbolTable.setRowHeight(25);
        JScrollPane symbolScroll = new JScrollPane(symbolTable);
        tabbedPane.addTab("符号表", symbolScroll);

        // 四元式列表
        irListModel = new DefaultListModel<>();
        irList = new JList<>(irListModel);
        irList.setFont(new Font("Consolas", Font.PLAIN, 13));
        irList.setBackground(new Color(252, 252, 252));
        JScrollPane irScroll = new JScrollPane(irList);
        tabbedPane.addTab("四元式", irScroll);

        splitPane.setRightComponent(tabbedPane);
        add(splitPane, BorderLayout.CENTER);

        // 状态栏
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusBar.setBackground(new Color(230, 230, 240));
        JLabel statusLabel = new JLabel(" 就绪");
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        statusBar.add(statusLabel, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);

        // 按钮事件
        lexerBtn.addActionListener(e -> doLexer(statusLabel));
        parserBtn.addActionListener(e -> doParser(statusLabel));
        semanticBtn.addActionListener(e -> doSemantic(statusLabel));
        codeGenBtn.addActionListener(e -> doCodeGen(statusLabel));
        runAllBtn.addActionListener(e -> {
            doLexer(statusLabel);
            doParser(statusLabel);
            doSemantic(statusLabel);
            doCodeGen(statusLabel);
        });
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(100, 32));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });
        return button;
    }

    private void doLexer(JLabel statusLabel) {
        String code = sourceArea.getText();
        Lexer lexer = new Lexer(code);
        try {
            lastTokens = lexer.tokenize();
            tokenModel.setRowCount(0);
            int count = 0;
            for (Token t : lastTokens) {
                if (t.getType() == TokenType.EOF) continue;
                tokenModel.addRow(new Object[]{
                        t.getType(), t.getValue(), t.getLine(), t.getColumn()
                });
                count++;
            }
            statusLabel.setText(" 词法分析完成，共 " + count + " 个 Token");
            JOptionPane.showMessageDialog(this, "词法分析完成，共 " + count + " 个 Token", "完成", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            statusLabel.setText(" 词法错误: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "词法错误: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doParser(JLabel statusLabel) {
        if (lastTokens == null) {
            JOptionPane.showMessageDialog(this, "请先进行词法分析", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Parser parser = new Parser(lastTokens);
        try {
            lastAst = parser.parse();

            System.out.println("\n=== AST 结构 ===");
            printAST(lastAst, 0);

            treePanel.setAST(lastAst);
            treePanel.revalidate();
            treePanel.repaint();

            statusLabel.setText(" 语法分析完成");
            JOptionPane.showMessageDialog(this, "语法分析完成", "完成", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText(" 语法错误: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "语法错误: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printAST(ASTNode node, int depth) {
        if (node == null) return;

        // 修复：使用 String 而不是 StringBuilder 进行拼接
        String indent = "";
        for (int i = 0; i < depth; i++) {
            indent = indent + "  ";
        }

        if (node instanceof parser.BlockNode) {
            System.out.println(indent + "Block");
            parser.BlockNode block = (parser.BlockNode) node;
            for (ASTNode stmt : block.getStatements()) {
                printAST(stmt, depth + 1);
            }
        } else if (node instanceof parser.AssignNode) {
            parser.AssignNode assign = (parser.AssignNode) node;
            System.out.println(indent + "= " + assign.getId());
            printAST(assign.getExpr(), depth + 1);
        } else if (node instanceof parser.IfNode) {
            parser.IfNode ifNode = (parser.IfNode) node;
            System.out.println(indent + "if");
            System.out.println(indent + "  C:");
            printAST(ifNode.getCondition(), depth + 2);
            System.out.println(indent + "  then:");
            printAST(ifNode.getThenStmt(), depth + 2);
            if (ifNode.getElseStmt() != null) {
                System.out.println(indent + "  else:");
                printAST(ifNode.getElseStmt(), depth + 2);
            }
        } else if (node instanceof parser.WhileNode) {
            parser.WhileNode whileNode = (parser.WhileNode) node;
            System.out.println(indent + "while");
            System.out.println(indent + "  C:");
            printAST(whileNode.getCondition(), depth + 2);
            System.out.println(indent + "  Body:");
            printAST(whileNode.getBody(), depth + 2);
        } else if (node instanceof parser.BinaryOpNode) {
            parser.BinaryOpNode bin = (parser.BinaryOpNode) node;
            System.out.println(indent + bin.getOp());
            printAST(bin.getLeft(), depth + 1);
            printAST(bin.getRight(), depth + 1);
        } else if (node instanceof parser.NumberNode) {
            System.out.println(indent + ((parser.NumberNode) node).getValue());
        } else if (node instanceof parser.IdNode) {
            System.out.println(indent + ((parser.IdNode) node).getName());
        } else {
            System.out.println(indent + node.getClass().getSimpleName());
        }
    }

    private void doSemantic(JLabel statusLabel) {
        if (lastAst == null) {
            JOptionPane.showMessageDialog(this, "请先进行语法分析", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        try {
            lastSymTable = semanticAnalyzer.analyze(lastAst);
            symbolModel.setRowCount(0);

            List<Symbol> symbols = lastSymTable.getAllSymbols();
            System.out.println("符号表内容: " + symbols.size() + " 个符号");

            for (Symbol sym : symbols) {
                symbolModel.addRow(new Object[]{
                        sym.getName(),
                        sym.getType(),
                        sym.getLine() == -1 ? "自动声明" : sym.getLine()
                });
            }

            statusLabel.setText(" 语义分析完成，共 " + symbols.size() + " 个符号");
            JOptionPane.showMessageDialog(this, "语义分析完成，共 " + symbols.size() + " 个符号", "完成", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText(" 语义错误: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "语义错误: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doCodeGen(JLabel statusLabel) {
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
            statusLabel.setText(" 生成四元式完成，共 " + lastQuadruples.size() + " 条");
            JOptionPane.showMessageDialog(this, "生成四元式完成，共 " + lastQuadruples.size() + " 条", "完成", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            statusLabel.setText(" 中间代码生成错误: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "中间代码生成错误: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new MainFrame().setVisible(true);
        });
    }
}