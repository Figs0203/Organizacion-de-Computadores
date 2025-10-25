@echo off
echo Compilando archivos Java...
javac *.java
if errorlevel 1 (
    echo Error en la compilacion
    pause
    exit /b
)
echo Compilacion exitosa.

echo.
set /p ruta=Introduce la ruta del archivo .vm o carpeta a traducir: 

echo Ejecutando el traductor...
java VMTranslator "%ruta%"
echo.
echo Traduccion completada.

pause
