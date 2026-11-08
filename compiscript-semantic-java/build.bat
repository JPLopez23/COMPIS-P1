@echo off
REM Compila el analizador semantico de Compiscript (ANTLR 4 + Java + Swing).
echo === Compiscript: build ===

if not exist out mkdir out

echo [1/2] Generando parser ANTLR (Java)...
java -jar lib\antlr-4.13.2-complete.jar -Dlanguage=Java -visitor -no-listener -package generated -Xexact-output-dir -o src\generated grammar\Compiscript.g4
if errorlevel 1 goto :error

echo [2/2] Compilando fuentes Java...
javac -encoding UTF-8 -cp lib\antlr-4.13.2-complete.jar -d out -sourcepath src ^
  src\generated\Compiscript*.java ^
  src\semantic\*.java ^
  src\codegen\*.java ^
  src\mips\*.java ^
  src\viz\*.java ^
  src\gui\*.java ^
  src\tests\TestRunner.java ^
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
goto :eof

:error
echo Error de compilacion.
exit /b 1
