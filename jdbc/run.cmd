@ECHO OFF

REM $Id: run.cmd 3194 2016-09-28 17:17:14Z SFB $

SETLOCAL EnableDelayedExpansion

CD /D %~dp0

SET run_dir=bin
SET run_file=%1.cmd
IF EXIST !run_dir!\!run_file! goto :run
SET run_dir=jdbc\bin

:run
SHIFT
PUSHD .
CD !run_dir!
CALL setup\rvpf-env
IF NOT "%ERRORLEVEL%"=="0" EXIT /B %ERRORLEVEL%
POPD
CALL "!run_dir!\!run_file!" %1 %2 %3 %4 %5 %6 %7 %8 %9

REM End.
