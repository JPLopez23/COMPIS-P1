class Animal {
  var name: string;
  function speak(): string {
    return this.name;
  }
}

class Dog : Animal {
  function bark(): string {
    return "guau";
  }
}

let d: Dog = new Dog();
d.name = "Rex";
print(d.speak());
print(d.bark());
