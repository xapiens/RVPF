@ECHO OFF

REM $Id: run-jdb.cmd 725 2007-10-12 17:32:28Z SFB $

SETLOCAL

CD /D %~dp0
IF NOT EXIST setup.cmd GOTO :run
CALL .\setup auto
IF NOT "%ERRORLEVEL%"=="0" EXIT /B %ERRORLEVEL%

:run
SET DEBUG_OPTS=-ea
SET JAVA=jdb
CALL "bin\%1" %2 %3 %4 %5 %6 %7 %8 %9

REM End.
