// ============================================================
// demo_con_errores.cps — errores intencionales, uno por bloque,
// para ver el reporte de la pestaña "Errores" en acción.
// ============================================================

// TYPE: se declara integer pero se inicializa con string
let edad: integer = "veinte";

function suma(a: integer, b: integer): integer {
  return a + b;
}

// FUNCTION: faltan argumentos (espera 2, recibe 1)
let total: integer = suma(5);

// FLOW: la condición del if no es boolean
if (edad) {
  print("hola");
}

class Persona {
  let nombre: string;
}

let p: Persona = new Persona();

// CLASS: 'apellido' no existe en Persona
print(p.apellido);

let numeros: integer[] = [1, 2, 3];

// LIST: el índice debe ser integer, no string
print(numeros["cero"]);

// SCOPE: 'y' nunca fue declarada
print(y);

let contador: integer = 1;
// SCOPE: redeclaración de 'contador' en el mismo ámbito
let contador: integer = 2;

// FLOW: break fuera de cualquier bucle o switch
break;

// GENERAL (advertencia, no error): código después del return
function siempreUno(): integer {
  return 1;
  print("esto nunca se ejecuta");
}
