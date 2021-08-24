#!/bin/bash

# $Id: h2.sh 3097 2016-07-13 20:12:59Z SFB $

shopt -s extglob

if [ -n "$1" ]; then
    H2TOOL=$1
    shift
else
    H2TOOL=Console
fi

CLASSPATH=$RVPF_CORE_SHARE_JAVA/h2.jar

[ "$OSTYPE" = "cygwin" ] && CLASSPATH="$(cygpath -mp $CLASSPATH)"

export CLASSPATH

[ -n "$DEBUG_OPTS" ] || DEBUG_OPTS=-ea
exec $JAVA $DEBUG_OPTS org.h2.tools.$H2TOOL $@

# End.
