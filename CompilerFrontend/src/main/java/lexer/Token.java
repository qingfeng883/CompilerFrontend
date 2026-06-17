package lexer;

public class Token {
    private TokenType type;
    private String value;
    private int line;
    private int column;
    private int code;
    private String category;

    // 正常Token构造函数
    public Token(TokenType type, String value, int line, int column, int code, String category) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
        this.code = code;
        this.category = category;
    }

    // 错误Token构造函数
    public Token(TokenType type, String value, int line, int column, int code, String category, String errorDetail) {
        this(type, value, line, column, code, category + " (ERROR: " + errorDetail + ")");
    }

    public TokenType getType() { return type; }
    public String getValue() { return value; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    public int getCode() { return code; }
    public String getCategory() { return category; }

    public Token(TokenType type, String value, int line, int column) {
        this(type, value, line, column, 0, "Unknown");
    }

    @Override
    public String toString() {
        return String.format("(%d, '%s', %s) at %d:%d", code, value, category, line, column);
    }
}