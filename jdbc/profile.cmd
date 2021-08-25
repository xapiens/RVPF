@ECHO OFF

REM $Id: profile.cmd 3673 2018-08-16 18:38:55Z SFB $

SETLOCAL EnableDelayedExpansion

CD /D %~dp0

SET DEBUG_OPTS=-ea

SET DEBUG_OPTS=%DEBUG_OPTS% -Drvpf.profile.start.millis=0
SET DEBUG_OPTS=%DEBUG_OPTS% -Drvpf.profile.sample.millis=100
REM SET DEBUG_OPTS=%DEBUG_OPTS% -Drvpf.profile.sample.count=600000
SET DEBUG_OPTS=%DEBUG_OPTS% -Drvpf.profile.snapshot.millis=60000
SET DEBUG_OPTS=%DEBUG_OPTS% -Drvpf.profile.snapshot.count=2
REM SET DEBUG_OPTS=%DEBUG_OPTS% -Drvpf.profile.snapshot.depth=3
REM SET DEBUG_OPTS=%DEBUG_OPTS% -Drvpf.profile.thread.group=main
REM SET DEBUG_OPTS=%DEBUG_OPTS% -Drvpf.profile.thread.state=runnable
REM SET DEBUG_OPTS=%DEBUG_OPTS% -Drvpf.profile.stop.ignored=yes

SET run_dir=bin
SET run_file=%1.cmd
IF NOT EXIST !run_dir!\!run_file! SET run_dir=jdbc\bin
SHIFT

PUSHD .
CD !run_dir!
CALL setup\rvpf-env
IF NOT "%ERRORLEVEL%"=="0" EXIT /B %ERRORLEVEL%
POPD
CALL "!run_dir!\!run_file!" %1 %2 %3 %4 %5 %6 %7 %8 %9

REM End.
