# $Id: recalc-point.py 3849 2018-12-04 19:54:00Z SFB $

from java.lang import System
import sys, os

_NAME = os.path.splitext(os.path.split(sys.argv[0])[1])[0]
System.setProperty('rvpf.log.prefix', _NAME)

from java.lang import Thread
from java.util import Optional

from org.rvpf.base import ElapsedTime, DateTime, UUID, BaseVersion
from org.rvpf.base.logger import StringLogger
from org.rvpf.base.som import QueueProxy, SOMServer
from org.rvpf.base.store import StoreValuesQuery
from org.rvpf.base.value import RecalcTrigger
from org.rvpf.document.loader import ConfigDocumentLoader, MetadataDocumentLoader
from org.rvpf.http.metadata import MetadataServerFilter
from org.rvpf.service.rmi import ServiceRegistry

try: True
except:
    True = 1
    False = not True


_LOGGER = StringLogger.getInstance('org.rvpf.' + _NAME)
_RECALC_UUID = UUID.fromString('e1b92947-36b8-4ef9-a90f-697443388fce')
_RECALC_QUEUE_PROPERTIES = 'recalc.queue'
_RECALC_QUEUE_NAME = 'Notifier'


class _Failed (Exception): pass


class _RecalcPoint:

    def __call__(self, args):

        self._parseArgs(args)

        self._setUp()

        queryBuilder = StoreValuesQuery.newBuilder()
        queryBuilder.point = self._point
        queryBuilder.notBefore = self._since
        queryBuilder.all = True
        for pointValue in queryBuilder.build().iterate():
            _LOGGER.trace("Recalc at {0}", pointValue.stamp)
            self._sender.send(
                RecalcTrigger(pointValue.point, pointValue.stamp))
        self._sender.commit()

        self._tearDown()

    def _parseArgs(self, args):

        if len(args) < 1 or 2 < len(args): raise _Failed(_usage())

        self._pointName = args[0]
        if len(args) > 1:
            since = args[1]
            if since[:1] == '-':
                since = str(DateTime.now().sub(
                    ElapsedTime.fromString(since[1:])))
            self._since = DateTime.fromString(since)
            _LOGGER.info("Since: {0}", self._since)
        else: self._since = None

    def _setUp(self):

        BaseVersion().logSystemInfo(_NAME, True)

        config = ConfigDocumentLoader.loadConfig("", None, None)
        if config is None: raise _Failed("Failed to load config!")
        ServiceRegistry.setUp(config.getProperties())

        Thread.currentThread().contextClassLoader = config.classLoader
        propertiesName = config.getStringValue(_RECALC_QUEUE_PROPERTIES)

        if propertiesName.isPresent():
            queueProperties = config.getPropertiesGroup(propertiesName.get()).copy()
            queueProperties.setValue(SOMServer.NAME_PROPERTY, _RECALC_QUEUE_NAME)
        else: queueProperties = None

        senderBuilder = QueueProxy.Sender.newBuilder().prepare(
            config.properties, queueProperties, _NAME, _LOGGER.getLogger())
        if senderBuilder is None:
            raise _Failed("Failed to create recalc queue sender")
        self._sender = senderBuilder.build()

        metadata = MetadataDocumentLoader.fetchMetadata(
            MetadataServerFilter(), Optional.of(config), Optional.of(_RECALC_UUID), Optional.empty())
        if metadata is None: raise _Failed("Failed to load metadata!")

        point = metadata.getPointByName(self._pointName)
        if not point.isPresent:
            raise _Failed("Point '%s' is unknown!" % self._pointName)
        self._point = point.get()
        _LOGGER.info("Point: %s (%s)"
            % (self._point.name.get(), self._point.UUID.get()))
        if not self._point.setUp(metadata):
            raise _Failed("Failed to set up point!")

        try: self._sender.connect()
        except QueueProxy.ConnectFailedException, e:
            raise _Failed("Failed to connect to recalc queue!")

    def _tearDown(self):

        self._sender.disconnect()
        self._point.tearDown()


def _main(argv):

    try: _RecalcPoint()(sys.argv[1:])
    except _Failed, failure:
        _LOGGER.error("{0}", str(failure))
        return -1


def _usage():

    return "Usage: %s <point> [<since>]" % _NAME


if __name__ == '__main__': sys.exit(_main(sys.argv))


# End.
