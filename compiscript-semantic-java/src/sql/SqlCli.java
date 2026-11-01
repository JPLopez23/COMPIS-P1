package sql;

import sql.exec.ExecResult;
import sql.storage.Database;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Línea de comandos del intérprete SQL. */
public final class SqlCli {
    private SqlCli() {}

    public static void usage() {
        System.err.println("Uso:");
        System.err.println("  java -cp out;lib/antlr-4.13.2-complete.jar Main sql <archivo.sql> [opciones]");
        System.err.println("  java -cp out;lib/antlr-4.13.2-complete.jar Main sql -e \"SELECT ...;\" [opciones]");
        System.err.println("  java -cp out;lib/antlr-4.13.2-complete.jar Main sql-gui [-db carpeta]");
        System.err.println();
        System.err.println("  -db carpeta   Carpeta de la base de datos (default: database)");
        System.err.println("  -check        Solo analiza (léxico, sintáctico, semántico); no ejecuta");
        System.err.println("  -tokens       Imprime los tokens");
        System.err.println("  -tree         Imprime el árbol sintáctico");
        System.err.println("  -ast          Imprime el AST");
    }

    /** Ejecuta el subcomando 'sql'; devuelve el código de salida. */
    public static int run(String[] args) {
        String dbDir = "database", source = null, file = null;
        boolean check = false, tokens = false, tree = false, ast = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-db": dbDir = args[++i]; break;
                case "-e": source = args[++i]; break;
                case "-check": check = true; break;
                case "-tokens": tokens = true; break;
                case "-tree": tree = true; break;
                case "-ast": ast = true; break;
                default:
                    if (args[i].startsWith("-")) { System.err.println("Argumento desconocido: " + args[i]); usage(); return 2; }
                    file = args[i];
            }
        }
        try {
            if (source == null) {
                if (file == null) { usage(); return 2; }
                source = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
            }
            Path dir = Paths.get(dbDir);
            Database db = new Database(dir);
            SqlPipeline.Result res = new SqlPipeline().run(source, db, !check);

            if (tokens) for (SqlPipeline.TokenInfo t : res.tokens)
                System.out.printf("%-10s %-20s %d:%d%n", t.type, t.text, t.line, t.col);
            if (tree) System.out.print(res.treeText);
            if (ast) System.out.print(res.astText);
            for (ExecResult r : res.results) {
                System.out.println("> " + r.statement);
                System.out.print(r.format());
            }
            if (!res.isOk() || check) System.out.print(res.report());
            return res.isOk() ? 0 : 1;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
