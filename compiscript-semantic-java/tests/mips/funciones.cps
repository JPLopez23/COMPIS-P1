function suma(a: integer, b: integer): integer { return a + b; }
function fib(n: integer): integer {
  if (n < 2) { return n; }
  return fib(n - 1) + fib(n - 2);
}
function fact(n: integer): integer {
  if (n <= 1) { return 1; }
  return n * fact(n - 1);
}
function esPar(n: integer): boolean {
  if (n == 0) { return true; }
  return esImpar(n - 1);
}
function esImpar(n: integer): boolean {
  if (n == 0) { return false; }
  return esPar(n - 1);
}
function muchos(a: integer, b: integer, c: integer, d: integer, e: integer, f: integer): integer {
  return a + b * 2 + c * 3 + d * 4 + e * 5 + f * 6;
}
function saluda(n: string) { print("hola " + n); }
function mk(base: integer): integer {
  let acc: integer = base;
  function add(x: integer): integer {
    acc = acc + x;
    return acc;
  }
  function dos(x: integer): integer {
    return add(x) + add(x);
  }
  return dos(3) + acc;
}
function externa(): integer {
  let a: integer = 1;
  function media(): integer {
    let b: integer = 2;
    function interna(): integer { return a + b; }
    return interna() * 10;
  }
  return media() + a;
}
print(suma(3, 4));
print(suma(suma(1, 2), suma(3, 4)));
print(fib(15));
print(fact(10));
print(esPar(10));
print(esImpar(7));
print(esPar(7));
print(muchos(1, 2, 3, 4, 5, 6));
saluda("Ana");
print(mk(1));
print(externa());
let f = fib;
print(f(10));
