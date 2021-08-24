// $Id: protect-config.js 3058 2016-06-13 12:28:43Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');

function _usage() {
    fail("Usage: " + NAME + " <key-store-path>", 1);
}

if (arguments.length != 1) _usage();

System.setProperty('line.separator', '\n');

var inputPath = arguments[0];

var _XML = '.xml';
var _PROTECTED = '-protected'

if (!inputPath.endsWith(_XML)) _usage();

var outputPath = inputPath.substring(0, inputPath.length() - _XML.length()) + _PROTECTED + _XML;

var ConfigDocumentLoader = Java.type('org.rvpf.document.loader.ConfigDocumentLoader');

var config = ConfigDocumentLoader.loadConfig();

if (config == null) fail("Failed to load config", 2);

var UnicodeStreamReader = Java.type('org.rvpf.base.util.UnicodeStreamReader');

var inputReader = new java.io.BufferedReader(new UnicodeStreamReader(inputPath));
var buffer = new java.lang.StringBuilder();

for (;;) {
    var line = inputReader.readLine();

    if (line == null) break;
    buffer.append(line);
    buffer.append('\n');
}
inputReader.close();

var SecurityContext = Java.type('org.rvpf.base.security.SecurityContext');
var KeyedGroups = Java.type('org.rvpf.base.util.container.KeyedGroups');

var securityContext = new SecurityContext(LOGGER.getLogger());

if (!securityContext.setUp(config.getProperties(), KeyedGroups.MISSING_KEYED_GROUP)) {
    fail("Failed to set up the security context", 3);
}

var Crypt = Java.type('org.rvpf.base.security.Crypt');

var crypt = new Crypt();

if (!crypt.setUp(securityContext.getCryptProperties(), null)) {
    fail("Failed to set up the crypt instance", 4);
}

var cryptResult = crypt.encryptAndSign(buffer.toString(), null, null);

if (cryptResult.isFailure()){
    fail("Failed to crypt the realm: " + cryptResult.getException(), 5);
}

var XMLDocument = Java.type('org.rvpf.base.xml.XMLDocument');

var xml = new XMLDocument(crypt.getStreamer().toXML(cryptResult.getSerializable()));
var outputWriter = new java.io.OutputStreamWriter(
    new java.io.FileOutputStream(outputPath), 'UTF-8');

outputWriter.write(xml.toString());
outputWriter.close();

print("Done.");

// End.
