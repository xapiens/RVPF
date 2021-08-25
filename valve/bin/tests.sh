#!/bin/bash

# $Id: tests.sh 3683 2018-09-02 13:43:05Z SFB $

shopt -s extglob

. $RVPF_BIN/setup/rvpf-tests-env.sh

sys_props="-Xmx256M"
sys_props="$sys_props -Duser.country=CA -Duser.language=en"
#sys_props="$sys_props -Djava.rmi.server.logCalls=true"

classes=$RVPF_SHARE_JAVA/rvpf-valve.jar
tests_classes=$RVPF_SHARE_JAVA/rvpf-valve-tests.jar
if [ -n "$RVPF_SUB" ]; then
[ -d $RVPF_SUB/build/main/classes ] && classes=$RVPF_SUB/shared:$RVPF_SUB/build/main/classes
[ -d $RVPF_SUB/build/test/classes ] && tests_classes=$RVPF_SUB/build/test/classes
fi
CLASSPATH=$RVPF_TESTS_CONFIG/local:$RVPF_TESTS_CONFIG:$tests_classes:$classes
CLASSPATH=$CLASSPATH:$RVPF_CORE_SHARE_JAVA/rvpf-tests.jar
CLASSPATH=$CLASSPATH:$RVPF_CORE_SHARE_JAVA/rvpf-service.jar

[ "$OSTYPE" = "cygwin" ] && CLASSPATH=`cygpath -mp $CLASSPATH`

PATH=$RVPF_LIB:$RVPF_CORE_LIB:$PATH

[ -n "$DEBUG_OPTS" ] || DEBUG_OPTS=-ea

export CLASSPATH PATH

tests_log=$RVPF_LOG/tests.log

rm -f $tests_log
rm -rf $RVPF_TESTS_DATA

$JAVA $sys_props $DEBUG_OPTS org.rvpf.tests.FrameworkTests $TESTNG_XML $@
status=$?
if [ -f $tests_log  ]; then
    echo
    grep 'FATAL\|ERROR\|WARN' $tests_log
fi

exit $status

# End.
