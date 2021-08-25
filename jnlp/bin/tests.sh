#!/bin/bash

# $Id: tests.sh 3813 2018-11-10 20:44:07Z SFB $

shopt -s extglob

. $RVPF_BIN/setup/rvpf-tests-env.sh

sys_props="-Xmx256M"
sys_props="$sys_props -Duser.country=CA -Duser.language=en"
#sys_props="$sys_props -Djava.rmi.server.logCalls=true"

classes=$RVPF_SHARE_JAVA/rvpf-jnlp-launcher.jar
tests_classes=$RVPF_SHARE_JAVA/rvpf-jnlp-tests.jar
[ -d $RVPF_SUB/build/test/classes ] && tests_classes=$RVPF_SUB/build/test/classes

CLASSPATH=$RVPF_TESTS_CONFIG/local:$RVPF_TESTS_CONFIG:$tests_classes:$classes
CLASSPATH=$CLASSPATH:$RVPF_CORE_SHARE_JAVA/rvpf-http.jar
CLASSPATH=$CLASSPATH:$RVPF_CORE_SHARE_JAVA/rvpf-tests.jar
LIBPATH=$RVPF_LIB:$RVPF_CORE_LIB
if [ "$OSTYPE" = "cygwin" ]; then
    export RVPF_SHARE_JAVA="`cygpath -mp $RVPF_SHARE_JAVA`"
    export RVPF_CORE_SHARE_JAVA="`cygpath -mp $RVPF_CORE_SHARE_JAVA`"
    CLASSPATH="`cygpath -mp $CLASSPATH`"
    export PATH="$LIBPATH:$PATH"
elif [ "${OSTYPE%%-*}" = "linux" ]; then
    [ -n "$LD_LIBRARY_PATH" ] && LD_LIBRARY_PATH=$LD_LIBRARY_PATH:
    export LD_LIBRARY_PATH="$LD_LIBRARY_PATH$LIBPATH"
elif [ "${OSTYPE:0:6}" = "darwin" ]; then
    [ -n "$DYLD_LIBRARY_PATH" ] && DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:
    export DYLD_LIBRARY_PATH="$DYLD_LIBRARY_PATH$LIBPATH"
else
    sys_props="-Djava.library.path=$LIBPATH"
fi
export CLASSPATH

if [ -n "$DEBUG_OPTS" ]; then
    sys_props="$sys_props -Drvpf.tests.notice.timeout=-1"
    sys_props="$sys_props -Drvpf.tests.request.timeout=-1"
fi

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
