# $Id: common.sh 3813 2018-11-10 20:44:07Z SFB $

add-java-option -client

set-main-class org.rvpf.jnlp.launcher.Launcher

add-app-parameter http://localhost:10080/service.jnlp

add-to-class-path $RVPF_CONFIG/service/local
add-to-class-path $RVPF_CONFIG/service
add-to-class-path $RVPF_SHARE_JAVA/rvpf-jnlp-launcher.jar

add-to-library-path $RVPF_CORE_LIB

add-system-property javax.net.ssl.trustStore=config/service/local/rvpf.truststore

set-initial-memory 4
set-maximum-memory 64

# End.