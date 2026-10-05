package gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Aplica una gramática editada desde el IDE: la escribe en grammar/Compiscript.g4,
 * regenera el parser con ANTLR, recompila el proyecto y relanza el IDE. Si algún
 * paso falla, restaura la gramática anterior. Asume que el IDE se ejecutó desde la
 * raíz del proyecto, igual que build.bat.
 */
public class GrammarBuilder {

    public static final Path GRAMMAR = Paths.get("grammar", "Compiscript.g4");
    private static final Path SRC = Paths.get("src");
    private static final Path OUT = Paths.get("out");
    private static final Path SRC_GENERATED = Paths.get("src", "generated");
    private static final Path OUT_GENERATED = Paths.get("out", "generated");

    /** Resultado de applyGrammar: éxito o fallo, más el registro de la compilación. */
    public static final class Result {
        public final boolean ok;
        public final String log;

        Result(boolean ok, String log) {
            this.ok = ok;
            this.log = log;
        }
    }

    /** Busca el jar de ANTLR en el classpath; si no aparece, usa el de lib/. */
    public static Path antlrJar() {
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            String norm = entry.replace('\\', '/').toLowerCase();
            if (norm.matches(".*antlr-[0-9.]+-complete\\.jar")) return Paths.get(entry);
        }
        return Paths.get("lib", "antlr-4.13.2-complete.jar");
    }

    private static String tool(String name) {
        return Paths.get(System.getProperty("java.home"), "bin", name).toString();
    }

    /**
     * Revisa que la gramática sirva para el pipeline antes de tocar el disco. Devuelve
     * null si es apta, o el motivo por el que no lo es. El resto del proyecto está
     * cableado a las clases generated.Compiscript* y a la regla inicial program.
     */
    public static String validate(String content) {
        String noComments = content
            .replaceAll("(?s)/\\*.*?\\*/", " ")
            .replaceAll("(?m)//.*$", " ");

        java.util.regex.Matcher name = Pattern
            .compile("(?m)^\\s*(?:lexer\\s+|parser\\s+)?grammar\\s+([A-Za-z_][A-Za-z_0-9]*)\\s*;")
            .matcher(noComments);
        if (!name.find()) {
            return "No se encontró la declaración 'grammar <Nombre>;' en el archivo.";
        }
        if (!name.group(1).equals("Compiscript")) {
            return "La gramática se llama '" + name.group(1) + "'. El pipeline del IDE solo funciona con\n"
                 + "la gramática de Compiscript: la declaración debe ser 'grammar Compiscript;' y el\n"
                 + "archivo se guarda como grammar/Compiscript.g4.";
        }
        if (!Pattern.compile("(?m)^\\s*program\\s*:").matcher(noComments).find()) {
            return "La gramática no define la regla inicial 'program:'. El IDE arranca el análisis en\n"
                 + "esa regla (parser.program() y SemanticAnalyzer.visitProgram), así que es obligatoria.";
        }
        return null;
    }

    /** Escribe la gramática, regenera el parser y recompila. Restaura la anterior si algo falla. */
    public static Result applyGrammar(String content) {
        String invalid = validate(content);
        if (invalid != null) {
            return new Result(false, "No se aplicó ningún cambio.\n\n" + invalid + "\n");
        }

        StringBuilder log = new StringBuilder();
        byte[] backup = null;
        try {
            if (Files.exists(GRAMMAR)) backup = Files.readAllBytes(GRAMMAR);

            Files.createDirectories(GRAMMAR.getParent());
            Files.write(GRAMMAR, content.getBytes(StandardCharsets.UTF_8));

            deleteRecursively(SRC_GENERATED);
            deleteRecursively(OUT_GENERATED);

            log.append("[1/2] Generando parser ANTLR...\n");
            int gen = runAntlr(log);
            if (gen != 0) throw new IOException("ANTLR terminó con código " + gen);

            log.append("[2/2] Compilando fuentes Java...\n");
            List<String> cmd = new ArrayList<>(Arrays.asList(
                tool("javac"), "-encoding", "UTF-8",
                "-cp", antlrJar().toString(),
                "-d", OUT.toString(),
                "-sourcepath", SRC.toString()));
            cmd.addAll(javaSources());
            int cc = run(log, cmd.toArray(new String[0]));
            if (cc != 0) throw new IOException("javac terminó con código " + cc);

            log.append("\nCompilación exitosa. Reiniciando el IDE...\n");
            return new Result(true, log.toString());
        } catch (IOException | InterruptedException ex) {
            log.append("\nError: ").append(ex.getMessage()).append('\n');
            if (backup != null) {
                try {
                    Files.write(GRAMMAR, backup);
                    log.append("Se restauró la gramática anterior.\n");
                    deleteRecursively(SRC_GENERATED);
                    runAntlr(log);
                } catch (IOException | InterruptedException restore) {
                    log.append("No se pudo restaurar la gramática: ").append(restore.getMessage()).append('\n');
                }
            }
            return new Result(false, log.toString());
        }
    }

    /** Relanza el IDE con el mismo classpath y termina el proceso actual. */
    public static void restartGui() {
        try {
            new ProcessBuilder(tool("java"), "-cp", System.getProperty("java.class.path"), "Main", "gui")
                .inheritIO()
                .start();
        } catch (IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(null,
                "No se pudo reiniciar automáticamente. Cierra y vuelve a abrir el IDE.\n" + ex.getMessage(),
                "Reinicio", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        System.exit(0);
    }

    private static int runAntlr(StringBuilder log) throws IOException, InterruptedException {
        return run(log, tool("java"), "-jar", antlrJar().toString(),
            "-Dlanguage=Java", "-visitor", "-no-listener", "-package", "generated",
            "-Xexact-output-dir", "-o", SRC_GENERATED.toString(), GRAMMAR.toString());
    }

    private static List<String> javaSources() throws IOException {
        try (Stream<Path> paths = Files.walk(SRC)) {
            List<String> files = new ArrayList<>();
            paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> files.add(p.toString()));
            return files;
        }
    }

    private static int run(StringBuilder log, String... cmd) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) log.append("  ").append(line).append('\n');
        }
        return p.waitFor();
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                    // Un archivo bloqueado no aborta el rebuild; javac lo sobreescribe.
                }
            });
        }
    }
}
