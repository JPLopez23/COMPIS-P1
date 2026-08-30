// ============================================================
// closures.cps — funciones anidadas, closures y recursión.
// Debe compilar SIN errores.
// ============================================================

// La función interna captura 'base' del entorno de definición.
function crearSumador(base: integer): integer {
  let acumulado: integer = base;

  function sumar(x: integer): integer {
    return acumulado + x;
  }

  return sumar(10);
}

let resultado: integer = crearSumador(5);
print(resultado);

// Recursión mutua-simulada: factorial clásico.
function factorial(n: integer): integer {
  if (n <= 1) {
    return 1;
  }
  return n * factorial(n - 1);
}

print(factorial(6));

// Closure de dos niveles: cada nivel abre su propio ámbito.
function externa(): integer {
  let a: integer = 1;
  function media(): integer {
    let b: integer = 2;
    function interna(): integer {
      return a + b;
    }
    return interna();
  }
  return media();
}

print(externa());
