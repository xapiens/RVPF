@ECHO OFF

REM $Id: setup-root.cmd 3512 2017-07-09 00:46:16Z SFB $

SETLOCAL

CD /D %~dp0

IF NOT EXIST ..\bin MKDIR ..\bin
IF NOT EXIST ..\bin\setup MKDIR ..\bin\setup
IF NOT EXIST ..\config MKDIR ..\config
IF NOT EXIST ..\config\service MKDIR ..\config\service
IF NOT EXIST ..\config\wrap MKDIR ..\config\wrap
IF NOT EXIST ..\log MKDIR ..\log
IF NOT EXIST ..\tmp MKDIR ..\tmp

IF NOT EXIST ..\run.cmd COPY run.cmd ..\
IF NOT EXIST ..\run COPY run ..\
IF NOT EXIST ..\wrap.cmd COPY wrap.cmd ..\
IF NOT EXIST ..\wrap COPY wrap ..\
IF NOT EXIST ..\bin\rvpf-service.exe COPY bin\rvpf-service.exe ..\bin\
IF NOT EXIST ..\bin\wrap.cmd COPY bin\wrap.cmd ..\bin\
IF NOT EXIST ..\bin\wrap.sh COPY bin\wrap.sh ..\bin\
IF NOT EXIST ..\bin\setup\rvpf.cmd COPY bin\setup\rvpf.cmd ..\bin\setup\
IF NOT EXIST ..\bin\setup\rvpf.sh COPY bin\setup\rvpf.sh ..\bin\setup\
IF NOT EXIST ..\config\service\log4j2.xml COPY config\service\log4j2.xml ..\config\service\

REM End.
