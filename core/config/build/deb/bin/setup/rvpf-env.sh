#!/bin/bash

# $Id: rvpf-env.sh 3831 2018-11-18 14:36:19Z SFB $

export RVPF_BIN=/opt/rvpf/bin
export RVPF_CONFIG=/etc/opt/rvpf
export RVPF_DATA=/srv/opt/rvpf
export RVPF_LIB=/opt/rvpf/lib
export RVPF_LOG=/var/opt/rvpf/log
export RVPF_RUN=/var/opt/rvpf/run
export RVPF_SCRIPT=/opt/rvpf/script
export RVPF_SHARE=/opt/rvpf/share
export RVPF_SHARE_JAVA=/opt/rvpf/share/java
export RVPF_SHARE_SH=/opt/rvpf/share/sh
export RVPF_TMP=/var/opt/rvpf/tmp

export RVPF_CORE_BIN=$RVPF_BIN
export RVPF_CORE_CONFIG=$RVPF_CONFIG
export RVPF_CORE_LIB=$RVPF_LIB
export RVPF_CORE_SCRIPT=$RVPF_SCRIPT
export RVPF_CORE_SHARE=$RVPF_SHARE
export RVPF_CORE_SHARE_JAVA=$RVPF_SHARE_JAVA
export RVPF_CORE_SHARE_SH=$RVPF_SHARE_SH

export RVPF_JDBC_SHARE_JAVA=$RVPF_SHARE_JAVA
export RVPF_PAP_SHARE_JAVA=$RVPF_SHARE_JAVA

[ "$OSTYPE" = "cygwin" ] && export RVPF_LOG="$(cygpath -ma $RVPF_LOG)"

if [ -e $RVPF_CONFIG/JAVA_HOME ]; then
    export JAVA_HOME="$(readlink -f $RVPF_CONFIG/JAVA_HOME)"
fi
if [ -z "$JAVA" ]; then
    if [ -n "$JAVA_HOME" ]; then
        export JAVA=$JAVA_HOME/bin/java
    else
        export JAVA=java
    fi
fi

# End.
