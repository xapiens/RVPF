@ECHO OFF

REM $Id: script.cmd 3594 2017-11-27 20:43:33Z SFB $

SETLOCAL

SET script=%1
SET engine=js
IF "%script%"=="" GOTO :set-classpath
SET script=%script%.js
IF EXIST %script% GOTO :set-classpath
SET script=script\%script%
IF EXIST %script% GOTO :set-classpath
SET script=%RVPF_HOME%\core\%script%
IF EXIST %script% GOTO :set-classpath
IF "%GROOVY_HOME%"=="" GOTO :try-jython
SET script=%1
SET engine=groovy
SET script=%script%.groovy
IF EXIST %script% GOTO :set-classpath
SET script=script\%script%
IF EXIST %script% GOTO :set-classpath
SET script=%RVPF_HOME%\core\%script%
IF EXIST %script% GOTO :set-classpath
:try-jython
IF "%JYTHON_HOME%"=="" GOTO :not-found
SET script=%1
SET engine=jython
SET script=%script%.py
IF EXIST %script% GOTO :set-classpath
SET script=script\%script%
IF EXIST %script% GOTO :set-classpath
SET script=%RVPF_HOME%\core\%script%
IF EXIST %script% GOTO :set-classpath
:not-found
ECHO Script %1 not found
EXIT /B 1

:set-classpath
SET CLASSPATH=%RVPF_CONFIG%\script\local;%RVPF_CONFIG%\script
SET CLASSPATH=%CLASSPATH%;%RVPF_CONFIG%\service\local;%RVPF_CONFIG%\service
IF "%engine%"=="groovy" SET CLASSPATH=%CLASSPATH%;%GROOVY_HOME%\embeddable\*
IF "%engine%"=="jython" SET CLASSPATH=%CLASSPATH%;%JYTHON_HOME%\jython.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\rvpf-http.jar;%RVPF_LIB%\rvpf-store.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\rvpf-processor.jar;%RVPF_LIB%\rvpf-forwarder.jar
SET CLASSPATH=%CLASSPATH%;%RVPF_LIB%\rvpf-tools.jar

SET PATH=%RVPF_LIB%;%PATH%

SHIFT
SET sys_props=-Djavax.net.ssl.trustStore=config/service/client.truststore
IF "%DEBUG_OPTS%"=="" SET DEBUG_OPTS=-ea
IF "%engine%"=="js" "%JAVA%" %sys_props% %DEBUG_OPTS% jdk.nashorn.tools.Shell %script% -- %1 %2 %3 %4 %5 %6 %7 %8 %9
IF "%engine%"=="groovy" "%JAVA%" %sys_props% %DEBUG_OPTS% groovy.ui.GroovyMain %script% %1 %2 %3 %4 %5 %6 %7 %8 %9
IF "%engine%"=="jython" "%JAVA%" %sys_props% %DEBUG_OPTS% org.python.util.jython %script% %1 %2 %3 %4 %5 %6 %7 %8 %9

REM End.
