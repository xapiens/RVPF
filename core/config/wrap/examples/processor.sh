# $Id: processor.sh 3096 2016-07-13 17:00:03Z SFB $

include common

set-main-class org.rvpf.processor.ProcessorServiceActivator

add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-processor.jar

add-system-property rvpf.log.prefix=processor
add-system-property rvpf.log.size=5MB
add-system-property rvpf.log.backups=9
add-system-property rvpf.log.id=Proc

set-initial-memory 16
set-maximum-memory 256

# End.