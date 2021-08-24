@ECHO OFF

REM $Id: create-client-cert.cmd 2546 2015-02-18 21:52:05Z SFB $

SETLOCAL

SET CLIENT=%1
SET SERVER=%2
SET CLIENT_PASSWORD=
IF NOT "%4"=="" SET CLIENT_PASSWORD=%3
SET SERVER_PASSWORD=%3
IF NOT "%4"=="" SET SERVER_PASSWORD=%4
SET VALIDITY=3652
SET TARGET_DIR=ssl

IF "%CLIENT%"=="" GOTO :usage
IF NOT "%SERVER%"=="" GOTO :make-cert

:usage
ECHO Usage: %~n0 ^<client^> ^<server^> [^<client-password^>] ^<server-password^>
ECHO Creates a new key and self-signed certificate for the specified client.
ECHO Adds or replace the certificate for this client in the server's truststore.
EXIT /B 1

:make-cert
SET CLIENT_CNF=%TARGET_DIR%\%CLIENT%.cnf
SET CLIENT_KEY=%TARGET_DIR%\%CLIENT%.key
SET CLIENT_CRT=%TARGET_DIR%\%CLIENT%.crt
SET CLIENT_PEM=%TARGET_DIR%\%CLIENT%.pem
SET CLIENT_P12=%TARGET_DIR%\%CLIENT%.p12
SET CLIENT_KS=%TARGET_DIR%\%CLIENT%.keystore
SET CLIENT_TS=%TARGET_DIR%\%CLIENT%.truststore
SET SERVER_CRT=%TARGET_DIR%\%SERVER%.crt
SET SERVER_TS=%TARGET_DIR%\%SERVER%.truststore
IF EXIST "%CLIENT_CNF%" DEL "%CLIENT_CNF%"
IF EXIST "%CLIENT_KEY%" DEL "%CLIENT_KEY%"
IF EXIST "%CLIENT_CRT%" DEL "%CLIENT_CRT%"
IF EXIST "%CLIENT_PEM%" DEL "%CLIENT_PEM%"
IF EXIST "%CLIENT_P12%" DEL "%CLIENT_P12%"
IF EXIST "%CLIENT_KS%" DEL "%CLIENT_KS%"
IF EXIST "%CLIENT_TS%" DEL "%CLIENT_TS%"

IF NOT "%CLIENT_PASSWORD%"=="" GOTO :make-ks
ECHO RANDFILE=$ENV::HOME/.rnd > "%CLIENT_CNF%"
ECHO [req] >> "%CLIENT_CNF%"
ECHO prompt=no >> "%CLIENT_CNF%"
ECHO distinguished_name=req_distinguished_name >> "%CLIENT_CNF%"
ECHO [req_distinguished_name] >> "%CLIENT_CNF%"
ECHO CN=%CLIENT% >> "%CLIENT_CNF%"
openssl req -config "%CLIENT_CNF%" -x509 -newkey rsa:1024 -nodes -x509 -days %VALIDITY% -keyout "%CLIENT_KEY%" -out "%CLIENT_CRT%" 2> NUL:
DEL "%CLIENT_CNF%"
ECHO Client key: "%CLIENT_KEY%"
ECHO Client certificate: "%CLIENT_CRT%"
openssl x509 -in "%CLIENT_CRT%" -noout -text -certopt no_sigdump,no_pubkey
COPY "%CLIENT_KEY%"+"%CLIENT_CRT%" "%CLIENT_PEM%"
openssl pkcs12 -export -in "%CLIENT_PEM%" -out "%CLIENT_P12%" -name "%CLIENT%" -password pass:
ECHO ---
ECHO Client PEM: "%CLIENT_PEM%"
ECHO Client PKCS12: "%CLIENT_P12%"
openssl pkcs12 -in "%CLIENT_P12%" -info -noout -password pass:
GOTO :update-ts

:make-ks
keytool -genkey -keystore "%CLIENT_KS%" -storepass "%CLIENT_PASSWORD%" -keypass "%CLIENT_PASSWORD%" -dname "CN=%CLIENT%" -alias "%CLIENT%" -keyalg RSA -validity %VALIDITY%
IF NOT "%ERRORLEVEL%"=="0" EXIT /B %ERRORLEVEL%
ECHO Client keystore: "%CLIENT_KS%"
keytool -list -v -keystore "%CLIENT_KS%" -storepass "%CLIENT_PASSWORD%" -alias "%CLIENT%"
keytool -export -keystore "%CLIENT_KS%" -storepass "%CLIENT_PASSWORD%" -alias "%CLIENT%" -rfc > "%CLIENT_CRT%"
keytool -import -keystore "%CLIENT_TS%" -storepass "%CLIENT_PASSWORD%" -alias "%SERVER%" -file "%SERVER_CRT%" -noprompt
ECHO ---
ECHO Client truststore: "%CLIENT_TS%"
keytool -list -v -keystore "%CLIENT_TS%" -storepass "%CLIENT_PASSWORD%" -alias "%SERVER%"

:update-ts
keytool -delete -keystore "%SERVER_TS%" -storepass "%SERVER_PASSWORD%" -alias "%CLIENT%" > NUL:
keytool -import -keystore "%SERVER_TS%" -storepass "%SERVER_PASSWORD%" -alias "%CLIENT%" -file "%CLIENT_CRT%" -noprompt
ECHO ---
ECHO Server truststore: "%SERVER_TS%"
keytool -list -v -keystore "%SERVER_TS%" -storepass "%SERVER_PASSWORD%" -alias "%CLIENT%"

REM End.
