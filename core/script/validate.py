# $Id: validate.py 2490 2015-01-07 21:00:17Z SFB $

from java.lang import System
import sys, os

_NAME = os.path.splitext(os.path.split(sys.argv[0])[1])[0]
System.setProperty('rvpf.log.prefix', _NAME)

from java.lang import System, Thread
from org.rvpf.base import UUID
from org.rvpf.base.logger import StringLogger
from org.rvpf.document.loader import ConfigDocumentLoader
from org.rvpf.document.loader import MetadataDocumentLoader
from org.rvpf.document.loader import MetadataCache
from org.rvpf.http.metadata import MetadataServerFilter
from org.rvpf.config import Config

try: True
except:
    True = 1
    False = not True

_LOGGER = StringLogger.getInstance('org.rvpf.' + _NAME)
_VALIDATE_UUID = UUID.fromString('364aa152-5b6c-4867-96a3-eb52e86ce869')


def _main(argv):

    argv = argv[1:]
    if len(argv) != 0: return "Usage: %s" % _NAME

    System.setProperty(
        Config.SYSTEM_PROPERTY_PREFIX
            + MetadataCache.METADATA_SERVER_PROPERTY,
        '')
    config = ConfigDocumentLoader.loadConfig()
    if config is None:
        _LOGGER.warn("Failed to load config!")
        return -1

    Thread.currentThread().contextClassLoader = config.classLoader

    metadata = MetadataDocumentLoader.fetchMetadata(
        MetadataServerFilter(), config, _VALIDATE_UUID)
    if metadata is None:
        _LOGGER.warn("Failed to load metadata!")
        return -2

    if not metadata.validatePointsRelationships():
        _LOGGER.warn("Failed to validate points relationships!")
        return -3

    _LOGGER.info("OK")


if __name__ == '__main__': sys.exit(_main(sys.argv))


# End.
