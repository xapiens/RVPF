#!/bin/bash

# $Id: setup-root.sh 3512 2017-07-09 00:46:16Z SFB $

mkdir -pv ../bin/setup
mkdir -pv ../config/script
mkdir -pv ../config/service
mkdir -pv ../config/wrap
mkdir -pv ../log
mkdir -pv ../tmp

cp -vn run ../
cp -vn wrap ../
cp -vn bin/wrap.sh ../bin/
cp -vn config/build/rvpf-root.sh ../bin/setup/rvpf.sh
cp -vn config/build/rvpf-env-root.sh ../bin/setup/rvpf-env.sh
cp -vn config/script/log4j2.xml ../config/script/

# End.
