// $Id: test-etherip.js 3535 2017-07-23 14:48:16Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');

function _usage() {
    fail("Usage: " + NAME, 1);
}

function _logTag(tag, data) {
    var value, type;

    if (data == null) {
        value = "Unkwnown";
        type = "";
    } else {
        if (data.isNumeric()) value = data.getNumber(0);
        else value = data.getString();
        type = " (" + data.getType().name() + ")";
    }
    LOGGER.info("Tag ''{0}''{1}: {2}", tag, type, value);
}

if (arguments.length != 0) _usage();

LOGGER.info("Starting");

var EtherNetIP = Java.type('etherip.EtherNetIP');
var address = "tests-ab";
var slot = 0;

var plc = new EtherNetIP(address, slot);

plc.connect();
LOGGER.info("Connected to ''{0}'' on slot ''{1}''", address, slot);

var tag;
var data;
var elements;

tag = "TESTS_BOOL_1";
data = plc.readTag(tag);
_logTag(tag, data);
if (data != null) {
    data.set(0, data.getNumber(0) == 0? 1: 0)
    plc.writeTag(tag, data);
    _logTag(tag, plc.readTag(tag));
}

tag = "TESTS_SINT_1";
data = plc.readTag(tag);
_logTag(tag, data);
if (data != null) {
    data.set(0, data.getNumber(0) + 10)
    plc.writeTag(tag, data);
    _logTag(tag, plc.readTag(tag));
}

tag = "TESTS_INT_1";
data = plc.readTag(tag);
_logTag(tag, data);
if (data != null) {
    data.set(0, data.getNumber(0) + 100);
    plc.writeTag(tag, data);
    _logTag(tag, plc.readTag(tag));
}

tag = "TESTS_DINT_1";
data = plc.readTag(tag);
_logTag(tag, data);
if (data != null) {
    data.set(0, data.getNumber(0) + 1000);
    plc.writeTag(tag, data);
    _logTag(tag, plc.readTag(tag));
}

// tag = "TESTS_LINT_1";
// data = plc.readTag(tag);
// _logTag(tag, data);
// if (data != null) {
//     data.set(0, data.getNumber(0) + 10000);
//     plc.writeTag(tag, data);
//     _logTag(tag, plc.readTag(tag));
// }

tag = "TESTS_REAL_1";
data = plc.readTag(tag);
_logTag(tag, data);
if (data != null) {
    data.set(0, (data.getNumber(0) + 1) * 1.5);
    plc.writeTag(tag, data);
    _logTag(tag, plc.readTag(tag));
}

tag = "TESTS_DINT_ARRAY_1";
data = plc.readTag(tag, 5);
if (data != null) {
    elements = data.getElementCount();
    for (var i = 0; i < elements; ++i) {
        LOGGER.info(
            "Tag ''{0}'' ({1}) [{2}]: {3}",
            tag, data.getType().name(), i, data.getNumber(i));
        data.set(i, data.getNumber(i) + (i + 1) * 1000);
    }
    plc.writeTag(tag, data);
    for (var i = 0; i < elements; ++i) {
        LOGGER.info(
            "Tag ''{0}'' ({1}) [{2}]: {3}",
            tag, data.getType().name(), i, data.getNumber(i));
    }
} else {
    _logTag(tag, data);
}

tag = "TESTS_BOOL_ARRAY_1";
data = plc.readTag(tag, 5);
if (data != null) {
    elements = data.getElementCount();
    for (var i = 0; i < elements; ++i) {
        LOGGER.info(
            "Tag ''{0}'' ({1}) [{2}]: {3}",
            tag, data.getType().name(), i, data.getNumber(i));
        data.set(i, i & 1);
    }
    plc.writeTag(tag, data);
    for (var i = 0; i < elements; ++i) {
        LOGGER.info(
            "Tag ''{0}'' ({1}) [{2}]: {3}",
            tag, data.getType().name(), i, data.getNumber(i));
    }
} else {
    _logTag(tag, data);
}

tag = "TESTS_REAL_ARRAY_1";
data = plc.readTag(tag, 5);
if (data != null) {
    elements = data.getElementCount();
    for (var i = 0; i < elements; ++i) {
        LOGGER.info(
            "Tag ''{0}'' ({1}) [{2}]: {3}",
            tag, data.getType().name(), i, data.getNumber(i));
        data.set(i, data.getNumber(i) + (i + 1) * 1000 + i / 10.0);
    }
    plc.writeTag(tag, data);
    for (var i = 0; i < elements; ++i) {
        LOGGER.info(
            "Tag ''{0}'' ({1}) [{2}]: {3}",
            tag, data.getType().name(), i, data.getNumber(i));
    }
    tag = tag + "[2]";
    data = plc.readTag(tag);
    _logTag(tag, data);
} else {
    _logTag(tag, data);
}

var tag_1, tag_2;

tag_1 = "TESTS_REAL_1";
tag_2 = "TESTS_BOOL_1";
data = plc.readTags(tag_1, tag_2);
_logTag(tag_1, data[0]);
_logTag(tag_2, data[1]);

// tag = "TESTS_STRING_1";
// data = plc.readTag(tag);
// _logTag(tag, data);

plc.close();

LOGGER.info("Completed");

// End.
