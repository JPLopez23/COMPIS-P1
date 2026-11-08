function mk(): integer {
  let acc: integer = 10;
  function add(x: integer): integer {
    acc = acc + x;
    return acc;
  }
  let g = add;
  return g(5) + g(6);
}
function aplica(f: integer, v: integer): integer { return v; }
function doble(x: integer): integer { return x * 2; }
function compone(n: integer): integer {
  let h = doble;
  let r: integer = n;
  let i: integer = 0;
  while (i < 3) {
    r = h(r);
    i = i + 1;
  }
  return r;
}
class Caja {
  var v: integer = 7;
  function usa(): integer {
    function interna(k: integer): integer { return this.v + k; }
    let q = interna;
    return q(1) + q(2);
  }
}
let sinInicial: string;
let numero: integer;
print(mk());
print(compone(3));
let c: Caja = new Caja();
print(c.usa());
print(sinInicial);
print(numero);
