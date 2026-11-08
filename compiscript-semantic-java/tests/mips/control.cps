let total: integer = 0;
let i: integer = 0;
while (i < 10) {
  i = i + 1;
  if (i % 2 == 0) { continue; }
  if (i > 7) { break; }
  total = total + i;
}
print(total);
for (let a: integer = 0; a < 3; a = a + 1) {
  for (let b: integer = 0; b < 3; b = b + 1) {
    if (b == 2) { break; }
    print(a * 10 + b);
  }
}
let n: integer = 10;
do {
  n = n - 3;
  print(n);
} while (n > 0);
let k: integer = 2;
switch (k) {
  case 1:
    print("uno");
    break;
  case 2:
    print("dos");
  case 3:
    print("tres");
    break;
  default:
    print("otro");
}
switch (9) {
  case 1:
    print("x");
  default:
    print("defecto");
}
let j: integer = 0;
for (;;) {
  j = j + 1;
  if (j == 5) { break; }
}
print(j);
let xs: integer[] = [4, 8, 15, 16, 23, 42];
let suma: integer = 0;
foreach (v in xs) {
  if (v == 15) { continue; }
  if (v == 23) { break; }
  suma = suma + v;
}
print(suma);
for (let q: integer = 5; q > 0; q = q - 2) { print(q); }
