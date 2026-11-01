-- Esquema y datos de ejemplo (Entrega 4: CREATE, INSERT)
CREATE TABLE estudiantes (
  id INT PRIMARY KEY,
  nombre VARCHAR(50) NOT NULL,
  edad INT,
  promedio FLOAT,
  activo BOOLEAN,
  nacimiento DATE
);
CREATE TABLE cursos (
  id INT PRIMARY KEY,
  nombre VARCHAR(40) NOT NULL,
  creditos INT
);
CREATE TABLE inscripciones (
  id INT PRIMARY KEY,
  estudiante_id INT NOT NULL,
  curso_id INT NOT NULL,
  nota FLOAT
);

INSERT INTO estudiantes VALUES
  (1, 'Ana Lopez',    20, 85.5, TRUE,  '2005-03-14'),
  (2, 'Luis Perez',   17, 70.0, TRUE,  '2008-11-02'),
  (3, 'Maria Gomez',  22, 92.25, FALSE, '2003-07-30'),
  (4, 'Carlos Ruiz',  19, 64.0, TRUE,  '2006-01-09'),
  (5, 'Sofia Diaz',   18, NULL, TRUE,  '2007-05-21');

INSERT INTO cursos (id, nombre, creditos) VALUES
  (1, 'Compiladores', 4), (2, 'Bases de Datos', 4), (3, 'Algoritmos', 5);

INSERT INTO inscripciones VALUES
  (1, 1, 1, 90.0), (2, 1, 3, 80.0), (3, 2, 3, 65.5),
  (4, 3, 1, 95.0), (5, 3, 2, 88.0), (6, 4, 3, 70.0);
