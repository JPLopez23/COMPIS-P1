let i: integer = 0;
let total: integer = 0;
while (i < 10) {
  i = i + 1;
  if (i % 2 == 0) { continue; }
  if (i > 7) { break; }
  total = total + i;
}
for (let a: integer = 0; a < 3; a = a + 1) {
  for (let b: integer = 0; b < 3; b = b + 1) {
    if (b == 2) { break; }
    total = total + a * b;
  }
}
do {
  total = total - 1;
  if (total < 5) { break; }
} while (total > 0);
let k: integer = 2;
switch (k) {
  case 1:
    print("uno");
    break;
  case 2:
    print("dos");
    break;
  default:
    print("otro");
}
for (;;) { break; }
let xs: integer[] = [1, 2, 3];
foreach (v in xs) { total = total + v; }
