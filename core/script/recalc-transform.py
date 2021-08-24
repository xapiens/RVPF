# $Id: recalc-transform.py 3849 2018-12-04 19:54:00Z SFB $

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
_RECALC_UUID = UUID.fromString('f5e64de4-9b34-47fc-b9f0-c76b514827f7')
_RECALC_QUEUE_PROPERTIES = 'recalc.queue'
_RECALC_QUEUE_NAME = 'Notifier'


class _Failed (Exception): pass


class _RecalcTransform:

    def __call__(self, args):

        self._parseArgs(args)

        self._setUp()

        for point in self._points: self._recalc(point)

        self._tearDown()

    def _parseArgs(self, args):

        if len(args) < 1 or 2 < len(args): raise _Failed(_usage())

        self._transformName = args[0]
        if len(args) > 1:
            since = args[1]
            if since[:1] == '-':
                since = str(DateTime.now().sub(
                    ElapsedTime.fromString(since[1:])))
            self._since = since.replace(' ', 'T')
        else: self._since = None

    def _recalc(self, point):

        query = StoreValuesQuery(point)
        query.notBefore = self._since
        query.all = True
        for pointValue in query.iterate():
            _LOGGER.trace(
                "Recalc ''{0}'' at {1}", point, pointValue.stamp)
            self._sender.send(
                RecalcTrigger(pointValue.point, pointValue.stamp))
        self._sender.commit()

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

        transform = metadata.getTransformEntity(
            Optional.of(self._transformName)).orElse(None)
        if transform is None:
            raise _Failed("Transform '%s' is unknown!"
                % self._transformName)
        _LOGGER.info("Transform: {0}", transform.name.get())

        self._points = []
        for point in metadata.pointsCollection:
            if point.transformEntity == transform:
                if not point.setUp(metadata):
                    raise _Failed(
                        "Failed to set up point '%s'!", point)
                self._points.append(point)
                _LOGGER.debug("Point: {0}", point.name.get())

        try: self._sender.connect()
        except QueueProxy.ConnectFailedException, e:
            raise _Failed("Failed to connect to recalc queue!")


    def _tearDown(self):

        self._sender.disconnect()
        for point in self._points: point.tearDown()


def _main(argv):

    try: _RecalcTransform()(sys.argv[1:])
    except _Failed, failure:
        _LOGGER.error("{0}", str(failure))
        return -1


def _usage():

    return "Usage: %s <transform> [<since>]" % _NAME


if __name__ == '__main__': sys.exit(_main(sys.argv))


# End.
