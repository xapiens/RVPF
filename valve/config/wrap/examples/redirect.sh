# $Id: redirect.sh 2247 2014-06-14 14:52:27Z SFB $

include common

add-app-parameter name=Redirect

add-system-property rvpf.log.prefix=redirect
add-system-property rvpf.log.id=Redr
add-system-property rvpf.valve.properties=rvpf-redirect.properties

# End.