
import ir.CodeGenerator;
import ir.Quadruple;
import lexer.Lexer;
import lexer.Token;
import parser.ASTNode;
import parser.Parser;
import semantic.SemanticAnalyzer;
import semantic.SymbolTable;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String code = """
                { 
                    a = 3;
                    b = 4;
                    if (a < b) {
                        c = a + b;
                    }
                }
                """;

        // 1. 词法分析
        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();
        System.out.println("=== Tokens ===");
        for (Token t : tokens) {
            System.out.println(t);
        }

        // 2. 语法分析 -> AST
        Parser parser = new Parser(tokens);
        ASTNode ast = parser.parse();
        System.out.println("\n=== AST ===");
        System.out.println(ast);

        // 3. 语义分析 -> 符号表
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        SymbolTable symTable = semanticAnalyzer.analyze(ast);
        System.out.println("\n=== Symbol Table ===");
        System.out.println(symTable);

        // 4. 中间代码生成 -> 四元式
        CodeGenerator codeGen = new CodeGenerator();
        List<Quadruple> quadruples = codeGen.generate(ast);
        System.out.println("\n=== Quadruples ===");
        for (Quadruple q : quadruples) {
            System.out.println(q);
        }
    }
}