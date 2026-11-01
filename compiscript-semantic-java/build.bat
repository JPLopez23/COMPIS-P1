@echo off
REM Compila el analizador semantico de Compiscript (ANTLR 4 + Java + Swing).
echo === Compiscript: build ===

if not exist out mkdir out

echo [1/2] Generando parsers ANTLR (Compiscript y SQL)...
java -jar lib\antlr-4.13.2-complete.jar -Dlanguage=Java -visitor -no-listener -package generated -Xexact-output-dir -o src\generated grammar\Compiscript.g4
if errorlevel 1 goto :error
java -jar lib\antlr-4.13.2-complete.jar -Dlanguage=Java -visitor -no-listener -package sqlgen -Xexact-output-dir -o src\sqlgen grammar\Sql.g4
if errorlevel 1 goto :error

echo [2/2] Compilando fuentes Java...
javac -encoding UTF-8 -cp lib\antlr-4.13.2-complete.jar -d out -sourcepath src ^
  src\generated\Compiscript*.java ^
  src\sqlgen\*.java ^
  src\sql\*.java ^
  src\sql\ast\*.java ^
  src\sql\semantic\*.java ^
  src\sql\storage\*.java ^
  src\sql\exec\*.java ^
  src\semantic\*.java ^
  src\codegen\*.java ^
  src\viz\*.java ^
  src\gui\*.java ^
  src\tests\TestRunner.java ^
  src\tests\SqlTestRunner.java ^
  src\Main.java
if errorlevel 1 goto :error

echo.
echo Compilacion exitosa.
echo.
echo Uso:
echo   java -cp "out;lib\antlr-4.13.2-complete.jar" Main gui
echo       (lanza el IDE grafico Swing)
echo.
echo   java -cp "out;lib\antlr-4.13.2-complete.jar" Main examples\ok\hello.cps -dot -png -symbols
echo       (modo batch: analiza un .cps y exporta arbol/tabla de simbolos)
echo.
echo   java -cp "out;lib\antlr-4.13.2-complete.jar" tests.TestRunner
echo       (corre la bateria de pruebas)
echo.
echo   Interprete SQL:
echo   java -cp "out;lib\antlr-4.13.2-complete.jar" Main sql-gui
echo   java -cp "out;lib\antlr-4.13.2-complete.jar" Main sql examples\sql\01_esquema.sql -db database
echo   java -cp "out;lib\antlr-4.13.2-complete.jar" tests.SqlTestRunner
goto :eof

:error
echo Error de compilacion.
exit /b 1
