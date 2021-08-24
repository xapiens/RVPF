# $Id: metadata.py 2490 2015-01-07 21:00:17Z SFB $

from java.lang import System
import sys, os

_NAME = os.path.splitext(os.path.split(sys.argv[0])[1])[0]
System.setProperty('rvpf.log.prefix', _NAME)

from java.lang import Thread
from org.rvpf.base.logger import StringLogger
from org.rvpf.document.loader import ConfigDocumentLoader
from org.rvpf.document.version import VersionControl

try: True
except:
    True = 1
    False = not True

_CHECKOUT_COMMAND = 'checkout'
_UPDATE_COMMAND = 'update'

_LOGGER = StringLogger.getInstance('org.rvpf.' + _NAME)


class _Main:

    def __init__(self):

        config = ConfigDocumentLoader.loadConfig()
        if config is None:
            self._error = "Failed to load the configuration files!"
            return

        workspace = config.getStringValue(VersionControl.WORKSPACE_PROPERTY)
        if workspace is None:
            self._error = "The '%s' property is missing!" % VersionControl.WORKSPACE_PROPERTY
            return
        self._workspaceDirectory = config.getFile(workspace)
        _LOGGER.info("Workspace location: {0}", self._workspaceDirectory.canonicalPath)

        properties = config.getPropertiesGroup(VersionControl.DOCUMENT_VERSION_PROPERTIES)
        if properties:
            classDef = properties.getClassDef(
                VersionControl.VERSION_CONTROL_PROPERTY,
                VersionControl.DEFAULT_VERSION_CONTROL)
            user = properties.getStringValue(VersionControl.REPOSITORY_USER_PROPERTY)
            password = properties.getPasswordValue(VersionControl.REPOSITORY_PASSWORD_PROPERTY)
        else:
            classDef = VersionControl.DEFAULT_VERSION_CONTROL
            user = None
            password = None

        Thread.currentThread().contextClassLoader = config.classLoader
        config.classLoader.addFromClassLib(config.getClassLibEntity('SVN'))
        self._versionControl = classDef.createInstance(VersionControl)
        if self._versionControl is None:
            self._error = "Initialisation failed!"
            return

        if user:
            self._versionControl.user = user
            self._versionControl.password = password

        self._error = None

    def checkout(self, modulePath, revision):

        if self._error: return self._error

        if self._workspaceDirectory.exists():
            return "The workspace directory already exists!"

        if self._versionControl.checkout(modulePath, self._workspaceDirectory, revision):
            if self._versionControl.selectWorkspace(self._workspaceDirectory):
                _LOGGER.info(
                    "Checked out revision ''{0}''",
                    self._versionControl.workspaceRevision)

    def update(self, revision):

        if self._error: return self._error

        if not self._workspaceDirectory.isDirectory():
            return "The workspace location '%s' is not a directory!" % self._workspaceDirectory

        if self._versionControl.selectWorkspace(self._workspaceDirectory):
            previousRevision = self._versionControl.workspaceRevision
            if self._versionControl.update(revision):
                updatedRevision = self._versionControl.workspaceRevision
                if updatedRevision == previousRevision:
                    _LOGGER.info(
                        "Updated revision ''{0}'' of workspace",
                        updatedRevision)
                else:
                    _LOGGER.info(
                        "Updated workspace from revision ''{0}'' to revision ''{1}''",
                        previousRevision,
                        updatedRevision)


def _main(argv):

    argv = argv[1:]
    if len(argv) < 1: return "No command specified!"
    if argv[0] == _CHECKOUT_COMMAND:
        if len(argv) < 2: return "The module path must be specified!"
        if len(argv) > 3: return "Too many arguments!"
        if len(argv) > 2: revision = argv[2]
        else: revision = None
        return _Main().checkout(argv[1], revision)
    elif argv[0] == _UPDATE_COMMAND:
        if len(argv) > 2: return "Too many arguments!"
        if len(argv) > 1: revision = argv[1]
        else: revision = None
        return _Main().update(revision)
    else : return "The command '%s' is unknown!" % argv[0]


if __name__ == '__main__': sys.exit(_main(sys.argv))


# End.
