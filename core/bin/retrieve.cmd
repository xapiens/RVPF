@ECHO OFF

REM $Id: retrieve.cmd 3194 2016-09-28 17:17:14Z SFB $

SETLOCAL

SET confs=core
IF "%1"=="" GOTO :run
SET confs=%1 %2 %3 %4 %5 %6 %7 %8 %9

:run
SET properties=-Dconfig.build.ivy.location=%RVPF_CONFIG%\build\ivy
SET ivy=%RVPF_CORE_CONFIG%\build\ivy.jar
SET settings=-settings %RVPF_CORE_CONFIG%\build\ivysettings.xml -novalidate
SET resolve=-ivy %RVPF_CONFIG%\build\ivy.xml -confs %confs%
SET retrieve=-retrieve %RVPF_LIB%/[artifact].[ext]

"%JAVA%" %properties% -jar %ivy% %settings% %resolve% %retrieve%

REM End.
