@ECHO OFF

REM $Id: jython.cmd 3194 2016-09-28 17:17:14Z SFB $

SETLOCAL

SET CLASSPATH=%RVPF_CONFIG%\script\local;%RVPF_CONFIG%\script;%JYTHON_HOME%\jython.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\rvpf-valve.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\log4j.jar

SET PATH=%RVPF_LIB%;%PATH%

SET script=%1
IF "%script%"=="" GOTO :exec-jython
SET script=%script%.py
IF EXIST %script% GOTO :exec-jython
SET script=script\%script%
IF EXIST %script% GOTO :exec-jython
SET script=%RVPF_HOME%\core\%script%
IF EXIST %script% GOTO :exec-jython
ECHO Script %1 not found
EXIT /B 1

:exec-jython
SHIFT
IF "%DEBUG_OPTS%"=="" SET DEBUG_OPTS=-ea
"%JAVA%" %DEBUG_OPTS% org.python.util.jython %script% %1 %2 %3 %4 %5 %6 %7 %8 %9

REM End.
