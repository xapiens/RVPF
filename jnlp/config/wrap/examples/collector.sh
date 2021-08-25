# $Id: collector.sh 2543 2015-02-16 18:56:11Z SFB $

include common

add-app-parameter org.rvpf.forwarder.ForwarderServiceActivator
add-app-parameter name=Collector

add-system-property rvpf.log.prefix=collector
add-system-property rvpf.log.id=Coll

# End.