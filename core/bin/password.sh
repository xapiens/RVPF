#!/bin/bash

# $Id: password.sh 3097 2016-07-13 20:12:59Z SFB $

shopt -s extglob

CLASSPATH=$RVPF_CORE_SHARE_JAVA/jetty-servlet.jar
unset sys_props
if [ "$OSTYPE" = "cygwin" ]; then
    CLASSPATH="`cygpath -mp $CLASSPATH`"
fi
export CLASSPATH

exec $JAVA org.eclipse.jetty.util.security.Password $@

# End.
