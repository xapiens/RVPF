# $Id: trigger.py 3872 2019-01-18 15:23:32Z SFB $

from java.lang import System
import sys, os

_NAME = os.path.splitext(os.path.split(sys.argv[0])[1])[0]
System.setProperty('rvpf.log.prefix', _NAME)

from java.lang import InterruptedException, Thread
from java.util import Optional

from org.rvpf.base import UUID
from org.rvpf.base.alert import Signal
from org.rvpf.base.logger import StringLogger
from org.rvpf.base.util.container import KeyedGroups
from org.rvpf.document.loader import ConfigDocumentLoader
from org.rvpf.service import Alerter
from org.rvpf.service.rmi import ServiceRegistry

try: True
except:
    True = 1
    False = not True

_LOGGER = StringLogger.getInstance('org.rvpf.' + _NAME)
_UUID = UUID.fromString('e4dd05c5e3cf1947a8c848a86f6b7f83')

class _Failed (Exception): pass

class _UncaughtExceptionHandler(Thread.UncaughtExceptionHandler):

    def uncaughtException(t, e): pass


def _main(argv):

    argv = argv[1:]
    if len(argv) != 1: return _usage()

    signalName = argv[0]
    if signalName == 'refresh': signalName = 'RefreshMetadata'
    elif signalName == 'reload': signalName = 'ReloadMetadata'
    elif signalName == 'resume': signalName = 'Resume'
    elif signalName == 'stop': signalName = 'Stop'
    elif signalName == 'suspend': signalName = 'Suspend'
    elif signalName == 'update': signalName = 'UpdateDocument'
    else: return _usage()

    config = ConfigDocumentLoader.loadConfig("", None, None)
    if config is None: raise _Failed("Failed to load config!")
    if not ServiceRegistry.setUp(config.getProperties()):
        raise _Failed("Failed to set up the service registry!")
    alerter = Alerter.Factory.getAnAlerter(config, _UncaughtExceptionHandler())
    if alerter is None: raise _Failed("Failed to get an alerter!")
    alerter.start()
    try:
        alerter.send(Signal(signalName, Optional.of(_NAME), Optional.empty(), Optional.of(_UUID), Optional.empty()))
    except InterruptedException, ie: raise _Failed(ie)
    alerter.tearDown()


def _usage():

    return "Usage: %s <refresh|reload|resume|stop|suspend|update>" % _NAME


if __name__ == '__main__': sys.exit(_main(sys.argv))


# End.
