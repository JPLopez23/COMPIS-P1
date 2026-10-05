let edad: integer = 21;
let nombre: string = "Ana";
let activa: boolean = true;
let nada = null;

let x: integer = 5 + 3 * 2;
let y: boolean = !(x < 10 || x > 20);
let z: integer = (1 + 2) * 3;

let apodo: string;
apodo = "Anita";

const PI: integer = 314;

function saludar(nombre: string): string {
  return "Hola " + nombre;
}
let mensaje: string = saludar("Mundo");

function factorial(n: integer): integer {
  if (n <= 1) {
    return 1;
  }
  return n * factorial(n - 1);
}
let f5: integer = factorial(5);

function crearContador(): integer {
  function siguiente(): integer {
    return 1;
  }
  return siguiente();
}
let contador: integer = crearContador();

let notas: integer[] = [90, 85, 100];
print(notas[0]);

let matriz: integer[][] = [[1, 2], [3, 4]];
print(matriz[1][0]);

class Animal {
  let nombre: string;

  function constructor(nombre: string) {
    this.nombre = nombre;
  }

  function hablar(): string {
    return this.nombre + " hace ruido.";
  }
}

class Perro : Animal {
  function hablar(): string {
    return this.nombre + " ladra.";
  }
}

let perro: Perro = new Perro("Toby");
print(perro.hablar());

if (edad > 18) {
  print("Mayor de edad");
} else {
  print("Menor de edad");
}

let contadorWhile: integer = 0;
while (contadorWhile < 3) {
  print(contadorWhile);
  contadorWhile = contadorWhile + 1;
}

let contadorDo: integer = 0;
do {
  contadorDo = contadorDo + 1;
} while (contadorDo < 3);

for (let i: integer = 0; i < 3; i = i + 1) {
  print(i);
}

foreach (nota in notas) {
  if (nota < 60) { continue; }
  if (nota == 100) { break; }
  print(nota);
}

switch (edad) {
  case 18:
    print("Dieciocho");
  case 21:
    print("Veintiuno");
  default:
    print("Otra edad");
}

try {
  let peligro: integer = notas[100];
  print(peligro);
} catch (err) {
  print("Error atrapado: " + err);
}

{
  let interno: integer = 42;
  print(interno);
}
