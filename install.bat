@echo off
rem Builds the mod and copies the jar into .minecraft/mods.
cd /d "%~dp0"
if not exist "build\libs\autoclicker-1.0.1.jar" (
    call gradle-9.7.1\bin\gradle.bat build
)
set "MODS=%APPDATA%\.minecraft\mods"
if not exist "%MODS%" mkdir "%MODS%"
del /Q "%MODS%\autoclicker-*.jar" 2>nul
copy /Y "build\libs\autoclicker-1.0.1.jar" "%MODS%\"
echo.
echo Installed to %MODS%
pause
