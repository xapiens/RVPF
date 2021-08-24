@ECHO OFF

REM $Id: tests.cmd 3927 2019-04-23 13:33:41Z SFB $

SETLOCAL

PUSHD %~dp0
CALL setup\rvpf-tests-env
IF NOT "%ERRORLEVEL%"=="0" EXIT /B %ERRORLEVEL%
POPD

SET sys_props=-Xmx256M
SET sys_props=%sys_props% -Duser.country=CA -Duser.language=en
REM SET sys_props=%sys_props% -Djava.rmi.server.logCalls=true

SET classes=%RVPF_LIB%\rvpf-http.jar;%RVPF_LIB%\rvpf-store.jar
SET classes=%classes%;%RVPF_LIB%\rvpf-processor.jar;%RVPF_LIB%\rvpf-forwarder.jar
SET tests_classes=%RVPF_LIB%\rvpf-core-tests.jar
IF EXIST %RVPF_HOME%\core\build\main\classes SET classes=%RVPF_HOME%\core\shared;%RVPF_HOME%\core\build\main\classes
IF EXIST %RVPF_HOME%\core\build\test\classes SET tests_classes=%RVPF_HOME%\core\build\test\classes

SET CLASSPATH=%RVPF_TESTS_CONFIG%\local;%RVPF_TESTS_CONFIG%;%tests_classes%;%classes%
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\javax.servlet-api.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\javax.json.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\javax.json-api.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\log4j-api.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\log4j-core.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\log4j-jul.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\javax.mail.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\xml-resolver.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\xstream.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\testng.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\jcommander.jar

SET PATH=%RVPF_LIB%;%PATH%

SET RVPF_TESTS_VALUE=OK

IF "%DEBUG_OPTS%"=="" SET DEBUG_OPTS=-ea
IF "%DEBUG_OPTS%"=="-ea" GOTO :run-tests
SET sys_props=%sys_props% -Drvpf.tests.timeout=-1

:run-tests
SET tests_log=%RVPF_LOG%\tests.log
IF EXIST %tests_log% DEL /Q %tests_log%
IF EXIST %RVPF_TESTS_DATA% RMDIR /S /Q %RVPF_TESTS_DATA%

"%JAVA%" %sys_props% %DEBUG_OPTS% org.rvpf.tests.FrameworkTests %TESTNG_XML% %*
SET error_level=%ERRORLEVEL%
IF NOT EXIST %tests_log% GOTO :exit

TYPE %tests_log% | FIND "FATAL"
TYPE %tests_log% | FIND "ERROR"
TYPE %tests_log% | FIND "WARN"

:exit
EXIT /B %error_level%

REM End.
