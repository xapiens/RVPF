// $Id: script-sink-tests.js 3840 2018-11-20 21:20:34Z SFB $

'use strict';

var ScriptContext = Java.type("javax.script.ScriptContext");
var ScriptSink = Java.type("org.rvpf.store.server.sink.ScriptSink");
var LogManager = Java.type("org.apache.logging.log4j.LogManager");

var metadata = this[ScriptSink.METADATA_ATTRIBUTE];

var _NAME = "script-sink-tests";
var _LOGGER = LogManager.getLogger("org.rvpf." + _NAME);

_LOGGER.debug("JavaScript engine version: " + this[ScriptSink.ENGINE_VERSION_PROPERTY]);

var global = this;

function assert(assertion)
{
    if (!assertion) throw new AssertionError();
}

function deleteValue()
{
    _LOGGER.debug("Delete value execution (" + metadata.getServiceUUID().get().toString() + ")");

    var pointValue = global[ScriptSink.UPDATE_ARG_ATTRIBUTE];

    assert(pointValue != null);
    _LOGGER.debug("Point value: " + pointValue);
    global[ScriptSink.UPDATE_COUNT_ATTRIBUTE] = 1;
}

function purgeValues()
{
    _LOGGER.debug("Purge values execution (" + metadata.getServiceUUID().get().toString() + ")");

    var points = global[ScriptSink.UPDATE_ARG_ATTRIBUTE];

    assert(points != null);
    _LOGGER.debug("Points: " + points);
    global[ScriptSink.UPDATE_COUNT_ATTRIBUTE] = points.size();
}

function updateValue()
{
    _LOGGER.debug("Update value execution (" + metadata.getServiceUUID().get().toString() + ")");

    var pointValue = global[ScriptSink.UPDATE_ARG_ATTRIBUTE];

    assert(pointValue != null);
    _LOGGER.debug("Point value: " + pointValue);
    global[ScriptSink.UPDATE_COUNT_ATTRIBUTE] = 1;
}

_LOGGER.debug("Script loaded (" + metadata.getServiceUUID().get().toString() + ")");

// End.
