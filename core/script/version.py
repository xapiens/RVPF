# $Id: version.py 1581 2012-01-16 21:48:08Z SFB $

import sys, os


def main(argv):

    versionPath = os.path.normpath(
        os.path.join(os.path.dirname(sys.argv[0]), '..', 'VERSION'))
    try: versionFile = open(versionPath)
    except IOError, e:
        return Exception("Access to file '%s' failed (%s)"
            % (versionPath, e.strerror))
    version = versionFile.readline().strip()
    versionFile.close()

    return version


if __name__ == '__main__': sys.exit(main(sys.argv))


# End.
