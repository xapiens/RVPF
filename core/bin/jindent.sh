#!/bin/bash

# $Id: jindent.sh 2990 2016-02-15 14:14:56Z SFB $

export CLASSPATH="$JINDENT_HOME/lib/*"

exec java JindentCommander $@ &

# End.
