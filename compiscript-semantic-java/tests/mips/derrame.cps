function f(a: integer, b: integer, c: integer, d: integer, e: integer, f2: integer, g: integer, h: integer, i: integer, j: integer, k: integer, l: integer): integer {
  return a + b + c + d + e + f2 + g + h + i + j + k + l;
}
let x: integer = 2;
print(f(x * 1, x * 2, x * 3, x * 4, x * 5, x * 6, x * 7, x * 8, x * 9, x * 10, x * 11, x * 12));
let ys: integer[] = [x + 1, x + 2, x + 3, x + 4, x + 5, x + 6, x + 7, x + 8, x + 9, x + 10];
print(ys[0] + ys[9]);
print(f(1, f(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
