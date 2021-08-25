// $Id: test-queue-size.js 3882 2019-01-28 19:35:03Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');

var Integer = Java.type('java.lang.Integer');

var Optional = Java.type('java.util.Optional');

var SOMContainerServiceActivator = Java.type('org.rvpf.som.SOMContainerServiceActivator');

var DateTime = Java.type('org.rvpf.base.DateTime');
var UUID = Java.type('org.rvpf.base.UUID');

var QueueProxy = Java.type('org.rvpf.base.som.QueueProxy');

var PointValue = Java.type('org.rvpf.base.value.PointValue');

var MetadataDocumentLoader = Java.type('org.rvpf.document.loader.MetadataDocumentLoader');
var MetadataFilter = Java.type('org.rvpf.document.loader.MetadataFilter');

var CONFIG = "script/" + NAME + "-config.xml";
var EVENTS = 172800;
var SCRIPT_UUID = UUID.fromString("1bc458d0-b647-4b41-b1a4-f722354c7eeb");
var TEST_QUEUE_PROPERTIES = "test.queue";

LOGGER.info("Starting.");

LOGGER.info("Configuration URL: {0}", CONFIG);

var serviceActivator = new SOMContainerServiceActivator();

serviceActivator.setConfigURL(CONFIG);
serviceActivator.create();
serviceActivator.start(true);

var config = serviceActivator.getService().getConfig();
var metadata = MetadataDocumentLoader.fetchMetadata(
    new MetadataFilter(true),
    Optional.of(config),
    serviceActivator.getService().getServiceUUID(),
    Optional.empty());
var points = metadata.getPointsCollection();

LOGGER.info("Points to update: {0}", points.size());
LOGGER.info("Update events: {0}", EVENTS);
LOGGER.info("Updates to send: {0}", EVENTS * points.size());

var queueProperties = config.getPropertiesGroup(TEST_QUEUE_PROPERTIES);
var sender = QueueProxy.Sender.newBuilder().prepare(
    config.getProperties(),
    queueProperties,
    NAME,
    LOGGER.getLogger()).build();

sender.connect();

for (var event = 1; event <= EVENTS; ++event) {
    var now = DateTime.now();
    var pointValues = new Array(points.size());
    var value = Integer.valueOf(event);
    var index = 0;

    for each (var point in points) {
        pointValues[index++] =
            new PointValue(point, Optional.of(now), null, value);
    }

    sender.send(pointValues, false);
}

sender.disconnect();
serviceActivator.stop();
serviceActivator.destroy();

LOGGER.info("Completed.");

// End.