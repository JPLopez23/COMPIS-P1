let saludo: string = "Hola";
let nombre: string = "Mundo";
let completo: string = saludo + ", " + nombre + "!";
print(completo);
print(saludo + nombre);
print(saludo == "Hola");
print(saludo != "Hola");
print(saludo == nombre);
print(completo == "Hola, Mundo!");
let x: string = "a";
let i: integer = 0;
while (i < 4) {
  x = x + "b";
  i = i + 1;
}
print(x);
let dia: string = "martes";
switch (dia) {
  case "lunes":
    print("L");
    break;
  case "martes":
    print("M");
    break;
  default:
    print("otro");
}
function repetir(s: string, n: integer): string {
  let r: string = "";
  let k: integer = 0;
  while (k < n) {
    r = r + s;
    k = k + 1;
  }
  return r;
}
print(repetir("ab", 3));
print("sin salto");
print("");
