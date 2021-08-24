# $Id: alerter.sh 3096 2016-07-13 17:00:03Z SFB $

include common

set-main-class org.rvpf.http.HTTPServerActivator

add-app-parameter name=Alerter

add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-http.jar
add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-processor.jar
add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-store.jar

add-system-property rvpf.log.prefix=alerter
add-system-property rvpf.log.id=Aler

# End.