# $Id: datalogger.sh 3392 2017-03-15 19:21:39Z SFB $

include common

set-main-class org.rvpf.service.pap.datalogger.DataloggerServiceActivator

add-to-class-path $RVPF_CORE_SHARE_JAVA/rvpf-service.jar

add-system-property rvpf.log.prefix=datalogger
add-system-property rvpf.log.size=3MB
add-system-property rvpf.log.backups=3
add-system-property rvpf.log.id=Dtlg

# End.