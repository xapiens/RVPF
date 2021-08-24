#!/bin/bash

# $Id: rvpf-tests-env.sh 3097 2016-07-13 20:12:59Z SFB $

export RVPF_TESTS_CONFIG=${RVPF_SUB}/tests/config
export RVPF_TESTS_CLASSES=${RVPF_SUB}/tests/classes
export RVPF_TESTS_DATA=${RVPF_SUB}/tests/data
if [ "$OSTYPE" = "cygwin" ]; then
    export RVPF_TESTS_CLASSES="$(cygpath -ma $RVPF_TESTS_CLASSES)"
    export RVPF_TESTS_DATA="$(cygpath -ma $RVPF_TESTS_DATA)"
fi

# End.
