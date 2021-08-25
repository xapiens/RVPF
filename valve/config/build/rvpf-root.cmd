@ECHO OFF

REM $Id: rvpf-root.cmd 3079 2016-06-27 19:12:15Z SFB $

SETLOCAL EnableDelayedExpansion

IF "%RVPF_HOME%"=="" (
    PUSHD ..
    SET pwd=!CD!
    POPD
)

IF "%JAVA_HOME%"=="" (
    IF EXIST !RVPF_HOME!\bin\setup\java.cmd (
        CALL !RVPF_HOME!\bin\setup\java.cmd
    )
)
IF "!JAVA_HOME!"=="" (
    SET JAVA=JAVA
) else (
    SET JAVA=%JAVA_HOME%/bin/java
)

ENDLOCAL & (
    SET RVPF_HOME=%RVPF_HOME%
    SET RVPF_SUB=%RVPF_SUB%
    SET JAVA=%JAVA%
)

EXIT /B 0

REM End.
