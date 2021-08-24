// $Id: show-pgp.js 3058 2016-06-13 12:28:43Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');

function _usage() {
    fail("Usage: " + NAME + " <PGP file>", 1);
}

function _fingerprint(fingerprintBytes) {
    var fingerprint = '';
    var byteLength = 0;

    for each (var fingerprintByte in fingerprintBytes) {
        if (byteLength > 1) {
            fingerprint += ' ';
            byteLength = 0;
        }

        var string = java.lang.Integer.toHexString(fingerprintByte & 0xFF);

        if (string.length < 2) {
            string = '0' + string;
        }
        fingerprint += string;
        ++byteLength;
    }

    return fingerprint;
}

function _hexString(number) {
    return java.lang.Long.toHexString(number);
}

if (arguments.length != 1) _usage();

var _PGP_FILENAME = arguments[0];

print("File: " + _PGP_FILENAME);
LOGGER.debug("File: {0}", _PGP_FILENAME);

var bcHome = System.env.get('BC_HOME');

if (bcHome == null) fail("BC_HOME is not defined!", 1);

classLoader.addFromLocation('file:' + bcHome + '/*.jar');
classLoader.activate();

var FileInputStream = Java.type('java.io.FileInputStream');
var FileNotFoundException = Java.type('java.io.FileNotFoundException');

var pgpStream;

try {
    pgpStream = new FileInputStream(_PGP_FILENAME);
} catch (exception if exception instanceof FileNotFoundException) {
    fail("File '" + _PGP_FILENAME + "' not found!");
}

var PGPUtil = JavaType('org.bouncycastle.openpgp.PGPUtil');

var manifest = Version.getManifest(PGPUtil.class);

LOGGER.debug(
    "{0} version: {1}",
    Version.getManifestAttribute(
        manifest,
        Version.APPLICATIONNAME_ATTRIBUTE),
    Version.getManifestAttribute(
        manifest,
        Version.BUNDLE_VERSION_ATTRIBUTE));

var PGPObjectFactory = JavaType('org.bouncycastle.openpgp.PGPObjectFactory');
var BcKeyFingerprintCalculator =
    JavaType('org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator');
var PGPPublicKeyRing = JavaType('org.bouncycastle.openpgp.PGPPublicKeyRing');
var PGPSecretKeyRing = JavaType('org.bouncycastle.openpgp.PGPSecretKeyRing');
var DateTime = JavaType('org.rvpf.base.DateTime');

var pgpFactory =
    new PGPObjectFactory(
        PGPUtil.getDecoderStream(pgpStream),
        new BcKeyFingerprintCalculator());

for each (var pgpObject in pgpFactory) {
    if (pgpObject instanceof PGPPublicKeyRing) {
        var keyRing = pgpObject;

        print("\tPublic key ring");
        print("\t\tPublic ID: " + _hexString(keyRing.getPublicKey().getKeyID()));
        for each (var key in keyRing) {
            var userIDs = key.getUserIDs();

            while (userIDs.hasNext()) {
                print("\t\t\tUser ID: " + userIDs.next());
            }
            print("\t\t\tKey ID: " + _hexString(key.getKeyID()));
            print("\t\t\t\tFingerprint: " + _fingerprint(key.getFingerprint()));
            print("\t\t\t\tCreation time: " + DateTime.fromMillis(key.getCreationTime().getTime()));
            if (key.isMasterKey()) print("\t\t\t\tMaster key");
            if (key.isEncryptionKey()) print("\t\t\t\tEncryption key");
            if (key.isRevoked()) print("\t\t\t\tRevoked");
        }
    } else if (pgpObject instanceof PGPSecretKeyRing) {
        var keyRing = pgpObject;

        print("\tSecret key ring");
        print("\t\tSecret ID: " + _hexString(keyRing.getSecretKey().getKeyID()));
        for each (var key in keyRing) {
            var userIDs = key.getUserIDs();

            while (userIDs.hasNext()) {
                print("\t\tUser ID: " + userIDs.next());
            }
            print("\t\tKey ID: " + _hexString(key.getKeyID()));
            if (key.isMasterKey()) print("\t\t\tMaster key");
            if (key.isSigningKey()) print("\t\t\tSigning key");
        }

        var publicKey = keyRing.getPublicKey();

        print("\t\tPublic ID: " + _hexString(publicKey.getKeyID()));
        print("\t\t\tFingerprint: " + _fingerprint(publicKey.getFingerprint()));
        print("\t\t\tCreation time: " + DateTime.fromMillis(publicKey.getCreationTime().getTime()));
    }
}

pgpStream.close();

// End.
