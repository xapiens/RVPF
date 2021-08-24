# $Id: script-sink-tests.py 3840 2018-11-20 21:20:34Z SFB $

from java.lang import Integer
from org.rvpf.base.logger import StringLogger
from org.rvpf.store.server.sink import ScriptSink
import sys, os

try: True
except:
    True = 1
    False = not True

metadata = globals()[ScriptSink.METADATA_ATTRIBUTE]

_NAME = 'script-sink-tests'
_LOGGER = StringLogger.getInstance('org.rvpf.' + _NAME)

_LOGGER.debug("Jython engine version: {0}", globals()[ScriptSink.ENGINE_VERSION_PROPERTY])

def deleteValue():

    _LOGGER.debug("Delete value execution ({0})", metadata.getServiceUUID().get())
    pointValue = globals()[ScriptSink.UPDATE_ARG_ATTRIBUTE]
    assert pointValue is not None
    _LOGGER.debug("Point value: {0}", pointValue)
    globals()[ScriptSink.UPDATE_COUNT_ATTRIBUTE] = Integer(1)

def purgeValues():

    _LOGGER.debug("Purge values execution ({0})", metadata.getServiceUUID().get())
    points = globals()[ScriptSink.UPDATE_ARG_ATTRIBUTE]
    assert points is not None
    _LOGGER.debug("Points: {0}", points)
    globals()[ScriptSink.UPDATE_COUNT_ATTRIBUTE] = Integer(points.size())

def updateValue():

    _LOGGER.debug("Update value execution ({0})", metadata.getServiceUUID().get())
    pointValue = globals()[ScriptSink.UPDATE_ARG_ATTRIBUTE]
    assert pointValue is not None
    _LOGGER.debug("Point value: {0}", pointValue)
    globals()[ScriptSink.UPDATE_COUNT_ATTRIBUTE] = Integer(1)

_LOGGER.debug("Script loaded ({0})", metadata.getServiceUUID().get())

# End.
