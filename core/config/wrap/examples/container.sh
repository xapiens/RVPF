# $Id: container.sh 3096 2016-07-13 17:00:03Z SFB $

include common

set-main-class org.rvpf.container.ContainerServiceActivator

add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-store.jar
add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-processor.jar
add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-http.jar
add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-forwarder.jar

add-system-property rvpf.log.prefix=container
add-system-property rvpf.log.size=5MB
add-system-property rvpf.log.backups=25
add-system-property rvpf.log.id=Cont

set-initial-memory 16
set-maximum-memory 256

# End.