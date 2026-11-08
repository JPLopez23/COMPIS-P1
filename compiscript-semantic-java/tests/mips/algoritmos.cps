function gcd(a: integer, b: integer): integer {
  while (b != 0) {
    let t: integer = b;
    b = a % b;
    a = t;
  }
  return a;
}
function esPrimo(n: integer): boolean {
  if (n < 2) { return false; }
  let d: integer = 2;
  while (d * d <= n) {
    if (n % d == 0) { return false; }
    d = d + 1;
  }
  return true;
}
function hanoi(n: integer, a: integer, b: integer, c: integer): integer {
  if (n == 0) { return 0; }
  return hanoi(n - 1, a, c, b) + 1 + hanoi(n - 1, c, b, a);
}
function ack(m: integer, n: integer): integer {
  if (m == 0) { return n + 1; }
  if (n == 0) { return ack(m - 1, 1); }
  return ack(m - 1, ack(m, n - 1));
}
function sumaRec(n: integer): integer {
  if (n == 0) { return 0; }
  return n + sumaRec(n - 1);
}
print(gcd(48, 18));
print(gcd(17, 5));
let c: integer = 0;
for (let i: integer = 1; i <= 50; i = i + 1) {
  if (esPrimo(i)) { c = c + 1; }
}
print(c);
print(hanoi(10, 1, 2, 3));
print(ack(2, 3));
print(sumaRec(2000));
let bs: integer[] = [1, 3, 5, 7, 9, 11, 13];
let lo: integer = 0;
let hi: integer = 6;
let objetivo: integer = 11;
let pos: integer = 0 - 1;
while (lo <= hi) {
  let mid: integer = (lo + hi) / 2;
  if (bs[mid] == objetivo) { pos = mid; break; }
  if (bs[mid] < objetivo) { lo = mid + 1; } else { hi = mid - 1; }
}
print(pos);
