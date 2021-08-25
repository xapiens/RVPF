// $Id: test-etherip.js 3382 2017-03-10 21:41:51Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');

function _usage() {
    fail("Usage: " + NAME + " <address>[,<slot>] <tag>[,<elements>] ...", 1);
}

if (arguments.length < 2) _usage();

var Integer = Java.type('java.lang.Integer');

var target = arguments[0].split(",");

if (target.length > 2) _usage();

var address = target[0];
var slot = target.length > 1? Integer.parseInt(target[1]): 0;

LOGGER.info("Starting");

var EtherNetIP = Java.type('etherip.EtherNetIP');

var plc = new EtherNetIP(address, slot);

plc.connect();
LOGGER.info("Connected to ''{0}'' on slot ''{1}''", address, slot);

for (var i = 1; i < arguments.length; ++i) {
    var source = arguments[i].split(",");

    if (source.length > 2) _usage();

    var tag = source[0];
    var elements = source.length > 1? Integer.parseInt(source[1]): 1;
    var data = plc.readTag(tag, elements);

    if (data != null) {
        for (var j = 0; j < elements; ++j) {
            LOGGER.info(
                "Tag ''{0}'' ({1}) [{2}]: {3}",
                tag, data.getType().name(), j, data.getNumber(j));
        }
    } else {
        LOGGER.info("Tag ''{0}''{1}: {2}", tag, "", plc.decodeStatus());
    }
}

plc.close();

LOGGER.info("Completed");

// End.
