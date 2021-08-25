// $Id: test-modbus-client.js 3880 2019-01-26 15:51:19Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');

var Float = Java.type('java.lang.Float');
var Integer = Java.type('java.lang.Integer');
var Math = Java.type('java.lang.Math');
var Optional = Java.type('java.util.Optional');

var Require = Java.type('org.rvpf.base.tool.Require');
var Traces = Java.type('org.rvpf.base.tool.Traces');
var PointValue = Java.type('org.rvpf.base.value.PointValue');

var PAPContext = Java.type('org.rvpf.pap.PAPContext');
var PAPMetadataFilter = Java.type('org.rvpf.pap.PAPMetadataFilter');
var ModbusSupport = Java.type('org.rvpf.pap.modbus.ModbusSupport');

var ORIGIN = "Modbus-PLC.1";
var COIL_ADDRESS = 1;
var REGISTER_ADDRESS = 1;
var COIL_NAME = "COIL";
var WORD_REGISTER_NAME = "WORD-REGISTER";
var FLOAT_REGISTER_NAME = "FLOAT-REGISTER";

var support = new ModbusSupport();

var metadata = Require.notNull(PAPContext.fetchMetadata(
    new PAPMetadataFilter(support.getAttributesUsage()),
    Optional.of("../../script/test-modbus-client-metadata.xml"),
    Optional.of(support.getMetadataFilterUUID())));

var traces = new Traces();

Require.trueValue(traces.setUp(
    new File("data/traces/test-modbus-client/protocol"),
    metadata.getProperties().getGroup(Traces.TRACES_PROPERTIES),
    support.getMetadataFilterUUID(),
    Optional.of(NAME)));

var context = Require.notNull(support.newClientContext(metadata, Optional.of(traces)));

var client = Require.notNull(support.newClient(context));
var origin = context.getRemoteOrigin(Optional.of(ORIGIN)).get();

Require.trueValue(client.connect(origin));

var request;
var response;
var values;
var value;
var pointValue;

request = client.writeSingleRegister(origin, REGISTER_ADDRESS, 1234).get();
response = Require.notNull(request.getResponse());
Require.trueValue(response.isSuccess());

request = client.readHoldingRegisters(origin, REGISTER_ADDRESS, 1).get();
response = Require.notNull(request.getResponse());
Require.trueValue(response.isSuccess());
values = response.getValues();
Require.trueValue(values.length == 1);
Require.trueValue(values[0] == 1234);

request = client.writeSingleRegister(origin, REGISTER_ADDRESS, 4321).get();
response = Require.notNull(request.getResponse());
Require.trueValue(response.isSuccess());

request = client.readHoldingRegisters(origin, REGISTER_ADDRESS, 1).get();
response = Require.notNull(request.getResponse());
Require.trueValue(response.isSuccess());
values = response.getValues();
Require.trueValue(values.length == 1);
Require.trueValue(values[0] == 4321);

request = client.readCoils(origin, COIL_ADDRESS, 1).get();
response = Require.notNull(request.getResponse());
Require.trueValue(response.isSuccess());
values = response.getValues();
Require.trueValue(values.length == 1);
Require.trueValue(values[0] == 0 || values[0] == 1);
value = values[0] == 0? 1: 0;

request = client.writeSingleCoil(origin, COIL_ADDRESS, value).get();
response = Require.notNull(request.getResponse());
Require.trueValue(response.isSuccess());

request = client.readCoils(origin, COIL_ADDRESS, 1).get();
response = Require.notNull(request.getResponse());
Require.trueValue(response.isSuccess());
values = response.getValues();
Require.trueValue(values.length == 1);
Require.trueValue(values[0] == value);

var register = metadata.getPoint(WORD_REGISTER_NAME).get();

pointValue = new PointValue(register, Optional.empty(), null, Integer.valueOf(1234));
request = client.writePointValue(pointValue).get();
response = Require.notNull(request.getResponse());
Require.trueValue(response.isSuccess());

request = client.requestPointValue(register).get();
response = Require.notNull(request.getResponse());
Require.trueValue(response.isSuccess());
pointValue = response.getPointValue().get();
Require.trueValue(pointValue.getValue().intValue() == 1234);

var coil = metadata.getPoint(COIL_NAME).get();

request = client.requestPointValue(coil).get();
response = Require.notNull(request.getResponse());
Require.trueValue(response.isSuccess());
pointValue = response.getPointValue().get();
value = !pointValue.getValue().booleanValue();

pointValue = new PointValue(coil, Optional.empty(), null, value);
request = client.writePointValue(pointValue).get();
response = Require.notNull(request.getResponse());
Require.trueValue(response.isSuccess());

request = client.requestPointValue(coil).get();
response = Require.notNull(request.getResponse());
Require.trueValue(response.isSuccess());
pointValue = response.getPointValue().get();
Require.trueValue(pointValue.getValue().equals(value));

register = metadata.getPoint(FLOAT_REGISTER_NAME).get();

value = Float.valueOf(5678.9);
pointValue = new PointValue(register, Optional.empty(), null, value);
request = client.writePointValue(pointValue).get();
response = Require.notNull(request.getResponse());
Require.trueValue(response.isSuccess());

request = client.requestPointValue(register).get();
response = Require.notNull(request.getResponse());
Require.trueValue(response.isSuccess());
pointValue = response.getPointValue().get();
Require.trueValue(Math.abs(pointValue.getValue().floatValue() - value) <= Math.ulp(value));
client.disconnect();

LOGGER.info("Completed.");

// End.
