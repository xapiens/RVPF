@ECHO OFF

REM $Id: setup-root.cmd 4113 2019-08-03 13:36:57Z SFB $

SETLOCAL

CD /D %~dp0

IF NOT EXIST ..\bin MKDIR ..\bin
IF NOT EXIST ..\bin\setup MKDIR ..\bin\setup
IF NOT EXIST ..\config MKDIR ..\config
IF NOT EXIST ..\config\script MKDIR ..\config\script
IF NOT EXIST ..\config\script\local MKDIR ..\config\script\local
IF NOT EXIST ..\config\service MKDIR ..\config\service
IF NOT EXIST ..\config\wrap MKDIR ..\config\wrap
IF NOT EXIST ..\log MKDIR ..\log
IF NOT EXIST ..\platform MKDIR ..\platform
IF NOT EXIST ..\script MKDIR ..\script
IF NOT EXIST ..\tmp MKDIR ..\tmp

IF NOT EXIST ..\run.cmd COPY run.cmd ..\
IF NOT EXIST ..\run COPY run ..\
IF NOT EXIST ..\wrap.cmd COPY wrap.cmd ..\
IF NOT EXIST ..\wrap COPY wrap ..\
IF NOT EXIST ..\bin\setup\rvpf.cmd COPY config\build\rvpf-root.cmd ..\bin\setup\rvpf.cmd
IF NOT EXIST ..\bin\setup\rvpf.sh COPY config\build\rvpf-root.sh ..\bin\setup\rvpf.sh
IF NOT EXIST ..\bin\setup\rvpf-env.cmd COPY config\build\rvpf-env-root.cmd ..\bin\setup\rvpf-env.cmd
IF NOT EXIST ..\bin\setup\rvpf-env.sh COPY config\build\rvpf-env-root.sh ..\bin\setup\rvpf-env.sh
IF NOT EXIST ..\bin\jython.cmd COPY bin\jython.cmd ..\bin\
IF NOT EXIST ..\bin\jython.sh COPY bin\jython.sh ..\bin\
IF NOT EXIST ..\bin\script.cmd COPY bin\script.cmd ..\bin\
IF NOT EXIST ..\config\script\log4j2.xml COPY config\script\log4j2.xml ..\config\script\
IF NOT EXIST ..\config\service\local.properties COPY config\service\local.properties ..\config\service\
IF NOT EXIST ..\config\service\log4j2.xml COPY config\service\log4j2.xml ..\config\service\

REM End.
