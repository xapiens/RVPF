#!/bin/bash

# $Id: rvpf-env-root.sh 3831 2018-11-18 14:36:19Z SFB $

. setup/rvpf.sh

export RVPF_BIN=$RVPF_HOME/bin
export RVPF_CONFIG=$RVPF_HOME/config
export RVPF_DATA=$RVPF_HOME/data
export RVPF_LIB=$RVPF_HOME/lib
export RVPF_LOG=$RVPF_HOME/log
export RVPF_RUN=$RVPF_HOME/pid
export RVPF_SCRIPT=$RVPF_HOME/script
export RVPF_SHARE=${RVPF_HOME}/lib
export RVPF_SHARE_JAVA=${RVPF_HOME}/lib
export RVPF_SHARE_SH=${RVPF_HOME}/bin
export RVPF_TMP=${RVPF_HOME}/tmp

export RVPF_CORE_BIN=$RVPF_HOME/core/bin
export RVPF_CORE_CONFIG=$RVPF_HOME/core/config
export RVPF_CORE_LIB=$RVPF_HOME/core/lib
export RVPF_CORE_SCRIPT=$RVPF_HOME/core/script
export RVPF_CORE_SHARE=$RVPF_HOME/core/lib
export RVPF_CORE_SHARE_JAVA=$RVPF_HOME/core/lib
export RVPF_CORE_SHARE_SH=$RVPF_HOME/core/bin

export RVPF_JDBC_SHARE_JAVA=$RVPF_HOME/jdbc/lib
export RVPF_PAP_SHARE_JAVA=$RVPF_HOME/pap/lib

# End.
