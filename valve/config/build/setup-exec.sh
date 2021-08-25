#!/bin/bash

# $Id: setup-exec.sh 2586 2015-04-24 13:31:40Z SFB $

shopt -s extglob
RUN_DIR=${0%%*([^/])}
[ -z "$RUN_DIR" ] || cd $RUN_DIR

chmod -R +x bin/*.sh
chmod +x run wrap

# End.
