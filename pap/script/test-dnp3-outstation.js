// $Id: test-dnp3-outstation.js 3927 2019-04-23 13:33:41Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');
getVersion('org.rvpf.pap.dnp3.DNP3Version').logImplementationIdent(true);

var ORIGIN = "DNP3-Outstation";

function _usage() {
    fail("Usage: " + NAME, 1);
}

var Require = Java.type('org.rvpf.base.tool.Require');

if (arguments.length != 0) _usage();

var UUID = JavaType('org.rvpf.base.UUID');
var Optional = JavaType('java.util.Optional');
var Traces = JavaType('org.rvpf.base.util.tool.Traces');
var PAPContext = JavaType('org.rvpf.pap.PAPContext');
var PAPMetadataFilter = JavaType('org.rvpf.pap.PAPMetadataFilter');
var DNP3 = JavaType('org.rvpf.pap.dnp3.DNP3');
var DNP3Support = JavaType('org.rvpf.pap.dnp3.DNP3Support');

LOGGER.info("Starting");

var metadata = PAPContext.fetchMetadata(
    new PAPMetadataFilter(DNP3.ATTRIBUTES_USAGE),
    "script/test-dnp3-metadata.xml",
    UUID.fromString('5b06913c-bf2d-41e7-8b2c-4981ca7bea2a'));

Require.notNull(metadata);

var support = new DNP3Support();
var traces = new Traces();

Require.equal(true, traces.setUp('data/traces/test-dnp3-outstation/protocol'));

var context = support.newServerContext(metadata, [], Optional.of(traces));

Require.notNull(context);

var outstation = support.newServer(context);

Require.notNull(outstation);
Require.equal(true, outstation.setUp(metadata.getPropertiesGroup('outstation.listener')));

outstation.respond();
outstation.setNeedTime();
outstation.start();

LOGGER.info("Started");

while (true) {
    var pointValue = outstation.nextUpdate(-1).get();

    LOGGER.info("Received point update: {0}", pointValue);
}

// End.
