function crearSumador(base: integer): integer {
  let acumulado: integer = base;

  function sumar(x: integer): integer {
    return acumulado + x;
  }

  return sumar(10);
}

let resultado: integer = crearSumador(5);
print(resultado);

function factorial(n: integer): integer {
  if (n <= 1) {
    return 1;
  }
  return n * factorial(n - 1);
}

print(factorial(6));

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
