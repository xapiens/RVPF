# $Id: proxy-store.sh 3096 2016-07-13 17:00:03Z SFB $

include common

set-main-class org.rvpf.store.server.proxy.ProxyStoreServiceActivator

add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-store.jar

add-system-property rvpf.log.prefix=proxy-store
add-system-property rvpf.log.id=PrSt

# End.