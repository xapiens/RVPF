#!/bin/bash

# $Id: stopper.sh 3097 2016-07-13 20:12:59Z SFB $

shopt -s extglob

CLASSPATH=$RVPF_CONFIG/service/local:$RVPF_CONFIG/service:$RVPF_CORE_SHARE_JAVA/rvpf-service.jar

[ "$OSTYPE" = "cygwin" ] && CLASSPATH=`cygpath -mp $CLASSPATH`

export CLASSPATH

[ -n "$DEBUG_OPTS" ] || DEBUG_OPTS=-ea
exec $JAVA $DEBUG_OPTS -Drvpf.log.id=Stop -Drvpf.log.prefix=stopper org.rvpf.service.Stopper $@

# End.
