#!/bin/bash

# $Id: derby.sh 2586 2015-04-24 13:31:40Z SFB $

shopt -s extglob

SQL_DIR=${0%%*([^/])}
[ -z "$SQL_DIR" ] || cd $SQL_DIR

[ -n "$JAVA_HOME" ] && JAVA=$JAVA_HOME/bin/java
[ -z "$JAVA" ] && JAVA=java

CP=../../../lib/opt/derbytools.jar:../../../lib/opt/derby.jar
[ "$OSTYPE" = "cygwin" ] && CP="`cygpath -mp $CP`"

OPTS="-cp $CP"
OPTS="$OPTS -Dderby.system.home=../../../data/derby"
OPTS="$OPTS -Dij.database=jdbc:derby:RVPF;create=true"

exec $JAVA $OPTS org.apache.derby.tools.ij $1

# End.
