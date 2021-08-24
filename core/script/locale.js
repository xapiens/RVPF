// $Id: locale.js 2713 2015-07-28 18:18:05Z SFB $

'use strict';

var System = Java.type('java.lang.System');
var NumberFormat = Java.type('java.text.NumberFormat');
var Locale = Java.type('java.util.Locale');

function _fail(message, code) {
    print(message);
    quit(code);
}

function _usage() {
    _fail("Usage: locale [<language> [<country> [<variant>]]]", 1);
}

if (arguments.length > 3) _usage();

if (arguments.length == 1 && arguments[0] == '?') {
    System.out.println("Available locales:");
    for each (locale in Locale.getAvailableLocales()) {
        System.out.printf("\t %s\n", locale);
    }
    quit();
}

var locale;

if (arguments.length > 0) {
    language = arguments[0];
    if (arguments.length > 1) country = arguments[1];
    else country = '';
    if (arguments.length > 2) variant = arguments[2];
    else variant = '';
    locale = new Locale(language, country, variant);
} else locale = Locale.getDefault();

System.out.printf("Locale: %s\n", locale);

var numberFormat = NumberFormat.getInstance(locale);
var decimalSymbols = numberFormat.getDecimalFormatSymbols();

System.out.printf("Grouping: '%s'\n",
    String.fromCharCode(decimalSymbols.getGroupingSeparator()));

// End.
