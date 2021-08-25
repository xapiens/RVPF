REM $Id: modbus-store.cmd 3191 2016-09-27 14:42:51Z SFB $

CALL %RVPF_CONFIG%/wrap/common

SET MAIN_CLASS=org.rvpf.store.server.pap.PAPStoreServiceActivator
SET PARAMETERS=name=Modbus

SET CLASS_PATH=%CLASS_PATH%;%RVPF_CORE_SHARE_JAVA%/rvpf-store.jar

SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.prefix=modbus-store
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.size=3MB
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.backups=3
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.id=MBSt

SET LOG_PREFIX=modbus-store

SET SERVICE_NAME=RVPFModbusStore
SET SERVICE_DISPLAY_NAME=RVPF ModbusStore
SET SERVICE_DESCRIPTION=RVPF ModbusStore Service
SET SERVICE_DEPENDS_ON=TCPIP

REM End.
