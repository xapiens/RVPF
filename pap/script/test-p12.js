// $Id: test-etherip.js 3385 2017-03-13 17:29:06Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');

function _usage() {
    fail("Usage: " + NAME + " <p12-file>", 1);
}

if (arguments.length != 1) _usage();

var FileInputStream = Java.type('java.io.FileInputStream');
var KeyStore = Java.type('java.security.KeyStore');
var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

keyStore.load(new FileInputStream(arguments[0]), null);
LOGGER.info("Type: {0}", keyStore.getType());
LOGGER.info("Provider: {0}", keyStore.getProvider());
LOGGER.info("Size: {0}", keyStore.size());

var aliases = keyStore.aliases();

while (aliases.hasMoreElements()) {
    var alias = aliases.nextElement();

    LOGGER.info("Alias: {0}", alias);
    if (keyStore.isCertificateEntry(alias)) {
        LOGGER.info("    Is certificate");
    }
    if (keyStore.isKeyEntry(alias)) {
        LOGGER.info("    Is key");
    }
}

// End.
