@echo off
rem Builds the Auto Clicker mod using the bundled local Gradle.
cd /d "%~dp0"
call gradle-9.7.1\bin\gradle.bat build %*
echo.
echo Jar: %~dp0build\libs\autoclicker-1.0.1.jar
pause
