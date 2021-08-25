#!/bin/bash

# $Id: rvpf.sh 3233 2016-10-20 18:07:28Z SFB $

function upper
{
    if [ "${OSTYPE:0:6}" = "darwin" ]; then
        echo "$1" | tr '[:lower:]' '[:upper:]'
    else
        echo "${1^^*}"
    fi
}

pushd .. >/dev/null
if [ -z "$RVPF_HOME" ]; then
    pwd=$PWD
    dirname=${pwd##*/}
    if [ "$(upper $dirname)" = 'RVPF' ]; then
        export RVPF_HOME=$PWD
    else
        cd ..
        dirname=${PWD##*/}
        if [ "$(upper $dirname)" = 'RVPF' ]; then
            export RVPF_HOME=$PWD
            export RVPF_SUB=$pwd
        fi
    fi
    unset dirname pwd
else
    [ "$OSTYPE" = "cygwin" ] && export RVPF_HOME="$(cygpath -a $RVPF_HOME)"
    export RVPF_SUB=$PWD
fi
popd >/dev/null

if [ -z "$JAVA_HOME" -a -n "$RVPF_HOME" ]; then
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
