-- Ejemplo de ERRORES SEMANTICOS. Ejecutar despues de 01_esquema.sql y ANTES de 05_cascade.sql.
-- El script se valida completo antes de ejecutarse: no se ejecuta ninguna sentencia y el IDE
-- (o el CLI) muestra todos los errores con su linea y columna.
--
--   java -cp "out;lib/antlr-4.13.2-complete.jar" Main sql examples/sql/04_errores.sql -db demo
--
-- Para ver errores lexicos o sintacticos, agrega por ejemplo:  SELECT @ FROM cursos;   o   SELECT nombre WHERE x = 1;

SELECT * FROM fantasma;                                           -- tabla inexistente
SELECT apellido FROM estudiantes;                                 -- columna inexistente
SELECT nombre FROM estudiantes WHERE edad = 'veinte';             -- tipos incompatibles (INT con VARCHAR)
SELECT nombre FROM estudiantes WHERE nacimiento > 'ayer';         -- fecha invalida
SELECT id FROM estudiantes e JOIN inscripciones i ON i.estudiante_id = e.id;   -- 'id' es ambigua
SELECT nombre, COUNT(*) FROM estudiantes;                         -- columna fuera de GROUP BY
SELECT SUM(nombre) FROM estudiantes;                              -- SUM sobre texto
SELECT nombre FROM estudiantes WHERE COUNT(*) > 1;                -- agregado en WHERE
INSERT INTO cursos VALUES (9, 'Redes');                           -- faltan valores
INSERT INTO cursos VALUES ('9', 'Redes', 3);                      -- tipo incorrecto en la primera columna
INSERT INTO estudiantes (id) VALUES (9);                          -- 'nombre' es NOT NULL y no recibe valor
UPDATE cursos SET creditos = 'muchos';                            -- tipo incorrecto
CREATE INDEX idx_x ON estudiantes(apellido);                      -- columna inexistente
DROP INDEX idx_que_no_existe;                                     -- indice inexistente
SELECT nombre FROM estudiantes
  WHERE id = (SELECT id, nombre FROM estudiantes);                -- subconsulta escalar con 2 columnas
CREATE TABLE notas (a INT REFERENCES cursos(nombre));             -- la columna referenciada no es PRIMARY KEY
DROP TABLE cursos;                                                -- la referencia inscripciones: usa CASCADE
