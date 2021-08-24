// $Id: script-service-tests.js 3840 2018-11-20 21:20:34Z SFB $

'use strict';

var ScriptServiceAppImpl = Java.type("org.rvpf.script.ScriptServiceAppImpl");
var LogManager = Java.type("org.apache.logging.log4j.LogManager");

var config = this[ScriptServiceAppImpl.CONFIG_ATTRIBUTE];

var _NAME = "script-service-tests";
var _LOGGER = LogManager.getLogger("org.rvpf." + _NAME);

_LOGGER.debug("JavaScript engine version: " + this[ScriptServiceAppImpl.ENGINE_VERSION_PROPERTY]);

function onStart()
{
    _LOGGER.debug("Start text execution (" + config.getServiceUUID().get().toString() + ")");
}

function onRun()
{
    _LOGGER.debug("Run text execution (" + config.getServiceUUID().get().toString() + ")");
}

function onStop()
{
    _LOGGER.debug("Stop text execution (" + config.getServiceUUID().get().toString() + ")");
}

_LOGGER.debug("Script loaded (" + config.getServiceUUID().get().toString() + ")");

// End.
