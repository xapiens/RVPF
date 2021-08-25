#!/bin/bash

# $Id: debug.sh 3097 2016-07-13 20:12:59Z SFB $

export DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=y"

shopt -s extglob
RUN_DIR=${0%%*([^/])}
[ -z "$RUN_DIR" ] || cd $RUN_DIR

if [ -e ./setup ]; then
    ./setup auto
fi

RUN_FILE="$1"
shift

exec bin/$RUN_FILE $@

# End.
