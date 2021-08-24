# $Id: rlp-server.sh 3644 2018-07-06 15:14:11Z SFB $

include common

set-main-class org.rvpf.service.rlp.RLPServiceActivator

add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-service.jar

add-system-property rvpf.log.prefix=rlp
add-system-property rvpf.log.id=RLPS

# End.