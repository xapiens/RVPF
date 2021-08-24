#!/bin/bash

# $Id: create-server-cert.sh 3535 2017-07-23 14:48:16Z SFB $

shopt -s extglob

function usage
{
    filename=${0##*/}
    echo
    echo "Usage: run ${filename%\.*} <server> <password>"
    echo
    echo "       Creates the \"$target_dir\" subdirectory if necessary."
    echo "       Creates a new keystore containing a fresh server key."
    echo "       Exports a self-signed certificate for the server."
    echo "       Creates an empty server truststore."
    echo
    exit 1
}

function check_status
{
    status=$?
    if [ $status -ne 0 ]; then
        echo "Failed at `caller 0`"
        exit $status
    fi
}

target_dir="ssl"

server="$1"
[ -n "$server" ] || usage
password="$2"
[ -n "$password" ] || usage

validity=3652

mkdir -p "$target_dir"

server_ks="$target_dir/$server.keystore"
server_ts="$target_dir/$server.truststore"
rm -f "$server_ks" "$server_ts"

server_crt=$target_dir/$server.crt

keytool -genkey -keystore "$server_ks" -storepass "$password" -keypass "$password" -dname "CN=$server" -alias "$server" -keyalg RSA -validity $validity
check_status
echo "Server keystore: \"$server_ks\""
keytool -list -v -keystore "$server_ks" -storepass "$password" -alias "$server"
check_status
keytool -export -keystore "$server_ks" -storepass "$password" -alias "$server" -rfc > "$server_crt"
check_status
echo ---
echo "Server certificate: \"$server_crt\""
openssl x509 -in "$server_crt" -noout -text -certopt no_sigdump,no_pubkey
check_status
keytool -import -keystore "$server_ts" -storepass "$password" -alias "$server" -file "$server_crt" -noprompt
check_status
keytool -delete -keystore "$server_ts" -storepass "$password" -alias "$server"
check_status

# End.
