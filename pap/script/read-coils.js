// $Id: read-coils.js 3880 2019-01-26 15:51:19Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');

var ConnectException = Java.type('java.net.ConnectException');

var Optional = Java.type('java.util.Optional');

var LogManager = Java.type('org.apache.logging.log4j.LogManager');

var Require = Java.type('org.rvpf.base.tool.Require');

var PAPContext = Java.type('org.rvpf.pap.PAPContext');
var PAPMetadataFilter = Java.type('org.rvpf.pap.PAPMetadataFilter');
var ModbusSupport = Java.type('org.rvpf.pap.modbus.ModbusSupport');

var ORIGIN = "Modbus-PLC.1";
var QUANTITY = 9500;
var START_ADDRESS = 1;
var BATCH = 1900;

var values = new Array(BATCH);

for (var i = 0; i < BATCH; ++i) {
    values[i] = i & 1;
}

var support = new ModbusSupport();

var metadata = Require.notNull(PAPContext.fetchMetadata(
    new PAPMetadataFilter(support.getAttributesUsage()),
    Optional.of("../script/modbus-client-metadata.xml"),
    Optional.of(support.getMetadataFilterUUID())));

var context = Require.notNull(support.newClientContext(metadata, Optional.empty()));

var client = Require.notNull(support.newClient(context));
var origin = context.getRemoteOrigin(Optional.of(ORIGIN)).get();

LOGGER.debug("Reads begin");

for (var address = START_ADDRESS; address < START_ADDRESS + QUANTITY; address += BATCH) {
    var request = client.readCoils(origin, address, BATCH).get();
    var response = request.getResponse();
    if (response == null) {
        fail("Failed: no response!", 2);
    }
    if (!response.isSuccess()) {
        fail("Failed: " + response.getExceptionCode() + "!", 2);
    }
    for (var i = 0; i < BATCH; ++i) {
        if (response.values[i] != values[i]) {
            print(response.values);
            print(values);
            fail("Unexpected values!")
        }
    }
}

LOGGER.debug("Reads ended");

client.disconnect();

// End.