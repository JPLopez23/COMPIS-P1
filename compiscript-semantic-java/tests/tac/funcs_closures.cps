function mk(base: integer): integer {
  let acc: integer = base;
  function add(x: integer): integer {
    acc = acc + x;
    return acc;
  }
  function twice(x: integer): integer {
    return add(x) + add(x);
  }
  return twice(3);
}
function fib(n: integer): integer {
  if (n < 2) { return n; }
  return fib(n - 1) + fib(n - 2);
}
function esPar(n: integer): boolean {
  if (n == 0) { return true; }
  return esImpar(n - 1);
}
function esImpar(n: integer): boolean {
  if (n == 0) { return false; }
  return esPar(n - 1);
}
function noop(): integer { return 0; }
let f = fib;
print(f(6));
print(mk(1) + fib(noop() + 4));
print(esPar(4));
