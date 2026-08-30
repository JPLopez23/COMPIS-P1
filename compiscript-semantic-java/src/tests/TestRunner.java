package tests;

import gui.PipelineResult;
import gui.PipelineRunner;
import semantic.SemanticError;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;

/** Batería de pruebas: corre el pipeline sobre cada fixture y compara con el resultado esperado. */
public class TestRunner {

    private static int passed = 0;
    private static int failed = 0;
    private static final Path FIXTURES = Paths.get("tests", "fixtures");
    private static final Path EXAMPLES = Paths.get("examples");

    public static void main(String[] args) {
        // ── Tipos ────────────────────────────────────────────────────────────
        check("arith_ok",            fixture("types_arith_ok.cps"),          PipelineResult::isOk);
        check("arith_err",           fixture("types_arith_err.cps"),         hasError("TYPE"));

        // ── Ámbito ───────────────────────────────────────────────────────────
        check("scope_ok",            fixture("scope_ok.cps"),                PipelineResult::isOk);
        check("scope_redeclare",     fixture("scope_err_redeclare.cps"),     hasError("SCOPE"));
        check("scope_undeclared",    fixture("scope_err_undeclared.cps"),    hasError("SCOPE"));
        check("const_ok",            fixture("const_ok.cps"),                PipelineResult::isOk);
        check("const_no_init_syntax",fixture("const_err_no_init.cps"),       r -> !r.syntaxErrors.isEmpty());

        // ── Funciones ────────────────────────────────────────────────────────
        check("fn_call_ok",          fixture("fn_call_ok.cps"),              PipelineResult::isOk);
        check("fn_call_arity_err",   fixture("fn_call_err_arity.cps"),       hasError("FUNCTION"));
        check("recursion_ok",        example("ok/functions.cps"),            PipelineResult::isOk);

        // ── Control de flujo ────────────────────────────────────────────────
        check("return_outside_fn",   fixture("return_err_outside.cps"),      hasError("FLOW"));
        check("break_outside_loop",  fixture("break_err_outside.cps"),       hasError("FLOW"));
        check("if_cond_not_bool",    fixture("if_err_cond.cps"),             hasError("FLOW"));

        // ── Clases ───────────────────────────────────────────────────────────
        check("class_ok",            fixture("class_ok.cps"),                PipelineResult::isOk);
        check("class_missing_member",fixture("class_err_missing_member.cps"),hasError("CLASS"));
        check("inheritance_this",    example("ok/classes.cps"),              PipelineResult::isOk);

        // ── Listas ───────────────────────────────────────────────────────────
        check("list_ok",             fixture("list_ok.cps"),                 PipelineResult::isOk);
        check("list_index_not_int",  fixture("list_err_index.cps"),          hasError("LIST"));
        check("list_heterogeneous",  fixture("list_err_hetero.cps"),         hasError("LIST"));

        // ── Generales ────────────────────────────────────────────────────────
        check("deadcode_after_return", fixture("deadcode_after_return.cps"),
            r -> r.isOk() && r.warnings.stream().anyMatch(w -> w.category.equals("GENERAL")));

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    private static String fixture(String name) { return read(FIXTURES.resolve(name)); }
    private static String example(String relPath) { return read(EXAMPLES.resolve(relPath)); }

    private static String read(Path p) {
        try {
            return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer " + p + ": " + e.getMessage());
        }
    }

    private static Predicate<PipelineResult> hasError(String category) {
        return r -> !r.isOk() && r.semanticErrors.stream().anyMatch(e -> e.category.equals(category));
    }

    private static void check(String name, String source, Predicate<PipelineResult> assertion) {
        PipelineResult res = new PipelineRunner().run(source);
        boolean ok;
        String detail = "";
        try {
            ok = assertion.test(res);
        } catch (Exception e) {
            ok = false;
            detail = "  (excepción: " + e + ")";
        }
        if (ok) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name + detail);
            for (SemanticError e : res.lexicalErrors) System.out.println("        lexical:  " + e.format());
            for (SemanticError e : res.syntaxErrors) System.out.println("        syntax:   " + e.format());
            for (SemanticError e : res.semanticErrors) System.out.println("        semantic: " + e.format());
            for (SemanticError e : res.warnings) System.out.println("        warning:  " + e.format());
        }
    }
}
