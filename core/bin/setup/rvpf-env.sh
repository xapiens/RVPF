#!/bin/bash

# $Id: rvpf-env.sh 3097 2016-07-13 20:12:59Z SFB $

. setup/rvpf.sh

export RVPF_BIN=${RVPF_SUB}/bin
export RVPF_CONFIG=${RVPF_SUB}/config
export RVPF_DATA=${RVPF_SUB}/data
export RVPF_LIB=${RVPF_SUB}/lib
export RVPF_LOG=${RVPF_SUB}/log
export RVPF_RUN=${RVPF_SUB}/pid
export RVPF_SCRIPT=${RVPF_SUB}/script
export RVPF_SHARE=${RVPF_SUB}/lib
export RVPF_SHARE_JAVA=${RVPF_SUB}/lib
export RVPF_SHARE_SH=${RVPF_SUB}/bin
export RVPF_TMP=${RVPF_SUB}/tmp

export RVPF_CORE_BIN=$RVPF_HOME/core/bin
export RVPF_CORE_CONFIG=$RVPF_HOME/core/config
export RVPF_CORE_LIB=$RVPF_HOME/core/lib
export RVPF_CORE_SCRIPT=$RVPF_HOME/core/script
export RVPF_CORE_SHARE=$RVPF_HOME/core/lib
export RVPF_CORE_SHARE_JAVA=$RVPF_HOME/core/lib
export RVPF_CORE_SHARE_SH=$RVPF_HOME/core/bin

[ "$OSTYPE" = "cygwin" ] && export RVPF_LOG="$(cygpath -ma $RVPF_LOG)"

# End.
