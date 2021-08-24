#!/bin/bash

# $Id: hsql.sh 2586 2015-04-24 13:31:40Z SFB $

shopt -s extglob

SQL_DIR=${0%%*([^/])}
[ -z "$SQL_DIR" ] || cd $SQL_DIR

[ -n "$JAVA_HOME" ] && JAVA=$JAVA_HOME/bin/java
[ -z "$JAVA" ] && JAVA=java

CP=../../../lib/hsqldb.jar

exec $JAVA -cp $CP org.hsqldb.util.ScriptTool -url jdbc:hsqldb:file:../../../data/hsqldb/ -database rvpf -script $1

# End.
