# Compiscript — Analizador Semántico

Proyecto 1 de Construcción de Compiladores. Analizador léxico, sintáctico y
semántico para Compiscript, un lenguaje basado en un subconjunto de
TypeScript, con un IDE de escritorio y batería de pruebas.

El análisis léxico y sintáctico usa ANTLR 4 sobre la gramática oficial
Compiscript.g4. El análisis semántico es un visitor propio que recorre el
árbol sintáctico, construye la tabla de símbolos y reporta errores de tipos,
ámbitos, funciones, control de flujo, clases y listas.

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

**Batería de pruebas**

```bash
java -cp "out;lib/antlr-4.13.2-complete.jar" tests.TestRunner
```

20 casos, uno por regla semántica, con caso válido e inválido. Imprime
`PASS`/`FAIL` por caso y sale con `0` si todos pasan.


## Integrantes

| Nombre | Carné | Aporte |
|---|---|---|
| Jose López | 23773 | Infraestructura ANTLR, gramática, build y árbol visual |
| Hansel López | 19026 | Sistema de tipos y tabla de símbolos |
| Pablo Cabrera | 231156 | Analizador semántico, pipeline y línea de comandos |
| André Pivaral | 23574 | IDE Swing y batería de pruebas |
