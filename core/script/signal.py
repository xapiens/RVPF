# $Id: signal.py 1944 2013-10-16 12:38:47Z SFB $

from java.lang import String
from java.io import BufferedReader, InputStreamReader
from java.net import Authenticator, HttpURLConnection
from java.net import PasswordAuthentication, URL, URLDecoder
from org.rvpf.base.xml import XMLDocument
import sys, os

try: True
except:
    True = 1
    False = not True


class ClientAuthenticator (Authenticator):

    def __init__(self, user, password):

        self._user = user
        self._password = String(password).toCharArray()

    def getPasswordAuthentication(self):

        return PasswordAuthentication(self._user, self._password)


class Failed (Exception): pass


def _getServices(url):

    connection = URL(url, '/alert/status').openConnection()
    connection.useCaches = False
    if connection.responseCode != HttpURLConnection.HTTP_OK:
        message = URLDecoder.decode(connection.responseMessage, 'UTF-8')
        raise Failed, "Get status from '%s' failed (%s)" % (connection.getURL(), message)

    reader = BufferedReader(InputStreamReader(connection.inputStream))
    root = XMLDocument().parse(reader)
    reader.close()
    connection.disconnect()

    services = []
    for service in root.getChildren('service'):
        services.append((
            service.getAttribute('name'),
            service.getAttribute('uuid'),
            service.getAttribute('event'),
            service.getAttribute('stamp'),
            service.getAttribute('entity')))

    return services

def _resolveTargets(targets, services):

    namesMap = {}
    uuids = []
    for (name, uuid, event, stamp, entity) in services:
        namesMap[name] = uuid
        uuids.append(uuid)

    resolved = []
    for target in targets:
        uuid = namesMap.get(target)
        if uuid is not None: resolved.append(uuid)
        elif target in uuids: resolved.append(target)
        else: raise Failed, "Target '%s' is unknown" % target

    return resolved

def _sendSignal(url, signal, targets):

    if targets:
        for target in targets:
            connection = URL(url, '/alert/trigger?signal=%s&target=%s' % (signal, target)).openConnection()
            if connection.responseCode != HttpURLConnection.HTTP_OK:
                message = URLDecoder.decode(connection.responseMessage, 'UTF-8')
                raise Failed, "Signal '%s' to '%s' on '%s' failed (%s)" % (signal, target, connection.getURL(), message)
    else:
        connection = URL(url, '/alert/trigger?signal=%s' % signal).openConnection()
        if connection.responseCode != HttpURLConnection.HTTP_OK:
            message = URLDecoder.decode(connection.responseMessage, 'UTF-8')
            raise Failed, "Broadcast of signal '%s' on '%s' failed (%s)" % (signal, connection.getURL(), message)

def _main(argv):

    argv = argv[1:]
    if len(argv) < 3: return _usage()

    url = URL(URL('http://localhost:80/'), argv[0])
    user = argv[1]
    password = argv[2]
    if len(argv) > 3:
        signal = argv[3]
        targets = []
        for target in argv[4:]:
            targets.append(target)
    else: signal = None

    try:
        Authenticator.setDefault(ClientAuthenticator(user, password))
        try:
            if signal is None or targets:
                services = _getServices(url)

            if signal is None:
                for service in services: print service
                return
            if targets: targets = _resolveTargets(targets, services)

            result = _sendSignal(url, signal, targets)
        finally:
            Authenticator.setDefault(None)
    except Failed, failure: return str(failure)

def _usage():

    name = os.path.splitext(os.path.split(sys.argv[0])[1])[0]

    return "Usage: %s <url> <user> <password> [<signal> [<target> ...]]" % name


if __name__ == '__main__': sys.exit(_main(sys.argv))


# End.
