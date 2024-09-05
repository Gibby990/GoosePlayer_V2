@echo off
set JRE_PATH=%~dp0jre\bin\java.exe
    "%JRE_PATH%" -jar .\gooseplayer2\target\gooseplayer2-1.0-jar-with-dependencies.jar
    pause