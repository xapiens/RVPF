#!/bin/bash

# $Id: debug.sh 2349 2014-08-15 02:26:59Z SFB $

export DEBUG_OPTS="-ea -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=y"

shopt -s extglob
RUN_DIR=${0%%*([^/])}
[ -z "$RUN_DIR" ] || cd $RUN_DIR

if [ -e ./setup ]; then
    ./setup auto
fi

RUN_FILE="$1"
shift

export JAVA=
exec bin/$RUN_FILE $@

# End.
