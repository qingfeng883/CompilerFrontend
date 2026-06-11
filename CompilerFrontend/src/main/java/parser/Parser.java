package parser;

import lexer.Token;
import lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private List<Token> tokens;
    private int pos;
    private Token currentToken;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        this.currentToken = peek();
    }

    private Token peek() {
        if (pos >= tokens.size()) return null;
        return tokens.get(pos);
    }

    private void advance() {
        pos++;
        if (pos < tokens.size()) {
            currentToken = tokens.get(pos);
        } else {
            currentToken = null;
        }
    }

    private Token consume(TokenType expected) {
        Token t = currentToken;
        if (t == null) throw new RuntimeException("Unexpected EOF, expected " + expected);
        if (t.getType() == expected) {
            advance();
            return t;
        }
        throw new RuntimeException("Expected " + expected + " but got " + t.getType() +
                " at " + t.getLine() + ":" + t.getColumn());
    }

    private Token consume(TokenType expected, String value) {
        Token t = currentToken;
        if (t == null) throw new RuntimeException("Unexpected EOF");
        if (t.getType() == expected && t.getValue().equals(value)) {
            advance();
            return t;
        }
        throw new RuntimeException("Expected " + expected + "(" + value + ") but got " + t);
    }

    private boolean match(TokenType type) {
        return currentToken != null && currentToken.getType() == type;
    }

    private boolean match(TokenType type, String value) {
        return currentToken != null && currentToken.getType() == type &&
                currentToken.getValue().equals(value);
    }

    // program → block
    public ASTNode parse() {
        return parseBlock();
    }

    // block → { stmts }
    private BlockNode parseBlock() {
        consume(TokenType.SEPARATOR, "{");
        List<ASTNode> stmts = parseStmts();
        consume(TokenType.SEPARATOR, "}");
        return new BlockNode(stmts);
    }

    // stmts → stmt stmts | ε
    private List<ASTNode> parseStmts() {
        List<ASTNode> stmts = new ArrayList<>();
        while (!match(TokenType.SEPARATOR, "}") && !match(TokenType.EOF)) {
            stmts.add(parseStmt());
        }
        return stmts;
    }

    // stmt → declaration | assignment | if_stmt | while_stmt | block
    private ASTNode parseStmt() {
        // 类型声明 (int, float, string, bool)
        if (isType()) {
            return parseDeclaration();
        }
        // if 语句
        else if (match(TokenType.IF)) {
            return parseIfStmt();
        }
        // while 语句
        else if (match(TokenType.WHILE)) {
            return parseWhileStmt();
        }
        // 代码块
        else if (match(TokenType.SEPARATOR, "{")) {
            return parseBlock();
        }
        // 赋值语句 (必须以 ID 开头)
        else if (match(TokenType.IDENTIFIER)) {
            return parseAssignment();
        }
        else {
            throw new RuntimeException("Unexpected token: " + currentToken);
        }
    }

    // 判断是否为类型关键字
    private boolean isType() {
        if (currentToken == null) return false;
        String value = currentToken.getValue();
        return value.equals("int") || value.equals("float") ||
                value.equals("string") || value.equals("bool");
    }

    // declaration → type id ; | type id = expr ;
    private DeclarationNode parseDeclaration() {
        String type = currentToken.getValue();
        consume(TokenType.IDENTIFIER);  // 消费类型关键字

        String id = currentToken.getValue();
        consume(TokenType.IDENTIFIER);

        ASTNode initExpr = null;
        if (match(TokenType.OPERATOR, "=")) {
            consume(TokenType.OPERATOR, "=");
            initExpr = parseExpr();
        }

        consume(TokenType.SEPARATOR, ";");
        return new DeclarationNode(type, id, initExpr);
    }

    // assignment → id = expr ;
    private AssignNode parseAssignment() {
        String id = currentToken.getValue();
        consume(TokenType.IDENTIFIER);
        consume(TokenType.OPERATOR, "=");
        ASTNode expr = parseExpr();
        consume(TokenType.SEPARATOR, ";");
        return new AssignNode(id, expr);
    }

    // if_stmt → if ( bool ) stmt | if ( bool ) stmt else stmt
    private IfNode parseIfStmt() {
        consume(TokenType.IF);
        consume(TokenType.SEPARATOR, "(");
        ASTNode cond = parseBool();
        consume(TokenType.SEPARATOR, ")");
        ASTNode thenStmt = parseStmt();
        ASTNode elseStmt = null;
        if (match(TokenType.ELSE)) {
            consume(TokenType.ELSE);
            elseStmt = parseStmt();
        }
        return new IfNode(cond, thenStmt, elseStmt);
    }

    // while_stmt → while ( bool ) stmt
    private WhileNode parseWhileStmt() {
        consume(TokenType.WHILE);
        consume(TokenType.SEPARATOR, "(");
        ASTNode cond = parseBool();
        consume(TokenType.SEPARATOR, ")");
        ASTNode body = parseStmt();
        return new WhileNode(cond, body);
    }

    // bool → expr rel_op expr | expr
    private ASTNode parseBool() {
        ASTNode left = parseExpr();
        if (isRelOp()) {
            String op = currentToken.getValue();
            consume(TokenType.OPERATOR);
            ASTNode right = parseExpr();
            return new BinaryOpNode(op, left, right);
        }
        return left;
    }

    private boolean isRelOp() {
        if (currentToken == null) return false;
        String op = currentToken.getValue();
        return op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=");
    }

    // expr → term expr_tail (消除左递归)
    private ASTNode parseExpr() {
        ASTNode node = parseTerm();
        return parseExprTail(node);
    }

    // expr_tail → + term expr_tail | - term expr_tail | ε
    private ASTNode parseExprTail(ASTNode left) {
        if (match(TokenType.OPERATOR)) {
            String op = currentToken.getValue();
            if (op.equals("+") || op.equals("-")) {
                consume(TokenType.OPERATOR);
                ASTNode right = parseTerm();
                ASTNode newNode = new BinaryOpNode(op, left, right);
                return parseExprTail(newNode);
            }
        }
        return left;
    }

    // term → factor term_tail (消除左递归)
    private ASTNode parseTerm() {
        ASTNode node = parseFactor();
        return parseTermTail(node);
    }

    // term_tail → * factor term_tail | / factor term_tail | ε
    private ASTNode parseTermTail(ASTNode left) {
        if (match(TokenType.OPERATOR)) {
            String op = currentToken.getValue();
            if (op.equals("*") || op.equals("/")) {
                consume(TokenType.OPERATOR);
                ASTNode right = parseFactor();
                ASTNode newNode = new BinaryOpNode(op, left, right);
                return parseTermTail(newNode);
            }
        }
        return left;
    }

    // factor → ( expr ) | ID | NUM
    private ASTNode parseFactor() {
        if (match(TokenType.SEPARATOR, "(")) {
            consume(TokenType.SEPARATOR, "(");
            ASTNode node = parseExpr();
            consume(TokenType.SEPARATOR, ")");
            return node;
        } else if (match(TokenType.NUMBER)) {
            Token num = consume(TokenType.NUMBER);
            // 支持浮点数
            String numStr = num.getValue();
            if (numStr.contains(".")) {
                return new NumberNode(Float.parseFloat(numStr));
            } else {
                return new NumberNode(Integer.parseInt(numStr));
            }
        } else if (match(TokenType.IDENTIFIER)) {
            Token id = consume(TokenType.IDENTIFIER);
            return new IdNode(id.getValue());
        } else {
            throw new RuntimeException("Unexpected token in factor: " + currentToken);
        }
    }
}