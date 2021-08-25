REM $Id: common.cmd 3191 2016-09-27 14:42:51Z SFB $

SET MAIN_CLASS=org.rvpf.jnlp.launcher.Launcher

SET PARAMETERS=http://localhost:10080/service.jnlp

SET CLASS_PATH=%RVPF_CONFIG%/service/local;%RVPF_CONFIG%/service
SET CLASS_PATH=%CLASS_PATH%;%RVPF_SHARE_JAVA%/rvpf-jnlp-launcher.jar

SET LIBRARY_PATH=%RVPF_CORE_LIB%

SET JVM_OPTIONS=%JVM_OPTIONS%;-Djavax.net.ssl.trustStore=config/service/local/rvpf.truststore

SET INITIAL_MEMORY=4
SET MAXIMUM_MEMORY=64

REM End.
