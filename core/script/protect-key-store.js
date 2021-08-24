// $Id: protect-key-store.js 3058 2016-06-13 12:28:43Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');

function _usage() {
    fail("Usage: " + NAME + " <key-store-path>", 1);
}

if (arguments.length != 1) _usage();

System.setProperty('line.separator', '\n');

var inputPath = arguments[0];

var _STORE = 'store';
var _XML = '.xml';

if (!inputPath.endsWith(_STORE)) _usage();

var outputPath = inputPath + _XML;

var ConfigDocumentLoader = Java.type('org.rvpf.document.loader.ConfigDocumentLoader');

var config = ConfigDocumentLoader.loadConfig();

if (config == null) fail("Failed to load config", 2);

var ByteArray = Java.type('byte[]');

var inputStream = new java.io.FileInputStream(inputPath);
var inputSize = inputStream.available();
var buffer = new ByteArray(inputSize);
var read = inputStream.read(buffer);

inputStream.close();
if (read != inputSize) fail("Failed to read " + inputPath, 3);

var SecurityContext = Java.type('org.rvpf.base.security.SecurityContext');
var KeyedGroups = Java.type('org.rvpf.base.util.container.KeyedGroups');

var securityContext = new SecurityContext(LOGGER.getLogger());

if (!securityContext.setUp(config.getProperties(), KeyedGroups.MISSING_KEYED_GROUP)) {
    fail("Failed to set up the security context", 4);
}

var Crypt = Java.type('org.rvpf.base.security.Crypt');

var crypt = new Crypt();

if (!crypt.setUp(securityContext.getCryptProperties(), null)) {
    fail("Failed to set up the crypt instance", 5);
}

var cryptResult = crypt.encryptAndSign(buffer, null, null);

if (cryptResult.isFailure()){
    fail("Failed to crypt the realm: " + cryptResult.getException(), 6);
}

var XMLDocument = Java.type('org.rvpf.base.xml.XMLDocument');

var xml = new XMLDocument(crypt.getStreamer().toXML(cryptResult.getSerializable()));
var outputWriter = new java.io.OutputStreamWriter(
    new java.io.FileOutputStream(outputPath), 'UTF-8');

outputWriter.write(xml.toString());
outputWriter.close();

print("Done.");

// End.
