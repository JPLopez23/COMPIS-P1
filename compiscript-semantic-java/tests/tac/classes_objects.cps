class Punto {
  var x: integer = 0;
  var y: integer = 0;
  function constructor(x: integer, y: integer) {
    this.x = x;
    this.y = y;
  }
  function suma(): integer {
    return this.x + this.y;
  }
  function mover(dx: integer): integer {
    this.x = this.x + dx;
    return this.suma();
  }
}
class Punto3 : Punto {
  var z: integer = 7;
  function total(): integer {
    return this.suma() + this.z;
  }
}
let p: Punto3 = new Punto3(1, 2);
p.z = 10;
print(p.total());
print(p.mover(5));
let ps: Punto[] = [new Punto(1, 1), new Punto(2, 2)];
ps[0].x = ps[1].y + 1;
print(ps[0].suma());
