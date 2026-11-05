# Compiscript — Analizador Semántico y Generador de Código Intermedio

Proyectos 1 y 2 de Construcción de Compiladores. Analizador léxico, sintáctico y
semántico para Compiscript, un lenguaje basado en un subconjunto de
TypeScript, con un IDE de escritorio y batería de pruebas.

El análisis léxico y sintáctico usa ANTLR 4 sobre la gramática oficial
Compiscript.g4. El análisis semántico es un visitor propio que recorre el
árbol sintáctico, construye la tabla de símbolos y reporta errores de tipos,
ámbitos, funciones, control de flujo, clases y listas.

**Proyecto 2:** a partir del árbol y la tabla de símbolos, el paquete `codegen`
genera código intermedio de tres direcciones (TAC) y completa la tabla de
símbolos con direcciones, offsets, tamaños, etiquetas y registros de
activación. El lenguaje intermedio está documentado en
[docs/TAC.md](docs/TAC.md).

## Arquitectura

```
.cps ─▶ ANTLR (lexer/parser) ─▶ SemanticAnalyzer ─▶ TacGenerator ─▶ TAC
                                  │  scopes, tipos      │  offsets, etiquetas, marcos
                                  └──── tabla de símbolos (compartida y extendida) ────┘
```

| Paquete | Contenido |
|---|---|
| `semantic` | tipos, símbolos, scopes y `SemanticAnalyzer` (además registra `scopeOf` y `nodeTypes` para el generador) |
| `codegen` | generador en capas `TacBase` (pre-pasada, emisión, declaraciones) → `AccessGenerator` (variables, llamadas, `new`) → `ExprGenerator` (expresiones y condiciones) → `TacGenerator` (sentencias, funciones y clases), `Frame` (registro de activación y pool de temporales), `Peephole`, `ActivationReport` |
| `gui` | IDE Swing y `PipelineRunner` (lexer → parser → semántico → TAC) |
| `viz` | árbol en DOT/PNG y tabla de símbolos en Markdown |
| `tests` | `TestRunner` (semántica + TAC) |

## Intérprete SQL

El mismo repositorio incluye el **intérprete SQL con almacenamiento en archivos** (sin
ningún DBMS): lexer y parser ANTLR (`grammar/Sql.g4`), AST, análisis semántico, ejecución sobre
`metadata.txt`, un `.csv` por tabla e índices en `indexes/*.idx`, IDE Swing y 153 pruebas
automáticas. Se compila con el mismo `build.bat` / `build.sh`:

```bash
java -cp "out;lib/antlr-4.13.2-complete.jar" Main sql-gui                       # IDE gráfico
java -cp "out;lib/antlr-4.13.2-complete.jar" Main sql examples/sql/01_esquema.sql -db demo
java -cp "out;lib/antlr-4.13.2-complete.jar" tests.SqlTestRunner                # pruebas
```

Documentación completa (formato de archivos, lenguaje, errores semánticos, índices y entregas):
[docs/SQL.md](docs/SQL.md).

## Requisitos

- **Java 17** o superior.
- **Graphviz**: Para renderizar el árbol como
  imagen.


## Compilar

```bash
build.bat        # Windows
./build.sh       # Linux / Mac
```

**IDE gráfico**

```bash
java -cp "out;lib/antlr-4.13.2-complete.jar" Main gui
```

**Línea de comandos**

```bash
java -cp "out;lib/antlr-4.13.2-complete.jar" Main archivo.cps
java -cp "out;lib/antlr-4.13.2-complete.jar" Main archivo.cps -dot -png -symbols -out output
java -cp "out;lib/antlr-4.13.2-complete.jar" Main archivo.cps -print-tac
java -cp "out;lib/antlr-4.13.2-complete.jar" Main archivo.cps -tac -symbols -out output
```

- `-print-tac` imprime el código intermedio por consola.
- `-tac` escribe `<nombre>.tac` y `<nombre>.frames.md` (registros de activación y
  disposición de objetos); con `-symbols` la tabla de símbolos incluye
  almacenamiento, offset, tamaño, etiqueta y nivel.

En el IDE, tras compilar aparecen las pestañas **Código TAC** y
**Registros de activación**.

Código de salida `0` si no hay errores, `1` si los hay. En Linux y Mac se usa
`:` en vez de `;` en el classpath.

**Batería de pruebas**

```bash
java -cp "out;lib/antlr-4.13.2-complete.jar" tests.TestRunner
```

53 casos: 20 semánticos (uno por regla, con caso válido e inválido) y 33 de
generación de código (expresiones, temporales, cortocircuito, control de flujo,
funciones, recursión, closures, clases, listas, tabla de símbolos extendida,
bien formación del TAC en todos los ejemplos y ausencia de TAC cuando hay
errores). Los programas de prueba del TAC están en `tests/tac/`. Imprime
`PASS`/`FAIL` por caso y sale con `0` si todos pasan.

## Ejemplos

- `examples/ok/` — programas que compilan sin errores.
- `examples/err/` — uno por fase: léxico, sintáctico y semántico.
- `tests/tac/` — programas para la batería de generación de TAC.
- `examples/.g4/` — gramáticas ANTLR de referencia.

## Integrantes

| Nombre | Carné | Aporte |
|---|---|---|
| Jose López | 23773 | Infraestructura ANTLR, gramática, build y árbol visual |
| Hansel López | 19026 | Sistema de tipos y tabla de símbolos |
| Pablo Cabrera | 231156 | Analizador semántico, pipeline y línea de comandos |
| André Pivaral | 23574 | IDE Swing y batería de pruebas |
