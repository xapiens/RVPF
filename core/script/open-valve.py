# $Id: open-valve.py 2484 2014-12-29 18:13:06Z SFB $

from java.lang import *
from org.rvpf.document.loader import ConfigDocumentLoader
from org.rvpf.valve import Controller
import sys

try: True
except:
    True = 1
    False = not True


def _main(argv):

    config = ConfigDocumentLoader.loadConfig()
    controller = Controller(config)
    controller.open('valve')
    try: raw_input("Press enter to close the valve.")
    except: print
    controller.close()


if __name__ == '__main__': sys.exit(_main(sys.argv))


# End.
