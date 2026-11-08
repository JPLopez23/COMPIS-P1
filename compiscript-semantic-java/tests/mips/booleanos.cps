function ruido(x: integer): boolean {
  print(x);
  return x > 0;
}
let a: integer = 3;
let b: integer = 7;
print(a < b);
print(a > b);
print(a == 3);
print(a != 3);
print(a <= 3 && b >= 7);
print(a < 0 || b > 5);
print(!(a < b));
print(!true);
let ok: boolean = a < b && b < 10;
print(ok);
let t: boolean = true;
let f: boolean = false;
print(t && f);
print(t || f);
print(f || f);
if (ruido(1) && ruido(0) && ruido(2)) { print("no"); } else { print("corto1"); }
if (ruido(0) || ruido(5) || ruido(9)) { print("corto2"); }
let m: integer = a > b ? a : b;
print(m);
let s: string = a == 3 ? "tres" : "otro";
print(s);
let anidado: integer = a > 10 ? 1 : (a > 2 ? 2 : 3);
print(anidado);
