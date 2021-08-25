// $Id: test-etherip.js 2920 2016-01-04 18:36:02Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');

function _usage() {
    fail("Usage: " + NAME, 1);
}

function _sleep(millis) {
    java.lang.Thread.sleep(millis);
}

if (arguments.length != 0) _usage();

var Math = Java.type('java.lang.Math');
var Double = Java.type('java.lang.Double');
var ElapsedTime = Java.type('org.rvpf.base.ElapsedTime');
var DateTime = Java.type('org.rvpf.base.DateTime');

var PLC = "tests-ab";
var SIN_AMPL = Double.valueOf(100.0);
var SIN_CYCLE = Double.valueOf(10000);
var SIN_STEP = Double.valueOf(100);
var SIN_TAG = 'TESTS_SIN';
var SQUARE_TAG = 'TESTS_SQUARE';

LOGGER.info("Starting");

var EtherNetIP = Java.type('etherip.EtherNetIP');
var CIPData = Java.type('etherip.types.CIPData');

var plc = new EtherNetIP(PLC, 0);

plc.connectTcp();
LOGGER.info("Connected");

var startTime = DateTime.now();
var lastSquareValue = Double.valueOf(0.0);
var data = new CIPData(CIPData.Type.INT, 1);

for (;;) {
    var elapsed = Double.valueOf(DateTime.now().sub(startTime).toMillis());
    var sin = Math.sin(elapsed * Math.PI * 2 / SIN_CYCLE) * SIN_AMPL;

    data.set(0, sin);
    plc.writeTag(SIN_TAG, data);
    // print("SIN: " + plc.readTag(SIN_TAG));

    var square = Math.signum(sin) * SIN_AMPL;

    if (!square.equals(lastSquareValue)) {
        data.set(0, square);
        plc.writeTag(SQUARE_TAG, data);
        // print("SQUARE: " + plc.readTag(SQUARE_TAG));
        lastSquareValue = square;
    }

    _sleep(SIN_STEP.longValue());
}

LOGGER.info("Completed");

// End.
