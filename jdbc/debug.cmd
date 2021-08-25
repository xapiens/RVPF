@ECHO OFF

REM $Id: debug.cmd 732 2007-10-26 14:33:23Z SFB $

SETLOCAL

CD /D %~dp0

SET DEBUG_OPTS=-ea -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=y

IF NOT EXIST setup.cmd GOTO :run
CALL .\setup auto
IF NOT "%ERRORLEVEL%"=="0" EXIT /B %ERRORLEVEL%

:run
CALL "bin\%1" %2 %3 %4 %5 %6 %7 %8 %9

REM End.
