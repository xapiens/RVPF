@ECHO OFF

REM $Id: js.cmd 3194 2016-09-28 17:17:14Z SFB $

SETLOCAL

SET CLASSPATH=%RVPF_CONFIG%\script\local;%RVPF_CONFIG%\script
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\log4j.jar

SET PATH=%RVPF_LIB%;%PATH%

SET script=%1
IF "%script%"=="" GOTO :exec-js
SET script=%script%.js
IF EXIST %script% GOTO :exec-js
SET script=script\%script%
IF EXIST %script% GOTO :exec-js
SET script=%RVPF_HOME%\core\%script%
IF EXIST %script% GOTO :exec-js
ECHO Script %1 not found
EXIT /B 1

:exec-js
SHIFT
IF "%DEBUG_OPTS%"=="" SET DEBUG_OPTS=-ea
"%JAVA%" %DEBUG_OPTS% jdk.nashorn.tools.Shell %script% -- %1 %2 %3 %4 %5 %6 %7 %8 %9

REM End.
