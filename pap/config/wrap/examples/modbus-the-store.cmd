REM $Id: modbus-the-store.cmd 4115 2019-08-04 14:17:56Z SFB $

CALL %RVPF_CONFIG%/wrap/common

SET MAIN_CLASS=org.rvpf.store.server.the.TheStoreServiceActivator
SET PARAMETERS=name=Modbus

SET CLASS_PATH=%CLASS_PATH%;%RVPF_CORE_SHARE_JAVA%/rvpf-store.jar

SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.prefix=modbus-the-store
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.size=3MB
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.backups=3
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.id=MbTS

SET LOG_PREFIX=modbus-store

SET SERVICE_NAME=RVPFModbusTheStore
SET SERVICE_DISPLAY_NAME=RVPF ModbusTheStore
SET SERVICE_DESCRIPTION=RVPF ModbusTheStore Service
SET SERVICE_DEPENDS_ON=TCPIP

REM End.
