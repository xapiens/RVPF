# $Id: server.sh 3813 2018-11-10 20:44:07Z SFB $

add-java-option -client

set-main-class org.rvpf.http.HTTPServerActivator

add-to-class-path $RVPF_CONFIG/service/local
add-to-class-path $RVPF_CONFIG/service
add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-http.jar

add-system-property rvpf.log.prefix=server
add-system-property rvpf.log.id=Serv

set-initial-memory 4
set-maximum-memory 64

# End.