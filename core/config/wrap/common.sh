# $Id: common.sh 3813 2018-11-10 20:44:07Z SFB $

add-java-option -client

add-to-class-path $RVPF_CONFIG/service/local
add-to-class-path $RVPF_CONFIG/service

add-to-library-path $RVPF_CORE_LIB

set-initial-memory 4
set-maximum-memory 64

set-user-language en

# End.