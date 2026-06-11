package parser;

import lexer.Token;
import lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private List<Token> tokens;
    private int pos;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    private Token peek() {
        if (pos >= tokens.size()) return null;
        return tokens.get(pos);
    }

    private Token consume(TokenType expected) {
        Token t = peek();
        if (t == null) throw new RuntimeException("Unexpected EOF, expected " + expected);
        if (t.getType() == expected) {
            pos++;
            return t;
        }
        throw new RuntimeException("Expected " + expected + " but got " + t.getType() + " at " + t.getLine() + ":" + t.getColumn());
    }

    private Token consume(TokenType expected, String value) {
        Token t = peek();
        if (t == null) throw new RuntimeException("Unexpected EOF, expected " + expected + "(" + value + ")");
        if (t.getType() == expected && t.getValue().equals(value)) {
            pos++;
            return t;
        }
        throw new RuntimeException("Expected " + expected + "(" + value + ") but got " + t + " at " + t.getLine() + ":" + t.getColumn());
    }

    private boolean match(TokenType type) {
        Token t = peek();
        return t != null && t.getType() == type;
    }

    private boolean match(TokenType type, String value) {
        Token t = peek();
        return t != null && t.getType() == type && t.getValue().equals(value);
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

    // stmt → id = expr ;
    //       | if ( bool ) stmt
    //       | if ( bool ) stmt else stmt
    //       | while ( bool ) stmt
    //       | block
    private ASTNode parseStmt() {
        if (match(TokenType.IDENTIFIER)) {
            Token idToken = consume(TokenType.IDENTIFIER);
            consume(TokenType.OPERATOR, "=");
            ASTNode expr = parseExpr();
            consume(TokenType.SEPARATOR, ";");
            return new AssignNode(idToken.getValue(), expr);
        } else if (match(TokenType.IF)) {
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
        } else if (match(TokenType.WHILE)) {
            consume(TokenType.WHILE);
            consume(TokenType.SEPARATOR, "(");
            ASTNode cond = parseBool();
            consume(TokenType.SEPARATOR, ")");
            ASTNode body = parseStmt();
            return new WhileNode(cond, body);
        } else if (match(TokenType.SEPARATOR, "{")) {
            return parseBlock();
        } else {
            throw new RuntimeException("Unexpected token at stmt: " + peek());
        }
    }

    // bool → expr < expr
    //       | expr <= expr
    //       | expr > expr
    //       | expr >= expr
    //       | expr
    private ASTNode parseBool() {
        ASTNode left = parseExpr();
        if (match(TokenType.OPERATOR)) {
            String op = peek().getValue();
            if (op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=")) {
                consume(TokenType.OPERATOR);
                ASTNode right = parseExpr();
                return new BinaryOpNode(op, left, right);
            }
        }
        return left;
    }

    // expr → term { (+|-) term }
    private ASTNode parseExpr() {
        ASTNode node = parseTerm();
        while (match(TokenType.OPERATOR)) {
            String op = peek().getValue();
            if (op.equals("+") || op.equals("-")) {
                consume(TokenType.OPERATOR);
                ASTNode right = parseTerm();
                node = new BinaryOpNode(op, node, right);
            } else {
                break;
            }
        }
        return node;
    }

    // term → factor { (*|/) factor }
    private ASTNode parseTerm() {
        ASTNode node = parseFactor();
        while (match(TokenType.OPERATOR)) {
            String op = peek().getValue();
            if (op.equals("*") || op.equals("/")) {
                consume(TokenType.OPERATOR);
                ASTNode right = parseFactor();
                node = new BinaryOpNode(op, node, right);
            } else {
                break;
            }
        }
        return node;
    }

    // factor → ( expr ) | NUM | ID
    private ASTNode parseFactor() {
        if (match(TokenType.SEPARATOR, "(")) {
            consume(TokenType.SEPARATOR, "(");
            ASTNode node = parseExpr();
            consume(TokenType.SEPARATOR, ")");
            return node;
        } else if (match(TokenType.NUMBER)) {
            Token num = consume(TokenType.NUMBER);
            return new NumberNode(Integer.parseInt(num.getValue()));
        } else if (match(TokenType.IDENTIFIER)) {
            Token id = consume(TokenType.IDENTIFIER);
            return new IdNode(id.getValue());
        } else {
            throw new RuntimeException("Unexpected token in factor: " + peek());
        }
    }
}