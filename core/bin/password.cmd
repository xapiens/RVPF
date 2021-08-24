@ECHO OFF

REM $Id: password.cmd 3194 2016-09-28 17:17:14Z SFB $

SETLOCAL

SET CLASSPATH=%RVPF_CORE_LIB%\jetty-servlet.jar

"%java%" org.eclipse.jetty.util.security.Password %1 %2 %3 %4 %5 %6 %7 %8 %9

REM End.
