# Generación de código MIPS (Fase 3)

La Fase 3 traduce el código de tres direcciones (TAC, ver [TAC.md](TAC.md)) a **ensamblador MIPS32** que se ejecuta en SPIM o MARS. Incluye las secuencias de llamada y retorno, la asignación de registros `getReg()` con derrame a la pila, las rutinas de soporte y un simulador de MIPS propio para ejecutar y validar el resultado.

```
.cps ─▶ ANTLR ─▶ SemanticAnalyzer ─▶ TacGenerator ─▶ TAC ─▶ MipsGenerator ─▶ .asm ─▶ simulador MIPS ─▶ salida
                       │ tabla de símbolos             │ marcos (offsets)    │ getReg()
```

## 1. Cómo ejecutarlo

Requiere Java 17. Se compila todo con `build.bat` (Windows) o `./build.sh`.

**IDE** — `java -cp "out;lib/antlr-4.13.2-complete.jar" Main gui`. Al pulsar *Compilar* aparecen las pestañas:

| Pestaña | Contenido |
|---|---|
| Código TAC | el código intermedio |
| Registros de activación | marcos de cada función y disposición de los objetos |
| **Código MIPS** | el ensamblador, precedido del resumen de uso de registros y derrames por función |
| **Ejecución** | la salida del programa al ejecutarse en el simulador, o el error de ejecución |

**Línea de comandos**

```bash
java -cp "out;lib/antlr-4.13.2-complete.jar" Main programa.cps -run          # compila y ejecuta en el simulador
java -cp "out;lib/antlr-4.13.2-complete.jar" Main programa.cps -mips         # escribe output/programa.asm
java -cp "out;lib/antlr-4.13.2-complete.jar" Main programa.cps -print-mips   # imprime el ensamblador
java -cp "out;lib/antlr-4.13.2-complete.jar" Main programa.cps -regs         # $s usados y derrames por función
java -cp "out;lib/antlr-4.13.2-complete.jar" mips.MipsSimulator programa.asm # ejecuta cualquier .asm
```

**En SPIM o MARS** — genera el `.asm` con `-mips` y ábrelo (MARS: *File → Open*, *Assemble*, *Run*; SPIM: `spim -file programa.asm`). El programa empieza en `main`, que es la primera instrucción de `.text`.

**Pruebas** — `java -cp "out;lib/antlr-4.13.2-complete.jar" tests.TestRunner` (ver sección 9).

## 2. Arquitectura (`src/mips/`)

| Clase | Responsabilidad |
|---|---|
| `TacOperand`, `TacInstr`, `TacParser` | Convierten las líneas de texto del TAC de cada marco en instrucciones estructuradas (operandos tipados: constante, cadena, temporal, variable, global, atributo, índice). |
| `RegisterAllocator` | `getReg()`: descriptores de registros, derrame y recarga de temporales. |
| `MipsGenerator` | Traduce cada función: prologue, cuerpo, epílogo; secuencias de llamada; tabla de cadenas; sección de datos. |
| `MipsRuntime` | Rutinas en ensamblador: impresión, concatenación y comparación de cadenas, excepciones. |
| `MipsSimulator` | Ensamblador y simulador de MIPS32 (subconjunto de SPIM/MARS) para ejecutar el resultado. |

`MipsGenerator` recibe el `TacGenerator` ya ejecutado: de él toma el TAC de cada marco (`Frame.code`), los desplazamientos de cada variable (`Frame.slots`), el tamaño de cada registro de activación y los desplazamientos de las globales. Así la tabla de símbolos «interactúa» también con esta fase.

## 3. Representación de datos

Todo valor ocupa **una palabra de 32 bits**:

| Tipo | Representación |
|---|---|
| `integer` | entero con signo |
| `boolean` | 0 / 1 |
| `string` | puntero a bytes terminados en 0 (literales en `.data`; las concatenaciones se crean en el montón) |
| lista | puntero a `[longitud][e0][e1]…` (los elementos empiezan en el byte 4) |
| objeto | puntero a un bloque con los atributos en los desplazamientos de la disposición de la clase (el padre primero) |
| función como valor (closure) | puntero a `[dirección del código][marco de entorno]` |
| `null` | 0 |

Las variables globales viven en `G_data` (área estática); el montón se obtiene con `sbrk` (llamada al sistema 9) y no se libera.

## 4. Secuencia de llamada y retorno

Registro de activación (se arma desde `$fp`; la pila crece hacia abajo):

```
 fp+0    dirección de retorno ($ra)
 fp+4    enlace de control (fp del llamador)
 fp+8    enlace de acceso (marco de la función que define a la actual)
 fp+12   valor de retorno
 fp+16   parámetros (this primero en métodos) · variables locales y ocultas
         temporales derramados (t0, t1…)
         registros $s guardados
```

**Llamador** (`param x`, `call f, n, env(k)`):

```mips
    addiu $sp, $sp, -4            # param: apila el argumento (el primero queda más arriba)
    sw    $s0, 0($sp)
    move  $a0, $fp                # enlace de acceso: el marco k niveles hacia afuera
    lw    $a0, 8($a0)             #   (k veces)
    jal   f_fib
    addiu $sp, $sp, 4             # desapila los n parámetros
    move  $s0, $v0                # resultado
```

**Llamado — prologue**

```mips
f_fib:
    addiu $sp, $sp, -40           # reserva todo el registro de activación (S bytes)
    sw    $ra, 0($sp)
    sw    $fp, 4($sp)
    sw    $a0, 8($sp)             # enlace de acceso
    move  $fp, $sp
    lw    $t8, 40($fp)            # copia cada parámetro apilado por el llamador
    sw    $t8, 16($fp)            #   a su posición fp+16, fp+20…
    sw    $s0, 28($fp)            # guarda los $s que usa la función
```

**Llamado — epílogo** (`return v` guarda `v` en `$v0` y en `fp+12`, y salta a `ret_f`):

```mips
ret_f_fib:
    lw    $s0, 28($fp)            # restaura los $s
    lw    $ra, 0($fp)
    lw    $t8, 4($fp)             # fp del llamador
    addiu $sp, $fp, 40            # libera el registro de activación (también los locales)
    move  $fp, $t8
    jr    $ra
```

El valor de retorno viaja en `$v0` y también queda en el campo `fp+12` del registro de activación, como pide el diseño. Una llamada indirecta (`callind c, n`) carga el código y el entorno de la closure (`lw $t8,0(c)`, `lw $a0,4(c)`) y usa `jalr $t8`.

`main` es el código de nivel superior: tiene su propio registro de activación y termina con la llamada al sistema 10.

## 5. Asignación de registros: `getReg()`

| Registros | Uso |
|---|---|
| `$s0`–`$s7` (guardados) | **temporales del TAC** (`t0`, `t1`…). Sobreviven a las llamadas porque cada función guarda en su prologue los `$s` que usa y los restaura en el epílogo. Una función con `try/catch` guarda los 8, porque una excepción puede saltarse los epílogos intermedios. |
| `$t0`–`$t7` (temporales) | registros de trabajo de **una sola instrucción**: constantes, variables cargadas de la pila, índices. Se liberan al terminar cada instrucción. |
| `$t8`, `$t9` | cálculo de direcciones, enlaces de acceso y rutinas del runtime |
| `$a0`–`$a1` | enlace de acceso en llamadas; argumentos del runtime y de los syscalls |
| `$v0` | valor de retorno y número de syscall |
| `$fp`, `$sp`, `$ra` | marco, tope de pila y retorno |

Las **variables** (locales, parámetros, globales, atributos) viven siempre en memoria y se cargan a un `$t` cuando se usan; los **temporales del TAC** viven en registros `$s` y solo van a la pila si hay que derramarlos. `RegisterAllocator.getReg(temporal, necesitaValor)`:

1. Si el temporal ya está en un registro, se reutiliza (descriptor de registro).
2. Si no, se toma un `$s` libre.
3. Si no hay ninguno, se **derrama** (spill) el temporal cuyo *próximo uso está más lejos* (o que ya no se usa) a su ranura del registro de activación (`fp + tempsOffset + 4·i`), salvo los que se usan en la instrucción actual.
4. Si se va a leer y su valor estaba en la pila, se **recarga** (`lw`).

Reglas de consistencia:

- Un registro «sucio» tiene un valor más nuevo que su ranura. Antes de cualquier salto, salto condicional o etiqueta se guardan los sucios (`flushDirty`); tras un `goto`/`return` o en una etiqueta se olvidan los descriptores (los valores ya están en la pila). Esto hace correcto el código aunque se llegue a una etiqueta desde varios caminos.
- Al terminar cada instrucción se liberan los registros **limpios** de temporales que ya no se usarán (próximo uso = ninguno).
- `-regs` y la pestaña «Código MIPS» muestran, por función, los `$s` que usa y cuántos derrames/recargas hizo.

El programa `tests/mips/derrame.cps` llama a una función con 12 argumentos calculados: los 12 temporales están vivos a la vez, así que se derraman 5.

## 6. Traducción de las instrucciones del TAC

| TAC | MIPS (resumen) |
|---|---|
| `x = y op z` (`+ - *`) | `addu` / `subu` / `mul` (con constante pequeña: `addiu`) |
| `/`, `%` | `beqz divisor, __div0`; `div`; `mflo` / `mfhi` |
| comparaciones como valor | `seq sne slt sle sgt sge` |
| `if a rel b goto L` | `beq bne blt ble bgt bge` |
| `if x goto L` / `ifFalse x goto L` | `bnez` / `beqz` |
| `x = - y`, `x = ! y` | `subu x, $zero, y` / `seq x, y, $zero` |
| `a ++ b`, `a ==s b`, `a !=s b` | `jal __concat` / `jal __streq` (y `xori` para `!=s`) |
| `x = a[i]`, `a[i] = y` | comprueba nulo y límites (`bgeu i, longitud, __oob`), `sll`/`addu`, `lw`/`sw` en `4(…)` |
| `x = len a` | `lw x, 0(a)` |
| `x = o.f@d`, `o.f@d = y` | comprueba nulo, `lw`/`sw` en `d(o)` |
| `x = newarray n` | `sbrk(4+4n)` y escribe la longitud |
| `x = new C, b` | `sbrk(b)` |
| `x = closure f, env(k)` | `sbrk(8)`, guarda `la f` y el marco k niveles hacia afuera |
| `param`, `call`, `callind`, `return` | sección 4 |
| `print` / `prints` / `printb` | `jal __print_int` / `__print_str` / `__print_bool` (cada uno imprime un salto de línea) |
| `pushhandler L` / `pophandler` / `x = exception` | sección 7 |

Cada instrucción TAC aparece como comentario (`# t0 = a + b`) encima de las instrucciones MIPS que genera.

## 7. Excepciones

`try/catch` y los errores en tiempo de ejecución usan una pila de manejadores en memoria (`__hstack`, hasta 64 entradas de 12 bytes: dirección del manejador, `$sp` y `$fp`).

- `pushhandler L` apila `(L, $sp, $fp)`; `pophandler` desapila.
- Los errores — índice fuera de rango, división o módulo por cero, referencia nula — saltan a `__throw` con un mensaje. `__throw` restaura `$sp`/`$fp` del manejador activo, guarda el mensaje en `__exc` y salta al manejador; `x = exception` lo lee.
- Sin manejador activo se imprime `Excepcion no capturada: <mensaje>` y el programa termina.

## 8. Ejemplo: `fib`

```
function fib(n: integer): integer {            if n >= 2 goto L2
  if (n < 2) { return n; }                     return n
  return fib(n - 1) + fib(n - 2);          ⇒   L2: t0 = n - 1 ; param t0 ; t0 = call fib, 1, env(1)
}                                                  t1 = n - 2 ; param t1 ; t1 = call fib, 1, env(1)
                                                   t0 = t0 + t1 ; return t0
```

```mips
f_fib:  ...prologue (sección 4)...
    # if n >= 2 goto L2
    lw $t0, 16($fp)
    li $t1, 2
    bge $t0, $t1, L2
    # return n
    lw $v0, 16($fp)
    sw $v0, 12($fp)
    j ret_f_fib
L2:
    # t0 = n - 1
    lw $t0, 16($fp)
    addiu $s0, $t0, -1                # t0 vive en $s0
    # param t0
    addiu $sp, $sp, -4
    sw $s0, 0($sp)
    # t0 = call fib, 1, env(1)
    move $a0, $fp
    lw $a0, 8($a0)
    jal f_fib
    addiu $sp, $sp, 4
    move $s0, $v0
    ...                               # t1 en $s1; t0 y t1 siguen vivos en $s0/$s1 durante la segunda llamada
    # t0 = t0 + t1
    addu $s0, $s0, $s1
```

## 9. Pruebas

`tests.TestRunner` ejecuta ahora **más de 90 casos**; los de la Fase 3 compilan un programa, generan el MIPS, lo ejecutan en el simulador y comparan la salida con `tests/mips/expected/*.out`.

| Grupo | Qué valida |
|---|---|
| Ejecución | 12 programas en `tests/mips/` (aritmética, booleanos y cortocircuito, cadenas, control de flujo, funciones y closures, clases y herencia, listas, excepciones, derrame de registros, algoritmos recursivos, programa integral, closures como valores) y los 6 ejemplos de `examples/ok`. |
| Llamadas y retorno | prologue/epílogo, apilado de parámetros, enlace de acceso, valor de retorno en `fp+12`, guardado de `$s`, llamada indirecta, salida de `main`. |
| `getReg()` | temporales en `$s`, programas sin derrames, programa que sí derrama y recarga, reporte por función. |
| Portabilidad | solo se emiten instrucciones que existen en SPIM y MARS (en el programa de funciones y en **todos** los de `tests/mips`); comentarios en ASCII. |
| Casos fallidos | errores léxicos, sintácticos y semánticos no generan MIPS; excepción no capturada, división por cero y referencia nula se reportan en tiempo de ejecución; el simulador corta bucles infinitos, rechaza instrucciones desconocidas y accesos desalineados. |
| Determinismo | la misma entrada produce el mismo MIPS y la misma salida. |
| MARS real (opcional) | `mips_mars_all` ensambla y ejecuta los 18 programas en MARS 4.5 (modo `nc sm`) y compara con la salida esperada. Se activa si existe `lib/Mars4_5.jar` (o con `-Dmars.jar=ruta`); si no, el `TestRunner` lo reporta como `SKIP`. El jar se descarga de <https://dpetersanderson.github.io/> (Download MARS 4.5) y está en `.gitignore`. |

Además, las salidas esperadas de los programas se contrastaron con un intérprete independiente (los mismos programas traducidos a TypeScript y ejecutados con Node): coinciden salvo en los programas que dependen de las excepciones de Compiscript, que JavaScript no lanza.

## 10. Supuestos y límites

- El código se valida con el simulador incluido (`MipsSimulator`) y además con **MARS 4.5 real**: los 18 programas de prueba producen la misma salida en ambos. Solo se emiten instrucciones y pseudo-instrucciones estándar de SPIM/MARS y syscalls 1, 4, 9, 10 y 11. SPIM no se probó.
- Todo es de 32 bits y con signo; los desbordamientos dan la vuelta (se usan `addu`/`subu`, que no producen trampa).
- `print` imprime enteros, cadenas y booleanos; listas y objetos se imprimen como entero (su dirección).
- El montón nunca se libera y las cadenas son inmutables. Los literales de cadena no interpretan secuencias de escape salvo las del ensamblador (`\n`, `\t`).
- Hay un máximo de 64 manejadores de excepciones anidados.
- Las closures guardan un enlace al marco vivo de la función que las creó, por lo que no pueden sobrevivir a ella.
- Las llamadas a métodos se resuelven con el tipo declarado (sin despacho dinámico).
