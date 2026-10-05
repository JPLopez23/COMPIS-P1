# Gramaticas `.g4` de referencia

## Solo para ver (visor "Abrir otra...")

`Calculator.g4` y `JSON.g4` no son Compiscript. Sirven para mostrar el visor
de gramaticas del IDE en modo lectura; **no** se pueden aplicar al proyecto.

| Archivo | Que muestra |
|---|---|
| `Calculator.g4` | precedencia de operadores y alternativas etiquetadas |
| `JSON.g4` | gramatica recursiva con reglas `fragment` |

## Aplicables al proyecto (boton "Usar en el proyecto")

Variantes de la gramatica de Compiscript. Mantienen `grammar Compiscript;` y
la regla inicial `program`, asi que el IDE las valida, regenera el parser y
recompila. Son compatibles hacia atras.

| Archivo | Cambio |
|---|---|
| `Compiscript_comentarios.g4` | comentarios de linea con `#` ademas de `//` |
| `Compiscript_alias.g4` | `func` como alias de `function`, `imprimir` de `print` |
| `Compiscript_numeros.g4` | enteros hexadecimales `0xFF` y con separador `1_000_000` |

Cada archivo trae en su cabecera un ejemplo `.cps` que solo compila con esa
variante. Un `.g4` que le cambie el nombre a la gramatica o quite la regla
`program` es rechazado: el analizador semantico y la tabla de simbolos
arrancan en `program`.

La gramatica real del proyecto esta en `../../grammar/Compiscript.g4`.
