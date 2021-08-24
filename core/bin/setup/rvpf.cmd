@ECHO OFF

REM $Id: rvpf.cmd 3233 2016-10-20 18:07:28Z SFB $

SETLOCAL EnableDelayedExpansion

PUSHD ..
IF "%RVPF_HOME%"=="" (
    SET pwd=!CD!
    FOR %%I IN ("!CD!") DO SET dirname=%%~nI
    IF /I "!dirname!"=="RVPF" (
        SET RVPF_HOME=!CD!
    ) ELSE (
        CD ..
        FOR %%I IN ("!CD!") DO SET dirname=%%~nI
        IF /I "!dirname!"=="RVPF" (
            SET RVPF_HOME=!CD!
            SET RVPF_SUB=!pwd!
        )
    )
) ELSE (
    SET RVPF_SUB=!CD!
)
POPD

IF "%JAVA_HOME%"=="" (
    IF NOT "!RVPF_HOME!"=="" (
        IF EXIST !RVPF_HOME!\bin\setup\java.cmd (
            CALL !RVPF_HOME!\bin\setup\java.cmd
        )
    )
)
IF "!JAVA!"=="" (
    IF "!JAVA_HOME!"=="" (
        SET JAVA=JAVA
    ) ELSE (
        SET JAVA=%JAVA_HOME%/bin/java
    )
)

ENDLOCAL & (
    SET RVPF_HOME=%RVPF_HOME%
    SET RVPF_SUB=%RVPF_SUB%
    SET JAVA=%JAVA%
)

EXIT /B 0

REM End.
