@ECHO OFF

REM $Id: js.cmd 3824 2018-11-17 15:29:27Z SFB $

SETLOCAL

SET CLASSPATH=%RVPF_CONFIG%\script\local;%RVPF_CONFIG%\script
SET CLASSPATH=%CLASSPATH%;%RVPF_CONFIG%\service\local;%RVPF_CONFIG%\service
SET CLASSPATH=%CLASSPATH%;%RVPF_CORE_LIB%\rvpf-http.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_CORE_LIB%\rvpf-store.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_CORE_LIB%\rvpf-processor.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_CORE_LIB%\rvpf-forwarder.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_CORE_LIB%\rvpf-tools.jar

SET PATH=%RVPF_CORE_LIB%;%PATH%

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
