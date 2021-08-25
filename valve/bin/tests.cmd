@ECHO OFF

REM $Id: tests.cmd 3683 2018-09-02 13:43:05Z SFB $

SETLOCAL

PUSHD %~dp0
CALL setup\rvpf-tests-env
IF NOT "%ERRORLEVEL%"=="0" EXIT /B %ERRORLEVEL%
POPD

SET sys_props=-Xmx256M
SET sys_props=%sys_props% -Duser.country=CA -Duser.language=en
REM SET sys_props=%sys_props% -Djava.rmi.server.logCalls=true

SET classes=%RVPF_LIB%\rvpf-valve.jar
SET tests_classes=%RVPF_LIB%\rvpf-valve-tests.jar
IF EXIST %RVPF_SUB%\build\main\classes SET classes=%RVPF_SUB%\build\main\classes
IF EXIST %RVPF_SUB%\build\test\classes SET tests_classes=%RVPF_SUB%\build\test\classes

SET CLASSPATH=%RVPF_TESTS_CONFIG%/local;%RVPF_TESTS_CONFIG%;%tests_classes%;%classes%
SET CLASSPATH=%CLASSPATH%;%RVPF_CORE_LIB%\rvpf-core-tests.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_CORE_LIB%\rvpf-service.jar

SET PATH=%RVPF_LIB%;%RVPF_CORE_LIB%;%PATH%

IF "%DEBUG_OPTS%"=="" SET DEBUG_OPTS=-ea

SET tests_log=%RVPF_LOG%\tests.log
IF EXIST %tests_log% DEL /Q %tests_log%
"%JAVA%" %sys_props% %DEBUG_OPTS% org.rvpf.tests.FrameworkTests %TESTNG_XML% %*
SET error_level=%ERRORLEVEL%
IF NOT EXIST %tests_log% GOTO :exit

TYPE %tests_log% | FIND "FATAL"
TYPE %tests_log% | FIND "ERROR"
TYPE %tests_log% | FIND "WARN"

:exit
EXIT /B %error_level%

REM End.
