@ECHO OFF

REM $Id: wrap.cmd 3519 2017-07-10 21:51:27Z SFB $

REM NOTE: DO NOT install a wrapped service from a SUBST drive.

SETLOCAL EnableDelayedExpansion

IF "%2"=="" GOTO :empty-arg2
SET RVPF_ACTION=%1
SHIFT
GOTO :set-target

:empty-arg2
IF "%1"=="" GOTO :empty-arg1
SET RVPF_ACTION=
GOTO :set-target

:empty-arg1
SET RVPF_ACTION=help

:set-target
SET RVPF_TARGET=%1

IF NOT DEFINED RVPF_ACTION GOTO :action-ok
IF "!RVPF_ACTION!"=="run" GOTO :action-ok
IF "!RVPF_ACTION!"=="install" GOTO :action-ok
IF "!RVPF_ACTION!"=="remove" GOTO :action-ok
IF "!RVPF_ACTION!"=="start" GOTO :action-ok
IF "!RVPF_ACTION!"=="stop" GOTO :action-ok
IF "!RVPF_ACTION!"=="restart" GOTO :action-ok
IF "!RVPF_ACTION!"=="automatic" GOTO :action-ok
IF "!RVPF_ACTION!"=="manual" GOTO :action-ok
ECHO WRAP [run|install|remove|start|stop|automatic|manual] <target>
GOTO :end

:action-ok
SET CLASSPATH=
IF NOT "!RVPF_CORE_BIN!"=="" GOTO :env-set
PUSHD .
CD bin
CALL setup\rvpf-env
IF NOT "!ERRORLEVEL!"=="0" EXIT /B !ERRORLEVEL!
POPD

:env-set
IF EXIST bin/wrap.cmd (
    SET RVPF_WRAP_BIN=!RVPF_BIN!
) ELSE (
    SET RVPF_WRAP_BIN=!RVPF_CORE_BIN!
)

IF NOT EXIST !RVPF_CONFIG!/wrap/!RVPF_TARGET!.cmd (
    ECHO Target '!RVPF_TARGET!' is unknown
    GOTO :end
)

CALL "!RVPF_WRAP_BIN!/wrap"

:end

REM End.
