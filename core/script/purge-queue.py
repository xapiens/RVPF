# $Id: purge-queue.py 2490 2015-01-07 21:00:17Z SFB $

from java.lang import System
import sys, os

_NAME = os.path.splitext(os.path.split(sys.argv[0])[1])[0]
System.setProperty('rvpf.log.prefix', _NAME)

from java.util.prefs import Preferences
from org.rvpf.base.logger import StringLogger
from org.rvpf.base.rmi import SessionProxy
from org.rvpf.base.som import QueueProxy, SOMServer
from org.rvpf.base.util.container import KeyedGroups
from org.rvpf.document.loader import ConfigDocumentLoader

try: True
except:
    True = 1
    False = not True

_LOGGER = StringLogger.getInstance('org.rvpf.' + _NAME)

_LOGIN_PREFS_PATH = '/org/rvpf/env/login'
_LOGIN_USER_KEY = 'user'
_LOGIN_PASSWORD_KEY = 'password'


def _main(argv):

    args = sys.argv[1:]
    if len(args) != 1: return _usage()
    queueName = args[0]

    config = ConfigDocumentLoader.loadConfig()
    if config is None: return "Failed to load config!"

    queueProperties = KeyedGroups()
    queueProperties.setValue(SOMServer.NAME_PROPERTY, queueName)
    receiver = QueueProxy.createReceiver(
        config.properties, queueProperties, _NAME, _LOGGER)
    if receiver is None:
        return "Failed to create a queue receiver!"

    node = Preferences.userRoot().node(_LOGIN_PREFS_PATH)
    receiver.setLoginInfo(
        node.get(_LOGIN_USER_KEY, None),
        node.get(_LOGIN_PASSWORD_KEY, ''))

    try: receiver.connect()
    except SessionProxy.ConnectFailedException, e:
        return ("Failed to connect to queue '%s' (%s)!"
            % (queueName, e.message))

    purged = receiver.purge()

    receiver.logout()

    _LOGGER.info("Purged {0} message(s)", purged)


def _usage():

    return "Usage: %s <queue-name>" % _NAME


if __name__ == '__main__': sys.exit(_main(sys.argv))


# End.
