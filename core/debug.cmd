@ECHO OFF

REM $Id: debug.cmd 1880 2013-09-06 13:51:38Z SFB $

SETLOCAL

CD /D %~dp0

SET DEBUG_OPTS=-ea -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=y

SET run_file=bin\%1
IF EXIST %run_file% goto :run
IF EXIST %run_file%.cmd goto :run
SET run_file=core\%run_file%

:run
SHIFT
CALL "%run_file%" %1 %2 %3 %4 %5 %6 %7 %8 %9

REM End.
