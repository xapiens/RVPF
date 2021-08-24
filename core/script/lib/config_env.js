// $Id: config_env.js 2713 2015-07-28 18:18:05Z SFB $

'use strict';

var GridBagConstraints = Java.type("java.awt.GridBagConstraints");
var GridBagLayout = Java.type("java.awt.GridBagLayout");
var File = Java.type("java.io.File");
var PrintWriter = Java.type("java.io.PrintWriter");
var System = Java.type("java.lang.System");
var Preferences = Java.type("java.util.prefs.Preferences");
var JLabel = Java.type("javax.swing.JLabel");
var JOptionPane = Java.type("javax.swing.JOptionPane");
var JPanel = Java.type("javax.swing.JPanel");
var JTextField = Java.type("javax.swing.JTextField");

function SettingsConfigurator(name, title, settings, prefsPath, envFile, args) {
    this._title = title;
    this._settings = settings;
    this._prefsPath = prefsPath;
    this._envFile = envFile;
    this._args = args;

    this._fail = function (message, code) {
        print(message);
        quit(code);
    }

    this._graphicalSelectionDialog = function (previousValues) {
        panel = new JPanel(new GridBagLayout());
        constraints = new GridBagConstraints();
        constraints.gridy = 0;
        fields = [];
        for (i = 0; i < previousValues.length; ++i) {
            setting = this._settings[i];
            previousValue = previousValues[i];
            if (previousValue.length == 0) previousValue = setting.defaultValue;
            label = new JLabel(setting.title + ": ");
            constraints.gridx = 0;
            constraints.anchor = GridBagConstraints.EAST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.weightx = 0.0;
            panel.add(label, constraints);
            field = new JTextField(32);
            field.text = previousValue;
            label.labelFor = field;
            constraints.gridx = 1;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.weightx = 1.0;
            panel.add(field, constraints);
            fields.push(field);
            constraints.gridy += 1;
        }

        response = JOptionPane.showOptionDialog(
            null, panel, this._title,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null, null, null);
        if (response != JOptionPane.OK_OPTION) return null;

        responses = [];
        for each (field in fields) responses.push(field.text);

        return responses;
    }

    this._raw_input = function () {
        const lineSeparator = System.getProperty('line.separator');
        var response = "";

        System.out.printf.apply(System.out, arguments);
        while (true) {
            next = System['in'].read();
            if (next < 0) return null;
            next = String.fromCharCode(next);
            if (next == '\n') break;
            if (next == '\r') {
                if (next == lineSeparator) break;
                continue;
            }
            response = response + next;
        }

        return response;
    }

    this._textSelectionDialog = function (previousValues) {
        responses = [];
        for (i = 0; i < previousValues.length; ++i) {
            setting = this._settings[i];
            previousValue = previousValues[i];
            if (previousValue.length == 0) previousValue = setting.defaultValue;
            response = this._raw_input("%s [%s]: ", setting.title, previousValue);
            if (response == null) return null;
            if (response.length == 0) response = previousValue;
            responses.push(response);
        }

        return responses;
    }

    this._usage = function () {
        this._fail("Usage: " + name + " [default] [text] [system]", 1);
    }

    this._write = function (ext, format, responses) {
        if (this._envFile) {
            file = new File(this._envFile + '.' + ext);
            parent = file.getParentFile();
            if (parent) parent.mkdirs();
            writer = new PrintWriter(file);
            for (i = 0; i < responses.length; ++i) {
                writer.printf(format, this._settings[i].symbol, responses[i]);
            }
            writer.close();
        }
    }

    this.configure = function () {
        usePrevious = false;
        if (System.getenv('HOMEDRIVE') != null) {
            selectionDialog = this._graphicalSelectionDialog;
        } else {
            selectionDialog = this._textSelectionDialog;
        }
        useSystem = false
        for each (arg in args) {
            arg = arg.toLowerCase();
            if (arg == 'default') usePrevious = true;
            else if (arg == 'text') selectionDialog = this._textSelectionDialog;
            else if (arg == 'system') useSystem = true;
            else return _usage();
        }

        previousValues = []
        if (useSystem) prefsNode = Preferences.systemRoot().node(this._prefsPath);
        else prefsNode = Preferences.userRoot().node(this._prefsPath);
        for each (setting in this._settings) {
            previousValues.push(new String(prefsNode.get(setting.name, '')));
        }

        if (usePrevious && previousValues[0].length > 0) responses = previousValues;
        else {
            responses = selectionDialog.call(this, previousValues);
            if (responses == null) {
                print("Configuration cancelled!");
                quit();
            }
        }

        for (i = 0; i < responses.length; ++i) {
            setting = this._settings[i];
            response = responses[i];
            if (setting.defaultValue == null && response != null
                    && new String(response).trim().length() == 0) {
                response = null;
            }
            if (response == null) prefsNode.remove(setting.name);
            else prefsNode.put(setting.name, response);
        }

        this._write('sh', "export %s=\"%s\"\n", responses);
        this._write('cmd', "SET %s=%s\n", responses);
    }
}

// End.
