#!/usr/bin/env bash
set -e
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$HERE"

echo "=== Compiscript: build ==="
mkdir -p out

echo "[1/2] Generando parsers ANTLR (Compiscript y SQL)..."
java -jar lib/antlr-4.13.2-complete.jar -Dlanguage=Java -visitor -no-listener -package generated -Xexact-output-dir -o src/generated grammar/Compiscript.g4
java -jar lib/antlr-4.13.2-complete.jar -Dlanguage=Java -visitor -no-listener -package sqlgen -Xexact-output-dir -o src/sqlgen grammar/Sql.g4

echo "[2/2] Compilando fuentes Java..."
javac -encoding UTF-8 -cp lib/antlr-4.13.2-complete.jar -d out -sourcepath src \
  src/generated/Compiscript*.java \
  src/sqlgen/*.java src/sql/*.java src/sql/*/*.java \
  src/semantic/*.java src/codegen/*.java \
  src/viz/*.java \
  src/gui/*.java \
  src/tests/TestRunner.java src/tests/SqlTestRunner.java \
  src/Main.java

echo
echo "Compilación exitosa."
echo
echo "Uso:"
echo "  java -cp out:lib/antlr-4.13.2-complete.jar Main gui"
echo "      (lanza el IDE gráfico Swing)"
echo
echo "  java -cp out:lib/antlr-4.13.2-complete.jar Main examples/ok/hello.cps -dot -png -symbols"
echo "      (modo batch: analiza un .cps y exporta árbol/tabla de símbolos)"
echo
echo "  java -cp out:lib/antlr-4.13.2-complete.jar tests.TestRunner"
echo "      (corre la batería de pruebas)"
echo
echo "Intérprete SQL:"
echo "  java -cp out:lib/antlr-4.13.2-complete.jar Main sql-gui"
echo "  java -cp out:lib/antlr-4.13.2-complete.jar Main sql examples/sql/01_esquema.sql -db database"
echo "  java -cp out:lib/antlr-4.13.2-complete.jar tests.SqlTestRunner"
