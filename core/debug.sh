#!/bin/bash

# $Id: debug.sh 2349 2014-08-15 02:26:59Z SFB $

export DEBUG_OPTS="-ea -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=y"

shopt -s extglob
run_dir=${0%%*([^/])}
[ -z "$run_dir" ] || cd $run_dir

run_file=bin/$1.sh
[ -e $run_file ] || run_file="core/$run_file"
shift

exec $run_file $@

# End.
