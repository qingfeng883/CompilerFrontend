package lexer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Lexer {
    private final String source;
    private int pos;
    private int line;
    private int wordCol;
    private final List<Token> tokens;
    private final List<String> errors;
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
        this.wordCol = 1;
        this.tokens = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    public List<Token> tokenize() {
        while (pos < source.length()) {
            try {
                char current = peekChar();

                // 跳过空白字符
                if (Character.isWhitespace(current)) {
                    skipWhitespace();
                    continue;
                }

                // 处理注释
                if (current == '/') {
                    if (peekNextChar() == '/') {
                        skipSingleLineComment();
                        continue;
                    } else if (peekNextChar() == '*') {
                        skipMultiLineComment();
                        continue;
                    }
                }

                int currentWordCol = wordCol;
                int currentLine = line;

                // 标识符：字母开头
                if (Character.isLetter(current)) {
                    readIdentifierOrKeyword(currentWordCol);
                }
                // 数字：以数字开头
                else if (Character.isDigit(current)) {
                    readNumber(currentWordCol);
                }
                // 运算符
                else if (isOperatorChar(current)) {
                    readOperator(currentWordCol);
                }
                // 分隔符
                else if (isSeparatorChar(current)) {
                    readSeparator(currentWordCol);
                }
                // 非法字符
                // 在 tokenize() 方法中，修改处理非法字符的 else 分支
                else {
                    // 处理非法字符，并将后续的字母数字一起读取作为整体错误
                    int illegalStartCol = wordCol;
                    int illegalStartLine = line;
                    StringBuilder errorWord = new StringBuilder();
                    errorWord.append(current);
                    advance();
                    wordCol++;

                    // 继续读取后续的字母、数字，直到遇到运算符、界符、空白符
                    while (pos < source.length()) {
                        char c = peekChar();
                        if (Character.isLetterOrDigit(c)) {
                            errorWord.append(c);
                            advance();
                        } else if (!isOperatorChar(c) && !isSeparatorChar(c) && !Character.isWhitespace(c)) {
                            // 如果遇到其他非法字符（如下划线），也包含进来
                            errorWord.append(c);
                            advance();
                        } else {
                            // 遇到运算符、界符、空白符，停止读取
                            break;
                        }
                    }

                    String fullError = errorWord.toString();
                    errors.add("[ERROR] Illegal character in identifier | '" + fullError + "' | at " + illegalStartLine + ":" + illegalStartCol);
                    tokens.add(new Token(TokenType.ERROR, fullError, illegalStartLine, illegalStartCol, -1, "Illegal character in identifier"));
                }

            } catch (RuntimeException e) {
                errors.add(e.getMessage());
                recover();
            }
        }
        tokens.add(new Token(TokenType.EOF, "", line, wordCol, 0, "End of file"));
        return tokens;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void printErrors() {
        for (String error : errors) {
            System.err.println(error);
        }
    }

    private char peekChar() {
        if (pos >= source.length()) return '\0';
        return source.charAt(pos);
    }

    private char peekNextChar() {
        if (pos + 1 >= source.length()) return '\0';
        return source.charAt(pos + 1);
    }

    private void advance() {
        if (pos >= source.length()) return;

        char c = source.charAt(pos);
        if (c == '\n') {
            line++;
            wordCol = 1;
        } else {
            wordCol++;
        }
        pos++;
    }

    private void skipWhitespace() {
        while (pos < source.length() && Character.isWhitespace(peekChar())) {
            advance();
        }
    }

    private void recover() {
        while (pos < source.length()) {
            char c = peekChar();
            if (c == '\n' || Character.isWhitespace(c) ||
                    Character.isLetter(c) || c == '_' || Character.isDigit(c) ||
                    isOperatorChar(c) || isSeparatorChar(c)) {
                return;
            }
            advance();
        }
    }

    // 跳过单行注释 //
    private void skipSingleLineComment() {
        advance(); advance();
        while (pos < source.length() && peekChar() != '\n') {
            advance();
        }
    }

     // 跳过多行注释 /* */
    private void skipMultiLineComment() {
        advance(); advance();
        while (pos < source.length()) {
            if (peekChar() == '*' && peekNextChar() == '/') {
                advance(); advance();
                return;
            }
            advance();
        }
        // 到达文件末尾，注释未闭合
        errors.add("[ERROR] Unclosed multi-line comment at end of file");
    }

    // 读取标识符或关键字
    private void readIdentifierOrKeyword(int col) {
        int startLine = line;
        int startCol = col;
        StringBuilder sb = new StringBuilder();

        // 读取字母、数字，遇到非法字符（如下划线、@等）时继续读取以便整体报错
        while (pos < source.length()) {
            char c = peekChar();
            // 如果是字母或数字，正常读取
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
                advance();
            }
            // 如果是下划线或其他非法字符（不是运算符、界符、空白符）
            else if (!isOperatorChar(c) && !isSeparatorChar(c) && !Character.isWhitespace(c)) {
                // 继续读取，将整个非法标识符作为一个整体报错
                while (pos < source.length()) {
                    char nextChar = peekChar();
                    // 遇到运算符、界符、空白符时停止
                    if (isOperatorChar(nextChar) || isSeparatorChar(nextChar) || Character.isWhitespace(nextChar)) {
                        break;
                    }
                    sb.append(nextChar);
                    advance();
                }
                // 报错：非法字符
                errors.add("[ERROR] Invalid character in identifier | '" + sb.toString() + "' | at " + startLine + ":" + startCol);
                tokens.add(new Token(TokenType.ERROR, sb.toString(), startLine, startCol, -1, "Invalid character in identifier"));
                return;
            }
            // 遇到运算符、界符、空白符，停止读取
            else {
                break;
            }
        }

        String word = sb.toString();

        // 如果word为空（理论上不应该发生）
        if (word.isEmpty()) {
            return;
        }

        // 规则：标识符不能以数字开头（已由入口保证，但二次确认）
        if (Character.isDigit(word.charAt(0))) {
            errors.add("[ERROR] Invalid identifier (cannot start with digit) | '" + word + "' | at " + startLine + ":" + startCol);
            tokens.add(new Token(TokenType.ERROR, word, startLine, startCol, -1, "Invalid identifier (starts with digit)"));
            return;
        }

        // 规则：标识符长度不超过8位
        if (word.length() > 8) {
            errors.add("[ERROR] Identifier too long (max 8 chars) | '" + word + "' | at " + startLine + ":" + startCol);
            tokens.add(new Token(TokenType.ERROR, word, startLine, startCol, -1, "Identifier too long"));
            return;
        }

        // 判断是否为关键字
        boolean isKeyword = keywords.contains(word);
        TokenType type = isKeyword ? TokenType.valueOf(word.toUpperCase()) : TokenType.IDENTIFIER;
        int code = isKeyword ? 1 : 2;
        String category = isKeyword ? "Keyword" : "Identifier";

        tokens.add(new Token(type, word, startLine, startCol, code, category));
    }
    // 读取数字
    private void readNumber(int col) {
        int startLine = line;
        int startCol = col;
        StringBuilder sb = new StringBuilder();

        // 读取数字部分
        while (pos < source.length() && Character.isDigit(peekChar())) {
            sb.append(peekChar());
            advance();
        }

        String numStr = sb.toString();

        // 规则：检查数字后是否紧跟字母（非法单词，如 123abc）
        if (pos < source.length() && (Character.isLetter(peekChar())  )) {
            String remaining = "";
            while (pos < source.length() && (Character.isLetterOrDigit(peekChar())  )) {
                remaining += peekChar();
                advance();
            }
            String fullErrorWord = numStr + remaining;
            errors.add("[ERROR] Invalid word (digit followed by letter/underscore) | '" + fullErrorWord + "' | at " + startLine + ":" + startCol);
            tokens.add(new Token(TokenType.ERROR, fullErrorWord, startLine, startCol, -1, "Invalid word (starts with digit)"));
            return;
        }

        // 规则：数字不能以0开头（单独的0除外）
        if (numStr.length() > 1 && numStr.charAt(0) == '0') {
            errors.add("[ERROR] Invalid number (cannot start with 0) | '" + numStr + "' | at " + startLine + ":" + startCol);
            tokens.add(new Token(TokenType.ERROR, numStr, startLine, startCol, -1, "Invalid number (starts with 0)"));
            return;
        }

        // 规则：数字长度不超过8位
        if (numStr.length() > 8) {
            errors.add("[ERROR] Number too long (max 8 digits) | '" + numStr + "' | at " + startLine + ":" + startCol);
            tokens.add(new Token(TokenType.ERROR, numStr, startLine, startCol, -1, "Number too long"));
            return;
        }

        // 规则：数值范围 0-99999999
        try {
            long value = Long.parseLong(numStr);
            if (value > 99999999) {
                errors.add("[ERROR] Number out of range (0-99999999) | '" + numStr + "' | at " + startLine + ":" + startCol);
                tokens.add(new Token(TokenType.ERROR, numStr, startLine, startCol, -1, "Number out of range"));
                return;
            }
        } catch (NumberFormatException e) {
            errors.add("[ERROR] Invalid number format | '" + numStr + "' | at " + startLine + ":" + startCol);
            tokens.add(new Token(TokenType.ERROR, numStr, startLine, startCol, -1, "Invalid number format"));
            return;
        }

        // 合法数字
        tokens.add(new Token(TokenType.NUMBER, numStr, startLine, startCol, 3, "Number"));
    }

    // 读取运算符
    private void readOperator(int col) {
        int startLine = line;
        int startCol = col;
        char c = peekChar();
        advance();

        String op;
        if ((c == '<' || c == '>' || c == '!') && peekChar() == '=') {
            op = "" + c + "=";
            advance();
        } else {
            op = String.valueOf(c);
        }

        tokens.add(new Token(TokenType.OPERATOR, op, startLine, startCol, 4, "Operator"));
    }

    // 读取分隔符
    private void readSeparator(int col) {
        int startLine = line;
        int startCol = col;
        char c = peekChar();
        advance();
        tokens.add(new Token(TokenType.SEPARATOR, String.valueOf(c), startLine, startCol, 5, "Separator"));
    }

    private boolean isOperatorChar(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '=' ||
                c == '>' || c == '<' || c == '!';
    }

    private boolean isSeparatorChar(char c) {
        return c == ',' || c == ';' || c == '{' || c == '}' || c == '(' || c == ')';
    }
}
