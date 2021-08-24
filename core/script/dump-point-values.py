# $Id: dump-point-values.py 3097 2016-07-13 20:12:59Z SFB $

from java.lang import System
import sys, os

_NAME = os.path.splitext(os.path.split(sys.argv[0])[1])[0]
System.setProperty('rvpf.log.prefix', _NAME)

from java.io import FileOutputStream, OutputStreamWriter
from java.nio.charset import Charset
from java.util.zip import GZIPOutputStream
from org.rvpf.base import DateTime, ElapsedTime, TimeInterval
from org.rvpf.base import Point, PointBinding, Points, UUID
from org.rvpf.base.logger import StringLogger
from org.rvpf.base.rmi import Session, SessionProxy, ConnectFailedException
from org.rvpf.base.store import Store, StoreSessionProxy, StoreValuesQuery
from org.rvpf.base.util import PointValuesDumper
from org.rvpf.base.xml.streamer import PointValuesDumperOutput, Streamer
from org.rvpf.config import Config
from org.rvpf.document.loader import ConfigDocumentLoader

try: True
except:
    True = 1
    False = not True

_LOGGER = StringLogger.getInstance('org.rvpf.' + _NAME)

_CLIENT_TRUSTSTORE_PROPERTY = 'client.truststore'
_SSL_TRUSTSTORE_PROPERTY = 'javax.net.ssl.trustStore'
_UTF_8 = Charset.forName('UTF-8')
_GZ_FILE_EXT = '.gz'


class _Failed (Exception): pass


class _Dump:

    def __call__(self, args):

        self._parseArgs(args)

        self._setUp()

        query = StoreValuesQuery(self._point)
        query.pull = self._pull
        query.synced = self._synced
        query.interval = self._interval
        query.all = True
        dumper = PointValuesDumper(
            query, self._storeProxy, self._points, self._output)
        self._count = dumper.call()

        self._tearDown()

    def _nextArg(self, args):

        if not args: raise _Failed(_usage())

        return args.pop(0)

    def _parseArgs(self, args):

        self._now = DateTime.now()
        self._with = None
        self._from = None
        self._user = None
        self._password = None
        self._into = None
        self._point = None
        self._pull = False
        self._synced = False
        self._interval = None

        while args:
            word = self._nextArg(args).upper()
            if self._with is None and word == 'WITH':
                self._with = self._nextArg(args)
            elif not self._pull and word == 'PULL':
                self._pull = True
            elif not self._synced and word == 'SYNCED':
                self._synced = True
            elif not self._point and word == 'POINT':
                self._point = self._nextArg(args)
            elif not self._from and word == 'FROM':
                self._from = self._nextArg(args)
            elif self._user is None and word == 'USER':
                self._user = self._nextArg(args)
            elif self._password is None and word == 'PASSWORD':
                self._password = self._nextArg(args)
            elif not self._into and word == 'INTO':
                self._into = self._nextArg(args)
            elif not self._interval and word == 'ALL':
                self._interval = TimeInterval()
            else:
                isNot = word == 'NOT'
                if isNot: word = self._nextArg(args).upper()
                dateTime = self._parseDateTime(args)
                if self._interval is None: self._interval = TimeInterval()
                if word == 'AFTER':
                    if isNot: self._interval.notAfter = dateTime
                    else: self._interval.after = dateTime
                elif word == 'BEFORE':
                    if isNot: self._interval.notBefore = dateTime
                    else: self._interval.before = dateTime
                else: raise _Failed(_usage())

    def _parseDateTime(self, args):

        arg = self._nextArg(args)
        if args and not args[0].isalpha():
            arg += ' ' + self._nextArg(args)
        try:
            if arg[:1] != '-': time = DateTime.now().valueOf(arg)
            else: time = self._now.before(ElapsedTime.fromString(arg[1:]))
        except: raise _Failed(_usage())

        return time

    def _setUp(self):

        if self._with:
            if not self._with.count('.'): self._with += ".properties"
            System.setProperty(Config.RVPF_PROPERTIES, self._with)

        config = ConfigDocumentLoader.loadConfig()
        None,
        if not self._from: self._from = Store.DEFAULT_STORE_NAME
        configProperties = config.properties
        truststore = configProperties.getStringValue(
            _CLIENT_TRUSTSTORE_PROPERTY)
        if truststore:
            System.setProperty(_SSL_TRUSTSTORE_PROPERTY, truststore)
        self._storeProxy = StoreSessionProxy.create(
            None,
            None,
            self._from,
            None,
            config.getIntValue(Session.REGISTRY_PORT_PROPERTY, -1),
            configProperties,
            None,
            self._user,
            self._password,
            _NAME,
            _LOGGER)

        if self._storeProxy is None:
            raise _Failed(_usage())
        try: self._storeProxy.connect()
        except ConnectFailedException, e:
            raise _Failed(e.message)

        if self._point:
            if UUID.isUUID(self._point):
                self._point = UUID.fromString(self._point)
            else:
                _LOGGER.debug("Point name: {0}", self._point)
                bindingsRequest = PointBinding.Request()
                bindingsRequest.selectName(self._point)
                pointsBindings = self._storeProxy.getPoints(
                    bindingsRequest)
                if not pointsBindings:
                    raise _Failed("Point '%s' is unknown" % self._point)
                self._point = Point.Named(
                    pointsBindings[0].name, pointsBindings[0].UUID)
            _LOGGER.debug("Point UUID: {0}", self._point)
            self._points = None
        else:
            bindingsRequest = PointBinding.Request()
            pointBindings = self._storeProxy.getPointBindings(bindingsRequest)
            self._points = Points.Impl(pointBindings)
            self._pull = True
            self._synced = False

        if self._interval: _LOGGER.debug("Interval: {0}" % self._interval)

        if not self._into: self._stream = System.out
        else:
            if not self._into.count('.'): self._into += ".xml"
            _LOGGER.debug("Output file: {0}", self._into)
            self._stream = FileOutputStream(self._into)
            if self._into.lower().endswith(_GZ_FILE_EXT):
                self._stream = GZIPOutputStream(self._stream)

        self._output = PointValuesDumperOutput(
            OutputStreamWriter(self._stream, _UTF_8))

    def _tearDown(self):

        self._output.close()
        self._stream.close()
        self._storeProxy.logout()
        _LOGGER.info("Dumped values: {0}", self._count)
        if self._count:
            _LOGGER.info("First version: {0}", self._output.firstVersion)
            _LOGGER.info("Last version: {0}", self._output.lastVersion)


def _main(argv):

    args = sys.argv[1:]
    if len(args) == 1 and args[0].upper() == 'HELP': return _usage()

    try: _Dump()(args)
    except _Failed, failure:
        _LOGGER.error("{0}", str(failure))
        return -1


def _usage():

    return (("Usage: %s [<properties>] [<point>] [<source>] [<user>] [<password>] [<destination] [<interval>]" % _NAME)
            + "\n\t<properties>: WITH <resource-file>"
            + "\n\t<point>: [PULL|SYNCED] POINT (<name>|<uuid>)"
            + "\n\t<source>: FROM <server-path>"
            + "\n\t<user>: USER <user-id>"
            + "\n\t<password>: PASSWORD <user-password>"
            + "\n\t<destination>: INTO <file-path>"
            + "\n\t<interval>: ALL|([<start>] [<stop>])"
            + "\n\t<start>: (AFTER|(NOT BEFORE)) <date-time>"
            + "\n\t<stop>: (BEFORE|(NOT AFTER)) <date-time>")


if __name__ == '__main__': sys.exit(_main(sys.argv))


# End.
