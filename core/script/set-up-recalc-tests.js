// $Id: set-up-recalc-tests.js 3854 2018-12-07 19:12:03Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');

var DateTime = Java.type('org.rvpf.base.DateTime');
var MetadataDocumentLoader = Java.type('org.rvpf.document.loader.MetadataDocumentLoader');
var MetadataServerFilter = Java.type('org.rvpf.http.metadata.MetadataServerFilter');
var Optional = Java.type('java.util.Optional');
var PointValue = Java.type('org.rvpf.base.value.PointValue');
var Require = Java.type('org.rvpf.base.tool.Require');
var ServiceRegistry = Java.type('org.rvpf.service.rmi.ServiceRegistry');
var Store = Java.type('org.rvpf.base.store.Store');

function _fail(message, code) {
    print(message);
    quit(code);
}

function _usage() {
    _fail("Usage: " + NAME + " [<store-name>]", 1);
}

if (arguments.length > 1) _usage();

var STAMP1 = DateTime.fromString('2018-01-01')
var STAMP2 = DateTime.fromString('2018-02-02')

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

ServiceRegistry.setUp(metadata.getConfig().getProperties());

Require.trueValue(metadata.validatePointsRelationships());

var storeEntity = metadata.getStoreEntity(Optional.of(storeName)).get();

Require.trueValue(storeEntity.setUp(metadata));

var store = Require.notNull(storeEntity.getStore());
var sampleInputPoint1 = metadata.getPoint('SampleInputPoint1').get();
var sampleInputPoint2 = metadata.getPoint('SampleInputPoint2').get();

store.addUpdate(new PointValue(sampleInputPoint1, Optional.of(STAMP1), null, 'Sample text 1 for point 1'));
store.addUpdate(new PointValue(sampleInputPoint2, Optional.of(STAMP1), null, 'Sample text 1 for point 2'));
store.addUpdate(new PointValue(sampleInputPoint1, Optional.of(STAMP2), null, 'Sample text 2 for point 1'));
store.addUpdate(new PointValue(sampleInputPoint2, Optional.of(STAMP2), null, 'Sample text 2 for point 2'));
store.sendUpdates();

// End.
