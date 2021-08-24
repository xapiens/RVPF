REM $Id: the-store.cmd 4115 2019-08-04 14:17:56Z SFB $

CALL "%RVPF_CONFIG%/wrap/common"

SET MAIN_CLASS=org.rvpf.store.server.the.TheStoreServiceActivator

SET CLASS_PATH=%CLASS_PATH%;%RVPF_CORE_SHARE_JAVA%/rvpf-store.jar

SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.prefix=the-store
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.size=3MB
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.backups=3
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.id=ThSt

SET LOG_PREFIX=the-store

SET SERVICE_NAME=RVPFTheStore
SET SERVICE_DISPLAY_NAME=RVPF TheStore
SET SERVICE_DESCRIPTION=RVPF TheStore Service
SET SERVICE_DEPENDS_ON=TCPIP

REM End.
