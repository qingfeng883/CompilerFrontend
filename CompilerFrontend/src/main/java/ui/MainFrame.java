package ui;

import ir.CodeGenerator;
import ir.Quadruple;
import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;
import parser.*;
import semantic.SemanticAnalyzer;
import semantic.Symbol;
import semantic.SymbolTable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    private JTextArea errorArea;
    private JTabbedPane tabbedPane;
    private JLabel statusLabel;
    private String currentFilePath = null;

    // ========== 中文字体缓存 ==========
    private Font chineseFont;
    private Font chineseBoldFont;
    private Font monospaceFont;

    public MainFrame() {
        setTitle("编译器前端系统 - 完整的词法/语法/语义分析");
        setSize(1400, 900);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initFonts();
        initMenuBar();
        initUI();
    }

    // ========== 获取支持中文的字体 ==========
    private Font getChineseFont(int style, int size) {
        String[] fontNames = {"微软雅黑", "Microsoft YaHei", "宋体", "SimSun", "Serif"};
        for (String name : fontNames) {
            Font f = new Font(name, style, size);
            if (f.getFamily() != null && !f.getFamily().equalsIgnoreCase("Serif")) {
                return f;
            }
        }
        return new Font("Serif", style, size);
    }

    // ========== 初始化字体 ==========
    private void initFonts() {
        chineseFont = getChineseFont(Font.PLAIN, 12);
        chineseBoldFont = getChineseFont(Font.BOLD, 12);
        monospaceFont = new Font("Consolas", Font.PLAIN, 13);
        if (!monospaceFont.canDisplay('中')) {
            monospaceFont = getChineseFont(Font.PLAIN, 13);
        }
    }

    /**
     * 初始化菜单栏
     */
    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // ========== 文件菜单 ==========
        JMenu fileMenu = new JMenu("文件");
        fileMenu.setFont(chineseFont);

        JMenuItem openItem = new JMenuItem("打开源文件");
        openItem.setFont(chineseFont);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        openItem.addActionListener(e -> openSourceFile());

        JMenuItem saveItem = new JMenuItem("保存源代码");
        saveItem.setFont(chineseFont);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> saveSourceFile());

        JMenuItem exportItem = new JMenuItem("导出四元式");
        exportItem.setFont(chineseFont);
        exportItem.addActionListener(e -> exportQuadruples());

        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.setFont(chineseFont);
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // ========== 视图菜单 ==========
        JMenu viewMenu = new JMenu("视图");
        viewMenu.setFont(chineseFont);

        JMenuItem resetViewItem = new JMenuItem("重置语法树视图");
        resetViewItem.setFont(chineseFont);
        resetViewItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, KeyEvent.CTRL_DOWN_MASK));
        resetViewItem.addActionListener(e -> {
            if (treePanel != null) {
                treePanel.resetView();
            }
        });

        JMenuItem zoomInItem = new JMenuItem("放大语法树");
        zoomInItem.setFont(chineseFont);
        zoomInItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK));
        zoomInItem.addActionListener(e -> {
            if (treePanel != null) {
                treePanel.zoomIn();
            }
        });

        JMenuItem zoomOutItem = new JMenuItem("缩小语法树");
        zoomOutItem.setFont(chineseFont);
        zoomOutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK));
        zoomOutItem.addActionListener(e -> {
            if (treePanel != null) {
                treePanel.zoomOut();
            }
        });

        viewMenu.add(resetViewItem);
        viewMenu.addSeparator();
        viewMenu.add(zoomInItem);
        viewMenu.add(zoomOutItem);
        menuBar.add(viewMenu);

        // ========== 运行菜单 ==========
        JMenu runMenu = new JMenu("运行");
        runMenu.setFont(chineseFont);

        JMenuItem lexerItem = new JMenuItem("词法分析");
        lexerItem.setFont(chineseFont);
        lexerItem.addActionListener(e -> doLexer(statusLabel));

        JMenuItem parserItem = new JMenuItem("语法分析");
        parserItem.setFont(chineseFont);
        parserItem.addActionListener(e -> doParser(statusLabel));

        JMenuItem semanticItem = new JMenuItem("语义分析");
        semanticItem.setFont(chineseFont);
        semanticItem.addActionListener(e -> doSemantic(statusLabel));

        JMenuItem codeGenItem = new JMenuItem("生成四元式");
        codeGenItem.setFont(chineseFont);
        codeGenItem.addActionListener(e -> doCodeGen(statusLabel));

        JMenuItem runAllItem = new JMenuItem("一键运行全部");
        runAllItem.setFont(chineseFont);
        runAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        runAllItem.addActionListener(e -> {
            doLexer(statusLabel);
            doParser(statusLabel);
            doSemantic(statusLabel);
            doCodeGen(statusLabel);
        });

        runMenu.add(lexerItem);
        runMenu.add(parserItem);
        runMenu.add(semanticItem);
        runMenu.add(codeGenItem);
        runMenu.addSeparator();
        runMenu.add(runAllItem);
        menuBar.add(runMenu);

        // ========== 帮助菜单 ==========
        JMenu helpMenu = new JMenu("帮助");
        helpMenu.setFont(chineseFont);

        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.setFont(chineseFont);
        aboutItem.addActionListener(e -> showAboutDialog());

        JMenuItem shortcutItem = new JMenuItem("快捷键说明");
        shortcutItem.setFont(chineseFont);
        shortcutItem.addActionListener(e -> showShortcutHelp());

        helpMenu.add(aboutItem);
        helpMenu.add(shortcutItem);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    // ==================== 文件操作 ====================

    /**
     * 打开源文件
     */
    private void openSourceFile() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("选择源程序文件");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "源程序文件 (*.c;*.txt;*.java;*.mini)", "c", "txt", "java", "mini"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String filePath = chooser.getSelectedFile().getAbsolutePath();
                String content = new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");
                sourceArea.setText(content);
                currentFilePath = filePath;
                setTitle("编译器前端系统 - " + chooser.getSelectedFile().getName());
                statusLabel.setText(" 已加载文件: " + chooser.getSelectedFile().getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "读取文件失败: " + ex.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 保存源代码到文件
     */
    private void saveSourceFile() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("保存源程序文件");

        if (currentFilePath != null) {
            chooser.setSelectedFile(new java.io.File(currentFilePath));
        }

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String filePath = chooser.getSelectedFile().getAbsolutePath();
                if (!filePath.contains(".")) {
                    filePath += ".txt";
                }
                Files.write(Paths.get(filePath), sourceArea.getText().getBytes("UTF-8"));
                currentFilePath = filePath;
                setTitle("编译器前端系统 - " + chooser.getSelectedFile().getName());
                statusLabel.setText(" 已保存文件: " + chooser.getSelectedFile().getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "保存文件失败: " + ex.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 导出四元式到文件
     */
    private void exportQuadruples() {
        if (lastQuadruples == null || lastQuadruples.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "请先生成四元式",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("导出四元式");

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String filePath = chooser.getSelectedFile().getAbsolutePath();
                if (!filePath.endsWith(".txt")) {
                    filePath += ".txt";
                }

                StringBuilder sb = new StringBuilder();
                sb.append("========== 四元式序列 ==========\n");
                for (Quadruple q : lastQuadruples) {
                    sb.append(q.toString()).append("\n");
                }
                sb.append("================================\n");

                Files.write(Paths.get(filePath), sb.toString().getBytes("UTF-8"));
                JOptionPane.showMessageDialog(this,
                        "四元式已导出到: " + filePath,
                        "导出成功", JOptionPane.INFORMATION_MESSAGE);
                statusLabel.setText(" 四元式已导出");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "导出失败: " + ex.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ==================== 帮助对话框 ====================

    /**
     * 显示关于对话框
     */
    private void showAboutDialog() {
        String message = "编译器前端系统\n" +
                "版本: 1.0\n" +
                "功能: 词法分析 | 语法分析 | 语义分析 | 四元式生成\n" +
                "基于递归下降分析法\n";
        JOptionPane.showMessageDialog(this, message, "关于", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 显示快捷键帮助
     */
    private void showShortcutHelp() {
        String message = "快捷键说明:\n\n" +
                "文件操作:\n" +
                "  Ctrl+O  - 打开源文件\n" +
                "  Ctrl+S  - 保存源文件\n\n" +
                "运行:\n" +
                "  F5      - 一键运行全部\n\n" +
                "语法树视图:\n" +
                "  Ctrl+滚轮 - 缩放语法树\n" +
                "  鼠标拖拽   - 平移语法树\n" +
                "  Ctrl+0   - 重置语法树视图\n" +
                "  Ctrl+=   - 放大语法树\n" +
                "  Ctrl+-   - 缩小语法树";
        JOptionPane.showMessageDialog(this, message, "快捷键说明", JOptionPane.INFORMATION_MESSAGE);
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
        JButton clearBtn = createStyledButton("清空所有", new Color(100, 100, 100));

        toolBar.add(lexerBtn);
        toolBar.add(parserBtn);
        toolBar.add(semanticBtn);
        toolBar.add(codeGenBtn);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(runAllBtn);
        toolBar.add(clearBtn);
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
                chineseBoldFont
        ));

        sourceArea = new JTextArea();
        sourceArea.setFont(monospaceFont);
        sourceArea.setBackground(new Color(252, 252, 252));
        sourceArea.setText(
                "{\n" +
                        "    x = 0;\n" +
                        "    y = 1;\n" +
                        "    if (x < 3) {\n" +
                        "        y = x + 1;\n" +
                        "    } else {\n" +
                        "        y = x - 1;\n" +
                        "    }\n" +
                        "    while (y < 10) {\n" +
                        "        y = y + 1;\n" +
                        "    }\n" +
                        "}\n"
        );
        JScrollPane sourceScroll = new JScrollPane(sourceArea);
        sourceScroll.setBorder(null);
        leftPanel.add(sourceScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);

        // 右边：标签页
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(chineseFont);

        // Token 表格
        tokenModel = new DefaultTableModel(new String[]{"种别码", "类型", "值", "行", "列"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tokenTable = new JTable(tokenModel);
        tokenTable.setFont(monospaceFont);
        tokenTable.getTableHeader().setFont(chineseBoldFont);
        tokenTable.setRowHeight(25);
        tokenTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        tokenTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        tokenTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        tokenTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        tokenTable.getColumnModel().getColumn(4).setPreferredWidth(50);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setFont(monospaceFont);
        tokenTable.setDefaultRenderer(Object.class, renderer);

        JScrollPane tokenScroll = new JScrollPane(tokenTable);
        tabbedPane.addTab("Token 流", tokenScroll);

        // AST 树
        treePanel = new TreePanel();
        treePanel.setFont(chineseFont);
        JScrollPane treeScroll = new JScrollPane(treePanel);
        treeScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        treeScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        treeScroll.getViewport().setBackground(Color.WHITE);
        tabbedPane.addTab("AST 树", treeScroll);

        // 符号表表格 - 显示变量名、类型和值
        symbolModel = new DefaultTableModel(new String[]{"变量名", "类型", "值"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        symbolTable = new JTable(symbolModel);
        symbolTable.setFont(monospaceFont);
        symbolTable.getTableHeader().setFont(chineseBoldFont);
        symbolTable.setRowHeight(25);
        symbolTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        symbolTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        symbolTable.getColumnModel().getColumn(2).setPreferredWidth(200);

        DefaultTableCellRenderer symbolRenderer = new DefaultTableCellRenderer();
        symbolRenderer.setFont(monospaceFont);
        symbolTable.setDefaultRenderer(Object.class, symbolRenderer);

        JScrollPane symbolScroll = new JScrollPane(symbolTable);
        tabbedPane.addTab("符号表", symbolScroll);

        // 四元式列表
        irListModel = new DefaultListModel<>();
        irList = new JList<>(irListModel);
        irList.setFont(monospaceFont);
        irList.setBackground(new Color(252, 252, 252));

        irList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setFont(monospaceFont);
                return label;
            }
        });

        JScrollPane irScroll = new JScrollPane(irList);
        tabbedPane.addTab("四元式", irScroll);

        // 错误信息区域
        errorArea = new JTextArea();
        errorArea.setFont(monospaceFont);
        errorArea.setForeground(Color.RED);
        errorArea.setEditable(false);
        errorArea.setBackground(new Color(255, 245, 245));
        JScrollPane errorScroll = new JScrollPane(errorArea);
        errorScroll.setPreferredSize(new Dimension(0, 150));
        tabbedPane.addTab("错误信息", errorScroll);

        splitPane.setRightComponent(tabbedPane);
        add(splitPane, BorderLayout.CENTER);

        // 状态栏
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusBar.setBackground(new Color(230, 230, 240));
        statusLabel = new JLabel(" 就绪");
        statusLabel.setFont(chineseFont);
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
        clearBtn.addActionListener(e -> doClear());
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(chineseBoldFont);
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

    // ==================== 词法分析 ====================
    private void doLexer(JLabel statusLabel) {
        String code = sourceArea.getText();
        Lexer lexer = new Lexer(code);
        try {
            lastTokens = lexer.tokenize();
            tokenModel.setRowCount(0);

            // 追加分隔线
            errorArea.append("\n========== 词法分析 ==========\n");

            int count = 0;
            for (Token t : lastTokens) {
                if (t.getType() == TokenType.EOF) continue;
                tokenModel.addRow(new Object[]{
                        t.getCode(),
                        t.getCategory(),
                        t.getValue(),
                        t.getLine(),
                        t.getColumn()
                });
                count++;
            }

            if (lexer.hasErrors()) {
                StringBuilder errorMsg = new StringBuilder();
                int errorIndex = 1;
                for (String err : lexer.getErrors()) {
                    errorMsg.append(errorIndex++).append(". ").append(err).append("\n");
                }
                errorArea.append(errorMsg.toString());
                statusLabel.setText(" 词法分析完成: " + count + " 个Token, " + lexer.getErrors().size() + " 个错误");
                JOptionPane.showMessageDialog(this,
                        "词法分析完成\nToken: " + count + "\n错误: " + lexer.getErrors().size(),
                        "完成", JOptionPane.WARNING_MESSAGE);
            } else {
                errorArea.append("词法分析成功: " + count + " 个Token, 无错误\n");
                statusLabel.setText(" 词法分析完成: " + count + " 个Token, 无错误");
                JOptionPane.showMessageDialog(this, "词法分析完成: " + count + " 个Token", "完成", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            statusLabel.setText(" 词法分析异常: " + ex.getMessage());
            errorArea.append("异常: " + ex.getMessage() + "\n");
            JOptionPane.showMessageDialog(this, "词法分析异常: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== 语法分析 ====================
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

            // 追加分隔线
            errorArea.append("\n========== 语法分析 ==========\n");

            if (parser.hasErrors()) {
                StringBuilder errorMsg = new StringBuilder();
                int errorIndex = 1;
                for (String err : parser.getErrors()) {
                    errorMsg.append(errorIndex++).append(". ").append(err).append("\n");
                }
                errorArea.append(errorMsg.toString());
                statusLabel.setText(" 语法分析完成: " + parser.getErrors().size() + " 个错误");
                JOptionPane.showMessageDialog(this,
                        "语法分析完成\n错误: " + parser.getErrors().size(),
                        "完成", JOptionPane.WARNING_MESSAGE);
            } else {
                errorArea.append("语法分析成功，无错误\n");
                statusLabel.setText(" 语法分析完成: 无错误");
                JOptionPane.showMessageDialog(this, "语法分析完成，无错误", "完成", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText(" 语法错误: " + ex.getMessage());
            errorArea.append("语法错误: " + ex.getMessage() + "\n");
            tabbedPane.setSelectedIndex(tabbedPane.indexOfTab("错误信息"));
            JOptionPane.showMessageDialog(this, "语法错误: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printAST(ASTNode node, int depth) {
        if (node == null) return;

        String indent = "  ".repeat(depth);

        if (node instanceof BlockNode) {
            BlockNode block = (BlockNode) node;
            System.out.println(indent + "Block (行 " + block.getLine() + ")");
            for (ASTNode stmt : block.getStatements()) {
                printAST(stmt, depth + 1);
            }
        } else if (node instanceof AssignNode) {
            AssignNode assign = (AssignNode) node;
            System.out.println(indent + "= " + assign.getId() + " (行 " + assign.getLine() + ")");
            printAST(assign.getExpr(), depth + 1);
        } else if (node instanceof IfNode) {
            IfNode ifNode = (IfNode) node;
            System.out.println(indent + "if (行 " + ifNode.getLine() + ")");
            System.out.println(indent + "  C:");
            printAST(ifNode.getCondition(), depth + 2);
            System.out.println(indent + "  then:");
            printAST(ifNode.getThenStmt(), depth + 2);
            if (ifNode.getElseStmt() != null) {
                System.out.println(indent + "  else:");
                printAST(ifNode.getElseStmt(), depth + 2);
            }
        } else if (node instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) node;
            System.out.println(indent + "while (行 " + whileNode.getLine() + ")");
            System.out.println(indent + "  C:");
            printAST(whileNode.getCondition(), depth + 2);
            System.out.println(indent + "  Body:");
            printAST(whileNode.getBody(), depth + 2);
        } else if (node instanceof BinaryOpNode) {
            BinaryOpNode bin = (BinaryOpNode) node;
            System.out.println(indent + bin.getOp() + " (行 " + bin.getLine() + ")");
            printAST(bin.getLeft(), depth + 1);
            printAST(bin.getRight(), depth + 1);
        } else if (node instanceof NumberNode) {
            NumberNode num = (NumberNode) node;
            System.out.println(indent + num.getValue() + " (行 " + num.getLine() + ")");
        } else if (node instanceof IdNode) {
            IdNode id = (IdNode) node;
            System.out.println(indent + id.getName() + " (行 " + id.getLine() + ")");
        } else {
            System.out.println(indent + node.getClass().getSimpleName());
        }
    }

    // ==================== 语义分析 ====================
    private void doSemantic(JLabel statusLabel) {
        if (lastAst == null) {
            JOptionPane.showMessageDialog(this, "请先进行语法分析", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
            lastSymTable = semanticAnalyzer.analyze(lastAst);

            // 清空符号表表格
            symbolModel.setRowCount(0);

            // 获取所有符号
            List<Symbol> symbols = lastSymTable.getAllSymbols();

            System.out.println("符号表内容: " + symbols.size() + " 个符号");
            for (Symbol sym : symbols) {
                System.out.println("  " + sym);
            }

            // 显示到UI - 包含类型
            for (Symbol sym : symbols) {
                symbolModel.addRow(new Object[]{
                        sym.getName(),
                        sym.getType() != null ? sym.getType() : "int",
                        sym.getValue() != null ? sym.getValue().toString() : "NULL"
                });
            }

            // 追加分隔线
            errorArea.append("\n========== 语义分析 ==========\n");

            // 显示语义错误
            if (semanticAnalyzer.hasErrors()) {
                StringBuilder errorMsg = new StringBuilder();
                int errorIndex = 1;
                for (String err : semanticAnalyzer.getErrors()) {
                    errorMsg.append(errorIndex++).append(". ").append(err).append("\n");
                }
                errorArea.append(errorMsg.toString());
                statusLabel.setText(" 语义分析完成: " + symbols.size() + " 个符号, " + semanticAnalyzer.getErrors().size() + " 个错误");
                JOptionPane.showMessageDialog(this,
                        "语义分析完成\n符号: " + symbols.size() + "\n错误: " + semanticAnalyzer.getErrors().size(),
                        "完成", JOptionPane.WARNING_MESSAGE);
            } else {
                errorArea.append("语义分析成功: " + symbols.size() + " 个符号, 无错误\n");
                statusLabel.setText(" 语义分析完成: " + symbols.size() + " 个符号, 无错误");
                JOptionPane.showMessageDialog(this, "语义分析完成: " + symbols.size() + " 个符号", "完成", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText(" 语义错误: " + ex.getMessage());
            errorArea.append("语义错误: " + ex.getMessage() + "\n");
            tabbedPane.setSelectedIndex(tabbedPane.indexOfTab("错误信息"));
            JOptionPane.showMessageDialog(this, "语义错误: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== 中间代码生成 ====================
    private void doCodeGen(JLabel statusLabel) {
        if (lastAst == null) {
            JOptionPane.showMessageDialog(this, "请先进行语法分析", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            CodeGenerator codeGen = new CodeGenerator();
            lastQuadruples = codeGen.generate(lastAst);

            irListModel.clear();
            for (Quadruple q : lastQuadruples) {
                irListModel.addElement(q.toString());
            }

            // 追加分隔线
            errorArea.append("\n========== 四元式生成 ==========\n");
            errorArea.append("生成四元式完成: " + lastQuadruples.size() + " 条\n");

            statusLabel.setText(" 生成四元式完成: " + lastQuadruples.size() + " 条");
            JOptionPane.showMessageDialog(this, "生成四元式完成: " + lastQuadruples.size() + " 条", "完成", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            statusLabel.setText(" 中间代码生成错误: " + ex.getMessage());
            errorArea.append("中间代码生成错误: " + ex.getMessage() + "\n");
            tabbedPane.setSelectedIndex(tabbedPane.indexOfTab("错误信息"));
            JOptionPane.showMessageDialog(this, "中间代码生成错误: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== 清空所有 ====================
    private void doClear() {
        tokenModel.setRowCount(0);
        symbolModel.setRowCount(0);
        irListModel.clear();
        errorArea.setText("");  // 清空错误区域
        treePanel.setAST(null);
        treePanel.repaint();

        lastTokens = null;
        lastAst = null;
        lastSymTable = null;
        lastQuadruples = null;

        statusLabel.setText(" 已清空所有分析结果");
        JOptionPane.showMessageDialog(this, "已清空所有分析结果", "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    // ==================== 主函数 ====================
    public static void main(String[] args) {
        // 解决中文乱码问题
        System.setProperty("file.encoding", "UTF-8");

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