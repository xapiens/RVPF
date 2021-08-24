#!/bin/bash

# $Id: create-client-cert.sh 2550 2015-02-20 02:14:03Z SFB $


shopt -s extglob

function usage
{
    filename=${0##*/}
    echo
    echo "Usage: run ${filename%\.*} <client> <server> [<client-password>] <server-password>"
    echo
    echo "       Creates a new key and self-signed certificate for the specified client."
    echo "       Adds or replace the certificate for this client in the server's truststore."
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

client="$1"
[ -n "$client" ] || usage
server="$2"
[ -n "$server" ] || usage
if [ -n "$4" ]; then
    client_password="$3"
    server_password="$4"
else
    client_password=""
    server_password="$3"
fi

validity=3652
target_dir="ssl"

client_cnf="$target_dir/$client.cnf"
rm -f "$client_cnf"
client_key="$target_dir/$client.key"
rm -f "$client_key"
client_crt="$target_dir/$client.crt"
rm -f "$client_crt"
client_pem="$target_dir/$client.pem"
rm -f "$client_pem"
client_p12="$target_dir/$client.p12"
rm -f "$client_p12"
client_ks="$target_dir/$client.keystore"
rm -f "$client_ks"
client_ts="$target_dir/$client.truststore"
rm -f "$client_ts"

server_crt="$target_dir/$server.crt"
server_ts="$target_dir/$server.truststore"

echo "$client_cnf"
if [ -z "$client_password"]; then
    cat <<EOF > "$client_cnf"
RANDFILE=\$ENV::HOME/.rnd
[req]
prompt=no
distinguished_name=req_distinguished_name
[req_distinguished_name]
CN=$client
EOF
    openssl req -config "$client_cnf" -x509 -newkey rsa:1024 -nodes -x509 -days $validity -keyout "$client_key" -out "$client_crt" 2> /dev/null
    check_status
    rm "$client_cnf"
    echo "Client key: \"$client_key\""
    echo "Client certificate: \"$client_crt\""
    openssl x509 -in "$client_crt" -noout -text -certopt no_sigdump,no_pubkey
    check_status
    cat "$client_key" "$client_crt" >"$client_pem"
    openssl pkcs12 -export -in "$client_pem" -out "$client_p12" -name "$client" -password pass:
    check_status
    echo "---"
    echo "Client PEM: \"$client_pem\""
    echo "Client PKCS12: \"$client_p12\""
    openssl pkcs12 -in "$client_p12" -info -noout -password pass:
    check_status
else
    keytool -genkey -keystore "$client_ks" -storepass "$client_password" -keypass "$client_password" -dname "CN=$client" -alias "$client" -keyalg RSA -validity $validity
    check_status
    echo "Client keystore: \"$client_ks\""
    keytool -list -v -keystore "$client_ks" -storepass "$client_password" -alias "$client"
    check_status
    keytool -export -keystore "$client_ks" -storepass "$client_password" -alias "$client" -rfc > "$client_crt"
    check_status
    keytool -import -keystore "$client_ts" -storepass "$client_password" -alias "$server" -file "$server_crt" -noprompt
    check_status
    echo "---"
    echo "Client truststore: \"$client_ts\""
    keytool -list -v -keystore "$client_ts" -storepass "$client_password" -alias "$server"
    check_status
fi

keytool -delete -keystore "$server_ts" -storepass "$server_password" -alias "$client" > /dev/null
keytool -import -keystore "$server_ts" -storepass "$server_password" -alias "$client" -file "$client_crt" -noprompt
check_status
echo "---"
echo "Server truststore: \"$server_ts\""
keytool -list -v -keystore "$server_ts" -storepass "$server_password" -alias "$client"
check_status

# End.
