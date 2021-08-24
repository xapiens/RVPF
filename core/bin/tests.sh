#!/bin/bash

# $Id: tests.sh 3927 2019-04-23 13:33:41Z SFB $

shopt -s extglob

. $RVPF_BIN/setup/rvpf-tests-env.sh

sys_props="-Xmx256M"
sys_props="$sys_props -Duser.country=CA -Duser.language=en"
#sys_props="$sys_props -Djava.rmi.server.logCalls=true"

[ -f "$RVPF_CORE_SHARE_JAVA//testng.jar" ] || $RVPF_CORE_BIN/retrieve.sh test

classes=$RVPF_SHARE_JAVA/rvpf-http.jar:$RVPF_SHARE_JAVA/rvpf-store.jar
classes=$classes:$RVPF_SHARE_JAVA/rvpf-processor.jar:$RVPF_SHARE_JAVA/rvpf-forwarder.jar
tests_classes=$RVPF_SHARE_JAVA/rvpf-core-tests.jar
if [ -n "$RVPF_HOME" ]; then
    [ -d $RVPF_HOME/core/build/main/classes ] && classes=$RVPF_HOME/core/shared:$RVPF_HOME/core/build/main/classes
    [ -d $RVPF_HOME/core/build/test/classes ] && tests_classes=$RVPF_HOME/core/build/test/classes
fi

CLASSPATH=$RVPF_TESTS_CONFIG/local:$RVPF_TESTS_CONFIG:$tests_classes:$classes
CLASSPATH=$CLASSPATH:$RVPF_SHARE_JAVA/javax.servlet-api.jar
CLASSPATH=$CLASSPATH:$RVPF_SHARE_JAVA/javax.json.jar
CLASSPATH=$CLASSPATH:$RVPF_SHARE_JAVA/javax.json-api.jar
CLASSPATH=$CLASSPATH:$RVPF_SHARE_JAVA/log4j-api.jar
CLASSPATH=$CLASSPATH:$RVPF_SHARE_JAVA/log4j-core.jar
CLASSPATH=$CLASSPATH:$RVPF_SHARE_JAVA/log4j-jul.jar
CLASSPATH=$CLASSPATH:$RVPF_SHARE_JAVA/javax.mail.jar
CLASSPATH=$CLASSPATH:$RVPF_SHARE_JAVA/xstream.jar
CLASSPATH=$CLASSPATH:$RVPF_SHARE_JAVA/xml-resolver.jar
CLASSPATH=$CLASSPATH:$RVPF_SHARE_JAVA/testng.jar
CLASSPATH=$CLASSPATH:$RVPF_SHARE_JAVA/jcommander.jar
LIBPATH=$RVPF_LIB
if [ "$OSTYPE" = "cygwin" ]; then
    export RVPF_CORE_SHARE_JAVA="$(cygpath -mp $RVPF_CORE_SHARE_JAVA)"
    CLASSPATH="$(cygpath -mp $CLASSPATH)"
    export PATH="$LIBPATH:$PATH"
elif [ "${OSTYPE%%-*}" = "linux" ]; then
    [ -n "$LD_LIBRARY_PATH" ] && LD_LIBRARY_PATH=$LD_LIBRARY_PATH:
    export LD_LIBRARY_PATH="$LD_LIBRARY_PATH$LIBPATH"
elif [ "${OSTYPE:0:6}" = "darwin" ]; then
    [ -n "$DYLD_LIBRARY_PATH" ] && DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:
    export DYLD_LIBRARY_PATH="$DYLD_LIBRARY_PATH$LIBPATH"
else
    sys_props="$sys_props -Djava.library.path=$LIBPATH"
fi
export CLASSPATH

export RVPF_TESTS_VALUE=OK

[ -n "$DEBUG_OPTS" ] && sys_props="$sys_props -Drvpf.tests.timeout=-1"

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
