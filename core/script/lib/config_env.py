# $Id: config_env.py 526 2006-10-30 14:15:05Z SFB $

from java.awt import GridBagConstraints, GridBagLayout
from java.lang import System
from java.util.prefs import Preferences
from javax.swing import JLabel, JOptionPane, JPanel, JTextField
import sys, os

try: True
except:
    True = 1
    False = not True


def _usage():

    name = os.path.splitext(os.path.split(sys.argv[0])[1])[0]

    return "Usage: %s [default] [text] [system]" % name


class SettingsConfigurator:

    def __init__(self, title, settings, prefsPath, envFile):

        self._title = title
        self._settings = settings
        self._prefsPath = prefsPath
        self._envFile = envFile

    def __call__(self):

        usePrevious = False
        if System.getenv('HOMEDRIVE') is not None:
            selectionDialog = self._graphicalSelectionDialog
        else: selectionDialog = self._textSelectionDialog
        useSystem = False
        for arg in sys.argv[1:]:
            arg = arg.lower()
            if arg == 'default': usePrevious = True
            elif arg == 'text': selectionDialog = self._textSelectionDialog
            elif arg == 'system': useSystem = True
            else: return _usage()

        previousValues = []
        if useSystem: prefsNode = Preferences.systemRoot().node(self._prefsPath)
        else: prefsNode = Preferences.userRoot().node(self._prefsPath)
        for (title, name, symbol, defaultValue) in self._settings:
            previousValues.append(prefsNode.get(name, ''))

        if usePrevious and previousValues[0]: responses = previousValues
        else:
            responses = selectionDialog(previousValues)
            if not responses: return "Configuration cancelled!"

        for ((title, name, symbol, defaultValue), response) in map(None, self._settings, responses):
            if defaultValue is None and response is not None and len(response.strip()) == 0:
                response = None
            if response is None: prefsNode.remove(name)
            else: prefsNode.put(name, response)

        self._write('sh', 'wb', "export %s=\"%s\"\n", responses)
        self._write('cmd', 'w', "SET %s=%s\n", responses)

        return 0

    def _graphicalSelectionDialog(self, previousValues):

        panel = JPanel(GridBagLayout())
        constraints = GridBagConstraints()
        constraints.gridy = 0
        fields = []
        for ((title, name, symbol, defaultValue), previousValue) in map(None, self._settings, previousValues):
            if not previousValue: previousValue = defaultValue
            label = JLabel(title + ": ")
            constraints.gridx = 0
            constraints.anchor = GridBagConstraints.EAST
            constraints.fill = GridBagConstraints.NONE
            constraints.weightx = 0.0
            panel.add(label, constraints)
            field = JTextField(32)
            field.text = previousValue
            label.labelFor = field
            constraints.gridx = 1
            constraints.anchor = GridBagConstraints.WEST
            constraints.fill = GridBagConstraints.HORIZONTAL
            constraints.weightx = 1.0
            panel.add(field, constraints)
            fields.append(field)
            constraints.gridy += 1

        response = JOptionPane.showOptionDialog(
            None, panel, self._title,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            None, None, None)
        if response != JOptionPane.OK_OPTION: return

        responses = []
        for field in fields: responses.append(field.text)

        return responses

    def _textSelectionDialog(self, previousValues):

        responses = []
        for ((title, name, symbol, defaultValue), previousValue) in map(None, self._settings, previousValues):
            if not previousValue: previousValue = defaultValue
            try:
                response = raw_input("%s [%s]: " % (title, previousValue))
                if not response: response = previousValue
            except: return
            responses.append(response)

        return responses

    def _write(self, ext, mode, text, responses):

        if self._envFile is not None:
            output = open(self._envFile + '.' + ext, mode)
            for ((title, name, symbol, defaultValue), response) in map(None, self._settings, responses):
                output.write(text % (symbol, response))
            output.close()


# End.
