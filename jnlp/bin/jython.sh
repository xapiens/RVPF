#!/bin/bash

# $Id: jython.sh 3097 2016-07-13 20:12:59Z SFB $

shopt -s extglob

rvpf_config=$RVPF_CONFIG
rvpf_lib=$RVPF_SHARE_JAVA
jython_home=$JYTHON_HOME
if [ "$OSTYPE" = "cygwin" ]; then
    rvpf_config="`cygpath $rvpf_config`"
    rvpf_lib="`cygpath $rvpf_lib`"
    jython_home="`cygpath $jython_home`"
fi

function set_script
{
    script=$1
    [ "${script%$2}" = "$script" ] && script=$script$2
    if [ "${script#*/}" = "$script" ]; then
        local script_path=$RVPF_SCRIPT/$script
        [ ! -s $script_path ] && script_path=$RVPF_CORE_SCRIPT/$script
        script=$script_path
    fi
}

script=$1
if [ -n "$script" ]; then
    set_script $1 .py
    if [ ! -s $script ]; then
        echo "Script '$1' not found"
        exit 1
    fi
    shift
fi

sys_props="-Drvpf.uuid.mac"

CLASSPATH=$rvpf_config/script/local:$rvpf_config/script:$jython_home/jython.jar
CLASSPATH=$CLASSPATH:$rvpf_lib/rvpf-jnlp-loader.jar
CLASSPATH=$CLASSPATH:$rvpf_lib/log4j.jar

[ "$OSTYPE" = "cygwin" ] && CLASSPATH=`cygpath -mp $CLASSPATH`

PATH=$rvpf_lib:$PATH

export CLASSPATH PATH

[ -n "$DEBUG_OPTS" ] || DEBUG_OPTS=-ea
exec $JAVA $DEBUG_OPTS $sys_props org.python.util.jython $script $@

# End.
