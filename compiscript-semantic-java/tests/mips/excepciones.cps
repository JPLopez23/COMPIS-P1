let xs: integer[] = [1, 2, 3];
try {
  print(xs[5]);
  print("no se imprime");
} catch (e) {
  print("capturado: " + e);
}
try {
  let z: integer = 0;
  print(10 / z);
} catch (e) {
  print("capturado: " + e);
}
function peligro(i: integer): integer {
  return xs[i];
}
try {
  print(peligro(1));
  print(peligro(9));
} catch (err) {
  print("desde funcion: " + err);
}
try {
  try {
    print(xs[7]);
  } catch (a) {
    print("interno " + a);
    print(xs[8]);
  }
} catch (b) {
  print("externo " + b);
}
print("despues");
let r: integer = 0;
for (let i: integer = 0; i < 4; i = i + 1) {
  try {
    r = r + xs[i];
  } catch (q) {
    r = r + 100;
  }
}
print(r);
print(xs[-1]);
print("nunca");
