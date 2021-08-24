// $Id: show-pgp.js 2750 2015-09-05 22:29:38Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');

function _usage() {
    fail("Usage: " + NAME + " <identifier> <password>", 1);
}

if (arguments.length != 2) _usage();

var _IDENTIFIER = arguments[0];
var _PASSWORD = arguments[1];

var UnixCrypt = Java.type('org.rvpf.base.security.UnixCrypt');

var salt = (_IDENTIFIER + '..').substring( 0, 2);
var crypted = 'CRYPT:' + UnixCrypt.crypt(_PASSWORD.toCharArray(), salt);

print(crypted);

var Toolkit = Java.type('java.awt.Toolkit');
var StringSelection = Java.type('java.awt.datatransfer.StringSelection');

var selection = new StringSelection(crypted);

Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);

// End.
