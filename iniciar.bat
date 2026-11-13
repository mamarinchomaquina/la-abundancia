@echo off
title La Abundancia
echo.
echo Iniciando La Abundancia...
echo.

REM Iniciar backend
echo Iniciando backend...
start "Backend" cmd /k "cd /d %~dp0backend && node index.js"
timeout /t 3 /nobreak >nul

REM Iniciar frontend
echo Iniciando frontend...
start "Frontend" cmd /k "cd /d %~dp0web-angular && ng serve --open"

echo Listo!
