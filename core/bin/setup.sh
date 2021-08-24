#!/bin/bash

# $Id: setup.sh 3097 2016-07-13 20:12:59Z SFB $

shopt -s extglob

confs="$@"
if [ -z "$confs" ]; then
    confs="run"
    if [ -d "src" ]; then
        confs="compile $confs"
        [ -f "$RVPF_SHARE_JAVA/hsqldb.jar" ] && confs="$confs optional"
    else
        [ -f "$RVPF_SHARE_JAVA/testng.jar" ] && confs="$confs test"
    fi
elif [ "$confs"="optional" ]; then
    confs="core optional"
fi

$RVPF_BIN/retrieve.sh $confs

[ -n "$RVPF_HOME" ] && $RVPF_CONFIG/build/setup-root.sh

# End.
