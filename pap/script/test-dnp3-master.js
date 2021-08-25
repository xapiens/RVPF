// $Id: test-etherip.js 2920 2016-01-04 18:36:02Z SFB $

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

var Short = JavaType('java.lang.Short');
var Thread = JavaType('java.lang.Thread');
var Optional = JavaType('java.util.Optional');
var Traces = JavaType('org.rvpf.base.util.tool.Traces');
var DateTime = JavaType('org.rvpf.base.DateTime');
var UUID = JavaType('org.rvpf.base.UUID');
var PointValue = JavaType('org.rvpf.base.value.PointValue');
var PAPContext = JavaType('org.rvpf.pap.PAPContext');
var PAPMetadataFilter = JavaType('org.rvpf.pap.PAPMetadataFilter');
var DNP3 = JavaType('org.rvpf.pap.dnp3.DNP3');
var DNP3Support = JavaType('org.rvpf.pap.dnp3.DNP3Support');
var DNP3Master = JavaType('org.rvpf.pap.dnp3.DNP3Master');

LOGGER.info("Starting");

var metadata = PAPContext.fetchMetadata(
    new PAPMetadataFilter(DNP3.ATTRIBUTES_USAGE),
    "script/test-dnp3-metadata.xml",
    UUID.fromString('5e20874a-1a6d-4179-9145-190ad2056086'));

Require.notNull(metadata);

var support = new DNP3Support();
var traces = new Traces();

Require.equal(true, traces.setUp("data/traces/test-dnp3-master/protocol"));

var context = support.newClientContext(metadata, Optional.of(traces));

Require.notNull(context);

var masterDevice = metadata.getIntValue('dnp3.master.device', 0);
var master = new DNP3Master(context, masterDevice);

Require.notNull(master);
Require.equal(true, master.setUp(metadata.getPropertiesGroup('master.listener')));

master.open();

var origin = context.getRemoteOrigin(ORIGIN);

Require.notNull(origin);

var outstationDevice = metadata.getIntValue('dnp3.outstation.device', 0);

if (!master.connect(origin, outstationDevice)) {
    fail("Failed to connect!", 2);
}

LOGGER.info("Connected");

// var analogOutputPoint = metadata.getPoint('TESTS-DNP3.AO-SHORT.1');

// Require.notNull(analogOutputPoint);

// var pointValue;

// pointValue = master.read(analogOutputPoint);

// LOGGER.info("Received point value: {0}", pointValue);

// pointValue = new PointValue(
//     analogOutputPoint,
//     DateTime.now(),
//     null,
//     Short.valueOf(1234));
// master.write(pointValue);

// LOGGER.info("Sent point update: {0}", pointValue);

// pointValue = master.read(analogOutputPoint);

// LOGGER.info("Received point value: {0}", pointValue);

var analogInputPoint = metadata.getPoint('TESTS-DNP3.AI-FLOAT-FLAGS.1');

Require.notNull(analogInputPoint);

var binaryInputPoint = metadata.getPoint('TESTS-DNP3.BI-FLAGS.1');

Require.notNull(binaryInputPoint);

for (var i = 1; i <= 3; ++i) {
    Thread.sleep(1000);

    var analogPointValue = master.read(analogInputPoint);

    LOGGER.info("Received analog point value: {0}", analogPointValue);

    var binaryPointValue = master.read(binaryInputPoint);

    LOGGER.info("Received binary point value: {0}", binaryPointValue);
}

var analogInputArray = metadata.getPoint('TESTS-DNP3.AI-ARRAY.1');

Require.notNull(analogInputArray);

var binaryInputArray = metadata.getPoint('TESTS-DNP3.BI-ARRAY.1');

Require.notNull(binaryInputArray);

for (var i = 1; i <= 3; ++i) {
    Thread.sleep(1000);

    var analogArrayValue = master.read(analogInputArray);

    LOGGER.info("Received analog point value: {0}", analogArrayValue);

    var binaryArrayValue = master.read(binaryInputArray);

    LOGGER.info("Received binary point value: {0}", binaryArrayValue);
}

master.disconnect();

LOGGER.info("Disconnected");

master.close();
master.tearDown();

LOGGER.info("Completed");

// End.
