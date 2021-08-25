REM $Id: datalogger.cmd 3392 2017-03-15 19:21:39Z SFB $

CALL %RVPF_CONFIG%/wrap/common

SET MAIN_CLASS=org.rvpf.service.pap.datalogger.DataloggerServiceActivator

SET CLASS_PATH=%CLASS_PATH%;%RVPF_CORE_SHARE_JAVA%/rvpf-service.jar

SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.prefix=datalogger
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.size=3MB
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.backups=3
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.id=Dtlg

SET LOG_PREFIX=datalogger

SET SERVICE_NAME=RVPFDatalogger
SET SERVICE_DISPLAY_NAME=RVPF Datalogger
SET SERVICE_DESCRIPTION=RVPF Datalogger Service
SET SERVICE_DEPENDS_ON=TCPIP

REM End.
