# $Id: modbus-store.sh 3875 2019-01-24 20:40:00Z SFB $

include common

set-main-class org.rvpf.store.server.pap.PAPStoreServiceActivator

add-app-parameter name=Modbus

add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-store.jar

add-system-property rvpf.log.prefix=modbus-store
add-system-property rvpf.log.size=3MB
add-system-property rvpf.log.backups=3
add-system-property rvpf.log.id=MbSt

# End.