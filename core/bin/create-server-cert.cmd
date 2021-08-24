@ECHO OFF

REM $Id: create-server-cert.cmd 2546 2015-02-18 21:52:05Z SFB $

SETLOCAL

SET SERVER=%1
SET PASSWORD=%2
SET VALIDITY=3652
SET TARGET_DIR=ssl

IF "%SERVER%"=="" GOTO :usage
IF NOT "%PASSWORD%"=="" GOTO :make-dir

:usage
ECHO.
ECHO Usage: run %~n0 ^<server^> ^<password^>
ECHO.
ECHO        Creates the "%TARGET_DIR%" subdirectory if necessary.
ECHO        Creates a new keystore containing a fresh server key.
ECHO        Exports a self-signed certificate for the server.
ECHO        Creates an empty server truststore.
EXIT /B 1

:make-dir
IF EXIST %TARGET_DIR% GOTO :clear-ks
MKDIR %TARGET_DIR%

:clear-ks
SET SERVER_KS=%TARGET_DIR%\%SERVER%.keystore
SET SERVER_TS=%TARGET_DIR%\%SERVER%.truststore
IF EXIST "%SERVER_KS%" DEL "%SERVER_KS%"
IF EXIST "%SERVER_TS%" DEL "%SERVER_TS%"

:make-cert
SET SERVER_CRT=%TARGET_DIR%\%SERVER%.crt
keytool -genkey -keystore "%SERVER_KS%" -storepass "%PASSWORD%" -keypass "%PASSWORD%" -dname "CN=%SERVER%" -alias "%SERVER%" -keyalg RSA -validity %VALIDITY%
IF NOT "%ERRORLEVEL%"=="0" EXIT /B %ERRORLEVEL%

ECHO Server keystore: "%SERVER_KS%"
keytool -list -v -keystore "%SERVER_KS%" -storepass "%PASSWORD%" -alias "%SERVER%"
keytool -export -keystore "%SERVER_KS%" -storepass "%PASSWORD%" -alias "%SERVER%" -rfc > "%SERVER_CRT%"
ECHO ---
ECHO Server certificate: "%SERVER_CRT%"
openssl x509 -in "%SERVER_CRT%" -noout -text -certopt no_sigdump,no_pubkey
keytool -import -keystore "%SERVER_TS%" -storepass "%PASSWORD%" -alias "%SERVER%" -file "%SERVER_CRT%" -noprompt
keytool -delete -keystore "%SERVER_TS%" -storepass "%PASSWORD%" -alias "%SERVER%"

REM End.
