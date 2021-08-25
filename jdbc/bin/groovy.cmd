@ECHO OFF

REM $Id: groovy.cmd 3194 2016-09-28 17:17:14Z SFB $

SETLOCAL

SET CLASSPATH=%RVPF_CONFIG%\script\local;%RVPF_CONFIG%\script;%GROOVY_HOME%\lib\*
SET PATH=%RVPF_CORE_LIB%;%PATH%

SET main=Console
SET script=%1
IF "%script%"=="" GOTO :config-grape
SET main=GroovyMain
SET script=%script%.groovy
IF EXIST %script% GOTO :config-grape
SET script=script\%script%
IF EXIST %script% GOTO :config-grape
SET script=%RVPF_HOME%\core\%script%
IF EXIST %script% GOTO :config-grape
ECHO Script %1 not found
EXIT /B 1

:config-grape
SET sys_props=-Drvpf.home=%RVPF_HOME% -Dgrape.root=%RVPF_HOME%\caches
SET config_script=%RVPF_HOME%\config\script
IF EXIST %config_script%\grapeConfig.xml GOTO :set-grape-config
SET config_script=config\script
IF EXIST %config_script%\grapeConfig.xml GOTO :set-grape-config
SET config_script=%RVPF_HOME%\core\config\script
IF EXIST %config_script%\grapeConfig.xml GOTO :set-grape-config
GOTO :exec-groovy

:set-grape-config
SET sys_props=%sys_props% -Dgrape.config=%config_script%\grapeConfig.xml

:exec-groovy
SHIFT
IF "%DEBUG_OPTS%"=="" SET DEBUG_OPTS=-ea
"%JAVA%" %DEBUG_OPTS% %sys_props% groovy.ui.%main% %script% %1 %2 %3 %4 %5 %6 %7 %8 %9

REM End.
