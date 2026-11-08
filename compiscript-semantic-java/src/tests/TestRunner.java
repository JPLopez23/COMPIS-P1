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

        // ── Generación de código intermedio (TAC) ───────────────────────────
        Path tacDir = Paths.get("tests", "tac");
        String exprLogic = read(tacDir.resolve("expr_logic.cps"));
        String funcs = read(tacDir.resolve("funcs_closures.cps"));
        String objs = read(tacDir.resolve("classes_objects.cps"));
        String flow = read(tacDir.resolve("control_flow.cps"));
        String edge = read(tacDir.resolve("edge_cases.cps"));

        check("tac_generated",          exprLogic, r -> r.isOk() && !r.tac.isEmpty());
        check("tac_arith_precedence",   exprLogic, tacHas("t0 = G.b * G.c", "t0 = G.a + t0", "G.r = t0 - t1"));
        check("tac_temp_recycling",     exprLogic, r -> r.tac.contains("temporales=2"));
        check("tac_unary",              exprLogic, tacHas("t0 = - G.a", "G.neg = t0 + -5"));
        check("tac_comparison_value",   exprLogic, tacHas("G.flag = G.a != G.b"));
        check("tac_short_circuit",      exprLogic, tacHas("if G.a >= G.b goto", "if G.b < G.c goto", "t0 = true", "t0 = false"));
        check("tac_ternary",            exprLogic, tacHas("G.t = t0"));
        check("tac_if_else",            exprLogic, tacHas("prints \"si\"", "prints \"no\""));
        check("tac_while_break_continue", flow,    tacHas("goto L1", "if G.i >= 10 goto L3", "if G.i > 7 goto L3"));
        check("tac_for",                flow,      tacHas("a = 0", "a = a + 1"));
        check("tac_do_while",           flow,      tacHas("G.total = G.total - 1", "if G.total > 0 goto"));
        check("tac_switch",             flow,      tacHas("$sw1 = G.k", "if $sw1 == 1 goto", "prints \"dos\""));
        check("tac_foreach",            flow,      tacHas("len $arr", "v = $arr"));
        check("tac_function_call",      funcs,     tacHas("param 6", "call fib", "function fib:"));
        check("tac_recursion",          funcs,     tacHas("t0 = call fib, 1", "t1 = call fib, 1", "t0 = t0 + t1"));
        check("tac_nested_closure",     funcs,     tacHas("function mk_add:", "env(1).acc = env(1).acc + x"));
        check("tac_function_value",     funcs,     tacHas("G.f = closure fib", "callind G.f, 1"));
        check("tac_mutual_recursion",   funcs,     tacHas("call esImpar", "call esPar"));
        check("tac_new_and_ctor",       objs,      tacHas("t0 = new Punto3, 12", "call Punto_constructor, 3", "call Punto_$init, 1"));
        check("tac_field_access",       objs,      tacHas("this.x@0 = x", "t0 = this.x@0", "G.p.z@8 = 10"));
        check("tac_method_call_this",   objs,      tacHas("param G.p", "call Punto_mover, 2", "param this"));
        check("tac_inheritance_layout", objs,      r -> r.activationMarkdown.contains("`Punto3` — 12 B")
                                                         && r.activationMarkdown.contains("| 8 | `z` |"));
        check("tac_list",               objs,      tacHas("newarray 2", "t0[0] = t1", "t0.x@0 = t1"));
        check("tac_edge_cases",         edge,      tacHas("call nada, 0", "G.m[t0] = t1", "G.w = t0"));
        check("tac_nested_fn_in_method", edge,     tacHas("function Caja_crear_interna:", "t0 = env(1).this.v@0", "call Caja_crear_interna, 0, env(0)"));
        check("tac_wellformed_all",     "",        r -> allWellFormed(tacDir, FIXTURES, EXAMPLES.resolve("ok"), MIPS_DIR));

        // Tabla de símbolos extendida y registros de activación
        check("sym_offsets",            funcs,     r -> symbolOffset(r, "acc") == 20 && symbolOffset(r, "x") == 16);
        check("sym_global_area",        exprLogic, r -> symbolOffset(r, "b") == 4 && symbolOffset(r, "c") == 8);
        check("sym_function_labels",    funcs,     r -> labelOf(r, "add").equals("mk_add") && labelOf(r, "fib").equals("fib"));
        check("frame_report",           funcs,     r -> r.activationMarkdown.contains("## `mk_add` — nivel 2")
                                                         && r.activationMarkdown.contains("enlace de acceso"));

        // Sin TAC cuando hay errores
        check("tac_not_on_semantic_err", fixture("types_arith_err.cps"),  r -> r.tac.isEmpty() && !r.isOk());
        check("tac_not_on_syntax_err",   example("err/syntax_error.cps"), r -> r.tac.isEmpty() && !r.syntaxErrors.isEmpty());
        check("tac_not_on_lexical_err",  example("err/lexical_error.cps"), r -> r.tac.isEmpty() && !r.lexicalErrors.isEmpty());

        // Operadores tipados del TAC (la información que necesita el generador de MIPS)
        String cadenas = read(MIPS_DIR.resolve("cadenas.cps"));
        check("tac_string_concat",      cadenas,   tacHas("++", "==s", "!=s", "prints "));
        check("tac_print_bool",         read(MIPS_DIR.resolve("booleanos.cps")), tacHas("printb "));
        check("tac_call_has_link",      funcs,     tacHas("call fib, 1, env(1)", "call mk_add, 1, env(1)"));

        // ── Generación de código MIPS: ejecución en el simulador ─────────────
        String[] programas = {"aritmetica", "booleanos", "cadenas", "control", "funciones", "clases", "listas",
                              "excepciones", "derrame", "algoritmos", "integral", "closures_valores"};
        for (String p : programas)
            check("mips_run_" + p, read(MIPS_DIR.resolve(p + ".cps")), runsAs(p));
        String[] ejemplos = {"hello", "arrays_control", "classes", "closures", "demo_completo", "functions"};
        for (String e : ejemplos)
            check("mips_run_ejemplo_" + e, example("ok/" + e + ".cps"), runsAs("ex_" + e));

        // Secuencias de llamada y retorno
        String fn = read(MIPS_DIR.resolve("funciones.cps"));
        check("mips_prologue_epilogue", fn, mipsHas("f_fib:", "addiu $sp, $sp, -", "sw $ra, 0($sp)", "sw $fp, 4($sp)",
                "sw $a0, 8($sp)", "move $fp, $sp", "lw $ra, 0($fp)", "lw $t8, 4($fp)", "addiu $sp, $fp,", "jr $ra"));
        check("mips_call_sequence",     fn, mipsHas("addiu $sp, $sp, -4", "sw $s0, 0($sp)", "jal f_fib", "addiu $sp, $sp, 4"));
        check("mips_return_value_slot", fn, mipsHas("sw $v0, 12($fp)"));
        check("mips_access_link",       fn, mipsHas("lw $a0, 8($a0)", "move $a0, $fp"));
        check("mips_callee_saved",      fn, mipsHas("sw $s0, 28($fp)      # guarda $s0", "lw $s0, 28($fp)      # restaura $s0"));
        check("mips_indirect_call",     fn, mipsHas("jalr $t8", "# G.f = closure fib"));
        check("mips_main_exits",        fn, mipsHas("main:", "li $v0, 10"));

        // Asignación de registros: getReg()
        check("mips_getreg_temps_in_s", read(MIPS_DIR.resolve("aritmetica.cps")), mipsHas("addu $s", "mul $s", "subu $s"));
        check("mips_getreg_no_spill",   read(MIPS_DIR.resolve("aritmetica.cps")), r -> spills(r) == 0);
        check("mips_getreg_spill",      read(MIPS_DIR.resolve("derrame.cps")), r -> spills(r) > 0
                && r.mips.contains("# derrama t") && r.mips.contains("# recarga t"));
        check("mips_registers_report",  fn, r -> r.registerReport.contains("fib: usa $s0") && r.registerReport.contains("total de derrames"));

        // Portabilidad: solo instrucciones que entienden SPIM y MARS
        check("mips_portable_instructions", fn, r -> unsupportedMnemonics(r.mips).isEmpty());
        check("mips_portable_all", "", r -> allPortable(MIPS_DIR));
        check("mips_comments_ascii",    fn, r -> commentsAscii(r.mips));

        // Casos fallidos
        check("mips_not_on_semantic_err", fixture("types_arith_err.cps"), r -> r.mips.isEmpty() && !r.isOk());
        check("mips_not_on_syntax_err",   example("err/syntax_error.cps"), r -> r.mips.isEmpty() && !r.syntaxErrors.isEmpty());
        check("mips_not_on_lexical_err",  example("err/lexical_error.cps"), r -> r.mips.isEmpty() && !r.lexicalErrors.isEmpty());
        check("mips_uncaught_exception",  "let a: integer[] = [1];\nprint(a[3]);\nprint(7);\n",
                r -> r.isOk() && r.runOutput.equals("Excepcion no capturada: indice fuera de rango\n"));
        check("mips_div_by_zero",         "let z: integer = 0;\nprint(5 % z);\n",
                r -> r.runOutput.equals("Excepcion no capturada: division por cero\n"));
        check("mips_null_reference",      "class A { var v: integer = 1; }\nlet a: A = null;\nprint(a.v);\n",
                r -> r.runOutput.equals("Excepcion no capturada: referencia nula\n"));
        check("mips_infinite_loop_stopped", "while (true) { }\n", r -> {
            mips.MipsSimulator.Result s = mips.MipsSimulator.run(r.mips, 10_000, "");
            return s.error != null && s.error.contains("límite");
        });
        check("mips_simulator_rejects_bad_asm", "", r -> {
            mips.MipsSimulator.Result s = mips.MipsSimulator.run("main:\n    frobnicate $t0, $t1\n");
            return s.error != null && s.error.contains("no soportada");
        });
        check("mips_simulator_alignment", "", r -> {
            mips.MipsSimulator.Result s = mips.MipsSimulator.run("main:\n    li $t0, 268500993\n    lw $t1, 0($t0)\n    li $v0, 10\n    syscall\n");
            return s.error != null && s.error.contains("desalineado");
        });
        check("mips_deterministic",       fn, r -> r.runOutput.equals(new PipelineRunner().run(fn).runOutput) && r.mips.equals(new PipelineRunner().run(fn).mips));

        // Validación en el simulador MARS real (opcional: requiere lib/Mars4_5.jar o -Dmars.jar=ruta)
        Path marsJar = Paths.get(System.getProperty("mars.jar", "lib/Mars4_5.jar"));
        if (Files.exists(marsJar)) {
            check("mips_mars_all", "", r -> allInMars(marsJar, programas, ejemplos));
        } else {
            System.out.println("SKIP  mips_mars_all (no se encontró " + marsJar + "; ver docs/MIPS.md)");
        }

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

    // ── Ayudas para las pruebas de MIPS ────────────────────────────────────
    private static final Path MIPS_DIR = Paths.get("tests", "mips");

    /** El programa se ejecuta sin error en el simulador y su salida es exactamente la esperada (tests/mips/expected). */
    private static Predicate<PipelineResult> runsAs(String name) {
        return r -> {
            if (!r.isOk() || r.mips.isEmpty()) return false;
            String expected = read(MIPS_DIR.resolve("expected").resolve(name + ".out")).replace("\r\n", "\n");
            if (!r.runOutput.equals(expected)) {
                System.out.println("        esperado: " + expected.replace("\n", "|"));
                System.out.println("        obtenido: " + r.runOutput.replace("\n", "|"));
                return false;
            }
            return true;
        };
    }

    private static Predicate<PipelineResult> mipsHas(String... parts) {
        return r -> {
            if (!r.isOk() || r.mips.isEmpty()) return false;
            for (String p : parts) {
                if (!r.mips.contains(p)) {
                    System.out.println("        falta en el MIPS: " + p);
                    return false;
                }
            }
            return true;
        };
    }

    /** Ejecuta cada programa de prueba en MARS (modo de línea de comandos) y compara con la salida esperada. */
    private static boolean allInMars(Path marsJar, String[] programas, String[] ejemplos) {
        try {
            Path tmp = Files.createTempDirectory("mars_");
            boolean ok = true;
            List<String[]> casos = new java.util.ArrayList<>();
            for (String p : programas) casos.add(new String[]{p, MIPS_DIR.resolve(p + ".cps").toString()});
            for (String e : ejemplos) casos.add(new String[]{"ex_" + e, EXAMPLES.resolve("ok").resolve(e + ".cps").toString()});
            for (String[] c : casos) {
                PipelineResult r = new PipelineRunner().run(read(Paths.get(c[1])));
                Path asm = tmp.resolve(c[0] + ".asm");
                Files.write(asm, r.mips.getBytes(StandardCharsets.UTF_8));
                Process pr = new ProcessBuilder("java", "-jar", marsJar.toAbsolutePath().toString(), "nc", "sm", asm.toString())
                        .redirectErrorStream(true).start();
                String out = new String(pr.getInputStream().readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
                if (!pr.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)) pr.destroyForcibly();
                if (out.endsWith("\n\n")) out = out.substring(0, out.length() - 1); // MARS agrega un salto de línea al terminar
                String expected = read(MIPS_DIR.resolve("expected").resolve(c[0] + ".out")).replace("\r\n", "\n");
                if (!out.equals(expected)) {
                    System.out.println("        MARS difiere en " + c[0] + ": " + out.replace("\n", "|"));
                    ok = false;
                }
            }
            return ok;
        } catch (IOException | InterruptedException e) {
            System.out.println("        no se pudo ejecutar MARS: " + e);
            return false;
        }
    }

    private static int spills(PipelineResult r) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("total de derrames a la pila: (\\d+)").matcher(r.registerReport);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** Instrucciones del conjunto que entienden tanto SPIM como MARS. */
    private static final java.util.Set<String> PORTABLE = new java.util.HashSet<>(java.util.Arrays.asList(
        "addu", "addiu", "subu", "mul", "div", "mflo", "mfhi", "and", "or", "xori", "sll", "slt", "seq", "sne", "sgt", "sge", "sle",
        "li", "la", "move", "lw", "sw", "lb", "sb", "j", "jal", "jr", "jalr", "beq", "bne", "beqz", "bnez", "blt", "ble", "bgt",
        "bge", "bgeu", "syscall"));

    private static java.util.Set<String> unsupportedMnemonics(String asm) {
        java.util.Set<String> bad = new java.util.TreeSet<>();
        boolean text = false;
        for (String raw : asm.split("\n")) {
            String line = raw.substring(0, commentStart(raw)).trim();
            if (line.isEmpty()) continue;
            if (line.equals(".text")) { text = true; continue; }
            if (line.equals(".data")) { text = false; continue; }
            if (!text) continue;
            int colon = line.indexOf(':');
            if (colon >= 0 && line.substring(0, colon).matches("[A-Za-z0-9_.$]+")) line = line.substring(colon + 1).trim();
            if (line.isEmpty() || line.startsWith(".")) continue;
            String op = line.split("\\s+")[0];
            if (!PORTABLE.contains(op)) bad.add(op);
        }
        return bad;
    }

    private static boolean allPortable(Path dir) {
        try {
            List<Path> files = new java.util.ArrayList<>();
            try (java.util.stream.Stream<Path> w = Files.walk(dir)) {
                w.filter(x -> x.toString().endsWith(".cps")).forEach(files::add);
            }
            for (Path f : files) {
                PipelineResult r = new PipelineRunner().run(read(f));
                java.util.Set<String> bad = unsupportedMnemonics(r.mips);
                if (!bad.isEmpty()) {
                    System.out.println("        instrucciones no portables en " + f + ": " + bad);
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Posición del '#' que inicia un comentario (fuera de comillas), o la longitud de la línea. */
    private static int commentStart(String line) {
        boolean inStr = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') inStr = !inStr;
            else if (c == '#' && !inStr) return i;
        }
        return line.length();
    }

    private static boolean commentsAscii(String asm) {
        for (String line : asm.split("\n")) {
            for (char c : line.substring(commentStart(line)).toCharArray()) if (c > 127) return false;
        }
        return true;
    }

    // ── Ayudas para las pruebas de TAC ─────────────────────────────────────
    private static Predicate<PipelineResult> tacHas(String... parts) {
        return r -> {
            if (!r.isOk() || r.tac.isEmpty()) return false;
            for (String p : parts) if (!r.tac.contains(p)) return false;
            return true;
        };
    }

    private static int symbolOffset(PipelineResult r, String name) {
        for (semantic.Scope sc : r.allScopes) {
            semantic.Symbol s = sc.symbols.get(name);
            if (s != null && s.offset >= 0) return s.offset;
        }
        return -1;
    }

    private static String labelOf(PipelineResult r, String name) {
        for (semantic.Scope sc : r.allScopes) {
            semantic.Symbol s = sc.symbols.get(name);
            if (s != null && !s.label.isEmpty()) return s.label;
        }
        return "";
    }

    /** Todo .cps sin errores de las carpetas dadas debe producir TAC bien formado. */
    private static boolean allWellFormed(Path... dirs) {
        try {
            for (Path dir : dirs) {
                List<Path> files = new java.util.ArrayList<>();
                try (java.util.stream.Stream<Path> w = Files.walk(dir)) {
                    w.filter(x -> x.toString().endsWith(".cps")).forEach(files::add);
                }
                for (Path f : files) {
                    PipelineResult r = new PipelineRunner().run(read(f));
                    if (!r.isOk()) continue;
                    String problem = wellFormed(r.tac);
                    if (problem != null) {
                        System.out.println("        TAC mal formado en " + f + ": " + problem);
                        return false;
                    }
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Verifica que cada salto apunte a una etiqueta de su función y cada call a una función existente. */
    static String wellFormed(String tac) {
        java.util.Set<String> funcs = new java.util.HashSet<>();
        for (String l : tac.split("\n")) if (l.startsWith("function ")) funcs.add(l.substring(9, l.indexOf(':')));
        java.util.Set<String> labels = new java.util.HashSet<>();
        java.util.List<String> jumps = new java.util.ArrayList<>();
        java.util.List<String> calls = new java.util.ArrayList<>();
        java.util.regex.Pattern jump = java.util.regex.Pattern.compile("(?:goto|pushhandler) (L\\d+)\\b");
        java.util.regex.Pattern call = java.util.regex.Pattern.compile("(?:^|= )call ([^,]+),");
        for (String l : (tac + "\nend x").split("\n")) {
            String t = l.trim();
            if (t.startsWith("function ") || t.startsWith("end ")) {
                for (String j : jumps) if (!labels.contains(j)) return "salto a etiqueta inexistente " + j;
                labels.clear();
                jumps.clear();
                continue;
            }
            if (!l.startsWith(" ") && t.endsWith(":") && !t.startsWith(";")) {
                labels.add(t.substring(0, t.length() - 1));
                continue;
            }
            java.util.regex.Matcher m = jump.matcher(t);
            if (m.find()) jumps.add(m.group(1));
            m = call.matcher(t);
            if (m.find()) calls.add(m.group(1));
        }
        for (String c : calls) if (!funcs.contains(c)) return "call a función inexistente " + c;
        return null;
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
