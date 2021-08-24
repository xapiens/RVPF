# $Id: script-service-tests.py 3840 2018-11-20 21:20:34Z SFB $

from org.rvpf.script import ScriptServiceAppImpl
from org.rvpf.base.logger import StringLogger
import sys, os

config = globals()[ScriptServiceAppImpl.CONFIG_ATTRIBUTE]

_NAME = 'script-service-tests'
_LOGGER = StringLogger.getInstance('org.rvpf.' + _NAME)

_LOGGER.debug("Jython engine version: {0}", globals()[ScriptServiceAppImpl.ENGINE_VERSION_PROPERTY])

def onStart():
    _LOGGER.debug("Start text execution ({0})", config.getServiceUUID().get())

def onRun():
    _LOGGER.debug("Run text execution ({0})", config.getServiceUUID().get())

def onStop():
    _LOGGER.debug("Stop text execution ({0})", config.getServiceUUID().get())

_LOGGER.debug("Script loaded ({0})", config.getServiceUUID().get())

# End.
