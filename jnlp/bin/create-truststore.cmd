@ECHO ON

REM $Id: create-truststore.cmd 3051 2016-06-05 13:12:28Z SFB $

SETLOCAL

SET KEYSTORE=%HOMEPATH%\.rvpf.keystore
SET PASSWORD=rvpf-password
SET ALIAS=rvpf
SET TRUSTSTORE=config\service\local\rvpf.truststore

IF EXIST %TRUSTSTORE% DEL %TRUSTSTORE%

keytool -export -keystore "%KEYSTORE%" -alias "%ALIAS%" -storepass "%PASSWORD%" -rfc >tmp/crt.tmp
keytool -import -keystore "%TRUSTSTORE%" -alias "%ALIAS%" -storepass "%PASSWORD%" -file tmp/crt.tmp

REM End.
