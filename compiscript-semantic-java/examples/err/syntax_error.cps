// ============================================================
// syntax_error.cps — errores SINTÁCTICOS: todos los tokens son
// válidos, pero la estructura viola la gramática de Compiscript.
// Fase esperada: "sintáctico" (naranja en el IDE).
// ============================================================

// Falta el ';' al final de la declaración.
let x: integer = 5

// Falta el paréntesis de cierre en la condición del 'if'.
if (x > 3 {
  print(x);
}

// 'function' sin nombre ni lista de parámetros.
function (): integer {
  return 1;
}
