package lexer;

public enum TokenType {
    // 关键字
    IF, INT,WHILE, DO, ELSE, RETURN, CONTINUE,
    // 标识符
    BOOL,IDENTIFIER,
    // 常数
    NUMBER,
    // 运算符
    OPERATOR,
    // 分隔符
    SEPARATOR,
    // 文件结尾
    ERROR,        // 错误Token (种别码 -1)
    EOF
}