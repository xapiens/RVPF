@ECHO OFF

REM $Id: stopper.cmd 3194 2016-09-28 17:17:14Z SFB $

SETLOCAL

SET CLASSPATH=%RVPF_CONFIG%\service\local;%RVPF_CONFIG%\service;%RVPF_LIB%\rvpf-service.jar

IF "%DEBUG_OPTS%"=="" SET DEBUG_OPTS=-ea
"%JAVA%" %DEBUG_OPTS% "-Drvpf.log.id=Stop" "-Drvpf.log.prefix=stopper" org.rvpf.service.Stopper %*

REM End.
