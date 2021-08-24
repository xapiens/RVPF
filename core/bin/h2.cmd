@ECHO OFF

REM $Id: h2.cmd 3194 2016-09-28 17:17:14Z SFB $

SETLOCAL

SET H2TOOL=Console
IF "%1"=="" GOTO :run
SET H2TOOL=%1
SHIFT

:run
SET CLASSPATH=%RVPF_CORE_LIB%\h2.jar

IF "%DEBUG_OPTS%"=="" SET DEBUG_OPTS=-ea
"%JAVA%" org.h2.tools.%H2TOOL% %1 %2 %3 %4 %5 %6 %7 %8 %9

REM End.
