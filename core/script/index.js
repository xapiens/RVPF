// $Id: index.js 2713 2015-07-28 18:18:05Z SFB $

'use strict';

var BufferedReader = Java.type('java.io.BufferedReader');
var File = Java.type('java.io.File');
var FileReader = Java.type('java.io.FileReader');
var PrintWriter = Java.type('java.io.PrintWriter');
var Timestamp = Java.type('java.sql.Timestamp');

var _BEGIN_MARK = String("<!-- [Begin index] -->");
var _END_MARK = String("<!-- [End index] -->");
var _INDEX_FILE = String('index.html');

function _fail(message, code) {
    print(message);
    quit(code);
}

function _index(directory) {
    var entries = [];

    for each (var file in directory.listFiles()) {
        var name = file.getName();

        if (_INDEX_FILE.equalsIgnoreCase(name)) continue;

        var length = file.length();
        var stamp = new Timestamp(file.lastModified()).toString();

        stamp = stamp.substring(0, stamp.indexOf('.'));
        entries.push('<tr><td><a href="' + name + '">' + name + '</a></td><td>'
            + stamp + '</td><td>' + length + '</td></tr>');
    }

    return entries;
}

function _usage() {
    _fail("Usage: index <directory>", 1);
}

if (arguments.length != 1) _usage();

var directory = new File(arguments[0]);

if (!directory.isDirectory()) _usage();

var indexFile = new File(directory, _INDEX_FILE);

if (!indexFile.isFile()) _fail("File '" + _INDEX_FILE + "' is missing", 2);

var reader = new BufferedReader(new FileReader(indexFile));
var lines = [];

while (true) {
    line = reader.readLine();
    if (line == null) _fail("Begin mark not found", 3);
    lines.push(line);
    if (_BEGIN_MARK.equalsIgnoreCase(line.trim())) break;
}

while (true) {
    line = reader.readLine();
    if (line == null) _fail("End mark not found", 3);
    if (_END_MARK.equalsIgnoreCase(line.trim())) break;
}

var indent = line.substring(0, line.indexOf('<'));

for each (var entry in _index(directory)) {
    lines.push(indent + entry);
}

do {
    lines.push(line);
    line = reader.readLine();
} while (line != null);

reader.close();

var writer = new PrintWriter(indexFile);

for each (var line in lines) {
    writer.println(line);
}

writer.close();

// End.
