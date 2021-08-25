# $Id: common.sh 3813 2018-11-10 20:44:07Z SFB $

add-java-option -client

add-to-class-path $RVPF_CONFIG/service/local
add-to-class-path $RVPF_CONFIG/service
add-to-class-path $RVPF_SHARE_JAVA/rvpf-valve.jar

add-to-library-path $RVPF_LIB

set-initial-memory 4
set-maximum-memory 64

set-main-class org.rvpf.valve.ValveServiceActivator

# End.