# Lenguaje intermedio (TAC) de Compiscript

Código de tres direcciones generado por `codegen.TacGenerator` a partir del árbol sintáctico y la tabla de símbolos del análisis semántico. La sintaxis es propia y está definida completamente en este documento.

## 1. Estructura de un programa TAC

```
function <etiqueta>:   ; nivel=<n> marco=<bytes>B temporales=<k>
    <instrucciones>
end <etiqueta>
```

- `main` contiene el código de nivel superior (nivel 0). Cada función, método y método anidado es otro bloque `function`.
- Las clases con atributos inicializados generan además `<Clase>_$init`.
- Una instrucción por línea, indentada con 4 espacios. Las etiquetas de salto (`L1:`) van en la columna 0. `;` inicia un comentario.

## 2. Operandos

| Forma | Significado |
|---|---|
| `42`, `"texto"`, `true`, `false`, `null` | constantes |
| `x` | variable local o parámetro del marco actual |
| `env(d).x` | variable de la función que está `d` niveles hacia afuera (cadena de enlaces de acceso) |
| `G.x` | variable global (área estática, direccionada con `gp`) |
| `this`, `env(d).this` | receptor del método actual / de un método externo |
| `t0`, `t1`… | temporales del generador |
| `$arr1`, `$i2`, `$sw3` | variables ocultas del compilador (foreach, switch) |

## 3. Conjunto de instrucciones

| Instrucción | Descripción |
|---|---|
| `x = y` | copia |
| `x = y op z` | binaria: `+ - * / %` y comparaciones `== != < <= > >=` (resultado boolean). Con cadenas: `++` (concatenación) y `==s` / `!=s` (comparación por contenido) |
| `x = - y`, `x = ! y` | unarias |
| `L:` | etiqueta |
| `goto L` | salto incondicional |
| `if y goto L` / `ifFalse y goto L` | salto según un booleano |
| `if y relop z goto L` | salto condicional con comparación |
| `param y` | empuja un argumento (en orden); en métodos el primero es el receptor |
| `call f, n, env(k)` / `x = call f, n, env(k)` | llamada directa con `n` parámetros; `env(k)` es el enlace de acceso que recibe el llamado |
| `callind c, n` / `x = callind c, n` | llamada a través de un valor-función (closure) |
| `return` / `return y` | retorno |
| `x = closure f, env(k)` | crea un valor-función para `f` capturando el entorno `k` |
| `x = newarray n` | crea una lista de `n` elementos |
| `x = a[i]` / `a[i] = y` | acceso indexado |
| `x = len a` | longitud de una lista |
| `x = new C, bytes` | reserva un objeto de la clase `C` |
| `x = o.f@d` / `o.f@d = y` | acceso a atributos; `d` es el desplazamiento (en bytes) del atributo dentro del objeto |
| `print y` / `prints y` / `printb y` | impresión de un entero / una cadena / un booleano (`true`/`false`) |
| `pushhandler L` / `pophandler` / `x = exception` | try/catch: `L` es el manejador, `x` recibe la excepción |

El enlace de acceso de `call f, n, env(k)` es el marco que define a `f`: `k = nivel_actual − (nivel_llamado − 1)`. Los comentarios después de `;` solo documentan.

> **Cambio de la Fase 3.** Para poder traducir el TAC a MIPS sin volver a consultar el árbol, se hicieron explícitos tres datos que antes eran implícitos: el enlace de acceso de cada `call` (antes un comentario), el desplazamiento de cada atributo (`o.f@d`, antes `o.f`) y los tipos de las operaciones sobre cadenas e impresiones (`++`, `==s`, `prints`, `printb`).

## 4. Ejemplos de traducción

**Expresiones y temporales** — `let r: integer = a + b * c - (a - b) / c % 2;`
```
t0 = G.b * G.c
t0 = G.a + t0
t1 = G.a - G.b
t1 = t1 / G.c
t1 = t1 % 2
G.r = t0 - t1
```
Solo se usan dos temporales; cada uno se libera apenas se consume su valor.

**Cortocircuito** — `if (a < b && (b < c || c == 0)) {...} else {...}`
```
    if G.a >= G.b goto L10
    if G.b < G.c goto L9
    if G.c != 0 goto L10
L9: ...cuerpo then...
    goto L13
L10: ...cuerpo else...
L13:
```

**while / break / continue** — `break` salta a la etiqueta de fin del bucle (o del `switch`) y `continue` a la del paso/condición.
```
L1: if G.i >= 10 goto L3
    G.i = G.i + 1
    t0 = G.i % 2
    if t0 == 0 goto L1        ; continue
    if G.i > 7 goto L3        ; break
    G.total = G.total + G.i
    goto L1
L3:
```

**switch** — se evalúa una vez en `$swN`, se despachan los `case` por comparación y los cuerpos caen en secuencia (semántica de TypeScript); `break` salta al fin.

**Funciones y recursión** — `fib(n-1) + fib(n-2)`
```
t0 = n - 1
param t0
t0 = call fib, 1, env(1)
t1 = n - 2
param t1
t1 = call fib, 1, env(1)
t0 = t0 + t1
return t0
```

**Funciones anidadas / closures** — una variable de una función exterior se alcanza con `env(d)`:
```
function mk_add:    ; nivel=2
    env(1).acc = env(1).acc + x
    return env(1).acc
```
Las funciones usadas como valor (`let f = fib;`) se traducen a `closure` y se invocan con `callind`.

**Clases** — `new Punto3(1, 2)`:
```
t0 = new Punto3, 12
param t0
call Punto_$init, 1, env(0)   ; inicializadores de atributos, de la clase raíz a la instanciada
param t0
call Punto3_$init, 1, env(0)
param t0
param 1
param 2
call Punto_constructor, 3, env(0)   ; constructor (propio o heredado)
```
Los métodos son funciones `Clase_metodo` cuyo primer parámetro es `this`.

**Listas** — `[[1,2],[3,4]]` crea cada lista con `newarray n` y asigna elemento por elemento; `m[2][1]` es `t0 = G.m[2]` seguido de `t0 = t0[1]`.

## 5. Manejo de temporales

`Frame.newTemp()` entrega el temporal libre de menor índice y `Frame.free()` lo devuelve al pool cuando su valor fue consumido (se liberan los operandos *antes* de pedir el temporal del resultado, lo que permite `t0 = t0 + t1`). Al terminar cada sentencia el pool se vacía. Además, si el último valor calculado es un temporal que se asigna a una variable, la instrucción se reescribe para escribir directamente en la variable (`G.x = 5 + t0` en lugar de `t1 = 5 + t0; G.x = t1`). El reporte indica el máximo de temporales simultáneos por función.

Un *peephole* final elimina `goto` al label siguiente, invierte `if c goto A; goto B; A:` en `if !c goto B` y borra etiquetas sin uso.

## 6. Tabla de símbolos extendida y registros de activación

Cada `Symbol` ahora lleva `storage` (GLOBAL/LOCAL/PARAM/FIELD/FUNCTION/CLASS), `offset`, `size`, `label`, `nestLevel` y `tacName`. Las funciones guardan `paramsSize`, `localsSize`, `tempCount` y `frameSize`; las clases `instanceSize` y `fieldOffsets`. Se muestran en la pestaña «Símbolos» del IDE y en `*.symbols.md`.

Registro de activación (offsets desde `fp`):

| offset | campo |
|---|---|
| 0 | dirección de retorno |
| 4 | enlace de control (fp del llamador) |
| 8 | enlace de acceso (marco definidor, para variables no locales) |
| 12 | valor de retorno |
| 16… | parámetros (en métodos `this` va primero), luego locales y ocultas, luego temporales |

Tamaños: `integer` 4, `string`/lista/objeto/función 4 (referencia), `boolean` 1; todo se alinea a 4 bytes. Los objetos se disponen con los atributos del padre primero. El reporte completo (`*.frames.md`, pestaña «Registros de activación») lista cada marco y cada clase.

## 7. Supuestos de diseño

- El TAC se genera solo si no hubo errores léxicos, sintácticos ni semánticos (las advertencias no lo impiden).
- Las llamadas a métodos se resuelven **estáticamente** con el tipo declarado del receptor (herencia: se busca hacia arriba; no hay despacho dinámico por sobreescritura).
- Una función anidada dentro de un método no recibe `this` como parámetro: usa el del método que la contiene (`env(1).this`).
- Los enlaces de acceso apuntan al marco vivo de la función definidora: una closure no puede sobrevivir a esa función (no se heapean los marcos).
- No hay plegado de constantes; `try/catch` se expresa con `pushhandler/pophandler` y queda al backend implementar las excepciones.
- La evaluación de argumentos y operandos es de izquierda a derecha; en una asignación `a[i] = v` primero se evalúa el destino y luego el valor.
- Las cadenas se concatenan con `++` y se comparan con `==s` / `!=s`: el generador del TAC elige el operador según los tipos del análisis semántico.
