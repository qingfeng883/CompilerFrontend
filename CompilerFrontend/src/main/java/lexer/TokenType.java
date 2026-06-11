package lexer;

public enum TokenType {
    // 关键字
    IF, INT, FLOAT, STRING, BOOL, WHILE, DO, ELSE, RETURN, CONTINUE,
    // 标识符
    IDENTIFIER,
    // 常数
    NUMBER,
    // 运算符
    OPERATOR,
    // 分隔符
    SEPARATOR,
    // 文件结尾
    EOF
}