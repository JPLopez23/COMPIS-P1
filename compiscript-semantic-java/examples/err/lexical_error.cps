// ============================================================
// lexical_error.cps — errores LÉXICOS: el lexer no reconoce
// estos caracteres, que no forman parte del alfabeto de
// Compiscript. El resto del programa es válido, así que el
// error queda aislado en la fase léxica ("amarillo" en el IDE).
// ============================================================

let x: integer = 5;

// '@', '#' y '$' no son tokens válidos de Compiscript.
@ # $

print(x);
