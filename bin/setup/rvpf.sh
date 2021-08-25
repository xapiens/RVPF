#!/bin/bash

# $Id: rvpf-root.sh 3078 2016-06-26 20:30:42Z SFB $

if [ -z "$RVPF_HOME" ]; then
    pushd .. >/dev/null
    export RVPF_HOME=$PWD
    popd >/dev/null
fi

if [ -z "$JAVA_HOME" ]; then
    if [ -e $RVPF_HOME/bin/setup/java.sh ]; then
        . $RVPF_HOME/bin/setup/java.sh
    elif [ -e $RVPF_HOME/bin/setup/JAVA_HOME ]; then
        export JAVA_HOME="$(readlink -f $RVPF_HOME/bin/setup/JAVA_HOME)"
    fi
fi
if [ -z "$JAVA" ]; then
    if [ -n "$JAVA_HOME" ]; then
        export JAVA=$JAVA_HOME/bin/java
    else
        export JAVA=java
    fi
fi

unset CLASSPATH

# End.
