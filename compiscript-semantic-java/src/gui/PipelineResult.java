package gui;

import semantic.Scope;
import semantic.SemanticError;

import java.util.ArrayList;
import java.util.List;

/** Resultados del pipeline: errores por fase, árbol en DOT, scopes y tabla de símbolos. */
public class PipelineResult {

    public String source;

    public final List<SemanticError> lexicalErrors = new ArrayList<>();
    public final List<SemanticError> syntaxErrors = new ArrayList<>();
    public final List<SemanticError> semanticErrors = new ArrayList<>();
    public final List<SemanticError> warnings = new ArrayList<>();

    public String treeDot = "";
    public List<Scope> allScopes;
    public String symbolsMarkdown = "";

    /** Código intermedio (TAC) y reporte de registros de activación; vacíos si hubo errores. */
    public String tac = "";
    public String activationMarkdown = "";

    /** Ensamblador MIPS generado a partir del TAC y reporte de asignación de registros; vacíos si hubo errores. */
    public String mips = "";
    public String registerReport = "";

    /** Resultado de ejecutar el MIPS en el simulador. */
    public String runOutput = "";
    public String runError;
    public long runSteps;

    /** Error fatal del pipeline, null si no hubo. */
    public String error;

    public boolean isOk() {
        return error == null && lexicalErrors.isEmpty() && syntaxErrors.isEmpty() && semanticErrors.isEmpty();
    }

    /** Reporte de texto plano con los errores de todas las fases. */
    public String report() {
        StringBuilder sb = new StringBuilder();
        appendGroup(sb, "Errores léxicos", lexicalErrors);
        appendGroup(sb, "Errores sintácticos", syntaxErrors);
        appendGroup(sb, "Errores semánticos", semanticErrors);
        appendGroup(sb, "Advertencias", warnings);
        if (isOk() && warnings.isEmpty()) sb.append("OK: sin errores.\n");
        return sb.toString();
    }

    private static void appendGroup(StringBuilder sb, String title, List<SemanticError> list) {
        if (list.isEmpty()) return;
        sb.append("== ").append(title).append(" ==\n");
        for (SemanticError e : list) sb.append("  ").append(e.format()).append('\n');
    }

    /** Fase para el color de la GUI: lexical, syntax, semantic, mixed u ok. */
    public String phase() {
        if (error != null) return "semantic";
        int types = 0;
        if (!lexicalErrors.isEmpty()) types++;
        if (!syntaxErrors.isEmpty()) types++;
        if (!semanticErrors.isEmpty()) types++;
        if (types >= 2) return "mixed";
        if (!lexicalErrors.isEmpty()) return "lexical";
        if (!syntaxErrors.isEmpty()) return "syntax";
        if (!semanticErrors.isEmpty()) return "semantic";
        return "ok";
    }
}
