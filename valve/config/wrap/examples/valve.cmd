REM $Id: valve.cmd 3077 2016-06-25 18:05:46Z SFB $

CALL %RVPF_CONFIG%/wrap/common

SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.prefix=valve
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.id=Valv

SET LOG_PREFIX=valve

SET SERVICE_NAME=RVPFValve
SET SERVICE_DISPLAY_NAME=RVPF Valve
SET SERVICE_DESCRIPTION=RVPF Valve Service
SET SERVICE_DEPENDS_ON=TCPIP

REM End.
