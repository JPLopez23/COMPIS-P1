-- DROP TABLE ... CASCADE. Ejecutar despues de 01_esquema.sql (la base debe tener las tablas originales).
--
-- Sin CASCADE, DROP TABLE cursos; falla porque inscripciones la referencia (ver 04_errores.sql).
-- Con CASCADE se elimina la tabla y se quitan las claves foraneas que otras tablas tenian hacia ella;
-- las tablas dependientes y sus datos se conservan.

DROP TABLE cursos CASCADE;

SELECT * FROM inscripciones ORDER BY id;     -- inscripciones sigue existiendo, con sus datos
INSERT INTO inscripciones VALUES (99, 1, 12345, 50.0);   -- ya no hay clave foranea hacia cursos: se acepta
SELECT COUNT(*) FROM inscripciones;

-- estudiantes sigue referenciada por inscripciones: sin CASCADE no se puede eliminar.
-- Descomenta la siguiente linea para ver el error:
-- DROP TABLE estudiantes;
DROP TABLE estudiantes CASCADE;
