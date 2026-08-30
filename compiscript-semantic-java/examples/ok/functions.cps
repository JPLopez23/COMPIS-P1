function add(a: integer, b: integer): integer {
  return a + b;
}

function factorial(n: integer): integer {
  if (n <= 1) {
    return 1;
  } else {
    return n * factorial(n - 1);
  }
}

let z: integer = add(2, factorial(4));
print(z);
