#!/bin/bash

# $Id: setup.sh 3097 2016-07-13 20:12:59Z SFB $

shopt -s extglob

confs="$@"
if [ -z "$confs" ]; then
    confs="run"
    if [ -d "src" ]; then
        confs="compile $confs"
    else
        [ -f "$RVPF_CORE_SHARE_JAVA/testng.jar" ] && confs="$confs test"
    fi
fi

$RVPF_BIN/retrieve.sh $confs

# End.
