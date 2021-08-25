# $Id: the-store.sh 4115 2019-08-04 14:17:56Z SFB $

add-java-option -client

add-to-class-path $RVPF_CONFIG/service/local
add-to-class-path $RVPF_CONFIG/service

add-to-library-path $RVPF_CORE_LIB

set-initial-memory 4
set-maximum-memory 64

set-main-class org.rvpf.store.server.the.TheStoreServiceActivator

add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-store.jar

add-system-property rvpf.log.prefix=the-store
add-system-property rvpf.log.size=3MB
add-system-property rvpf.log.backups=3
add-system-property rvpf.log.id=ThSt

# End.