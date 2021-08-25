REM $Id: modbus-forwarder.cmd 3191 2016-09-27 14:42:51Z SFB $

CALL %RVPF_CONFIG%/wrap/common

SET MAIN_CLASS=org.rvpf.forwarder.ForwarderServiceActivator
SET PARAMETERS=name=Modbus

SET CLASS_PATH=%CLASS_PATH%;%RVPF_CORE_SHARE_JAVA%/rvpf-forwarder.jar

SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.prefix=modbus-forwarder
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.id=MBFw

SET LOG_PREFIX=modbus-forwarder

SET SERVICE_NAME=RVPFModbusForwarder
SET SERVICE_DISPLAY_NAME=RVPF ModbusForwarder
SET SERVICE_DESCRIPTION=RVPF ModbusForwarder Service
SET SERVICE_DEPENDS_ON=TCPIP

REM End.
