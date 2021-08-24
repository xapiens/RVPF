# $Id: forwarder.sh 3096 2016-07-13 17:00:03Z SFB $

include common

set-main-class org.rvpf.forwarder.ForwarderServiceActivator

add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-forwarder.jar

add-system-property rvpf.log.prefix=forwarder
add-system-property rvpf.log.id=Forw

# End.