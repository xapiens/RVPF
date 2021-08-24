# $Id: locale.py 784 2008-02-19 15:01:42Z SFB $

from java.text import NumberFormat
from java.util import Locale

import sys, os

try: True
except:
    True = 1
    False = not True


def programName(): return os.path.splitext(os.path.split(sys.argv[0])[1])[0]


def usage():

    return "Usage: %s [<language> [<country> [<variant>]]]" % programName()


def main(argv):

    if len(argv) > 4: return usage()

    if len(argv) == 2 and argv[1] == '?':
        print "Available locales:"
        for locale in Locale.getAvailableLocales():
            print "\t %s" % locale
        return

    if len(argv) > 1:
        language = argv[1]
        if len(argv) > 2: country = argv[2]
        else: country = ''
        if len(argv) > 3: variant = argv[3]
        else: variant = ''
        locale = Locale(language, country, variant)
    else: locale = Locale.getDefault()

    print "Locale: %s" % locale
    numberFormat = NumberFormat.getInstance(locale)
    decimalSymbols = numberFormat.getDecimalFormatSymbols()
    print "Grouping: '%s'" % decimalSymbols.getGroupingSeparator()


if __name__ == '__main__': sys.exit(main(sys.argv))


# End.
