# $Id: load-point-values.py 3097 2016-07-13 20:12:59Z SFB $

from java.lang import System
import sys, os

_NAME = os.path.splitext(os.path.split(sys.argv[0])[1])[0]
System.setProperty('rvpf.log.prefix', _NAME)

from java.io import FileInputStream, InputStreamReader
from java.nio.charset import Charset
from java.util.zip import GZIPInputStream
from org.rvpf.base.rmi import Session, SessionProxy, ConnectFailedException
from org.rvpf.base.store import Store, StoreSessionProxy
from org.rvpf.base.util import PointValuesLoader
from org.rvpf.base.logger import StringLogger
from org.rvpf.base.xml.streamer import PointValuesLoaderInput, Streamer
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


class _Load:

    def __call__(self, args):

        self._parseArgs(args)

        self._setUp()

        loader = PointValuesLoader(self._storeProxy, self._input)
        count = loader.call()
        self._storeProxy.logout()
        self._input.close()
        self._stream.close()
        _LOGGER.info("Loaded values: {0}", count)
        if count:
            _LOGGER.info("First version: {0}", self._input.firstVersion)
            _LOGGER.info("Last version: {0}", self._input.lastVersion)

    def _nextArg(self, args):

        if not args: raise _Failed(_usage())

        return args.pop(0)

    def _parseArgs(self, args):

        self._with = None
        self._from = None
        self._into = None
        self._user = None
        self._password = None

        while args:
            word = self._nextArg(args).upper()
            if self._with is None and word == 'WITH':
                self._with = self._nextArg(args)
            elif self._from is None and word == 'FROM':
                self._from = self._nextArg(args)
            elif self._into is None and word == 'INTO':
                self._into = self._nextArg(args)
            elif self._user is None and word == 'USER':
                self._user = self._nextArg(args)
            elif self._password is None and word == 'PASSWORD':
                self._password = self._nextArg(args)
            else: raise _Failed(_usage())

        if not self._from: raise _Failed(_usage())

    def _setUp(self):

        if self._with:
            if not self._with.count('.'): self._with += ".properties"
            System.setProperty(Config.RVPF_PROPERTIES, self._with)

        config = ConfigDocumentLoader.loadConfig()
        if not self._into: self._into = Store.DEFAULT_STORE_NAME
        configProperties = config.properties

        if not self._from.count('.'): self._from += ".xml"
        _LOGGER.debug("Input file: {0}", self._from)
        self._stream = FileInputStream(self._from)
        if self._from.lower().endswith(_GZ_FILE_EXT):
            self._stream = GZIPInputStream(self._stream)

        self._input = PointValuesLoaderInput(InputStreamReader(self._stream, _UTF_8))

        truststore = configProperties.getStringValue(
            _CLIENT_TRUSTSTORE_PROPERTY)
        if truststore:
            System.setProperty(_SSL_TRUSTSTORE_PROPERTY, truststore)
        self._storeProxy = StoreSessionProxy.create(
            None,
            None,
            self._into,
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


def _main(argv):

    args = sys.argv[1:]
    if not args: return _usage()
    if len(args) == 1 and args[0].upper() == 'HELP': return _usage()

    try: _Load()(args)
    except _Failed, failure: return str(failure)


def _usage():

    return (("Usage: %s [<properties>] <source> [<destination] [<user>] [<password>]" % _NAME)
            + "\n\t<properties>: WITH <resource-file>"
            + "\n\t<source>: FROM <file-path>"
            + "\n\t<destination>: INTO <server-path>"
            + "\n\t<user>: USER <user-id>"
            + "\n\t<password>: PASSWORD <user-password>")


if __name__ == '__main__': sys.exit(_main(sys.argv))


# End.
