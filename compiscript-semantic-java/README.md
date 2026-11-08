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

**Proyecto 3 (generación de código):** el paquete `mips` traduce el TAC a
ensamblador MIPS32 (SPIM/MARS) con secuencias de llamada y retorno sobre el
registro de activación, asignación de registros `getReg()` con derrame a la
pila, rutinas de soporte (cadenas, impresión, excepciones) y un simulador de
MIPS para ejecutar y validar el resultado. Está documentado en
[docs/MIPS.md](docs/MIPS.md).

## Arquitectura

```
.cps ─▶ ANTLR ─▶ SemanticAnalyzer ─▶ TacGenerator ─▶ TAC ─▶ MipsGenerator ─▶ .asm ─▶ simulador MIPS
                   │  scopes, tipos      │  offsets, etiquetas, marcos   │  getReg(), llamadas
                   └──── tabla de símbolos (compartida y extendida) ─────┘
```

| Paquete | Contenido |
|---|---|
| `semantic` | tipos, símbolos, scopes y `SemanticAnalyzer` (además registra `scopeOf` y `nodeTypes` para el generador) |
| `codegen` | generador en capas `TacBase` (pre-pasada, emisión, declaraciones) → `AccessGenerator` (variables, llamadas, `new`) → `ExprGenerator` (expresiones y condiciones) → `TacGenerator` (sentencias, funciones y clases), `Frame` (registro de activación y pool de temporales), `Peephole`, `ActivationReport` |
| `mips` | `TacParser` (TAC → instrucciones), `RegisterAllocator` (`getReg()`), `MipsGenerator` (prologue/epílogo, llamadas, traducción), `MipsRuntime` (rutinas en ensamblador), `MipsSimulator` (ensamblador y simulador de MIPS32) |
| `gui` | IDE Swing y `PipelineRunner` (lexer → parser → semántico → TAC → MIPS → ejecución) |
| `viz` | árbol en DOT/PNG y tabla de símbolos en Markdown |
| `tests` | `TestRunner` (semántica, TAC y MIPS) |

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

En el IDE, tras compilar aparecen las pestañas **Código TAC**, **Registros de
activación**, **Código MIPS** y **Ejecución** (salida del programa en el
simulador).

Generación y ejecución de MIPS desde la línea de comandos:

```bash
java -cp "out;lib/antlr-4.13.2-complete.jar" Main archivo.cps -run          # ejecuta en el simulador
java -cp "out;lib/antlr-4.13.2-complete.jar" Main archivo.cps -mips         # escribe <nombre>.asm
java -cp "out;lib/antlr-4.13.2-complete.jar" Main archivo.cps -print-mips   # imprime el ensamblador
java -cp "out;lib/antlr-4.13.2-complete.jar" Main archivo.cps -regs         # uso de registros y derrames
java -cp "out;lib/antlr-4.13.2-complete.jar" mips.MipsSimulator archivo.asm # ejecuta un .asm
```

El `.asm` generado se probó en MARS 4.5 y también se puede abrir en SPIM.

Código de salida `0` si no hay errores, `1` si los hay. En Linux y Mac se usa
`:` en vez de `;` en el classpath.

**Batería de pruebas**

```bash
java -cp "out;lib/antlr-4.13.2-complete.jar" tests.TestRunner
```

99 casos: 20 semánticos (uno por regla, con caso válido e inválido), 36 de
generación de TAC (expresiones, temporales, cortocircuito, control de flujo,
funciones, recursión, closures, clases, listas, tabla de símbolos extendida,
bien formación del TAC en todos los ejemplos y ausencia de TAC cuando hay
errores) y 43 de generación de MIPS: 18 programas que se ejecutan en el
simulador y se comparan con su salida esperada, secuencias de llamada y
retorno, `getReg()` con derrames, portabilidad a SPIM/MARS y casos fallidos
(errores de compilación, excepciones en ejecución, bucle infinito, ensamblador
inválido), más una prueba opcional que ejecuta los 18 programas en MARS 4.5 real
(si `lib/Mars4_5.jar` existe). Los programas están en `tests/tac/` y `tests/mips/` (salidas
esperadas en `tests/mips/expected/`). Imprime `PASS`/`FAIL` por caso y sale con
`0` si todos pasan.

## Ejemplos

- `examples/ok/` — programas que compilan sin errores.
- `examples/err/` — uno por fase: léxico, sintáctico y semántico.
- `tests/tac/` — programas para la batería de generación de TAC.
- `tests/mips/` — programas de la Fase 3 y sus salidas esperadas (`expected/`).
- `examples/.g4/` — gramáticas ANTLR de referencia.

## Integrantes

| Nombre | Carné | Aporte |
|---|---|---|
| Jose López | 23773 | Infraestructura ANTLR, gramática, build y árbol visual |
| Hansel López | 19026 | Sistema de tipos y tabla de símbolos |
| Pablo Cabrera | 231156 | Analizador semántico, pipeline y línea de comandos |
| André Pivaral | 23574 | IDE Swing y batería de pruebas |
