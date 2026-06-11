package lexer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Lexer {
    private final String source;
    private int pos;
    private int line;
    private int col;
    private final List<Token> tokens;
    private static final Set<String> keywords = new HashSet<>();

    static {
        keywords.add("if");
        keywords.add("int");
        keywords.add("while");
        keywords.add("do");
        keywords.add("else");
        keywords.add("return");
        keywords.add("continue");
    }

    public Lexer(String source) {
        this.source = source;
        this.pos = 0;
        this.line = 1;
        this.col = 1;
        this.tokens = new ArrayList<>();
    }

    public List<Token> tokenize() {
        while (pos < source.length()) {
            char current = peekChar();
            if (Character.isWhitespace(current)) {
                skipWhitespace();
                continue;
            }
            if (Character.isLetter(current)) {
                readIdentifierOrKeyword();
            } else if (Character.isDigit(current)) {
                readNumber();
            } else if (isOperatorChar(current)) {
                readOperator();
            } else if (isSeparatorChar(current)) {
                readSeparator();
            } else {
                throw new RuntimeException("非法字符 '" + current + "' 在 " + line + ":" + col);
            }
        }
        tokens.add(new Token(TokenType.EOF, "", line, col));
        return tokens;
    }

    private char peekChar() {
        if (pos >= source.length()) return '\0';
        return source.charAt(pos);
    }

    private void advance() {
        if (peekChar() == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        pos++;
    }

    private void skipWhitespace() {
        while (pos < source.length() && Character.isWhitespace(peekChar())) {
            advance();
        }
    }

    private void readIdentifierOrKeyword() {
        int startLine = line;
        int startCol = col;
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && (Character.isLetterOrDigit(peekChar()))) {
            sb.append(peekChar());
            advance();
        }
        String word = sb.toString();
        TokenType type = keywords.contains(word) ? TokenType.valueOf(word.toUpperCase()) : TokenType.IDENTIFIER;
        tokens.add(new Token(type, word, startLine, startCol));
    }

    private void readNumber() {
        int startLine = line;
        int startCol = col;
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && Character.isDigit(peekChar())) {
            sb.append(peekChar());
            advance();
        }
        // 检查数字后是否紧跟字母（非法标识符如 123abc）
        if (pos < source.length() && Character.isLetter(peekChar())) {
            throw new RuntimeException("数字后不能直接跟字母 '" + peekChar() + "' 在 " + line + ":" + col);
        }
        tokens.add(new Token(TokenType.NUMBER, sb.toString(), startLine, startCol));
    }

    private void readOperator() {
        int startLine = line;
        int startCol = col;
        char c = peekChar();
        advance();
        // 处理双字符运算符 <= >= !=
        if ((c == '<' || c == '>' || c == '!') && peekChar() == '=') {
            String op = "" + c + "=";
            advance();
            tokens.add(new Token(TokenType.OPERATOR, op, startLine, startCol));
        } else {
            tokens.add(new Token(TokenType.OPERATOR, String.valueOf(c), startLine, startCol));
        }
    }

    private void readSeparator() {
        int startLine = line;
        int startCol = col;
        char c = peekChar();
        advance();
        tokens.add(new Token(TokenType.SEPARATOR, String.valueOf(c), startLine, startCol));
    }

    private boolean isOperatorChar(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '=' ||
                c == '>' || c == '<' || c == '!';
    }

    private boolean isSeparatorChar(char c) {
        return c == ',' || c == ';' || c == '{' || c == '}' || c == '(' || c == ')';
    }
}
