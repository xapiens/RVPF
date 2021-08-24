# $Id: raw-date-time.py 709 2007-09-18 14:35:57Z SFB $

from java.lang import *
from org.rvpf.base import DateTime
import sys, os, string

def main(argv):

    if len(argv) < 2 or 3 < len(argv): return usage()
    now = DateTime.now()
    try: then = now.valueOf(string.join(argv[1:3]))
    except IllegalArgumentException, e:
        print e.message
        return 1

    print "%s: %s" % (then, then.toHexString())

    return 0


def usage():

    name = os.path.splitext(os.path.split(sys.argv[0])[1])[0]

    return "Usage: %s <date> <time>" % name


if __name__ == '__main__': raise SystemExit(main(sys.argv))


# End.
