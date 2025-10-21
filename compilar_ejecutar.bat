@echo off
:: Compila todos los archivos .java
javac *.java

:: Ejecuta el traductor con el archivo SimpleAdd.vm
java VMTranslator Prueba.vm

pause
