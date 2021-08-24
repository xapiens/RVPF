// $Id: uuid.js 3340 2017-01-21 16:19:52Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');

var Toolkit = Java.type('java.awt.Toolkit');
var StringSelection = Java.type('java.awt.datatransfer.StringSelection');
var JOptionPane = Java.type('javax.swing.JOptionPane');

var UUID = Java.type('org.rvpf.base.UUID');

function _fail(message, code) {
    print(message);
    quit(code);
}

function _paste(text) {
    var selection = new StringSelection(text);

    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
        selection, null);
}

function _gui() {
    while (true) {
        var response = JOptionPane.showConfirmDialog(
            null, "Press 'OK' to generate, 'Cancel' to quit", "UUID generator",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (response != JOptionPane.OK_OPTION) break;

        _paste(UUID.generate().toString());
    }

    quit();
}

function _usage() {
    _fail("Usage: uuid [gui | <count>]", 1);
}

if (arguments.length > 1) _usage();

var count;

if (arguments.length == 1) {
    var argument = arguments[0];

    while (argument.length > 0 && argument[0] == '-') {
        argument = argument.substring(1);
    }
    if (argument == 'gui') _gui();
    count = new Number(argument);
    if (isNaN(count) || count != count.toFixed()) _usage();
} else count = 1;

var uuid;

for (var i = 0; i < count; ++i) {
    uuid = UUID.generate();
    print(uuid);
}

if (count == 1) _paste(uuid.toString());

// End.
