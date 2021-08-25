#!/bin/bash

# $Id: wrap.sh 3615 2018-03-15 14:45:47Z SFB $

function add-app-parameter
{
    [ -n "$APP_PARAMETERS" ] && APP_PARAMETERS="$APP_PARAMETERS "
    APP_PARAMETERS="$APP_PARAMETERS$1"
}

function add-java-option
{
    [ -n "$JAVA_OPTIONS" ] && JAVA_OPTIONS="$JAVA_OPTIONS "
    JAVA_OPTIONS="$JAVA_OPTIONS$1"
}

function add-system-property
{
    add-java-option -D$1
}

function add-to-class-path
{
    [ -n "$CLASSPATH" ] && CLASSPATH="$CLASSPATH:"
    CLASSPATH="$CLASSPATH$1"
}

function add-to-library-path
{
    [ -n "$LIBPATH" ] && LIBPATH="$LIBPATH:"
    LIBPATH="$LIBPATH$1"
}

function be-nice
{
    BE_NICE="nice"
    [ -n "$1" ] && BE_NICE="$BE_NICE -n $1"
}

function include
{
. $RVPF_CONFIG/wrap/$1.sh
}

function set-initial-memory
{
    INITIAL_MEMORY="$1M"
}

function set-log-path
{
    export RVPF_LOG=$1
}

function set-main-class
{
    MAIN_CLASS=$1
}

function set-maximum-memory
{
    MAXIMUM_MEMORY="$1M"
}

function set-user-language
{
    add-system-property user.language=$1
}

function use-authbind
{
    AUTHBIND=authbind
}

. $RVPF_CONFIG/wrap/$RVPF_TARGET.sh

[ -n "$INITIAL_MEMORY" ] && add-java-option -Xms$INITIAL_MEMORY
[ -n "$MAXIMUM_MEMORY" ] && add-java-option -Xmx$MAXIMUM_MEMORY

if [ "$OSTYPE" = "cygwin" ]; then
    CLASSPATH="$(cygpath -mp $CLASSPATH)"
    export RVPF_SHARE_JAVA="$(cygpath -mp $RVPF_SHARE_JAVA)"
    export RVPF_CORE_SHARE_JAVA="$(cygpath -mp $RVPF_CORE_SHARE_JAVA)"
fi
export CLASSPATH

if [ -n "$LIBPATH" ]; then
    if [ "${OSTYPE%%-*}" = "linux" ]; then
        [ -n "$LD_LIBRARY_PATH" ] && LD_LIBRARY_PATH=$LD_LIBRARY_PATH:
        export LD_LIBRARY_PATH="$LD_LIBRARY_PATH$LIBPATH"
    elif [ "${OSTYPE:0:6}" = "darwin" ]; then
        [ -n "$DYLD_LIBRARY_PATH" ] && DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:
        export DYLD_LIBRARY_PATH="$DYLD_LIBRARY_PATH$LIBPATH"
    elif [ "$OSTYPE" = "cygwin" ]; then
        export PATH="$LIBPATH:$PATH"
    else
        add-system-property java.library.path=$LIBPATH
    fi
fi

trap "rm $RVPF_PID_FILE" EXIT
[ "$RVPF_ACTION" = "start" ] && trap '' HUP

function shutdown
{
    echo >&2
    echo -n "$(date +'%F %T') " >&2
    echo 'Termination requested: shutting down!' >&2
    echo >&2
    [ "$OSTYPE" != "cygwin" ] && kill $!
    wait $!
}

$BE_NICE $AUTHBIND $JAVA $JAVA_OPTIONS $MAIN_CLASS $APP_PARAMETERS &
trap shutdown TERM INT
wait $!

# End.
