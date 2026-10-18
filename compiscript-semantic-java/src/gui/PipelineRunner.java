package gui;

import generated.CompiscriptLexer;
import generated.CompiscriptParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import codegen.ActivationReport;
import codegen.TacGenerator;
import semantic.SemanticAnalyzer;
import viz.ParseTreeToDot;
import viz.SymbolTableWriter;

/** Ejecuta el pipeline: lexer y parser de ANTLR y, si no hubo errores, el analizador semántico. */
public class PipelineRunner {

    public PipelineResult run(String source) {
        PipelineResult res = new PipelineResult();
        res.source = source;

        try {
            CharStream input = CharStreams.fromString(source);
            CompiscriptLexer lexer = new CompiscriptLexer(input);
            CollectingErrorListener lexListener = new CollectingErrorListener("LEXICAL");
            lexer.removeErrorListeners();
            lexer.addErrorListener(lexListener);

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            CompiscriptParser parser = new CompiscriptParser(tokens);
            CollectingErrorListener parseListener = new CollectingErrorListener("SYNTAX");
            parser.removeErrorListeners();
            parser.addErrorListener(parseListener);

            ParseTree tree = parser.program();
            res.lexicalErrors.addAll(lexListener.errors);
            res.syntaxErrors.addAll(parseListener.errors);

            try {
                res.treeDot = new ParseTreeToDot().toDot(tree, parser, "ParseTree");
            } catch (Exception e) {
                res.treeDot = "// error generando DOT: " + e.getMessage();
            }

            if (res.lexicalErrors.isEmpty() && res.syntaxErrors.isEmpty()) {
                SemanticAnalyzer analyzer = new SemanticAnalyzer();
                analyzer.visitProgram((CompiscriptParser.ProgramContext) tree);
                res.semanticErrors.addAll(analyzer.errors);
                res.warnings.addAll(analyzer.warnings);
                res.allScopes = analyzer.stack.allScopes;
                res.symbolsMarkdown = new SymbolTableWriter().toMarkdown(res.allScopes);

                if (analyzer.errors.isEmpty()) {
                    TacGenerator gen = new TacGenerator(analyzer);
                    res.tac = gen.generate((CompiscriptParser.ProgramContext) tree);
                    res.activationMarkdown = ActivationReport.toMarkdown(gen);
                    // la tabla de símbolos ya tiene direcciones, offsets y etiquetas
                    res.symbolsMarkdown = new SymbolTableWriter().toMarkdown(res.allScopes);
                }
            }
        } catch (Exception e) {
            res.error = e.getClass().getSimpleName() + ": " + e.getMessage();
        }

        return res;
    }
}
