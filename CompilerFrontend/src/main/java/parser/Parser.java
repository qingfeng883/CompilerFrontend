package parser;

import lexer.Token;
import lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int pos;
    private Token currentToken;
    private final List<String> errors;
    private boolean hasError;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        this.errors = new ArrayList<>();
        this.hasError = false;
        if (!tokens.isEmpty()) {
            this.currentToken = tokens.get(0);
        } else {
            this.currentToken = null;
        }
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
        if (t == null) {
            addError("Unexpected EOF, expected " + expected);
            return createErrorToken(expected);
        }
        if (t.getType() == expected) {
            advance();
            return t;
        }
        addError("Expected " + expected + " but got " + t.getType() +
                " ('" + t.getValue() + "') at " + t.getLine() + ":" + t.getColumn());
        advance();
        return createErrorToken(expected);
    }

    private Token consume(TokenType expected, String value) {
        Token t = currentToken;
        if (t == null) {
            addError("Unexpected EOF, expected " + expected + "(" + value + ")");
            return createErrorToken(expected);
        }
        if (t.getType() == expected && t.getValue().equals(value)) {
            advance();
            return t;
        }
        addError("Expected " + expected + "(" + value + ") but got " + t.getType() +
                " ('" + t.getValue() + "') at " + t.getLine() + ":" + t.getColumn());
        advance();
        return createErrorToken(expected);
    }

    private boolean match(TokenType type) {
        return currentToken != null && currentToken.getType() == type;
    }

    private boolean match(TokenType type, String value) {
        return currentToken != null && currentToken.getType() == type &&
                currentToken.getValue().equals(value);
    }

    private void addError(String message) {
        errors.add("[ERROR] " + message);
        hasError = true;
    }

    private Token createErrorToken(TokenType type) {
        return new Token(type, "ERROR", -1, -1, -1, "Error token");
    }

    private void synchronize() {
        while (currentToken != null && !match(TokenType.EOF)) {
            if (match(TokenType.SEPARATOR, ";") ||
                    match(TokenType.SEPARATOR, "}") ||
                    match(TokenType.IF) ||
                    match(TokenType.WHILE) ||
                    match(TokenType.IDENTIFIER)) {
                return;
            }
            advance();
        }
    }

    private boolean isRelOp() {
        if (currentToken == null) return false;
        String op = currentToken.getValue();
        return op.equals("<") || op.equals("<=") ||
                op.equals(">") || op.equals(">=");
    }

    // ============ 解析方法 ============

    public ASTNode parse() {
        ASTNode node = parseBlock();
        if (currentToken != null && !match(TokenType.EOF)) {
            addError("Unexpected token after program: " + currentToken.getValue() +
                    " at " + currentToken.getLine() + ":" + currentToken.getColumn());
        }
        return node;
    }

    private BlockNode parseBlock() {
        int line = currentToken != null ? currentToken.getLine() : -1;
        int col = currentToken != null ? currentToken.getColumn() : -1;

        try {
            consume(TokenType.SEPARATOR, "{");
            List<ASTNode> stmts = parseStmts();
            consume(TokenType.SEPARATOR, "}");
            return new BlockNode(stmts, line, col);
        } catch (RuntimeException e) {
            synchronize();
            return new BlockNode(new ArrayList<>(), line, col);
        }
    }

    private List<ASTNode> parseStmts() {
        List<ASTNode> stmts = new ArrayList<>();

        while (currentToken != null &&
                !match(TokenType.SEPARATOR, "}") &&
                !match(TokenType.EOF)) {
            try {
                ASTNode stmt = parseStmt();
                if (stmt != null) {
                    stmts.add(stmt);
                }
            } catch (RuntimeException e) {
                addError("Error parsing statement: " + e.getMessage());
                synchronize();
                if (match(TokenType.SEPARATOR, ";")) {
                    advance();
                }
            }
        }
        return stmts;
    }

    private ASTNode parseStmt() {
        if (currentToken == null) {
            return null;
        }

        if (match(TokenType.SEPARATOR, "{")) {
            return parseBlock();
        }

        if (match(TokenType.IF)) {
            return parseIfStmt();
        }

        if (match(TokenType.WHILE)) {
            return parseWhileStmt();
        }

        if (match(TokenType.IDENTIFIER)) {
            return parseAssignment();
        }

        if (match(TokenType.SEPARATOR, ";")) {
            advance();
            return null;
        }

        if (currentToken != null) {
            addError("Unexpected token in statement: " + currentToken.getType() +
                    " ('" + currentToken.getValue() + "') at " +
                    currentToken.getLine() + ":" + currentToken.getColumn());
            advance();
            return null;
        }

        return null;
    }

    private AssignNode parseAssignment() {
        int line = currentToken.getLine();
        int col = currentToken.getColumn();
        String id = currentToken.getValue();
        consume(TokenType.IDENTIFIER);

        if (!match(TokenType.OPERATOR, "=")) {
            addError("Expected '=' in assignment at " + line + ":" + col);
            if (match(TokenType.SEPARATOR, ";") || match(TokenType.SEPARATOR, "}")) {
                return new AssignNode(id, new NumberNode(0, line, col), line, col);
            }
        }

        consume(TokenType.OPERATOR, "=");
        ASTNode expr = parseExpr();

        if (!match(TokenType.SEPARATOR, ";")) {
            addError("Expected ';' after assignment at " +
                    (currentToken != null ? currentToken.getLine() : "EOF") +
                    ":" + (currentToken != null ? currentToken.getColumn() : 0));
        }
        consume(TokenType.SEPARATOR, ";");

        return new AssignNode(id, expr, line, col);
    }

    private IfNode parseIfStmt() {
        int line = currentToken.getLine();
        int col = currentToken.getColumn();
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

        return new IfNode(cond, thenStmt, elseStmt, line, col);
    }

    private WhileNode parseWhileStmt() {
        int line = currentToken.getLine();
        int col = currentToken.getColumn();
        consume(TokenType.WHILE);

        consume(TokenType.SEPARATOR, "(");
        ASTNode cond = parseBool();
        consume(TokenType.SEPARATOR, ")");

        ASTNode body = parseStmt();

        return new WhileNode(cond, body, line, col);
    }

    private ASTNode parseBool() {
        ASTNode left = parseExpr();

        if (isRelOp()) {
            String op = currentToken.getValue();
            int line = currentToken.getLine();
            int col = currentToken.getColumn();
            consume(TokenType.OPERATOR);
            ASTNode right = parseExpr();
            return new BinaryOpNode(op, left, right, line, col);
        }

        return left;
    }

    private ASTNode parseExpr() {
        ASTNode node = parseTerm();

        while (match(TokenType.OPERATOR)) {
            String op = currentToken.getValue();
            if (op.equals("+") || op.equals("-")) {
                int line = currentToken.getLine();
                int col = currentToken.getColumn();
                consume(TokenType.OPERATOR);
                ASTNode right = parseTerm();
                node = new BinaryOpNode(op, node, right, line, col);
            } else {
                break;
            }
        }

        return node;
    }

    private ASTNode parseTerm() {
        ASTNode node = parseFactor();

        while (match(TokenType.OPERATOR)) {
            String op = currentToken.getValue();
            if (op.equals("*") || op.equals("/")) {
                int line = currentToken.getLine();
                int col = currentToken.getColumn();
                consume(TokenType.OPERATOR);
                ASTNode right = parseFactor();
                node = new BinaryOpNode(op, node, right, line, col);
            } else {
                break;
            }
        }

        return node;
    }

    private ASTNode parseFactor() {
        if (match(TokenType.SEPARATOR, "(")) {
            int line = currentToken.getLine();
            int col = currentToken.getColumn();
            consume(TokenType.SEPARATOR, "(");
            ASTNode node = parseExpr();
            consume(TokenType.SEPARATOR, ")");
            return node;
        }

        if (match(TokenType.NUMBER)) {
            Token num = consume(TokenType.NUMBER);
            try {
                int value = Integer.parseInt(num.getValue());
                return new NumberNode(value, num.getLine(), num.getColumn());
            } catch (NumberFormatException e) {
                addError("Invalid number format: " + num.getValue() +
                        " at " + num.getLine() + ":" + num.getColumn());
                return new NumberNode(0, num.getLine(), num.getColumn());
            }
        }

        if (match(TokenType.IDENTIFIER)) {
            Token id = consume(TokenType.IDENTIFIER);
            return new IdNode(id.getValue(), id.getLine(), id.getColumn());
        }

        if (currentToken != null) {
            addError("Unexpected token in factor: " + currentToken.getType() +
                    " ('" + currentToken.getValue() + "') at " +
                    currentToken.getLine() + ":" + currentToken.getColumn());
            advance();
            return new NumberNode(0, -1, -1);
        }

        addError("Unexpected end of input in factor");
        return new NumberNode(0, -1, -1);
    }

    // ============ 公开接口 ============

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return hasError || !errors.isEmpty();
    }

    public void printErrors() {
        for (String error : errors) {
            System.err.println(error);
        }
    }

    public int getPosition() {
        return pos;
    }

    public Token getCurrentToken() {
        return currentToken;
    }
}