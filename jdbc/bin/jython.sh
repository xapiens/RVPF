#!/bin/bash

# $Id: jython.sh 3187 2016-09-24 14:38:52Z SFB $

shopt -s extglob

rvpf_script=$RVPF_SCRIPT
rvpf_core_script=$RVPF_CORE_SCRIPT
if [ "$OSTYPE" = "cygwin" ]; then
    rvpf_script="$(cygpath -ma $rvpf_script)"
    rvpf_core_script="$(cygpath -ma $rvpf_core_script)"
    JYTHON_HOME="$(cygpath $JYTHON_HOME)"
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
    set_script $1 .py
    if [ ! -s $script ]; then
        echo "Script '$1' not found"
        exit 1
    fi
    shift
fi

sys_props="-Drvpf.uuid.mac"

CLASSPATH=$RVPF_CONFIG/script/local:$RVPF_CONFIG/script:$JYTHON_HOME/jython.jar
CLASSPATH=$CLASSPATH:$RVPF_SHARE_JAVA/rvpf-jdbc.jar
CLASSPATH=$CLASSPATH:$RVPF_CORE_SHARE_JAVA/rvpf-service.jar
LIBPATH=$RVPF_LIB:$RVPF_CORE_LIB
unset sys_props
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
exec $JAVA $DEBUG_OPTS $sys_props org.python.util.jython $script $@

# End.
