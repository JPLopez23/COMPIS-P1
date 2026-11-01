package tests;

import sql.SqlPipeline;
import sql.exec.ExecResult;
import sql.storage.Database;
import sql.storage.Values;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Batería de pruebas del intérprete SQL (léxico, sintaxis, AST, semántica, ejecución, índices y subconsultas). */
public class SqlTestRunner {

    private static int passed = 0, failed = 0;
    private static final List<Path> tempDirs = new ArrayList<>();

    private static final String SEED =
        "CREATE TABLE estudiantes (id INT PRIMARY KEY, nombre VARCHAR(50) NOT NULL, edad INT, promedio FLOAT, activo BOOLEAN, nacimiento DATE);"
        + "CREATE TABLE cursos (id INT PRIMARY KEY, nombre VARCHAR(40) NOT NULL, creditos INT);"
        + "CREATE TABLE inscripciones (id INT PRIMARY KEY, estudiante_id INT NOT NULL, curso_id INT NOT NULL, nota FLOAT);"
        + "INSERT INTO estudiantes VALUES (1,'Ana Lopez',20,85.5,TRUE,'2005-03-14'),(2,'Luis Perez',17,70.0,TRUE,'2008-11-02'),"
        + "(3,'Maria Gomez',22,92.25,FALSE,'2003-07-30'),(4,'Carlos Ruiz',19,64.0,TRUE,'2006-01-09'),(5,'Sofia Diaz',18,NULL,TRUE,'2007-05-21');"
        + "INSERT INTO cursos VALUES (1,'Compiladores',4),(2,'Bases de Datos',4),(3,'Algoritmos',5);"
        + "INSERT INTO inscripciones VALUES (1,1,1,90.0),(2,1,3,80.0),(3,2,3,65.5),(4,3,1,95.0),(5,3,2,88.0),(6,4,3,70.0);";

    /** Base de datos temporal con utilidades para las pruebas. */
    private static class Db {
        final Path dir;
        Database db;
        Db(boolean seeded) throws IOException {
            dir = Files.createTempDirectory("sqltest");
            tempDirs.add(dir);
            db = new Database(dir);
            if (seeded) ok(SEED);
        }
        void reopen() throws IOException { db = new Database(dir); }
        SqlPipeline.Result run(String sql) { return new SqlPipeline().run(sql, db, true); }
        SqlPipeline.Result check(String sql) { return new SqlPipeline().run(sql, db, false); }
        void ok(String sql) {
            SqlPipeline.Result r = run(sql);
            if (!r.isOk()) throw new AssertionError("se esperaba éxito para: " + sql + "\n" + r.report());
        }
        /** Filas del último SELECT como "a|b|c". */
        List<String> rows(String sql) {
            SqlPipeline.Result r = run(sql);
            if (!r.isOk()) throw new AssertionError("se esperaba éxito para: " + sql + "\n" + r.report());
            return format(r.results.get(r.results.size() - 1));
        }
        ExecResult last(String sql) {
            SqlPipeline.Result r = run(sql);
            if (!r.isOk()) throw new AssertionError("se esperaba éxito para: " + sql + "\n" + r.report());
            return r.results.get(r.results.size() - 1);
        }
        /** Texto de errores (todas las fases) al analizar la sentencia, "" si no hay. */
        String errors(String sql) {
            SqlPipeline.Result r = check(sql);
            return r.isOk() ? "" : r.report();
        }
    }

    private static List<String> format(ExecResult r) {
        List<String> out = new ArrayList<>();
        for (Object[] row : r.rows) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < row.length; i++) sb.append(i > 0 ? "|" : "").append(Values.display(row[i]));
            out.add(sb.toString());
        }
        return out;
    }

    private interface Body { void run() throws Exception; }

    private static void test(String name, Body body) {
        try {
            body.run();
            passed++;
            System.out.println("  ok    " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("  FALLA " + name + "\n        " + String.valueOf(t.getMessage()).replace("\n", "\n        "));
            if (!(t instanceof AssertionError)) t.printStackTrace(System.out);
        }
    }

    private static void eq(Object expected, Object actual) {
        if (!String.valueOf(expected).equals(String.valueOf(actual)))
            throw new AssertionError("esperado: " + expected + "\n        obtenido: " + actual);
    }

    private static void has(String text, String fragment) {
        if (!text.contains(fragment)) throw new AssertionError("se esperaba encontrar \"" + fragment + "\" en:\n" + text);
    }

    private static void isTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }

    private static List<String> list(String... items) { return List.of(items); }

    public static void main(String[] args) throws Exception {
        System.out.println("== Léxico y sintaxis ==");
        lexerAndParser();
        System.out.println("== AST ==");
        astTests();
        System.out.println("== Análisis semántico ==");
        semantic();
        System.out.println("== CREATE / DROP / INSERT y almacenamiento ==");
        storage();
        System.out.println("== SELECT / WHERE / UPDATE / DELETE ==");
        selectTests();
        System.out.println("== JOIN, agregaciones, GROUP BY ==");
        joinsAndAggregates();
        System.out.println("== Índices ==");
        indexes();
        System.out.println("== Subconsultas ==");
        subqueries();

        for (Path d : tempDirs) deleteTree(d);
        System.out.println();
        System.out.println("Resultado: " + passed + " pruebas correctas, " + failed + " con falla");
        System.exit(failed == 0 ? 0 : 1);
    }

    private static void deleteTree(Path dir) {
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) { }
    }

    // ── Entregas 1 y 2 ──────────────────────────────────────────────────────
    private static void lexerAndParser() throws Exception {
        Db d = new Db(false);
        test("tokens de un SELECT, sin distinguir mayúsculas", () -> {
            SqlPipeline.Result r = d.check("sElEcT nombre FROM estudiantes WHERE edad >= 18;");
            eq(9, r.tokens.size());
            eq("SELECT", r.tokens.get(0).type);
            eq("ID", r.tokens.get(1).type);
            eq("GE", r.tokens.get(6).type);
            eq("INTLIT", r.tokens.get(7).type);
            eq("1", r.tokens.get(0).line);
        });
        test("comentarios y saltos de línea se ignoran", () -> {
            SqlPipeline.Result r = d.check("-- hola\nSELECT 1 /* x */ FROM t;");
            eq(0, r.lexicalErrors.size());
            eq(0, r.syntaxErrors.size());
        });
        test("error léxico: carácter desconocido, con posición", () -> {
            SqlPipeline.Result r = d.check("SELECT @ FROM t;");
            isTrue(!r.lexicalErrors.isEmpty(), "debía haber error léxico");
            eq(1, r.lexicalErrors.get(0).line);
            eq(8, r.lexicalErrors.get(0).column);
        });
        test("error léxico: cadena sin cerrar", () -> {
            isTrue(!d.check("INSERT INTO t VALUES ('abc);").lexicalErrors.isEmpty(), "debía haber error léxico");
        });
        test("error sintáctico: falta FROM", () -> {
            SqlPipeline.Result r = d.check("SELECT nombre WHERE x = 1;");
            isTrue(!r.syntaxErrors.isEmpty(), "debía haber error sintáctico");
            eq(1, r.syntaxErrors.get(0).line);
        });
        test("error sintáctico en la segunda línea", () -> {
            SqlPipeline.Result r = d.check("SELECT 1 FROM t;\nDELETE t;");
            eq(2, r.syntaxErrors.get(0).line);
        });
        test("árbol sintáctico y DOT disponibles", () -> {
            SqlPipeline.Result r = d.check("SELECT a FROM t;");
            has(r.treeText, "selectStmt");
            has(r.treeDot, "digraph");
        });
        test("varias sentencias separadas por ;", () -> {
            SqlPipeline.Result r = d.check(";; DROP TABLE a; DROP TABLE b;;");
            eq(0, r.syntaxErrors.size());
            eq(2, r.ast.statements.size());
        });
        test("precedencia: AND antes que OR, NOT sobre comparación", () -> {
            SqlPipeline.Result r = d.check("DELETE FROM t WHERE NOT a = 1 OR b = 2 AND c = 3;");
            has(r.astText, "Binary OR");
            has(r.astText, "Unary NOT");
        });
    }

    private static void astTests() throws Exception {
        Db d = new Db(true);
        test("AST de un SELECT con WHERE, JOIN, GROUP BY y ORDER BY", () -> {
            SqlPipeline.Result r = d.check("SELECT e.nombre, COUNT(*) AS n FROM estudiantes e JOIN inscripciones i ON i.estudiante_id = e.id "
                + "WHERE e.edad > 18 GROUP BY e.nombre ORDER BY n DESC;");
            eq(0, r.semanticErrors.size());
            for (String frag : new String[]{"Select", "Table estudiantes AS e", "InnerJoin", "Aggregate COUNT : INT", "GroupBy", "OrderBy", "Order DESC", "Binary > : BOOLEAN"})
                has(r.astText, frag);
        });
        test("AST de INSERT, UPDATE, CREATE TABLE e INDEX", () -> {
            SqlPipeline.Result r = d.check("INSERT INTO cursos VALUES (9,'X',1); UPDATE cursos SET creditos = 2 WHERE id = 9;"
                + "CREATE TABLE z (a INT PRIMARY KEY, b VARCHAR(5)); CREATE INDEX iz ON z(b);");
            for (String frag : new String[]{"Insert cursos", "Row", "Update cursos", "Set creditos", "CreateTable z", "Column a INT PRIMARY KEY", "Column b VARCHAR(5)", "CreateIndex iz ON z(b)"})
                has(r.astText, frag);
        });
    }

    // ── Entrega 3 ───────────────────────────────────────────────────────────
    private static void semantic() throws Exception {
        Db d = new Db(true);
        String[][] cases = {
            {"SELECT * FROM fantasma;", "La tabla 'fantasma' no existe"},
            {"SELECT nope FROM estudiantes;", "La columna 'nope' no existe"},
            {"SELECT e.nope FROM estudiantes e;", "La columna 'nope' no existe en la tabla 'e'"},
            {"SELECT x.nombre FROM estudiantes;", "la tabla o alias 'x' no está en el FROM"},
            {"SELECT nombre FROM estudiantes WHERE edad = 'veinte';", "no se puede comparar INT con VARCHAR"},
            {"SELECT nombre FROM estudiantes WHERE nombre > 5;", "no se puede comparar VARCHAR con INT"},
            {"SELECT nombre FROM estudiantes WHERE edad;", "debe ser booleana"},
            {"SELECT nombre FROM estudiantes WHERE nacimiento > 'ayer';", "Fecha inválida"},
            {"SELECT edad + nombre FROM estudiantes;", "requiere números"},
            {"SELECT nombre FROM estudiantes WHERE edad > 18 AND nombre;", "AND requiere operandos booleanos"},
            {"INSERT INTO estudiantes VALUES (9,'X');", "Se esperaban 6 valores y hay 2"},
            {"INSERT INTO estudiantes (id, nombre) VALUES (9,'X',3);", "Se esperaban 2 valores y hay 3"},
            {"INSERT INTO estudiantes VALUES ('9','X',1,1.0,TRUE,'2000-01-01');", "Tipos incompatibles"},
            {"INSERT INTO estudiantes VALUES (9,'X',1.5,1.0,TRUE,'2000-01-01');", "Tipos incompatibles"},
            {"INSERT INTO estudiantes VALUES (9,NULL,1,1.0,TRUE,'2000-01-01');", "es NOT NULL"},
            {"INSERT INTO estudiantes (id) VALUES (9);", "'nombre' es NOT NULL y no recibe valor"},
            {"INSERT INTO estudiantes VALUES (9,'X',1,1.0,TRUE,'2000-13-45');", "Fecha inválida"},
            {"INSERT INTO cursos VALUES (9,'" + "x".repeat(41) + "',1);", "excede la longitud máxima 40"},
            {"INSERT INTO estudiantes (id, zzz) VALUES (9,1);", "La columna 'zzz' no existe"},
            {"INSERT INTO fantasma VALUES (1);", "La tabla 'fantasma' no existe"},
            {"UPDATE estudiantes SET edad = 'x' WHERE id = 1;", "Tipos incompatibles"},
            {"UPDATE estudiantes SET zzz = 1;", "La columna 'zzz' no existe"},
            {"UPDATE estudiantes SET nombre = NULL;", "es NOT NULL"},
            {"DELETE FROM estudiantes WHERE zzz = 1;", "La columna 'zzz' no existe"},
            {"SELECT id FROM estudiantes e JOIN inscripciones i ON i.estudiante_id = e.id;", "Referencia ambigua: la columna 'id' existe en e, i"},
            {"SELECT * FROM estudiantes e JOIN cursos e ON e.id = e.id;", "'e' aparece más de una vez"},
            {"CREATE TABLE estudiantes (a INT);", "La tabla 'estudiantes' ya existe"},
            {"CREATE TABLE t (a INT, a INT);", "Columna duplicada 'a'"},
            {"CREATE TABLE t (a INT PRIMARY KEY, b INT PRIMARY KEY);", "más de una PRIMARY KEY"},
            {"CREATE TABLE t (a VARCHAR(0));", "longitud mayor que 0"},
            {"DROP TABLE fantasma;", "La tabla 'fantasma' no existe"},
            {"CREATE INDEX i1 ON fantasma(a);", "La tabla 'fantasma' no existe"},
            {"CREATE INDEX i1 ON estudiantes(zzz);", "La columna 'zzz' no existe en la tabla 'estudiantes'"},
            {"DROP INDEX nada;", "El índice 'nada' no existe"},
            {"CREATE INDEX i1 ON estudiantes(edad); CREATE INDEX i1 ON estudiantes(nombre);", "El índice 'i1' ya existe"},
            {"SELECT nombre FROM estudiantes WHERE id = (SELECT id, nombre FROM estudiantes);", "subconsulta escalar debe devolver una sola columna y devuelve 2"},
            {"SELECT nombre FROM estudiantes WHERE id IN (SELECT id, nombre FROM estudiantes);", "subconsulta de IN debe devolver una sola columna"},
            {"SELECT nombre FROM estudiantes WHERE id IN (SELECT nombre FROM estudiantes);", "no se puede comparar INT con VARCHAR"},
            {"SELECT nombre, COUNT(*) FROM estudiantes;", "debe aparecer en GROUP BY"},
            {"SELECT edad, nombre FROM estudiantes GROUP BY edad;", "debe aparecer en GROUP BY"},
            {"SELECT nombre FROM estudiantes WHERE COUNT(*) > 1;", "no se permite en esta cláusula"},
            {"SELECT SUM(nombre) FROM estudiantes;", "SUM requiere una columna numérica"},
            {"SELECT AVG(activo) FROM estudiantes;", "AVG requiere una columna numérica"},
            {"SELECT nombre FROM estudiantes HAVING nombre;", "HAVING"},
            {"SELECT z.* FROM estudiantes;", "'z' no está en el FROM"},
        };
        for (String[] c : cases) {
            test("error: " + c[0].substring(0, Math.min(c[0].length(), 60)), () -> {
                String err = d.errors(c[0]);
                has(err, c[1]);
                has(err, "[1:");
            });
        }
        test("el error indica la posición (línea:columna) de la expresión", () -> {
            String err = d.errors("SELECT nombre\nFROM estudiantes\nWHERE edad = 'x';");
            has(err, "[3:7]");
        });
        test("un script se valida completo antes de ejecutarse (CREATE + INSERT en el mismo script)", () -> {
            Db e = new Db(false);
            SqlPipeline.Result r = e.check("CREATE TABLE t (a INT); INSERT INTO t VALUES (1); SELECT a FROM t;");
            eq(0, r.semanticErrors.size());
        });
        test("un script con error semántico no ejecuta nada", () -> {
            Db e = new Db(false);
            SqlPipeline.Result r = e.run("CREATE TABLE t (a INT); INSERT INTO t VALUES ('x');");
            isTrue(!r.semanticErrors.isEmpty(), "debía haber error");
            isTrue(e.db.catalog().table("t") == null, "no debía crearse la tabla");
        });
        test("sentencias válidas no producen errores", () -> {
            eq("", d.errors("SELECT nombre FROM estudiantes WHERE edad >= 18 AND activo = TRUE ORDER BY nombre;"));
            eq("", d.errors("SELECT * FROM estudiantes WHERE nacimiento >= '2005-01-01';"));
            eq("", d.errors("SELECT e.nombre FROM estudiantes e WHERE EXISTS (SELECT 1 FROM inscripciones i WHERE i.estudiante_id = e.id);"));
        });
    }

    // ── Entrega 4 ───────────────────────────────────────────────────────────
    private static void storage() throws Exception {
        test("CREATE TABLE crea metadata.txt y el .csv", () -> {
            Db d = new Db(false);
            d.ok("CREATE TABLE t (id INT PRIMARY KEY, n VARCHAR(10) NOT NULL, f FLOAT, b BOOLEAN, d DATE);");
            isTrue(Files.exists(d.dir.resolve("metadata.txt")), "falta metadata.txt");
            isTrue(Files.exists(d.dir.resolve("t.csv")), "falta t.csv");
            String meta = new String(Files.readAllBytes(d.dir.resolve("metadata.txt")), "UTF-8");
            has(meta, "TABLE t");
            has(meta, "COLUMN id INT PRIMARY_KEY");
            has(meta, "COLUMN n VARCHAR(10) NOT_NULL");
            has(meta, "COLUMN d DATE");
        });
        test("INSERT guarda las filas en el .csv", () -> {
            Db d = new Db(true);
            String csv = new String(Files.readAllBytes(d.dir.resolve("cursos.csv")), "UTF-8");
            has(csv, "id,nombre,creditos");
            has(csv, "2,Bases de Datos,4");
        });
        test("los datos persisten al reabrir la base desde los archivos", () -> {
            Db d = new Db(true);
            d.reopen();
            eq(list("Compiladores", "Bases de Datos", "Algoritmos"), d.rows("SELECT nombre FROM cursos;"));
            eq("Sofia Diaz|NULL", String.join("", d.rows("SELECT nombre, promedio FROM estudiantes WHERE id = 5;")));
        });
        test("INSERT con lista de columnas deja NULL en las omitidas", () -> {
            Db d = new Db(true);
            d.ok("INSERT INTO estudiantes (id, nombre) VALUES (10, 'Nuevo');");
            eq(list("Nuevo|NULL|NULL"), d.rows("SELECT nombre, edad, activo FROM estudiantes WHERE id = 10;"));
        });
        test("INSERT convierte INT a FLOAT y texto a DATE", () -> {
            Db d = new Db(true);
            d.ok("INSERT INTO estudiantes VALUES (11, 'Z', 30, 80, TRUE, '1990-12-31');");
            eq(list("80.0|1990-12-31"), d.rows("SELECT promedio, nacimiento FROM estudiantes WHERE id = 11;"));
        });
        test("texto con comas, comillas, saltos de línea y espacios sobrevive al archivo", () -> {
            Db d = new Db(false);
            d.ok("CREATE TABLE t (id INT, s VARCHAR(50));");
            d.ok("INSERT INTO t VALUES (1, 'a,b'), (2, 'dice \"hola\"'), (3, ''), (4, ' pad '), (5, 'it''s'), (6, 'x\\y');");
            d.reopen();
            eq(list("a,b", "dice \"hola\"", "", " pad ", "it's", "x\\y"), d.rows("SELECT s FROM t ORDER BY id;"));
        });
        test("clave primaria duplicada se rechaza (existente y dentro del mismo INSERT)", () -> {
            Db d = new Db(true);
            SqlPipeline.Result r = d.run("INSERT INTO cursos VALUES (1,'Otro',1);");
            has(r.report(), "Clave primaria duplicada: id = 1");
            r = d.run("INSERT INTO cursos VALUES (50,'A',1),(50,'B',1);");
            has(r.report(), "Clave primaria duplicada");
            eq(3, d.rows("SELECT id FROM cursos;").size());
        });
        test("DROP TABLE borra el .csv, los índices y el esquema", () -> {
            Db d = new Db(true);
            d.ok("CREATE INDEX ic ON cursos(creditos);");
            isTrue(Files.exists(d.dir.resolve("indexes").resolve("ic.idx")), "falta ic.idx");
            d.ok("DROP TABLE cursos;");
            isTrue(!Files.exists(d.dir.resolve("cursos.csv")), "cursos.csv sigue existiendo");
            isTrue(!Files.exists(d.dir.resolve("indexes").resolve("ic.idx")), "ic.idx sigue existiendo");
            has(d.errors("SELECT * FROM cursos;"), "no existe");
            d.reopen();
            isTrue(d.db.catalog().table("cursos") == null && d.db.catalog().index("ic") == null, "el esquema no se actualizó");
        });
        test("se puede recrear una tabla eliminada", () -> {
            Db d = new Db(true);
            d.ok("DROP TABLE cursos; CREATE TABLE cursos (id INT);");
            eq(0, d.rows("SELECT * FROM cursos;").size());
        });
    }

    // ── Entrega 5 ───────────────────────────────────────────────────────────
    private static void selectTests() throws Exception {
        Db d = new Db(true);
        test("SELECT con WHERE (el ejemplo del enunciado)", () ->
            eq(list("Ana Lopez", "Maria Gomez", "Carlos Ruiz", "Sofia Diaz"), d.rows("SELECT nombre FROM estudiantes WHERE edad >= 18;")));
        test("SELECT * devuelve todas las columnas", () -> {
            ExecResult r = d.last("SELECT * FROM cursos WHERE id = 2;");
            eq(list("id", "nombre", "creditos"), r.columns);
            eq(list("2|Bases de Datos|4"), format(r));
        });
        test("AND, OR y paréntesis", () -> {
            eq(list("Ana Lopez", "Luis Perez"), d.rows("SELECT nombre FROM estudiantes WHERE activo = TRUE AND promedio > 65;"));
            eq(list("Luis Perez", "Maria Gomez"), d.rows("SELECT nombre FROM estudiantes WHERE edad = 17 OR edad = 22;"));
            eq(list("Luis Perez"), d.rows("SELECT nombre FROM estudiantes WHERE (edad = 17 OR edad = 22) AND activo = TRUE;"));
        });
        test("NOT, <>, IS NULL, IS NOT NULL", () -> {
            eq(list("Sofia Diaz"), d.rows("SELECT nombre FROM estudiantes WHERE promedio IS NULL;"));
            eq(4, d.rows("SELECT nombre FROM estudiantes WHERE promedio IS NOT NULL;").size());
            eq(list("Maria Gomez"), d.rows("SELECT nombre FROM estudiantes WHERE NOT activo = TRUE;"));
            eq(4, d.rows("SELECT id FROM estudiantes WHERE id <> 3;").size());
            eq(4, d.rows("SELECT id FROM estudiantes WHERE id != 3;").size());
        });
        test("NULL en comparaciones nunca es verdadero (lógica de tres valores)", () -> {
            eq(0, d.rows("SELECT id FROM estudiantes WHERE promedio = NULL;").size());
            eq(list("1", "2", "3", "4"), d.rows("SELECT id FROM estudiantes WHERE promedio > 0 OR promedio < 0 ORDER BY id;"));
            eq(list("1", "2", "3", "4", "5"), d.rows("SELECT id FROM estudiantes WHERE promedio > 0 OR id = 5 OR promedio < 0 ORDER BY id;"));
        });
        test("comparar fechas con literales de texto", () -> {
            eq(list("Ana Lopez", "Maria Gomez"), d.rows("SELECT nombre FROM estudiantes WHERE nacimiento < '2006-01-01' ORDER BY nombre;"));
            eq(list("Luis Perez"), d.rows("SELECT nombre FROM estudiantes WHERE nacimiento = '2008-11-02';"));
        });
        test("ORDER BY ascendente, descendente y por varias columnas", () -> {
            eq(list("2", "5", "4", "1", "3"), d.rows("SELECT id FROM estudiantes ORDER BY edad;"));
            eq(list("3", "1", "4", "5", "2"), d.rows("SELECT id FROM estudiantes ORDER BY edad DESC;"));
            eq(list("1", "2", "4", "5", "3"), d.rows("SELECT id FROM estudiantes ORDER BY activo DESC, id;"));
            eq(list("Sofia Diaz", "Carlos Ruiz", "Luis Perez", "Ana Lopez", "Maria Gomez"), d.rows("SELECT nombre FROM estudiantes ORDER BY promedio;"));
        });
        test("ORDER BY por alias y por expresión", () -> {
            eq(list("2|34", "5|36", "4|38", "1|40", "3|44"), d.rows("SELECT id, edad * 2 AS doble FROM estudiantes ORDER BY doble;"));
        });
        test("expresiones aritméticas en SELECT y WHERE", () -> {
            eq(list("2|6.5"), d.rows("SELECT 1 + 1, 13 / 2.0 FROM cursos WHERE id = 1;"));
            eq(list("3"), d.rows("SELECT 7 / 2 FROM cursos WHERE id = 1;"));
            eq(list("Algoritmos"), d.rows("SELECT nombre FROM cursos WHERE creditos * 2 > 9;"));
            eq(list("-5"), d.rows("SELECT -creditos FROM cursos WHERE id = 3;"));
        });
        test("división entre cero es un error de ejecución con posición", () -> {
            SqlPipeline.Result r = d.run("SELECT 1 / 0 FROM cursos;");
            has(r.report(), "División entre cero");
            has(r.report(), "[1:");
        });
        Db u = new Db(true);
        test("UPDATE con WHERE y expresiones sobre la fila", () -> {
            eq("2 filas actualizadas en 'cursos'", u.last("UPDATE cursos SET creditos = creditos + 1 WHERE creditos = 4;").message);
            eq(list("5", "5", "5"), u.rows("SELECT creditos FROM cursos;"));
        });
        test("UPDATE de varias columnas y de una fila", () -> {
            eq("1 fila actualizada en 'estudiantes'", u.last("UPDATE estudiantes SET nombre = 'Ana M.', edad = 21, promedio = NULL WHERE id = 1;").message);
            eq(list("Ana M.|21|NULL"), u.rows("SELECT nombre, edad, promedio FROM estudiantes WHERE id = 1;"));
        });
        test("UPDATE sin WHERE afecta todas las filas y persiste", () -> {
            u.ok("UPDATE cursos SET creditos = 1;");
            u.reopen();
            eq(list("1", "1", "1"), u.rows("SELECT creditos FROM cursos;"));
        });
        test("UPDATE de clave primaria a un valor existente se rechaza", () -> {
            has(u.run("UPDATE cursos SET id = 2 WHERE id = 1;").report(), "Clave primaria duplicada");
            eq(list("1", "2", "3"), u.rows("SELECT id FROM cursos ORDER BY id;"));
        });
        test("UPDATE de clave primaria a un valor libre funciona", () -> {
            u.ok("UPDATE cursos SET id = 30 WHERE id = 3;");
            eq(list("1", "2", "30"), u.rows("SELECT id FROM cursos ORDER BY id;"));
        });
        test("DELETE con WHERE y sin WHERE", () -> {
            eq("2 filas eliminadas de 'inscripciones'", u.last("DELETE FROM inscripciones WHERE nota < 80;").message);
            eq(4, u.rows("SELECT id FROM inscripciones;").size());
            u.ok("DELETE FROM inscripciones;");
            u.reopen();
            eq(0, u.rows("SELECT id FROM inscripciones;").size());
        });
        test("DELETE que no coincide con nada no cambia el archivo", () -> {
            eq("0 filas eliminadas de 'cursos'", u.last("DELETE FROM cursos WHERE id = 999;").message);
            eq(3, u.rows("SELECT id FROM cursos;").size());
        });
    }

    // ── Entrega 6 ───────────────────────────────────────────────────────────
    private static void joinsAndAggregates() throws Exception {
        Db d = new Db(true);
        test("INNER JOIN de dos tablas", () ->
            eq(list("Ana Lopez|Algoritmos", "Ana Lopez|Compiladores", "Carlos Ruiz|Algoritmos", "Luis Perez|Algoritmos", "Maria Gomez|Bases de Datos", "Maria Gomez|Compiladores"),
                d.rows("SELECT e.nombre, c.nombre FROM estudiantes e INNER JOIN inscripciones i ON i.estudiante_id = e.id JOIN cursos c ON c.id = i.curso_id ORDER BY e.nombre, c.nombre;")));
        test("JOIN con WHERE sobre columnas de ambas tablas", () ->
            eq(list("Ana Lopez|90.0", "Maria Gomez|95.0"),
                d.rows("SELECT e.nombre, i.nota FROM estudiantes e JOIN inscripciones i ON e.id = i.estudiante_id WHERE i.nota >= 90 AND e.edad > 18 ORDER BY e.nombre;")));
        test("estudiantes sin inscripciones no aparecen en el INNER JOIN", () -> {
            eq(2, d.rows("SELECT e.id FROM estudiantes e JOIN inscripciones i ON e.id = i.estudiante_id WHERE e.id = 1;").size());
            eq(0, d.rows("SELECT e.id FROM estudiantes e JOIN inscripciones i ON e.id = i.estudiante_id WHERE e.id = 5;").size());
        });
        test("SELECT * en un JOIN trae las columnas de todas las tablas", () -> {
            ExecResult r = d.last("SELECT * FROM cursos c JOIN inscripciones i ON i.curso_id = c.id WHERE i.id = 1;");
            eq(7, r.columns.size());
            eq(list("1|Compiladores|4|1|1|1|90.0"), format(r));
        });
        test("tabla.* en un JOIN", () ->
            eq(list("1|Compiladores|4"), d.rows("SELECT c.* FROM cursos c JOIN inscripciones i ON i.curso_id = c.id WHERE i.id = 1;")));
        test("autojoin con alias", () ->
            eq(list("Ana Lopez|Maria Gomez"), d.rows("SELECT a.nombre, b.nombre FROM estudiantes a JOIN estudiantes b ON a.edad + 2 = b.edad WHERE a.id = 1;")));
        test("COUNT, SUM, AVG, MIN, MAX", () ->
            eq(list("3|13|4.333333333333333|Algoritmos|5"), d.rows("SELECT COUNT(*), SUM(creditos), AVG(creditos), MIN(nombre), MAX(creditos) FROM cursos;")));
        test("COUNT(columna) ignora NULL; AVG/MIN/MAX ignoran NULL", () -> {
            eq(list("5|4"), d.rows("SELECT COUNT(*), COUNT(promedio) FROM estudiantes;"));
            eq(list("77.9375|64.0|92.25"), d.rows("SELECT AVG(promedio), MIN(promedio), MAX(promedio) FROM estudiantes;"));
        });
        test("agregados sobre una tabla vacía: COUNT=0 y el resto NULL", () -> {
            Db e = new Db(false);
            e.ok("CREATE TABLE v (n INT);");
            eq(list("0|NULL|NULL|NULL|NULL"), e.rows("SELECT COUNT(*), SUM(n), AVG(n), MIN(n), MAX(n) FROM v;"));
        });
        test("SUM de FLOAT y MIN/MAX de fechas", () -> {
            eq(list("311.75"), d.rows("SELECT SUM(promedio) FROM estudiantes;"));
            eq(list("2003-07-30|2008-11-02"), d.rows("SELECT MIN(nacimiento), MAX(nacimiento) FROM estudiantes;"));
        });
        test("GROUP BY con COUNT y AVG", () ->
            eq(list("1|2|92.5", "2|1|88.0", "3|3|71.83333333333333"), d.rows("SELECT curso_id, COUNT(*), AVG(nota) FROM inscripciones GROUP BY curso_id ORDER BY curso_id;")));
        test("GROUP BY con HAVING", () ->
            eq(list("1", "3"), d.rows("SELECT curso_id FROM inscripciones GROUP BY curso_id HAVING COUNT(*) >= 2 ORDER BY curso_id;")));
        test("HAVING con agregado que no está en el SELECT", () ->
            eq(list("3"), d.rows("SELECT curso_id FROM inscripciones GROUP BY curso_id HAVING SUM(nota) > 200;")));
        test("GROUP BY sobre un JOIN", () ->
            eq(list("Algoritmos|3", "Bases de Datos|1", "Compiladores|2"),
                d.rows("SELECT c.nombre, COUNT(*) FROM cursos c JOIN inscripciones i ON i.curso_id = c.id GROUP BY c.nombre ORDER BY c.nombre;")));
        test("ORDER BY un agregado", () ->
            eq(list("3|3", "1|2", "2|1"), d.rows("SELECT curso_id, COUNT(*) AS n FROM inscripciones GROUP BY curso_id ORDER BY COUNT(*) DESC;")));
        test("agrupar por una expresión", () ->
            eq(list("true|4", "false|1"), d.rows("SELECT edad >= 18, COUNT(*) FROM estudiantes GROUP BY edad >= 18 ORDER BY edad >= 18 DESC;")));
        test("WHERE se aplica antes de agrupar", () ->
            eq(list("1|2", "2|1", "3|1"), d.rows("SELECT curso_id, COUNT(*) FROM inscripciones WHERE nota > 70 GROUP BY curso_id ORDER BY curso_id;")));
    }

    // ── Entrega 7: índices ──────────────────────────────────────────────────
    private static void indexes() throws Exception {
        test("CREATE INDEX crea el archivo .idx y lo registra en metadata.txt", () -> {
            Db d = new Db(true);
            d.ok("CREATE INDEX idx_edad ON estudiantes(edad);");
            Path idx = d.dir.resolve("indexes").resolve("idx_edad.idx");
            isTrue(Files.exists(idx), "falta idx_edad.idx");
            String text = new String(Files.readAllBytes(idx), "UTF-8");
            has(text, "idx_edad");
            has(new String(Files.readAllBytes(d.dir.resolve("metadata.txt")), "UTF-8"), "INDEX idx_edad estudiantes edad");
        });
        test("el índice se usa en WHERE (igualdad) y evita recorrer todo el archivo", () -> {
            Db d = new Db(true);
            String without = String.join(",", d.rows("SELECT nombre FROM estudiantes WHERE edad = 19;"));
            long fullScan = d.last("SELECT nombre FROM estudiantes WHERE edad = 19;").rowsRead;
            eq(5, fullScan);
            d.ok("CREATE INDEX idx_edad ON estudiantes(edad);");
            ExecResult r = d.last("SELECT nombre FROM estudiantes WHERE edad = 19;");
            eq(without, String.join(",", format(r)));
            eq(1, r.rowsRead);
            has(String.join(" ", r.notes), "Índice 'idx_edad' usado");
        });
        test("el índice se usa en rangos (<, <=, >, >=) con el mismo resultado que sin índice", () -> {
            Db d = new Db(true);
            String[] where = {"edad > 18", "edad >= 18", "edad < 19", "edad <= 19", "18 < edad", "edad >= 17 AND edad < 21", "edad = 99"};
            List<String> plain = new ArrayList<>();
            for (String w : where) plain.add(String.join(",", d.rows("SELECT id FROM estudiantes WHERE " + w + " ORDER BY id;")));
            d.ok("CREATE INDEX idx_edad ON estudiantes(edad);");
            for (int i = 0; i < where.length; i++) {
                ExecResult r = d.last("SELECT id FROM estudiantes WHERE " + where[i] + " ORDER BY id;");
                eq(plain.get(i), String.join(",", format(r)));
                has(String.join(" ", r.notes), "idx_edad");
            }
            isTrue(d.last("SELECT id FROM estudiantes WHERE edad > 21;").rowsRead == 1, "el rango debía leer solo 1 fila");
            eq(0, d.last("SELECT id FROM estudiantes WHERE edad = 99;").rowsRead);
        });
        test("el índice funciona con VARCHAR, DATE y FLOAT", () -> {
            Db d = new Db(true);
            d.ok("CREATE INDEX i1 ON estudiantes(nombre); CREATE INDEX i2 ON estudiantes(nacimiento); CREATE INDEX i3 ON estudiantes(promedio);");
            ExecResult r = d.last("SELECT id FROM estudiantes WHERE nombre = 'Luis Perez';");
            eq(list("2"), format(r)); eq(1, r.rowsRead);
            r = d.last("SELECT id FROM estudiantes WHERE nacimiento >= '2007-01-01' ORDER BY id;");
            eq(list("2", "5"), format(r)); eq(2, r.rowsRead);
            r = d.last("SELECT id FROM estudiantes WHERE promedio > 80 ORDER BY id;");
            eq(list("1", "3"), format(r)); eq(2, r.rowsRead);
        });
        test("el WHERE completo se sigue evaluando sobre las filas que da el índice", () -> {
            Db d = new Db(true);
            d.ok("CREATE INDEX idx_edad ON estudiantes(edad);");
            ExecResult r = d.last("SELECT nombre FROM estudiantes WHERE edad >= 18 AND activo = TRUE ORDER BY nombre;");
            eq(list("Ana Lopez", "Carlos Ruiz", "Sofia Diaz"), format(r));
            has(String.join(" ", r.notes), "idx_edad");
            r = d.last("SELECT nombre FROM estudiantes WHERE edad = 20 OR edad = 22 ORDER BY nombre;");
            eq(list("Ana Lopez", "Maria Gomez"), format(r));
            eq(5, r.rowsRead); // con OR no se puede usar el índice
        });
        test("IN con lista de constantes usa el índice", () -> {
            Db d = new Db(true);
            d.ok("CREATE INDEX idx_edad ON estudiantes(edad);");
            ExecResult r = d.last("SELECT id FROM estudiantes WHERE edad IN (17, 22, 99) ORDER BY id;");
            eq(list("2", "3"), format(r));
            eq(2, r.rowsRead);
        });
        test("INSERT mantiene el índice (memoria y archivo)", () -> {
            Db d = new Db(true);
            d.ok("CREATE INDEX idx_edad ON estudiantes(edad);");
            d.ok("INSERT INTO estudiantes VALUES (6,'Nuevo',19,50.0,TRUE,'2006-02-02');");
            ExecResult r = d.last("SELECT id FROM estudiantes WHERE edad = 19 ORDER BY id;");
            eq(list("4", "6"), format(r)); eq(2, r.rowsRead);
            d.reopen();
            r = d.last("SELECT id FROM estudiantes WHERE edad = 19 ORDER BY id;");
            eq(list("4", "6"), format(r)); eq(2, r.rowsRead);
        });
        test("UPDATE mantiene el índice: el valor viejo ya no aparece y el nuevo sí", () -> {
            Db d = new Db(true);
            d.ok("CREATE INDEX idx_edad ON estudiantes(edad);");
            d.ok("UPDATE estudiantes SET edad = 40 WHERE id = 4;");
            eq(0, d.rows("SELECT id FROM estudiantes WHERE edad = 19;").size());
            ExecResult r = d.last("SELECT id, nombre FROM estudiantes WHERE edad = 40;");
            eq(list("4|Carlos Ruiz"), format(r)); eq(1, r.rowsRead);
            // las demás filas conservan su posición correcta aunque el archivo se reescribió
            eq(list("1|Ana Lopez"), d.rows("SELECT id, nombre FROM estudiantes WHERE edad = 20;"));
            eq(list("3|Maria Gomez"), d.rows("SELECT id, nombre FROM estudiantes WHERE edad = 22;"));
            d.reopen();
            eq(list("4"), d.rows("SELECT id FROM estudiantes WHERE edad = 40;"));
            eq(0, d.rows("SELECT id FROM estudiantes WHERE edad = 19;").size());
        });
        test("UPDATE que cambia el tamaño de las filas deja el índice consistente", () -> {
            Db d = new Db(true);
            d.ok("CREATE INDEX idx_edad ON estudiantes(edad); CREATE INDEX idx_nom ON estudiantes(nombre);");
            d.ok("UPDATE estudiantes SET nombre = 'Un nombre bastante mas largo que antes' WHERE id = 1;");
            eq(list("1"), d.rows("SELECT id FROM estudiantes WHERE nombre = 'Un nombre bastante mas largo que antes';"));
            for (int id = 2; id <= 5; id++) {
                String nombre = d.rows("SELECT nombre FROM estudiantes WHERE id = " + id + ";").get(0);
                eq(list(String.valueOf(id)), d.rows("SELECT id FROM estudiantes WHERE nombre = '" + nombre + "';"));
            }
        });
        test("DELETE mantiene el índice y las posiciones de las demás filas", () -> {
            Db d = new Db(true);
            d.ok("CREATE INDEX idx_edad ON estudiantes(edad);");
            d.ok("DELETE FROM estudiantes WHERE edad < 19;");
            eq(0, d.rows("SELECT id FROM estudiantes WHERE edad = 17;").size());
            ExecResult r = d.last("SELECT id FROM estudiantes WHERE edad >= 0 ORDER BY id;");
            eq(list("1", "3", "4"), format(r));
            eq(3, r.rowsRead);
            eq(list("3|Maria Gomez"), d.rows("SELECT id, nombre FROM estudiantes WHERE edad = 22;"));
            d.reopen();
            eq(list("4|Carlos Ruiz"), d.rows("SELECT id, nombre FROM estudiantes WHERE edad = 19;"));
        });
        test("UPDATE y DELETE usan el índice para encontrar las filas", () -> {
            Db d = new Db(true);
            d.ok("CREATE INDEX idx_edad ON estudiantes(edad);");
            ExecResult r = d.last("UPDATE estudiantes SET activo = FALSE WHERE edad = 20;");
            has(String.join(" ", r.notes), "idx_edad");
            eq(1, r.rowsRead);
            r = d.last("DELETE FROM estudiantes WHERE edad = 22;");
            has(String.join(" ", r.notes), "idx_edad");
            eq(1, r.rowsRead);
        });
        test("NULL no se indexa y no aparece en búsquedas por índice", () -> {
            Db d = new Db(true);
            d.ok("CREATE INDEX i ON estudiantes(promedio);");
            eq(4, d.rows("SELECT id FROM estudiantes WHERE promedio >= 0;").size());
            d.ok("UPDATE estudiantes SET promedio = 10.0 WHERE id = 5;");
            eq(5, d.rows("SELECT id FROM estudiantes WHERE promedio >= 0;").size());
            d.ok("UPDATE estudiantes SET promedio = NULL WHERE id = 1;");
            eq(4, d.rows("SELECT id FROM estudiantes WHERE promedio >= 0;").size());
        });
        test("DROP INDEX borra el archivo y las consultas vuelven a recorrer la tabla", () -> {
            Db d = new Db(true);
            d.ok("CREATE INDEX idx_edad ON estudiantes(edad);");
            d.ok("DROP INDEX idx_edad;");
            isTrue(!Files.exists(d.dir.resolve("indexes").resolve("idx_edad.idx")), "idx_edad.idx sigue existiendo");
            ExecResult r = d.last("SELECT id FROM estudiantes WHERE edad = 19;");
            eq(5, r.rowsRead);
            isTrue(r.notes.isEmpty(), "no debía usar índice");
            has(d.errors("DROP INDEX idx_edad;"), "no existe");
        });
        test("el índice usado en un JOIN evita recorrer la tabla interna por cada fila", () -> {
            Db d = new Db(true);
            String sql = "SELECT i.nota FROM estudiantes e JOIN inscripciones i ON i.estudiante_id = e.id WHERE e.id = 1 ORDER BY i.nota;";
            long plain = d.last(sql).rowsRead;
            eq(11, plain);
            d.ok("CREATE INDEX i_est ON inscripciones(estudiante_id);");
            ExecResult r = d.last(sql);
            eq(list("80.0", "90.0"), format(r));
            has(String.join(" ", r.notes), "i_est");
            eq(7, r.rowsRead); // 5 estudiantes + las 2 inscripciones de la estudiante 1
        });
        test("el índice se reconstruye si se repite la sesión muchas veces (carga desde archivo)", () -> {
            Db d = new Db(true);
            d.ok("CREATE INDEX idx_edad ON estudiantes(edad);");
            for (int i = 0; i < 3; i++) {
                d.reopen();
                d.ok("INSERT INTO estudiantes VALUES (" + (100 + i) + ",'N" + i + "'," + (50 + i) + ",1.0,TRUE,'1970-01-01');");
            }
            d.reopen();
            eq(list("100", "101", "102"), d.rows("SELECT id FROM estudiantes WHERE edad >= 50 ORDER BY id;"));
            eq(3, d.last("SELECT id FROM estudiantes WHERE edad >= 50;").rowsRead);
        });
    }

    // ── Entrega 7: subconsultas ─────────────────────────────────────────────
    private static void subqueries() throws Exception {
        Db d = new Db(true);
        test("IN con subconsulta (el ejemplo del enunciado)", () ->
            eq(list("Ana Lopez", "Luis Perez", "Carlos Ruiz"),
                d.rows("SELECT nombre FROM estudiantes WHERE id IN (SELECT estudiante_id FROM inscripciones WHERE curso_id = 3);")));
        test("NOT IN con subconsulta", () ->
            eq(list("Sofia Diaz"), d.rows("SELECT nombre FROM estudiantes WHERE id NOT IN (SELECT estudiante_id FROM inscripciones);")));
        test("NOT IN con NULL en la subconsulta no devuelve filas (semántica SQL)", () -> {
            Db e = new Db(true);
            e.ok("INSERT INTO inscripciones VALUES (7, 5, 1, NULL);");
            e.ok("CREATE TABLE t (a INT); INSERT INTO t VALUES (1), (NULL);");
            eq(0, e.rows("SELECT id FROM cursos WHERE id NOT IN (SELECT a FROM t);").size());
            eq(list("1"), e.rows("SELECT id FROM cursos WHERE id IN (SELECT a FROM t);"));
        });
        test("IN con lista de valores", () -> {
            eq(list("1", "3"), d.rows("SELECT id FROM cursos WHERE id IN (1, 3, 7);"));
            eq(list("2"), d.rows("SELECT id FROM cursos WHERE id NOT IN (1, 3, 7);"));
        });
        test("EXISTS correlacionado", () ->
            eq(list("Ana Lopez", "Maria Gomez"),
                d.rows("SELECT nombre FROM estudiantes e WHERE EXISTS (SELECT 1 FROM inscripciones i WHERE i.estudiante_id = e.id AND i.nota >= 90);")));
        test("NOT EXISTS correlacionado", () ->
            eq(list("Sofia Diaz"), d.rows("SELECT nombre FROM estudiantes e WHERE NOT EXISTS (SELECT 1 FROM inscripciones i WHERE i.estudiante_id = e.id);")));
        test("EXISTS no correlacionado", () -> {
            eq(3, d.rows("SELECT id FROM cursos WHERE EXISTS (SELECT 1 FROM estudiantes WHERE edad > 21);").size());
            eq(0, d.rows("SELECT id FROM cursos WHERE EXISTS (SELECT 1 FROM estudiantes WHERE edad > 99);").size());
        });
        test("subconsulta escalar con = , < y >", () -> {
            eq(list("Maria Gomez"), d.rows("SELECT nombre FROM estudiantes WHERE edad = (SELECT MAX(edad) FROM estudiantes);"));
            eq(list("Ana Lopez", "Maria Gomez"), d.rows("SELECT nombre FROM estudiantes WHERE promedio > (SELECT AVG(promedio) FROM estudiantes);"));
            eq(list("Luis Perez"), d.rows("SELECT nombre FROM estudiantes WHERE edad < (SELECT MIN(edad) FROM estudiantes WHERE edad > 17);"));
            eq(list("Algoritmos"), d.rows("SELECT nombre FROM cursos WHERE creditos >= (SELECT MAX(creditos) FROM cursos);"));
        });
        test("subconsulta escalar correlacionada", () ->
            eq(list("2|Algoritmos", "4|Compiladores", "5|Bases de Datos"),
                d.rows("SELECT i.id, c.nombre FROM inscripciones i JOIN cursos c ON c.id = i.curso_id WHERE i.nota = (SELECT MAX(x.nota) FROM inscripciones x WHERE x.curso_id = i.curso_id);")));
        test("subconsulta escalar en la lista del SELECT y en el lado izquierdo", () -> {
            eq(list("Maria Gomez|1"), d.rows("SELECT nombre, 1 FROM estudiantes WHERE (SELECT COUNT(*) FROM inscripciones WHERE estudiante_id = 3) = 2 AND id = 3;"));
            eq(list("3"), d.rows("SELECT (SELECT COUNT(*) FROM cursos) FROM cursos WHERE id = 1;"));
        });
        test("subconsulta escalar sin filas da NULL (ninguna comparación es verdadera)", () ->
            eq(0, d.rows("SELECT id FROM cursos WHERE creditos = (SELECT creditos FROM cursos WHERE id = 99);").size()));
        test("subconsulta escalar que devuelve más de un registro es un error con posición", () -> {
            SqlPipeline.Result r = d.run("SELECT nombre FROM estudiantes WHERE edad = (SELECT edad FROM estudiantes);");
            has(r.report(), "devolvió 5 registros y se esperaba uno solo");
            has(r.report(), "SUBQUERY");
            isTrue(r.semanticErrors.isEmpty() && !r.executionErrors.isEmpty(), "debía ser un error de ejecución");
        });
        test("subconsultas anidadas", () ->
            eq(list("Ana Lopez", "Luis Perez", "Carlos Ruiz"),
                d.rows("SELECT nombre FROM estudiantes WHERE id IN (SELECT estudiante_id FROM inscripciones WHERE curso_id IN (SELECT id FROM cursos WHERE creditos = 5));")));
        test("subconsulta con GROUP BY / HAVING dentro de IN", () ->
            eq(list("1", "3"), d.rows("SELECT id FROM cursos WHERE id IN (SELECT curso_id FROM inscripciones GROUP BY curso_id HAVING COUNT(*) >= 2) ORDER BY id;")));
        test("subconsultas en UPDATE y DELETE", () -> {
            Db e = new Db(true);
            e.ok("UPDATE estudiantes SET activo = FALSE WHERE id IN (SELECT estudiante_id FROM inscripciones WHERE nota >= 90);");
            eq(list("1", "3"), e.rows("SELECT id FROM estudiantes WHERE activo = FALSE ORDER BY id;"));
            e.ok("DELETE FROM estudiantes WHERE NOT EXISTS (SELECT 1 FROM inscripciones i WHERE i.estudiante_id = estudiantes.id);");
            eq(list("1", "2", "3", "4"), e.rows("SELECT id FROM estudiantes ORDER BY id;"));
        });
        test("la subconsulta usa índices cuando puede (correlacionada con índice)", () -> {
            Db e = new Db(true);
            e.ok("CREATE INDEX i_est ON inscripciones(estudiante_id);");
            ExecResult r = e.last("SELECT nombre FROM estudiantes e WHERE EXISTS (SELECT 1 FROM inscripciones i WHERE i.estudiante_id = e.id);");
            eq(list("Ana Lopez", "Luis Perez", "Maria Gomez", "Carlos Ruiz"), format(r));
            has(String.join(" ", r.notes), "i_est");
        });
    }
}
