# Gramáticas `.g4` de referencia

Estos archivos **no** son Compiscript y **no** se usan para compilar nada.
Son gramáticas ANTLR de ejemplo para demostrar el visor de gramáticas del
IDE:

> **Ver gramática → Abrir otra gramática…**

Ese visor abre cualquier `.g4` del disco en modo **solo lectura**, útil
para mostrar o comparar otra gramática ANTLR durante la exposición. No se
puede compilar con ellas ni se recompila el analizador.

| Archivo | Qué muestra |
|---|---|
| `Calculator.g4` | Gramática mínima con precedencia de operadores y alternativas etiquetadas (`# printExpr`). |
| `JSON.g4` | Gramática recursiva con reglas `fragment` en el lexer; versión canónica de los ejemplos de ANTLR. |

La gramática real del proyecto está en `../../grammar/Compiscript.g4`.
