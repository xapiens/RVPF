REM $Id: from082.cmd 3191 2016-09-27 14:42:51Z SFB $

CALL "%RVPF_CONFIG%/wrap/common"

SET MAIN_CLASS=org.rvpf.forwarder.ForwarderServiceActivator

SET PARAMETERS=name=From082

SET CLASS_PATH=%CLASS_PATH%;%RVPF_CORE_SHARE_JAVA%/rvpf-forwarder.jar

SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.prefix=from082
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.id=F082

SET LOG_PREFIX=forwarder

SET SERVICE_NAME=RVPFForwarderFrom082
SET SERVICE_DISPLAY_NAME=RVPF Forwarder from 082
SET SERVICE_DESCRIPTION=RVPF Forwarder from 082 Service
SET SERVICE_DEPENDS_ON=TCPIP

REM End.
