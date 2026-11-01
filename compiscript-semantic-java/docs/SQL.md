# Intérprete SQL con almacenamiento en archivos

Proyecto de Construcción de Compiladores (septiembre 2026). Intérprete de SQL propio: **no usa ningún DBMS**
(ni SQLite, MySQL ni PostgreSQL). La base de datos son archivos y carpetas de texto.
Comparte el repositorio, el IDE y la forma de compilar con el proyecto de Compiscript: el IDE de
Compiscript tiene un botón «Ir a SQL» y el de SQL uno «Ir a Compiscript».

```
SQL ─▶ léxico ─▶ sintáctico ─▶ AST ─▶ semántico ─▶ interpretación ─▶ archivos ─▶ resultado
       (ANTLR)    (ANTLR)     (AstBuilder) (SqlSemanticAnalyzer) (Executor)  (Database)
```

## Cómo compilar y ejecutar

Requiere Java 17+ (el jar de ANTLR ya viene en `lib/`).

```bash
build.bat          # Windows        ./build.sh   # Linux / Mac
```

| Qué | Comando (Windows; en Linux/Mac usa `:` en vez de `;`) |
|---|---|
| IDE gráfico SQL | `java -cp "out;lib\antlr-4.13.2-complete.jar" Main sql-gui [-db carpeta]` |
| Ejecutar un script | `java -cp "out;lib\antlr-4.13.2-complete.jar" Main sql archivo.sql [-db carpeta]` |
| Ejecutar una sentencia | `java -cp "out;lib\antlr-4.13.2-complete.jar" Main sql -e "SELECT * FROM t;" -db database` |
| Solo analizar | agrega `-check`; con `-tokens`, `-tree` o `-ast` imprime esas fases |
| Pruebas automáticas | `java -cp "out;lib\antlr-4.13.2-complete.jar" tests.SqlTestRunner` |

La base de datos por defecto es la carpeta `database/` (se crea sola). Los ejemplos de
`examples/sql/` se ejecutan en orden: `01_esquema.sql` (tablas y datos), `02_consultas.sql`
(SELECT, JOIN, agregaciones, subconsultas) y `03_indices_y_cambios.sql` (índices, UPDATE, DELETE).

```bash
java -cp "out;lib\antlr-4.13.2-complete.jar" Main sql examples\sql\01_esquema.sql -db demo
java -cp "out;lib\antlr-4.13.2-complete.jar" Main sql examples\sql\02_consultas.sql -db demo
java -cp "out;lib\antlr-4.13.2-complete.jar" Main sql examples\sql\03_indices_y_cambios.sql -db demo
```

## Interfaz gráfica

Editor de sentencias (el análisis se actualiza solo al dejar de escribir) y pestañas:
**Resultados** (una tabla por SELECT, mensajes de DDL/DML, qué índice se usó y cuántas filas se
leyeron de los archivos), **Tokens**, **Árbol sintáctico** (texto, o imagen con Graphviz si está
instalado), **AST**, **Errores** (léxicos, sintácticos, semánticos y de ejecución, con línea y
columna; doble clic salta a la posición) y **Base de datos** (esquema y archivos en disco).
«Solo analizar» no ejecuta; «Ejecutar» corre todo el script si no hay errores. El color de la
ventana indica la fase con error, igual que en el IDE de Compiscript.

## Almacenamiento

```
database/
├── metadata.txt            esquemas: tablas, columnas, tipos, restricciones e índices
├── estudiantes.csv         una tabla: cabecera + una fila por línea
├── cursos.csv
└── indexes/
    └── idx_edad.idx        un índice
```

**metadata.txt**

```
TABLE estudiantes
COLUMN id INT PRIMARY_KEY
COLUMN nombre VARCHAR(50) NOT_NULL
COLUMN edad INT
INDEX idx_edad estudiantes edad
```

**Tabla (`.csv`)**: la primera línea son los nombres de columna; cada fila es una línea. NULL es un
campo vacío; los textos solo se entrecomillan si contienen coma, comillas, `\`, saltos de línea,
espacios en los bordes o están vacíos (`""`). Dentro de comillas, `"` se escribe `""` y el salto de
línea `\n`. BOOLEAN se guarda `true`/`false` y DATE como `YYYY-MM-DD`.

**Índice (`.idx`)**: una cabecera y una línea `valor<TAB>posición,posición,…` por cada valor
distinto, ordenado por valor. La *posición* es el offset en bytes de la fila dentro del `.csv`;
así una búsqueda por índice lee solo las filas necesarias (`seek` directo), sin recorrer el
archivo. Los NULL no se indexan.

**Restricciones**: `PRIMARY KEY` (única y no nula; una por tabla) y `NOT NULL`.

## Lenguaje soportado

- Tipos: `INT`, `FLOAT`, `VARCHAR(n)`, `BOOLEAN`, `DATE` (`'YYYY-MM-DD'`). Las palabras reservadas no
  distinguen mayúsculas; los nombres de tablas, columnas e índices tampoco.
- DDL: `CREATE TABLE`, `DROP TABLE`, `CREATE INDEX nombre ON tabla(columna)`, `DROP INDEX nombre`.
- DML: `INSERT INTO t [(cols)] VALUES (...), (...)`, `UPDATE t SET c = expr, ... [WHERE]`,
  `DELETE FROM t [WHERE]`.
- Consultas: `SELECT lista | * | t.* FROM t [alias] [INNER] JOIN t2 [alias] ON cond ... [WHERE]
  [GROUP BY] [HAVING] [ORDER BY expr [ASC|DESC], ...]`; `AS` para alias de columnas.
- Expresiones: `+ - * /`, `= <> != < <= > >=`, `AND OR NOT`, `IS [NOT] NULL`, `[NOT] IN (lista)`,
  `COUNT SUM AVG MIN MAX` (también `COUNT(*)`). Lógica de tres valores con NULL.
- Subconsultas en WHERE (y en HAVING / lista del SELECT): `[NOT] IN (SELECT ...)`,
  `EXISTS (SELECT ...)` / `NOT EXISTS`, y escalares en comparaciones (`edad = (SELECT MAX(...))`),
  también correlacionadas.
- Comentarios `-- línea` y `/* bloque */`; varias sentencias separadas por `;`.

## Análisis semántico

Corre sobre el script completo **antes de ejecutar nada**, con una copia del catálogo a la que va
aplicando los `CREATE`/`DROP` del propio script (por eso `CREATE TABLE t ...; INSERT INTO t ...`
en un mismo archivo es válido). Si hay errores no se ejecuta ninguna sentencia. Cada error indica
`[línea:columna] CATEGORÍA: mensaje`. Verifica, entre otros:

- tabla o columna inexistente; tabla/alias duplicado; tabla o alias desconocido en `t.col` o `t.*`;
- referencia ambigua (columna presente en varias tablas del FROM) y referencias inválidas
  (columna fuera de `GROUP BY`/agregado en consultas agrupadas);
- tipos incompatibles en comparaciones, aritmética, `AND/OR/NOT`, `SUM/AVG`, condiciones no booleanas,
  fechas con formato inválido;
- INSERT/UPDATE: cantidad de valores, tipo del valor, `NOT NULL`, longitud de `VARCHAR(n)`, fecha válida,
  columnas repetidas u omitidas que son `NOT NULL`;
- `CREATE INDEX`: tabla/columna inexistente, índice duplicado; `DROP INDEX` de un índice inexistente;
- subconsulta escalar o de `IN` con más de una columna; agregados en `WHERE`/`ON`; agregados anidados.

Hay dos errores que solo se pueden saber al ejecutar y se reportan con su posición como errores de
ejecución: una subconsulta escalar que devuelve **más de un registro**, la división entre cero y la
**clave primaria duplicada**. Las sentencias anteriores del mismo script ya quedaron aplicadas
(no hay transacciones).

## Ejecución e índices

- Una consulta resuelve el WHERE con un índice cuando hay una condición `columna op constante`
  (`= < <= > >=`, o `columna IN (constantes)`) sobre una columna indexada, unida con `AND` al resto
  del WHERE; el WHERE completo se sigue evaluando sobre las filas que devuelve el índice.
  También se usa en `UPDATE`/`DELETE`, en las condiciones de `JOIN` (`ON t2.col = t1.col`, buscando
  en el índice por cada fila de la tabla anterior) y dentro de subconsultas correlacionadas.
  El resultado indica `Índice 'x' usado en tabla.col: condición` y las filas leídas.
- Las condiciones del WHERE que solo usan las primeras tablas del FROM se aplican antes de seguir
  uniendo. Las subconsultas no correlacionadas se calculan una sola vez por sentencia.
- **Mantenimiento**: `INSERT` agrega la fila al final del `.csv` y registra su posición en cada índice;
  `UPDATE`/`DELETE` reescriben el `.csv` copiando intacto el resto de las filas y reasignan las
  posiciones de los índices (las filas modificadas se reindexan, las borradas se descartan). Los
  archivos `.idx` se reescriben al terminar cada sentencia. `DROP TABLE` elimina el `.csv` y sus índices.

## Estructura del código

| Paquete | Contenido |
|---|---|
| `grammar/Sql.g4`, `sqlgen` | gramática ANTLR (léxico + sintaxis, sin distinguir mayúsculas) y parser generado |
| `sql.ast` | `Ast` (nodos del AST con posición, tipo y vista de árbol) y `AstBuilder` (árbol de ANTLR → AST) |
| `sql.semantic` | `SqlSemanticAnalyzer` (tipos, resolución de columnas, errores; anota el AST) |
| `sql.storage` | `Database` (archivos), `Catalog`/`TableSchema` (esquemas), `IndexData` (índices), `Values` (tipos y formato) |
| `sql.exec` | `Executor` (interpretación, uso de índices), `ExecResult`, `SqlRuntimeException` |
| `sql` | `SqlPipeline` (une todas las fases), `SqlCli` |
| `gui.SqlWindow` | IDE Swing; reutiliza `Theme`, `Pill`, `RoundedButton`, `CollectingErrorListener`, `ParseTreeToDot` y `GraphvizUtil` del IDE de Compiscript |
| `tests.SqlTestRunner` | 134 pruebas automáticas |

## Entregas

| # | Entrega | Dónde |
|---|---|---|
| 1 | Lexer y parser | `Sql.g4`; pruebas «Léxico y sintaxis» |
| 2 | AST | `sql.ast`; pestaña AST; pruebas «AST» |
| 3 | Semántico y errores | `SqlSemanticAnalyzer`; pruebas «Análisis semántico» |
| 4 | CREATE, DROP, INSERT | `Executor`, `Database`; pruebas «CREATE / DROP / INSERT y almacenamiento» |
| 5 | SELECT, WHERE, UPDATE, DELETE | `Executor`; pruebas «SELECT / WHERE / UPDATE / DELETE» |
| 6 | JOIN, agregaciones, GROUP BY | `Executor`; pruebas «JOIN, agregaciones, GROUP BY» |
| 7 | Índices y subconsultas | `IndexData`, `Executor`; pruebas «Índices» y «Subconsultas» |
| 8 | Integración final | `SqlWindow`, `SqlCli`, `SqlPipeline`, build y documentación |

## Limitaciones conocidas

Sin `LEFT/RIGHT JOIN`, `DISTINCT`, `LIMIT`, `LIKE`, `BETWEEN`, `ALTER TABLE`, claves foráneas,
transacciones ni concurrencia (un solo proceso a la vez sobre la carpeta). Cada sentencia lee el
`.csv` completo cuando no puede usar un índice.
