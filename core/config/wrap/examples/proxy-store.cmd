REM $Id: proxy-store.cmd 3191 2016-09-27 14:42:51Z SFB $

CALL "%RVPF_CONFIG%/wrap/common"

SET MAIN_CLASS=org.rvpf.store.server.proxy.ProxyStoreServiceActivator

SET CLASS_PATH=%CLASS_PATH%;%RVPF_CORE_SHARE_JAVA%/rvpf-store.jar

SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.prefix=proxy-store
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.id=PrSt

SET LOG_PREFIX=proxy-store

SET SERVICE_NAME=RVPFProxyStore
SET SERVICE_DISPLAY_NAME=RVPF ProxyStore
SET SERVICE_DESCRIPTION=RVPF ProxyStore Service
SET SERVICE_DEPENDS_ON=TCPIP

REM End.
