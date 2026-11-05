-- Índices y su mantenimiento (Entrega 7)
CREATE INDEX idx_edad ON estudiantes(edad);
CREATE INDEX idx_est_insc ON inscripciones(estudiante_id);
SELECT nombre FROM estudiantes WHERE edad >= 19;       -- usa idx_edad
INSERT INTO estudiantes VALUES (6, 'Pedro Vega', 30, 75.0, TRUE, '1996-02-02');
UPDATE estudiantes SET edad = 31 WHERE id = 6;
DELETE FROM inscripciones WHERE estudiante_id = 2;   -- hay que borrar primero lo que referencia a Luis
DELETE FROM estudiantes WHERE edad < 18;
SELECT id, nombre, edad FROM estudiantes WHERE edad > 20;
DROP INDEX idx_edad;
UPDATE inscripciones SET nota = nota + 5 WHERE curso_id = 3;
SELECT * FROM inscripciones ORDER BY id;
