REM $Id: clock.cmd 3644 2018-07-06 15:14:11Z SFB $

CALL "%RVPF_CONFIG%/wrap/common"

SET MAIN_CLASS=org.rvpf.clock.ClockServiceActivator

SET CLASS_PATH=%CLASS_PATH%;%RVPF_CORE_SHARE_JAVA%/rvpf-service.jar

SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.prefix=clock
SET JVM_OPTIONS=%JVM_OPTIONS%;-Drvpf.log.id=Clck

SET LOG_PREFIX=clock

SET SERVICE_NAME=RVPFClock
SET SERVICE_DISPLAY_NAME=RVPF Clock
SET SERVICE_DESCRIPTION=RVPF Clock Service
SET SERVICE_DEPENDS_ON=TCPIP

REM End.
