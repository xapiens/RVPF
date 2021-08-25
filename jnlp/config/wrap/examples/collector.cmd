REM $Id: collector.cmd 3077 2016-06-25 18:05:46Z SFB $

CALL "%RVPF_CONFIG%/wrap/common"

SET PARAMETERS=%PARAMETERS%;org.rvpf.forwarder.ForwarderServiceActivator;name=Collector

SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.prefix=collector
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.id=Coll

SET LOG_PREFIX=collector

SET SERVICE_NAME=RVPFCollector
SET SERVICE_DISPLAY_NAME=RVPF Collector
SET SERVICE_DESCRIPTION=RVPF HTTP Collector Service
SET SERVICE_DEPENDS_ON=TCPIP

REM End.
