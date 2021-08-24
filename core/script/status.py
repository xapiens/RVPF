# $Id: status.py 3873 2019-01-19 15:05:33Z SFB $

from java.lang import System
import sys, os

_NAME = os.path.splitext(os.path.split(sys.argv[0])[1])[0]
System.setProperty('rvpf.log.prefix', _NAME)

from java.io import BufferedReader, InputStreamReader
from java.lang import String, System
from java.net import Authenticator, ConnectException, HttpURLConnection
from java.net import PasswordAuthentication, URL, URLDecoder
from java.util import Optional
from java.util.prefs import Preferences
from java.util.regex import Pattern

from org.rvpf.base import DateTime
from org.rvpf.base.xml import XMLDocument
from org.rvpf.document.loader import ConfigDocumentLoader
from org.rvpf.http import AbstractServlet
from org.rvpf.http.alert import AlertModule, EventsServlet
from org.rvpf.http.alert import StatusServlet, TriggerServlet
from org.rvpf.service import Service

try: True
except:
    True = 1
    False = not True


def fullStamp(stamp):

    return DateTime.fromString(stamp).toFullString()


class _ClientAuthenticator (Authenticator):

    def __init__(self, user, password):

        self._user = user
        self._password = String(password).toCharArray()

    def getPasswordAuthentication(self):

        return PasswordAuthentication(self._user, self._password)


class _Failed (Exception): pass


_HTTP_ALERTER_HOST = 'http.alerter.host'
_HTTP_ALERTER_PORT = 'http.alerter.port'
_PREFS_PATH = '/org/rvpf/env/login'
_USER_KEY = 'user'
_PASSWORD_KEY = 'password'

_NAME_PATTERN = Pattern.compile(".*:type=([^,]+)(?:,name=([^,]+))?")


class _Event: pass


class _Service: pass


class _Status:

    def __init__(self, url):

        self._url = url

    def __call__(self, timeout):

        if timeout is None: self._fetchStatus()
        else: self._updateStatus(timeout)

        self._printStatus()

    def _fetchEvents(self, after, wait):

        request = "%s?%s=%s" % (
            AlertModule.EVENTS_PATH,
            EventsServlet.AFTER_PARAMETER,
            after)
        if wait is not None:
            request += "&%s=%s" % (EventsServlet.WAIT_PARAMETER, wait)

        rootElement = self._submit(request)
        self._last = self._stamp

        self._events = []
        for element in rootElement.getChildren(EventsServlet.EVENT_ELEMENT):
            event = _Event()
            event.name = element.getAttributeValue(
                EventsServlet.NAME_ATTRIBUTE, None)
            event.stamp = fullStamp(element.getAttributeValue(
                EventsServlet.STAMP_ATTRIBUTE, None))
            event.service = element.getAttributeValue(
                EventsServlet.SERVICE_ATTRIBUTE, None)
            event.uuid = element.getAttributeValue(
                EventsServlet.UUID_ATTRIBUTE, None)
            event.entity = element.getAttributeValue(
                EventsServlet.ENTITY_ATTRIBUTE, None)
            event.info = element.getAttributeValue(
                EventsServlet.INFO_ATTRIBUTE, None)
            self._events.append(event)

    def _fetchStatus(self):

        rootElement = self._submit(AlertModule.STATUS_PATH)

        self._status = []
        for element in rootElement.getChildren(StatusServlet.SERVICE_ELEMENT):
            service = _Service()
            service.name = element.getAttributeValue(
                StatusServlet.NAME_ATTRIBUTE, None)
            service.entity = element.getAttributeValue(
                StatusServlet.ENTITY_ATTRIBUTE, None)
            service.uuid = element.getAttributeValue(
                StatusServlet.UUID_ATTRIBUTE, None)
            service.event = element.getAttributeValue(
                StatusServlet.EVENT_ATTRIBUTE, None)
            service.stamp = fullStamp(element.getAttributeValue(
                StatusServlet.STAMP_ATTRIBUTE, None))
            self._status.append(service)

    def _handleRedirect(self, connection):

        print "#connection: " + str(connection)
        if (connection.responseCode == HttpURLConnection.HTTP_MOVED_TEMP
            or connection.responseCode == HttpURLConnection.HTTP_MOVED_PERM):
            connection = URL(connection.getHeaderField('Location')).openConnection()
            connection.useCaches = False
            connection = self._handleRedirect(connection)

        return connection

    def _ping(self):

        self._submit("%s?%s=%s" % (
            AlertModule.TRIGGER_PATH,
            TriggerServlet.SIGNAL_PARAMETER,
            Service.PING_SIGNAL))

    def _printStatus(self):

        print " "*31, self._stamp

        status = []
        for service in self._status:
            matcher = _NAME_PATTERN.matcher(String(service.name))
            if matcher.matches():
                name = matcher.group(2)
                if name is None: name = matcher.group(1)
            else: name = service.name
            status.append((name, service.stamp, service.event))

        status.sort()
        for service in status: print "%31s %-23s %s" % service

    def _statusUpdated(self, reference):

        for service in self._status:
            if DateTime.fromString(service.stamp).compareTo(reference) < 0:
                return False

        return True

    def _submit(self, request):

        request = "%s%s" % (AlertModule.DEFAULT_PATH, request)
        connection = URL(self._url, request).openConnection()
        connection.useCaches = False
        try:
            connection = self._handleRedirect(connection)
            if connection.responseCode != HttpURLConnection.HTTP_OK:
                raise _Failed, "Request '%s' to '%s' failed (%s)" % (
                    request, self._url,
                    URLDecoder.decode(connection.responseMessage, 'UTF-8'))
        except ConnectException, e:
            raise _Failed("Connection to '%s' failed (%s)" % (self._url, e.message))

        reader = BufferedReader(InputStreamReader(connection.inputStream))
        rootElement = XMLDocument().parse(reader)
        reader.close()

        self._stamp = rootElement.getAttributeValue(
            AbstractServlet.STAMP_ATTRIBUTE, None)

        return rootElement

    def _updateStatus(self, timeout):

        self._fetchEvents(EventsServlet.AFTER_LAST_VALUE, None)
        reference = DateTime.fromString(self._stamp)
        self._ping()
        while True:
            sys.stdout.write('.')
            sys.stdout.flush()
            elapsed = (System.currentTimeMillis() - reference.toMillis()) / 1000
            self._fetchEvents(self._last, timeout - elapsed)
            self._fetchStatus()
            if self._statusUpdated(reference): break
            if elapsed >= timeout:
                sys.stdout.write('!')
                break
        sys.stdout.write('\n')
        sys.stdout.flush()


def _main(argv):

    argv = argv[1:]
    if len(argv) > 1: return _usage()

    if len(argv) > 0:
        try: timeout = int(argv[0])
        except: return _usage()
    else: timeout = None

    config = ConfigDocumentLoader.loadConfig("", None, None)
    host = config.getStringValue(_HTTP_ALERTER_HOST, Optional.of('localhost')).orElse(None)
    port = config.getIntValue(_HTTP_ALERTER_PORT, 0)
    if port == 0: return "Failed to get the HTTP alerter port"
    url = URL("http://%s:%s/" % (host, port))

    node = Preferences.userRoot().node(_PREFS_PATH)
    user = node.get(_USER_KEY, None)
    if user is not None:
        password = node.get(_PASSWORD_KEY, '')
        Authenticator.setDefault(_ClientAuthenticator(user, password))

    try:
        try: _Status(url)(timeout)
        except _Failed, failure: return str(failure)
    finally:
        if user is not None:
            Authenticator.setDefault(None)


def _usage():

    return "Usage: %s [<ping timeout in seconds>]" % _NAME


if __name__ == '__main__': raise SystemExit(_main(sys.argv))


# End.
