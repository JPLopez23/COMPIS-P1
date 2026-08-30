class Point {
  var x: integer;
  var y: integer;
  function get_x(): integer { return this.x; }
}
let p: Point = new Point();
p.x = 3;
p.y = 4;
print(p.get_x());
