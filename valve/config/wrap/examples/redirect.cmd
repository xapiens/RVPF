REM $Id: redirect.cmd 3077 2016-06-25 18:05:46Z SFB $

CALL %RVPF_CONFIG%/wrap/common

SET PARAMETERS=name=Redirect

SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.prefix=redirect
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.id=Redr
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.valve.properties=rvpf-redirect.properties

SET LOG_PREFIX=redirect

SET SERVICE_NAME=RVPFRedirect
SET SERVICE_DISPLAY_NAME=RVPF Redirect
SET SERVICE_DESCRIPTION=RVPF Redirect Service
SET SERVICE_DEPENDS_ON=TCPIP

REM End.
