REM $Id: rlp-server.cmd 3644 2018-07-06 15:14:11Z SFB $

CALL "%RVPF_CONFIG%/wrap/common"

SET MAIN_CLASS=org.rvpf.service.rlp.RLPServiceActivator

SET CLASS_PATH=%CLASS_PATH%;%RVPF_CORE_SHARE_JAVA%/rvpf-service.jar

SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.prefix=rlp
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.id=RLPS

SET LOG_PREFIX=rlp

SET SERVICE_NAME=RVPFRLP
SET SERVICE_DISPLAY_NAME=RVPF RLP
SET SERVICE_DESCRIPTION=RVPF RLP Service
SET SERVICE_DEPENDS_ON=TCPIP

REM End.
