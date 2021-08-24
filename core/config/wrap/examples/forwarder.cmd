REM $Id: forwarder.cmd 3191 2016-09-27 14:42:51Z SFB $

CALL "%RVPF_CONFIG%/wrap/common"

SET MAIN_CLASS=org.rvpf.forwarder.ForwarderServiceActivator

SET CLASS_PATH=%CLASS_PATH%;%RVPF_CORE_SHARE_JAVA%/rvpf-forwarder.jar

SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.prefix=forwarder
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.id=Forw

SET LOG_PREFIX=forwarder

SET SERVICE_NAME=RVPFForwarder
SET SERVICE_DISPLAY_NAME=RVPF Forwarder
SET SERVICE_DESCRIPTION=RVPF HTTP Forwarder Service
SET SERVICE_DEPENDS_ON=TCPIP

REM End.
