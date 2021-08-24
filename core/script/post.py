# $Id: post.py 355 2006-03-28 02:14:43Z SFB $

from java.lang import *
from java.io import BufferedReader, InputStreamReader
from java.io import IOException, OutputStreamWriter
from java.net import Authenticator, HttpURLConnection
from java.net import PasswordAuthentication, URL, URLDecoder
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


def post(url, filePath):

    try: file = open(filePath)
    except IOError, e: return e
    text = file.read()
    file.close()

    connection = url.openConnection()
    connection.doOutput = True
    connection.useCaches = False
    connection.requestMethod = 'POST'
    connection.setRequestProperty('CONTENT_LENGTH', str(len(text)))
    try : writer = OutputStreamWriter(connection.outputStream)
    except IOException, e:
        return "Connection to '%s' failed (%s)" % (url, e.message)
    writer.write(text)
    writer.close()

    if connection.responseCode != HttpURLConnection.HTTP_OK:
        message = URLDecoder.decode(connection.responseMessage, 'UTF-8')
        return "POST to '%s' failed (%s)" % (url, message)

    reader = BufferedReader(InputStreamReader(connection.inputStream))
    while True:
        line = reader.readLine()
        if line is None: break
        print line
    reader.close()
    connection.disconnect()


def main(argv):

    argv = argv[1:]
    if len(argv) < 2 or 4 < len(argv): return usage()

    url = URL(URL('http://localhost:80/'), argv[0])
    filePath = argv[1]
    if len(argv) >= 3: user = argv[2]
    else: user = None
    if len(argv) >= 4: password = argv[3]
    else: password = ""

    if user is not None:
        Authenticator.setDefault(ClientAuthenticator(user, password))

    result = post(url, filePath)

    if user is not None:
        Authenticator.setDefault(None)

    return result


def usage():

    name = os.path.splitext(os.path.split(sys.argv[0])[1])[0]

    return "Usage: %s <url> <file> [<user> <password>]" % name


if __name__ == '__main__': raise SystemExit(main(sys.argv))


# End.
