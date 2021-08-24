#!/bin/bash

# $Id: h2.sh 2586 2015-04-24 13:31:40Z SFB $

shopt -s extglob

SQL_DIR=${0%%*([^/])}
[ -z "$SQL_DIR" ] || cd $SQL_DIR

[ -n "$JAVA_HOME" ] && JAVA=$JAVA_HOME/bin/java
[ -z "$JAVA" ] && JAVA=java

CP=../../../lib/opt/h2.jar

exec $JAVA -cp $CP org.h2.tools.RunScript -url jdbc:h2:file:../../../data/h2/rvpf -user sa -script $1

# End.
