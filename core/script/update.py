# $Id: update.py 2484 2014-12-29 18:13:06Z SFB $

from java.lang import System
import sys, os

_NAME = os.path.splitext(os.path.split(sys.argv[0])[1])[0]
System.setProperty('rvpf.log.prefix', _NAME)

from java.lang import String
from java.io import BufferedReader, InputStreamReader
from java.net import Authenticator, ConnectException, HttpURLConnection
from java.net import PasswordAuthentication, URL, URLDecoder
from java.util.prefs import Preferences
from org.rvpf.base import ElapsedTime, DateTime
from org.rvpf.base.xml import XMLDocument
from org.rvpf.document.loader import ConfigDocumentLoader
from org.rvpf.http.update import UpdateModule, UpdateServlet

try: True
except:
    True = 1
    False = not True


class _ClientAuthenticator (Authenticator):

    def __init__(self, user, password):

        self._user = user
        self._password = String(password).toCharArray()

    def getPasswordAuthentication(self):

        return PasswordAuthentication(self._user, self._password)


_HTTP_SERVER_PORT = 'http.server.port'
_PREFS_PATH = '/org/rvpf/env/login'
_USER_KEY = 'user'
_PASSWORD_KEY = 'password'


def _update(url, point, time, value):

    request = "%s%s?%s=%s&%s=%s" % (
        UpdateModule.DEFAULT_PATH, UpdateModule.ACCEPT_PATH,
        UpdateServlet.POINT_ATTRIBUTE, point,
        UpdateServlet.STAMP_ATTRIBUTE, time)
    if value is not None:
        request += "&%s=%s" % (
            UpdateServlet.VALUE_ATTRIBUTE, value)

    connection = URL(url, request).openConnection()
    connection.useCaches = False

    try:
        if connection.responseCode != HttpURLConnection.HTTP_OK:
            message = URLDecoder.decode(connection.responseMessage, 'UTF-8')
            return "Request '%s' to '%s' failed (%s)" % (request, url, message)
    except ConnectException, e:
        return "Connection to '%s' failed (%s)" % (url, e.message)

    reader = BufferedReader(InputStreamReader(connection.inputStream))
    rootElement = XMLDocument().parse(reader)
    reader.close()
    connection.disconnect()

    for response in rootElement.getChildren(UpdateServlet.RESPONSE_ELEMENT):
        for element in response.getChildren():
            if element.name == UpdateServlet.MESSAGE_ELEMENT:
                exception = element.getAttributeValue(UpdateServlet.EXCEPTION_ATTRIBUTE, None)
                return "Exception '%s': %s" % (exception, element.text)


def _main(argv):

    argv = argv[1:]
    if len(argv) != 3: return _usage()

    point = argv[0]
    time = argv[1]
    value = argv[2]

    if time == 'now':
        time = str(DateTime.now().floored(DateTime.SECOND))
    if time[:1] == '-':
        time = str(DateTime.now().floored(DateTime.SECOND).sub(ElapsedTime.fromString(time[1:])))
    time = time.replace(' ', 'T')
    if value == 'null': value = None

    config = ConfigDocumentLoader.loadConfig()
    port = config.getIntValue(_HTTP_SERVER_PORT, 0)
    if port == 0: return "Failed to get the HTTP server port"
    url = URL("http://localhost:%s/" % port)

    node = Preferences.userRoot().node(_PREFS_PATH)
    user = node.get(_USER_KEY, None)
    if user is not None:
        password = node.get(_PASSWORD_KEY, '')
        Authenticator.setDefault(_ClientAuthenticator(user, password))

    message = _update(url, point, time, value)

    if user is not None:
        Authenticator.setDefault(None)

    return message


def _usage():

    return "Usage: %s <point> <time> <value>" % _NAME


if __name__ == '__main__': raise SystemExit(_main(sys.argv))


# End.
