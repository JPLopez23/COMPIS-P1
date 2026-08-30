// ============================================================
// semantic_error.cps — errores SEMÁNTICOS: el programa es
// léxica y sintácticamente válido, pero rompe las reglas de
// tipos / ámbito / flujo / clases.
// Fase esperada: "semántico" (morado en el IDE).
// ============================================================

// TYPE: el cuerpo devuelve string pero se declaró integer.
function edad(): integer {
  return "treinta";
}

// FLOW: 'continue' fuera de cualquier bucle.
continue;

// FLOW: 'this' usado fuera de una clase.
this.nombre = "x";

// TYPE: operación aritmética entre boolean e integer.
let mezcla: integer = true + 1;

// SCOPE: 'faltante' nunca fue declarada.
print(faltante);
