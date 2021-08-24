#!/bin/bash

# $Id: rvpf-tests-env.sh 3097 2016-07-13 20:12:59Z SFB $

export RVPF_TESTS_CONFIG=/etc/opt/rvpf/tests/$1/config
export RVPF_TESTS_CLASSES=/etc/opt/rvpf/tests/$1/classes
export RVPF_TESTS_DATA=/var/opt/rvpf/tests/$1/data
if [ "$OSTYPE" = "cygwin" ]; then
    export RVPF_TESTS_CLASSES="$(cygpath -ma $RVPF_TESTS_CLASSES)"
    export RVPF_TESTS_DATA="$(cygpath -ma $RVPF_TESTS_DATA)"
fi

# End.
