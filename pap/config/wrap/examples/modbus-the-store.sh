# $Id: modbus-the-store.sh 4115 2019-08-04 14:17:56Z SFB $

include common

set-main-class org.rvpf.store.server.the.TheStoreServiceActivator

add-app-parameter name=Modbus

#use-authbind

add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-store.jar

add-system-property rvpf.log.prefix=modbus-the-store
add-system-property rvpf.log.size=3MB
add-system-property rvpf.log.backups=3
add-system-property rvpf.log.id=MbTS

# End.