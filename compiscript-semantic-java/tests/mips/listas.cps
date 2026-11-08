let xs: integer[] = [5, 3, 8, 1, 9, 2];
print(xs[0]);
print(xs[5]);
xs[0] = 100;
print(xs[0] + xs[1]);
let m: integer[][] = [[1, 2, 3], [4, 5, 6]];
print(m[1][2]);
m[0][1] = 20;
print(m[0][0] + m[0][1] + m[0][2]);
let n: integer = 0;
foreach (fila in m) {
  foreach (v in fila) { n = n + v; }
}
print(n);
let k: integer = 0;
let s: integer = 0;
while (k < 6) { s = s + xs[k]; k = k + 1; }
print(s);
let vacia: integer[] = [];
let c: integer = 0;
foreach (v in vacia) { c = c + 1; }
print(c);
let nombres: string[] = ["ana", "luis", "eva"];
foreach (nm in nombres) { print(nm + "!"); }
// ordenamiento burbuja
let arr: integer[] = [9, 4, 7, 1, 8, 2];
let i: integer = 0;
while (i < 6) {
  let j: integer = 0;
  while (j < 5 - i) {
    if (arr[j] > arr[j + 1]) {
      let tmp: integer = arr[j];
      arr[j] = arr[j + 1];
      arr[j + 1] = tmp;
    }
    j = j + 1;
  }
  i = i + 1;
}
foreach (v in arr) { print(v); }
