#!/bin/bash

# $Id: groovy.sh 3186 2016-09-23 01:07:19Z SFB $

shopt -s extglob

rvpf_config=$RVPF_CONFIG
rvpf_lib=$RVPF_LIB
groovy_home=$GROOVY_HOME
if [ "$OSTYPE" = "cygwin" ]; then
    rvpf_config="$(cygpath $rvpf_config)"
    rvpf_lib="$(cygpath $rvpf_lib)"
    groovy_home="$(cygpath $groovy_home)"
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
    main=GroovyMain
    set_script $1 .groovy
    if [ ! -s $script ]; then
        echo "Script '$1' not found"
        exit 1
    fi
    shift
else
    main=Console
fi

CLASSPATH=$rvpf_config/script/local:$rvpf_config/script:$groovy_home/lib/*
LIBPATH=$rvpf_lib

if [ "$OSTYPE" = "cygwin" ]; then
    RVPF_HOME="$(cygpath -wa $RVPF_HOME)"
    RVPF_CONFIG="$(cygpath -wa $RVPF_CONFIG)"
    RVPF_CORE_CONFIG="$(cygpath -wa $RVPF_CORE_CONFIG)"
    script="$(cygpath -wa $script)"
fi

sys_props="-Drvpf.home=$RVPF_HOME"
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
sys_props="$sys_props -Dgrape.root=$RVPF_HOME/caches"

unset config_script
if [ -n "$RVPF_HOME" -a -f "$RVPF_HOME/config/script/grapeConfig.xml" ]; then
    config_script="$RVPF_HOME/config/script"
elif [ -f "$RVPF_CONFIG/script/grapeConfig.xml" ]; then
    config_script="$RVPF_CONFIG/script"
elif [ -f "$RVPF_CORE_CONFIG/script/grapeConfig.xml" ]; then
    config_script="$RVPF_CORE_CONFIG/script"
fi
[ -n "$config_script" ] && sys_props="$sys_props -Dgrape.config=$config_script/grapeConfig.xml"

[ -n "$DEBUG_OPTS" ] || DEBUG_OPTS=-ea
exec $JAVA $DEBUG_OPTS $sys_props groovy.ui.$main $script $@

# End.
