package lexer;

public enum TokenType {
    // 关键字 (种别码1)
    IF, INT, WHILE, DO, ELSE, RETURN, CONTINUE,
    // 标识符 (2)
    IDENTIFIER,
    // 常数 (3)
    NUMBER,
    // 运算符 (4)
    OPERATOR,
    // 分隔符 (5)
    SEPARATOR,
    // 文件结尾
    EOF
}
