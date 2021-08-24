# $Id: uuid.py 2772 2015-09-21 20:35:47Z SFB $

from org.rvpf.base import UUID
from java.awt import Toolkit
from java.awt.datatransfer import StringSelection
from javax.swing import JOptionPane
import sys, os

try: True
except:
    True = 1
    False = not True


def programName(): return os.path.splitext(os.path.split(sys.argv[0])[1])[0]


def paste(text):
    selection = StringSelection(text)
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
        selection, None)


def gui():

    while True:
        response = JOptionPane.showConfirmDialog(
            None, "Press 'OK' to generate, 'Cancel' to quit", "UUID generator",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
        if response != JOptionPane.OK_OPTION: break
        paste(UUID.generate().toString())


def usage():

    return "Usage: %s [gui | <count>]" % programName()


def main(argv):

    if len(argv) > 2: return usage()
    if len(argv) == 2:
        while argv[1][:1] == '-': argv[1] = argv[1][1:]
        if argv[1] == 'gui': return gui()
        try: count = int(argv[1])
        except: return usage()
    else: count = 1

    uuid = None
    for i in xrange(count):
        uuid = UUID.generate()
        print uuid

    if count == 1: paste(uuid.toString())

if __name__ == '__main__': sys.exit(main(sys.argv))


# End.
