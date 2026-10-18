let s: string;
let n: integer;
let m: integer[];
function saludar(): integer { print("hola"); return 1; }
function nada() { print("x"); }
function ident(x: integer): integer { return x; }
class Caja {
  var v: integer;
  function constructor(v: integer) { this.v = v; }
  function crear(): integer {
    function interna(): integer { return this.v + 1; }
    return interna();
  }
  function otro(): integer { return crear() + 1; }
}
nada();
saludar();
n = ident(ident(2) + ident(3));
let c: Caja = new Caja(4);
print(c.crear());
print(c.otro());
if ("a" == "b" || n > 2 && true) { n = 1; }
if (false) { n = 2; }
let w: integer = n > 0 ? (n > 1 ? 2 : 1) : 0;
m = [ident(1), ident(2)];
m[ident(0)] = m[1] + 1;
