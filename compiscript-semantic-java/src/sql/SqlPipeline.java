package sql;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import gui.CollectingErrorListener;
import semantic.SemanticError;
import sql.ast.Ast;
import sql.ast.AstBuilder;
import sql.exec.ExecResult;
import sql.exec.Executor;
import sql.exec.SqlRuntimeException;
import sql.semantic.SqlSemanticAnalyzer;
import sql.storage.Database;
import sqlgen.SqlLexer;
import sqlgen.SqlParser;
import viz.ParseTreeToDot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Pipeline de SQL: léxico → sintáctico → AST → semántico → ejecución sobre los archivos de la base. */
public class SqlPipeline {

    /** Un token tal como lo ve el lexer. */
    public static class TokenInfo {
        public final String type, text; public final int line, col;
        public TokenInfo(String type, String text, int line, int col) { this.type = type; this.text = text; this.line = line; this.col = col; }
    }

    /** Todo lo que produce una corrida: tokens, árboles, errores por fase y resultados. */
    public static class Result {
        public String source;
        public final List<TokenInfo> tokens = new ArrayList<>();
        public final List<SemanticError> lexicalErrors = new ArrayList<>();
        public final List<SemanticError> syntaxErrors = new ArrayList<>();
        public final List<SemanticError> semanticErrors = new ArrayList<>();
        /** Errores que solo se detectan al ejecutar (subconsulta escalar con varias filas, clave duplicada, ...). */
        public final List<SemanticError> executionErrors = new ArrayList<>();
        public String treeText = "", treeDot = "", astText = "";
        public Ast.Script ast;
        public final List<ExecResult> results = new ArrayList<>();
        public boolean executed;
        /** Error fatal (por ejemplo de entrada/salida), null si no hubo. */
        public String error;

        public boolean isOk() {
            return error == null && lexicalErrors.isEmpty() && syntaxErrors.isEmpty()
                && semanticErrors.isEmpty() && executionErrors.isEmpty();
        }

        /** Fase para el color de la GUI: lexical, syntax, semantic, mixed u ok. */
        public String phase() {
            int kinds = 0;
            if (!lexicalErrors.isEmpty()) kinds++;
            if (!syntaxErrors.isEmpty()) kinds++;
            if (!semanticErrors.isEmpty() || !executionErrors.isEmpty() || error != null) kinds++;
            if (kinds >= 2) return "mixed";
            if (!lexicalErrors.isEmpty()) return "lexical";
            if (!syntaxErrors.isEmpty()) return "syntax";
            if (kinds == 1) return "semantic";
            return "ok";
        }

        /** Reporte de texto con los errores de todas las fases. */
        public String report() {
            StringBuilder sb = new StringBuilder();
            group(sb, "Errores léxicos", lexicalErrors);
            group(sb, "Errores sintácticos", syntaxErrors);
            group(sb, "Errores semánticos", semanticErrors);
            group(sb, "Errores de ejecución", executionErrors);
            if (error != null) sb.append("Error: ").append(error).append('\n');
            if (isOk()) sb.append("OK: sin errores.\n");
            return sb.toString();
        }

        private static void group(StringBuilder sb, String title, List<SemanticError> list) {
            if (list.isEmpty()) return;
            sb.append("== ").append(title).append(" ==\n");
            for (SemanticError e : list) sb.append("  ").append(e.format()).append('\n');
        }
    }

    /** Analiza el texto; solo lo ejecuta si execute es true y no hubo errores. db aporta el catálogo para el análisis semántico. */
    public Result run(String source, Database db, boolean execute) {
        Result res = new Result();
        res.source = source;
        try {
            runUnchecked(source, db, execute, res);
        } catch (IOException e) {
            res.error = "Error de archivos: " + e.getMessage();
        } catch (RuntimeException e) {
            res.error = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        return res;
    }

    private void runUnchecked(String source, Database db, boolean execute, Result res) throws IOException {
        CharStream input = CharStreams.fromString(source);
        SqlLexer lexer = new SqlLexer(input);
        CollectingErrorListener lexListener = new CollectingErrorListener("LEXICAL");
        lexer.removeErrorListeners();
        lexer.addErrorListener(lexListener);
        CommonTokenStream stream = new CommonTokenStream(lexer);
        SqlParser parser = new SqlParser(stream);
        CollectingErrorListener parseListener = new CollectingErrorListener("SYNTAX");
        parser.removeErrorListeners();
        parser.addErrorListener(parseListener);

        SqlParser.ScriptContext tree = parser.script();
        for (SemanticError e : lexListener.errors) res.lexicalErrors.add(translated(e));
        for (SemanticError e : parseListener.errors) res.syntaxErrors.add(translated(e));

        for (Token t : stream.getTokens()) {
            if (t.getType() == Token.EOF) continue;
            String name = SqlLexer.VOCABULARY.getSymbolicName(t.getType());
            res.tokens.add(new TokenInfo(name, t.getText(), t.getLine(), t.getCharPositionInLine() + 1));
        }
        res.treeText = treeText(tree, parser);
        res.treeDot = new ParseTreeToDot().toDot(tree, parser, "SqlParseTree");

        if (!res.lexicalErrors.isEmpty() || !res.syntaxErrors.isEmpty()) return;

        Ast.Script script = (Ast.Script) new AstBuilder().visit(tree);
        res.ast = script;

        SqlSemanticAnalyzer analyzer = new SqlSemanticAnalyzer(db.catalog());
        analyzer.analyze(script);
        res.semanticErrors.addAll(analyzer.errors);
        res.astText = Ast.dump(script);
        if (!analyzer.errors.isEmpty() || !execute) return;

        Executor exec = new Executor(db);
        res.executed = true;
        for (Ast.Stmt s : script.statements) {
            try {
                res.results.add(exec.execute(s));
            } catch (SqlRuntimeException e) {
                res.executionErrors.add(new SemanticError(e.category, e.getMessage(), e.line, e.col));
                return;
            }
        }
    }

    /** Mensajes de ANTLR en español. */
    private static SemanticError translated(SemanticError e) {
        String m = e.message
            .replace("token recognition error at:", "símbolo no reconocido:")
            .replace("mismatched input", "entrada inesperada")
            .replace("extraneous input", "entrada sobrante")
            .replace("no viable alternative at input", "no hay una forma válida de continuar en")
            .replace("missing", "falta")
            .replace("expecting", "se esperaba")
            .replace(" at ", " antes de ")
            .replace("<EOF>", "el fin del texto");
        int open = m.indexOf("se esperaba {");
        if (open >= 0) { // la lista de ANTLR puede ser enorme: se dejan los primeros
            String[] items = m.substring(open + 13, m.length() - (m.endsWith("}") ? 1 : 0)).split(", ");
            if (items.length > 6) m = m.substring(0, open) + "se esperaba uno de {" + String.join(", ", java.util.Arrays.copyOf(items, 6)) + ", ...}";
        }
        return new SemanticError(e.category, m, e.line, e.column);
    }

    /** Árbol sintáctico de ANTLR con sangría: reglas en negrita lógica (nombre) y tokens entre comillas. */
    static String treeText(ParseTree tree, SqlParser parser) {
        StringBuilder sb = new StringBuilder();
        printTree(tree, parser, 0, sb);
        return sb.toString();
    }

    private static void printTree(ParseTree t, SqlParser parser, int depth, StringBuilder sb) {
        for (int i = 0; i < depth; i++) sb.append("  ");
        if (t instanceof ParserRuleContext) {
            ParserRuleContext ctx = (ParserRuleContext) t;
            sb.append(parser.getRuleNames()[ctx.getRuleIndex()]).append('\n');
            for (int i = 0; i < t.getChildCount(); i++) printTree(t.getChild(i), parser, depth + 1, sb);
        } else if (t instanceof TerminalNode) {
            sb.append('\'').append(t.getText().replace("\n", "\\n")).append("'\n");
        }
    }
}
