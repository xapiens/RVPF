REM $Id: version.cmd 3191 2016-09-27 14:42:51Z SFB $

CALL "%RVPF_CONFIG%/wrap/common"

SET MAIN_CLASS=org.rvpf.document.version.DocumentVersionControlActivator

SET CLASS_PATH=%CLASS_PATH%;%RVPF_CORE_SHARE_JAVA%/rvpf-service.jar

SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.prefix=version
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.id=Vers

SET LOG_PREFIX=version

SET SERVICE_NAME=RVPFVersion
SET SERVICE_DISPLAY_NAME=RVPF Version
SET SERVICE_DESCRIPTION=RVPF HTTP Version Service
SET SERVICE_DEPENDS_ON=TCPIP

REM End.
