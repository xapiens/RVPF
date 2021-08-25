// $Id: make-registers.js 4115 2019-08-04 14:17:56Z SFB $

'use strict';

var FileWriter = Java.type('java.io.FileWriter');
var Optional = Java.type('java.util.Optional');

var UUID = Java.type('org.rvpf.base.UUID');
var XMLDocument = Java.type('org.rvpf.base.xml.XMLDocument');
var XMLElement = Java.type('org.rvpf.base.xml.XMLElement');

var ConfigDocumentLoader = Java.type('org.rvpf.document.loader.ConfigDocumentLoader');

var QUANTITY = 9600;
var NAME_PREFIX = "REGISTER.";
var ORIGIN = "Modbus-PLC.1";
var STORE = "ModbusTheStore";
var CONTENT = "Count"
var ADDRESS_ATTRIBUTE = "REGISTER_ADDRESS";
var START_ADDRESS = 1;
var OUTPUT = "config/service/local/rvpf-modbus-registers.xml";

var document = new XMLDocument("metadata");

document.setDocTypeStrings(ConfigDocumentLoader.DOCTYPE_STRINGS);

var root = document.getRootElement();

for (var register = 0; register < QUANTITY; ++register) {
    var point = new XMLElement("Point");

    point.setAttribute("name", NAME_PREFIX + (register + 1));
    point.setAttribute("uuid", UUID.generate().toString());
    point.setAttribute("origin", ORIGIN);
    point.setAttribute("store", STORE);
    point.setAttribute("content", CONTENT);

    var attributes = new XMLElement("attributes");

    attributes.setAttribute("usage", "MODBUS");

    var attribute = new XMLElement("attribute");

    attribute.setAttribute("name", ADDRESS_ATTRIBUTE);
    attribute.setAttribute("value", START_ADDRESS + register);
    attributes.addChild(attribute);

    point.addChild(attributes);
    root.addChild(point);
}

var writer = new FileWriter(OUTPUT);

document.toXML(Optional.empty(), true, writer);
writer.close();

// End.