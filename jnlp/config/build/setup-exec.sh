#!/bin/bash

# $Id: setup-exec.sh 3097 2016-07-13 20:12:59Z SFB $

shopt -s extglob
RUN_DIR=${0%%*([^/])}
[ -z "$RUN_DIR" ] || cd $RUN_DIR

chmod -R +x bin/*.sh
chmod +x run wrap

# End.
