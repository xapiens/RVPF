#!/bin/bash

# $Id: js.sh 3097 2016-07-13 20:12:59Z SFB $

shopt -s extglob

rvpf_script=$RVPF_SCRIPT
rvpf_core_script=$RVPF_CORE_SCRIPT
if [ "$OSTYPE" = "cygwin" ]; then
    rvpf_script="$(cygpath -ma $rvpf_script)"
    rvpf_core_script="$(cygpath -ma $rvpf_core_script)"
fi

function set_script
{
    script=$1
    [ "${script%$2}" = "$script" ] && script=$script$2
    if [ "${script#*/}" = "$script" ]; then
        local script_path=$rvpf_script/$script
        [ ! -s $script_path ] && script_path=$rvpf_core_script/$script
        script=$script_path
    fi
}

script=$1
if [ -n "$script" ]; then
    set_script $1 .js
    if [ ! -s $script ]; then
        echo "Script '$1' not found"
        exit 1
    fi
    shift
fi

CLASSPATH=$RVPF_CONFIG/script/local:$RVPF_CONFIG/script:$RVPF_SHARE_JAVA/rvpf-valve.jar
CLASSPATH=$CLASSPATH:$RVPF_SHARE_JAVA/log4j.jar

[ "$OSTYPE" = "cygwin" ] && CLASSPATH=$(cygpath -mp $CLASSPATH)

PATH=$RVPF_LIB:$PATH

export CLASSPATH PATH

[ -n "$DEBUG_OPTS" ] || DEBUG_OPTS=-ea
exec $JAVA $DEBUG_OPTS $sys_props jdk.nashorn.tools.Shell $script -- $@

# End.
