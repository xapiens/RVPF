// $Id: clean-up-store.js 3854 2018-12-07 19:12:03Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');

var CleanUpArchiver = JavaType('org.rvpf.store.server.archiver.CleanUpArchiver');
var KeyedGroups = Java.type('org.rvpf.base.util.container.KeyedGroups');
var MetadataDocumentLoader = Java.type('org.rvpf.document.loader.MetadataDocumentLoader');
var MetadataServerFilter = Java.type('org.rvpf.http.metadata.MetadataServerFilter');
var Optional = Java.type('java.util.Optional');
var Store = Java.type('org.rvpf.base.store.Store');

function _fail(message, code) {
    print(message);
    quit(code);
}

function _usage() {
    _fail("Usage: " + NAME + " [<store-name>]", 1);
}

if (arguments.length > 1) _usage();

var storeName;

if (arguments.length == 1) {
    storeName = arguments[0];
} else {
    storeName = Store.DEFAULT_STORE_NAME;
}

var metadata = MetadataDocumentLoader.fetchMetadata(
    new MetadataServerFilter(),
    Optional.empty(),
    Optional.empty(),
    Optional.empty());
var succeeded = new CleanUpArchiver().cleanUp(
    storeName,
    metadata,
    KeyedGroups.MISSING_KEYED_GROUP);

if (!succeeded) quit(1);

// End.
