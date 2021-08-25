#!/bin/bash

# $Id: script.sh 3824 2018-11-17 15:29:27Z SFB $

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

engine=js
if [ -n "$1" ]; then
    set_script $1 .js
    if [ ! -s $script ]; then
        if [ -n "$GROOVY_HOME" ]; then
            [ "$OSTYPE" = "cygwin" ] && GROOVY_HOME="$(cygpath $GROOVY_HOME)"
            engine=groovy
            set_script $1 .groovy
        fi
    fi
    if [ ! -s $script ]; then
        if [ -n "$JYTHON_HOME" ]; then
            [ "$OSTYPE" = "cygwin" ] && JYTHON_HOME="$(cygpath $JYTHON_HOME)"
            engine=jython
            set_script $1 .py
        fi
    fi
    if [ ! -s $script ]; then
        echo "Script '$1' not found"
        exit 1
    fi
    shift
fi

CLASSPATH=$RVPF_CONFIG/script/local:$RVPF_CONFIG/script
CLASSPATH=$CLASSPATH:$RVPF_CONFIG/service/local:$RVPF_CONFIG/service
[ "$engine" = "groovy" ] && CLASSPATH=$CLASSPATH:$GROOVY_HOME/embeddable/*
[ "$engine" = "jython" ] && CLASSPATH=$CLASSPATH:$JYTHON_HOME/jython.jar
CLASSPATH=$CLASSPATH:$RVPF_CORE_SHARE_JAVA/rvpf-http.jar:$RVPF_CORE_SHARE_JAVA/rvpf-store.jar
CLASSPATH=$CLASSPATH:$RVPF_CORE_SHARE_JAVA/rvpf-processor.jar:$RVPF_CORE_SHARE_JAVA/rvpf-forwarder.jar
CLASSPATH=$CLASSPATH:$RVPF_CORE_SHARE_JAVA/rvpf-tools.jar
classpath_dir=$RVPF_CONFIG/script/classpath
shopt -s nullglob
if [ -d $classpath_dir ]; then
    for name in $classpath_dir/*
    do
        link=$(readlink $name)
        [ -n "$link" ] && name=$link
        CLASSPATH=$CLASSPATH:$name
    done
fi
shopt -u nullglob
LIBPATH=$RVPF_LIB

log_prefix=${script##*/}
log_prefix=${log_prefix%.*}

sys_props="-Djavax.net.ssl.trustStore=$RVPF_CONFIG/service/client.truststore"
sys_props="$sys_props -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager"
sys_props="$sys_props -Dlog4j2.disable.jmx=true"
sys_props="$sys_props -Drvpf.log.prefix=$log_prefix"
if [ "$OSTYPE" = "cygwin" ]; then
    CLASSPATH="$(cygpath -mp $CLASSPATH)"
    export PATH="$LIBPATH:$PATH"
elif [ "${OSTYPE%%-*}" = "linux" ]; then
    [ -n "$LD_LIBRARY_PATH" ] && LD_LIBRARY_PATH=$LD_LIBRARY_PATH:
    export LD_LIBRARY_PATH="$LD_LIBRARY_PATH$LIBPATH"
elif [ "${OSTYPE:0:6}" = "darwin" ]; then
    [ -n "$DYLD_LIBRARY_PATH" ] && DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:
    export DYLD_LIBRARY_PATH="$DYLD_LIBRARY_PATH$LIBPATH"
else
    sys_props="$sys_props -Djava.library.path=$LIBPATH"
fi
export CLASSPATH

[ -n "$DEBUG_OPTS" ] || DEBUG_OPTS=-ea
[ "$engine" = "js" ] && exec $JAVA $sys_props $DEBUG_OPTS jdk.nashorn.tools.Shell $script -- $@
[ "$engine" = "groovy" ] && exec $JAVA $sys_props $DEBUG_OPTS groovy.ui.GroovyMain $script $@
[ "$engine" = "jython" ] && exec $JAVA $sys_props $DEBUG_OPTS org.python.util.jython $script $@

# End.
