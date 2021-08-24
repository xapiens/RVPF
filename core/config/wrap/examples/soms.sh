# $Id: soms.sh 3096 2016-07-13 17:00:03Z SFB $

include common

set-main-class org.rvpf.som.SOMContainerServiceActivator

add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-service.jar

add-system-property rvpf.log.prefix=soms
add-system-property rvpf.log.id=SomS

# End.