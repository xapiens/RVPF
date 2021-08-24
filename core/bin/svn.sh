#!/bin/bash

# $Id: svn.sh 3280 2016-12-14 20:20:41Z SFB $

shopt -s extglob

CLASSPATH=$RVPF_CORE_SHARE_JAVA/svnkit-all.jar

[ "$OSTYPE" = "cygwin" ] && CLASSPATH=`cygpath -mp $CLASSPATH`

export CLASSPATH

exec $JAVA -Dsun.io.useCanonCaches=false org.tmatesoft.svn.cli.SVN "$@"

# End.
