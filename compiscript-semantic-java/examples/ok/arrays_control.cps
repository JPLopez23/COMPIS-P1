let notas: integer[] = [90, 85, 100, 70];
let matriz: integer[][] = [[1, 2], [3, 4], [5, 6]];

print(notas[0]);
print(matriz[2][1]);

let suma: integer = 0;
for (let i: integer = 0; i < 4; i = i + 1) {
  suma = suma + notas[i];
}
print(suma);

foreach (nota in notas) {
  if (nota < 60) {
    continue;
  }
  if (nota == 100) {
    break;
  }
  print(nota);
}

let k: integer = 3;
while (k > 0) {
  k = k - 1;
}
do {
  k = k + 1;
} while (k < 3);

switch (k) {
  case 0:
    print("cero");
  case 3:
    print("tres");
  default:
    print("otro");
}

try {
  let x: integer = notas[10];
  print(x);
} catch (err) {
  print("indice fuera de rango: " + err);
}
