# $Id: values.py 2484 2014-12-29 18:13:06Z SFB $

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
from org.rvpf.http.query import QueryModule, ValuesServlet

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


def _values(url, point, since):

    request = "%s%s?%s=%s&%s=%s&%s=no" % (
        QueryModule.DEFAULT_PATH, QueryModule.VALUES_PATH,
        ValuesServlet.POINT_ATTRIBUTE, point,
        ValuesServlet.COUNT_ATTRIBUTE, -1,
        ValuesServlet.REVERSE_ATTRIBUTE)
    if since is not None:
        request += "&%s=%s" % (
            ValuesServlet.NOT_BEFORE_ATTRIBUTE, since)

    connection = URL(url, request).openConnection()
    connection.useCaches = False

    try:
        if connection.responseCode != HttpURLConnection.HTTP_OK:
            message = URLDecoder.decode(connection.responseMessage, 'UTF-8')
            return "Request '%s' to '%s' failed (%s)" % (request, url, message)
    except ConnectException, e:
        return "Connection to '%s' failed (%s)" % (url, e.message)

    reader = BufferedReader(InputStreamReader(connection.inputStream))
    document = XMLDocument().parse(reader)
    reader.close()
    connection.disconnect()

    for response in document.rootElement.getChildren(ValuesServlet.RESPONSE_ELEMENT):
        print "Point:", response.getAttribute(ValuesServlet.POINT_ATTRIBUTE)
        for element in response.getChildren():
            if element.name == ValuesServlet.VALUE_ELEMENT:
                stamp = element.getAttribute(ValuesServlet.STAMP_ATTRIBUTE)
                state = element.getAttribute(ValuesServlet.STATE_ATTRIBUTE)
                if state is not None: state = "[%s] " % str(state)
                else: state = ""
                value = element.getAttribute(ValuesServlet.VALUE_ATTRIBUTE)
                if value is None:
                    value = element.text
                    if len(value) == 0: value = None
                if value is None: value = 'null'
                else: value = '"' + value + '"'
                if value is not None: value = str(value)
                else: value = 'null'
                print "\tValue at %s: %s%s" % (stamp, state, value)
            elif element.name == ValuesServlet.MARK_ELEMENT:
                stamp = element.getAttribute(ValuesServlet.STAMP_ATTRIBUTE)
                print "\tMark at", stamp
            elif element.name == ValuesServlet.MESSAGE_ELEMENT:
                exception = element.getAttribute(ValuesServlet.EXCEPTION_ATTRIBUTE)
                print "\tException '%s':" % exception, element.text


def _main(argv):

    argv = argv[1:]
    if len(argv) < 1 or 2 < len(argv): return _usage()

    point = argv[0]
    if len(argv) > 1:
        since = argv[1]
        if since[:1] == '-':
            since = str(DateTime.now().sub(ElapsedTime.fromString(since[1:])))
        since = since.replace(' ', 'T')
    else: since = None

    config = ConfigDocumentLoader.loadConfig()
    port = config.getIntValue(_HTTP_SERVER_PORT, 0)
    if port == 0: return "Failed to get the HTTP server port"
    url = URL("http://localhost:%s/" % port)

    node = Preferences.userRoot().node(_PREFS_PATH)
    user = node.get(_USER_KEY, None)
    if user is not None:
        password = node.get(_PASSWORD_KEY, '')
        Authenticator.setDefault(_ClientAuthenticator(user, password))

    message = _values(url, point, since)

    if user is not None:
        Authenticator.setDefault(None)

    return message


def _usage():

    return "Usage: %s <point> [<since>]" % _NAME


if __name__ == '__main__': raise SystemExit(_main(sys.argv))


# End.
