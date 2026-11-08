class Animal {
  var nombre: string = "sin nombre";
  var patas: integer = 4;
  function constructor(nombre: string) {
    this.nombre = nombre;
  }
  function hablar(): string {
    return this.nombre + " hace un ruido";
  }
  function cuentaPatas(): integer {
    return this.patas;
  }
}
class Perro : Animal {
  var raza: string = "criollo";
  function hablar(): string {
    return this.nombre + " ladra";
  }
  function info(): string {
    return this.nombre + " (" + this.raza + ")";
  }
}
class Punto {
  var x: integer = 0;
  var y: integer = 0;
  function constructor(x: integer, y: integer) {
    this.x = x;
    this.y = y;
  }
  function suma(): integer { return this.x + this.y; }
  function mover(dx: integer, dy: integer): integer {
    this.x = this.x + dx;
    this.y = this.y + dy;
    return this.suma();
  }
}
let a: Animal = new Animal("Gato");
let p: Perro = new Perro("Toby");
print(a.hablar());
print(p.hablar());
print(p.info());
print(p.cuentaPatas());
print(p.raza);
p.raza = "labrador";
print(p.info());
let q: Punto = new Punto(1, 2);
print(q.suma());
print(q.mover(10, 20));
print(q.x);
q.y = 100;
print(q.suma());
let ps: Punto[] = [new Punto(1, 1), new Punto(2, 2), new Punto(3, 3)];
let t: integer = 0;
foreach (pt in ps) { t = t + pt.suma(); }
print(t);
ps[1].x = 50;
print(ps[1].suma());
