REM $Id: from080.cmd 3191 2016-09-27 14:42:51Z SFB $

CALL "%RVPF_CONFIG%/wrap/common"

SET MAIN_CLASS=org.rvpf.forwarder.ForwarderServiceActivator

SET PARAMETERS=name=From080

SET CLASS_PATH=%CLASS_PATH%;%RVPF_CORE_SHARE_JAVA%/rvpf-forwarder.jar

SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.prefix=from080
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.id=F080

SET LOG_PREFIX=forwarder

SET SERVICE_NAME=RVPFForwarderFrom080
SET SERVICE_DISPLAY_NAME=RVPF Forwarder from 080
SET SERVICE_DESCRIPTION=RVPF Forwarder from 080 Service
SET SERVICE_DEPENDS_ON=TCPIP

REM End.
