#!/bin/bash

# $Id: retrieve.sh 3189 2016-09-24 23:38:11Z SFB $

shopt -s extglob

if [ -n "$1" ]; then
    confs=$@
else
    confs=core
fi

rvpf_config=$RVPF_CONFIG
rvpf_core_config=$RVPF_CORE_CONFIG
rvpf_lib=$RVPF_SHARE_JAVA
if [ "$OSTYPE" = "cygwin" ]; then
    rvpf_config="$(cygpath -ma $rvpf_config)"
    rvpf_core_config="$(cygpath -ma $rvpf_core_config)"
    rvpf_lib="$(cygpath -ma $rvpf_lib)"
fi

properties="-Dconfig.build.ivy.location=$rvpf_config/build/ivy"
ivy="$rvpf_core_config/build/ivy.jar"
settings="-settings $rvpf_core_config/build/ivysettings.xml -novalidate"
resolve="-ivy $rvpf_config/build/ivy.xml -confs $confs"
retrieve="-retrieve $rvpf_lib/[artifact].[ext]"

exec $JAVA $properties -jar $ivy $settings $resolve $retrieve

# End.
