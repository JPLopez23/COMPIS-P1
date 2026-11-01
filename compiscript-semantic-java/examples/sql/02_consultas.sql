-- SELECT, WHERE, ORDER BY (Entrega 5)
SELECT nombre FROM estudiantes WHERE edad >= 18;
SELECT id, nombre, promedio FROM estudiantes WHERE activo = TRUE AND promedio > 65 ORDER BY promedio DESC;
SELECT * FROM cursos WHERE creditos = 4 OR nombre = 'Algoritmos';
SELECT nombre FROM estudiantes WHERE nacimiento < '2006-01-01' ORDER BY nombre;

-- JOIN, agregaciones y GROUP BY / HAVING (Entrega 6)
SELECT e.nombre, c.nombre AS curso, i.nota
  FROM estudiantes e
  INNER JOIN inscripciones i ON i.estudiante_id = e.id
  INNER JOIN cursos c ON c.id = i.curso_id
  ORDER BY e.nombre, c.nombre;
SELECT COUNT(*), SUM(creditos), AVG(creditos), MIN(nombre), MAX(creditos) FROM cursos;
SELECT curso_id, COUNT(*) AS inscritos, AVG(nota) AS promedio
  FROM inscripciones GROUP BY curso_id HAVING COUNT(*) >= 2 ORDER BY curso_id;

-- Subconsultas (Entrega 7)
SELECT nombre FROM estudiantes WHERE id IN (SELECT estudiante_id FROM inscripciones WHERE curso_id = 3);
SELECT nombre FROM estudiantes WHERE id NOT IN (SELECT estudiante_id FROM inscripciones);
SELECT nombre FROM estudiantes e WHERE EXISTS (SELECT 1 FROM inscripciones i WHERE i.estudiante_id = e.id AND i.nota > 90);
SELECT nombre FROM estudiantes WHERE promedio > (SELECT AVG(promedio) FROM estudiantes);
