# $Id: index.py 1153 2009-08-01 10:36:31Z sfb $

from java.lang import String
from java.io import File
from java.sql import Timestamp
import sys, os

try: True
except:
    True = 1
    False = not True

_BEGIN_MARK = String("<!-- [Begin index] -->")
_END_MARK = String("<!-- [End index] -->")
_INDEX_FILE = String('index.html')


def _index(directory):

    entries = []
    for file in directory.listFiles():
        name = file.name
        if _INDEX_FILE.equalsIgnoreCase(name): continue

        stamp = str(Timestamp(file.lastModified())).split('.')[0]
        length = file.length()
        entries.append('<tr><td><a href="%(name)s">%(name)s</a></td><td>%(stamp)s</td><td>%(length)d</td></tr>' % {
            'name': name, 'stamp': stamp, 'length': length})

    return entries


def _main(argv):

    argv = argv[1:]
    if len(argv) != 1: return _usage()

    directory = File(argv[0])
    if not directory.isDirectory(): return _usage()

    indexFile = File(directory, _INDEX_FILE)
    if not indexFile.isFile():
        return "File %s is missing" % _INDEX_FILE

    input = open(indexFile.path)
    lines = []

    while True:
        line = input.readline()
        if not line: return "Begin mark not found"
        lines.append(line)
        if _BEGIN_MARK.equalsIgnoreCase(line.strip()): break

    while True:
        line = input.readline()
        if not line: return "End mark not found"
        if _END_MARK.equalsIgnoreCase(line.strip()): break

    indent = line[:line.index('<')]

    for entry in _index(directory):
        lines.append(indent + entry + '\n')

    while True:
        lines.append(line)
        line = input.readline()
        if not line: break

    input.close()

    output = open(indexFile.path, 'w')
    output.writelines(lines)
    output.close()


def _usage():

    name = os.path.splitext(os.path.split(sys.argv[0])[1])[0]

    return "Usage: %s <directory>" % name


if __name__ == '__main__': sys.exit(_main(sys.argv))


# End.
