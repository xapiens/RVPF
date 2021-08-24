#!/bin/bash

# $Id: setup-deb-sim.sh 3103 2016-07-21 18:23:00Z SFB $

shopt -s extglob

mkdir -p /etc/opt/rvpf/script
mkdir -p /etc/opt/rvpf/service
mkdir -p /etc/opt/rvpf/wrap
mkdir -p /opt/rvpf/bin
mkdir -p /opt/rvpf/lib
mkdir -p /opt/rvpf/share/java
mkdir -p /opt/rvpf/share/sh/setup
mkdir -p /srv/opt/rvpf
mkdir -p /var/opt/rvpf/log
mkdir -p /var/opt/rvpf/run

rm -f /opt/rvpf/bin/*.sh
rm -f /opt/rvpf/bin/setup/*.sh

cp bin/js.sh /opt/rvpf/bin/
cp bin/tests.sh /opt/rvpf/bin/
cp bin/wrap.sh /opt/rvpf/bin/
cp config/build/deb/bin/run /opt/rvpf/bin/
cp config/build/deb/bin/setup/*.sh /opt/rvpf/share/sh/setup
cp bin/../wrap /opt/rvpf/bin/

rm -f /opt/rvpf/share/java/*.jar
cp lib/*.jar /opt/rvpf/share/java/

mkdir -p /etc/opt/rvpf/tests/core/config/local
mkdir -p /etc/opt/rvpf/tests/core/config/scenarios/local
mkdir -p /etc/opt/rvpf/tests/core/script
rm -f /etc/opt/rvpf/tests/core/config/*.*
cp tests/config/*.* /etc/opt/rvpf/tests/core/config/
rm -f /etc/opt/rvpf/tests/core/config/scenarios/*.*
cp tests/config/scenarios/*.* /etc/opt/rvpf/tests/core/config/scenarios/
rm -rf /etc/opt/rvpf/tests/core/classes
cp -r tests/classes /etc/opt/rvpf/tests/core/
rm -f /etc/opt/rvpf/tests/core/script/*.*
cp tests/script/*.* /etc/opt/rvpf/tests/core/script/

# End.
