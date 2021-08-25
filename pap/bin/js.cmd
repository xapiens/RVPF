@ECHO OFF

REM $Id: js.cmd 3535 2017-07-23 14:48:16Z SFB $

SETLOCAL

SET CLASSPATH=%RVPF_CONFIG%\script\local;%RVPF_CONFIG%\script
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\rvpf-pap-cip.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\rvpf-pap-dnp3.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\rvpf-pap-modbus.jar

SET PATH=%RVPF_LIB%;%RVPF_CORE_LIB%;%PATH%

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
