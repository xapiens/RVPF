#!/bin/bash

# $Id: create-truststore.sh 3051 2016-06-05 13:12:28Z SFB $

KEYSTORE=$HOME/.rvpf.keystore
PASSWORD=rvpf-password
ALIAS=rvpf
TRUSTSTORE=config/service/local/rvpf.truststore

[ "$OSTYPE" = "cygwin" ] && KEYSTORE="`cygpath -w $KEYSTORE`"
rm -f $TRUSTSTORE

keytool -export -keystore "$KEYSTORE" -alias "$ALIAS" -storepass "$PASSWORD" -rfc >tmp/crt.tmp
keytool -import -keystore "$TRUSTSTORE" -alias "$ALIAS" -storepass "$PASSWORD" -file tmp/crt.tmp

# End.
