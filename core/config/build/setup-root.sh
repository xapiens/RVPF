#!/bin/bash

# $Id: setup-root.sh 4113 2019-08-03 13:36:57Z SFB $

mkdir -pv ../bin/setup
mkdir -pv ../config/script/ivy
mkdir -pv ../config/script/local
mkdir -pv ../config/wrap
mkdir -pv ../lib
mkdir -pv ../log
mkdir -pv ../script
mkdir -pv ../tmp

chmod -R +x bin/*.sh
chmod +x run wrap

cp -vn rc.rvpf ../
cp -vn run ../
cp -vn wrap ../
cp -vn bin/js.sh ../bin/
cp -vn bin/jython.sh ../bin/
cp -vn bin/script.sh ../bin/
cp -vn bin/wrap.sh ../bin/
cp -vn config/script/log4j2.xml ../config/script/

cp -vn config/build/rvpf-root.sh ../bin/setup/rvpf.sh
cp -vn config/build/rvpf-env-root.sh ../bin/setup/rvpf-env.sh

if [ ! -d "../config/service" ]; then
    mkdir -pv ../config/service/local
    for file in config/service/*; do
        [ -f "$file" ] && cp -v $file ../config/service/
    done
fi

# End.
