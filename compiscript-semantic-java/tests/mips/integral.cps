const LIMITE: integer = 5;
let contador: integer = 0;
let historial: integer[] = [0, 0, 0, 0, 0];

function registrar(v: integer) {
  if (contador >= LIMITE) { return; }
  historial[contador] = v;
  contador = contador + 1;
}

class Cuenta {
  var saldo: integer = 100;
  var movimientos: integer[] = [0, 0, 0];
  var titular: string = "anonimo";
  function constructor(titular: string, saldo: integer) {
    this.titular = titular;
    this.saldo = saldo;
  }
  function depositar(m: integer): integer {
    this.saldo = this.saldo + m;
    this.movimientos[0] = this.movimientos[0] + 1;
    registrar(m);
    return this.saldo;
  }
  function retirar(m: integer): boolean {
    if (m > this.saldo) { return false; }
    this.saldo = this.saldo - m;
    this.movimientos[1] = this.movimientos[1] + 1;
    registrar(0 - m);
    return true;
  }
  function resumen(): string {
    return this.titular + ":ok";
  }
}

class Banco {
  var a: Cuenta = null;
  var b: Cuenta = null;
  function constructor(x: Cuenta, y: Cuenta) {
    this.a = x;
    this.b = y;
  }
  function transferir(m: integer): boolean {
    if (this.a.retirar(m)) {
      this.b.depositar(m);
      return true;
    }
    return false;
  }
  function total(): integer { return this.a.saldo + this.b.saldo; }
}

function fabrica(nombre: string, n: integer): Cuenta {
  return new Cuenta(nombre, n * 10);
}

function mayor(c1: Cuenta, c2: Cuenta): Cuenta {
  return c1.saldo >= c2.saldo ? c1 : c2;
}

function contadorDe(paso: integer): integer {
  let v: integer = 0;
  function sube(): integer {
    v = v + paso;
    return v;
  }
  sube();
  sube();
  return sube() * 100 + v;
}

let c1: Cuenta = fabrica("ana", 5);
let c2: Cuenta = fabrica("luis", 3);
let banco: Banco = new Banco(c1, c2);
print(banco.total());
print(banco.transferir(20));
print(banco.transferir(500));
print(c1.resumen());
print(c2.resumen());
print(mayor(c1, c2).resumen());
print(c1.depositar(7) + c2.depositar(1));
print(c1.movimientos[0] + c1.movimientos[1] * 10);
foreach (h in historial) { print(h); }
print(contadorDe(3));
let e: integer = ((contador + 4) * (9 - 2)) / 2 + (17 % 5 + 1);
print(e);
let i: integer = 0;
let acum: string = "";
while (i < 6 && !(acum == "----")) {
  acum = acum + "-";
  i = i + 1;
}
print(acum);
