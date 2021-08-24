# $Id: http.sh 3096 2016-07-13 17:00:03Z SFB $

include common

set-main-class org.rvpf.http.HTTPServerActivator

add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-http.jar

add-system-property rvpf.log.prefix=http
add-system-property rvpf.log.id=Http

be-nice

# End.