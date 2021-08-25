// $Id: test-store-size.js 4115 2019-08-04 14:17:56Z SFB $

'use strict';

var FILE = __FILE__;

load('script/common.js');

var term = System.getenv("TERM");

if (term != null && term.equals("cygwin")) {
    fail("The script '" + NAME + "' is incompatible with Cygwin!", 1);
}

var Optional = Java.type('java.util.Optional');

var TheStoreServiceActivator = Java.type('org.rvpf.store.server.the.TheStoreServiceActivator');

var DateTime = Java.type('org.rvpf.base.DateTime');
var Point = Java.type('org.rvpf.base.Point');
var StoreSessionProxy = Java.type('org.rvpf.base.store.StoreSessionProxy');
var PointValue = Java.type('org.rvpf.base.value.PointValue');

var CONFIG = "script/" + NAME + "-config.xml";
var EVENTS = 172800;

LOGGER.info("Starting.");

LOGGER.info("Configuration URL: {0}", CONFIG);

var serviceActivator = new TheStoreServiceActivator();

serviceActivator.setConfigURL(CONFIG);
serviceActivator.create();
serviceActivator.start(true);

var metadata = serviceActivator.getService().getMetadata();
var points = metadata.getPointsCollection();

LOGGER.info("Points to update: {0}", points.size());
LOGGER.info("Update events: {0}", EVENTS);
LOGGER.info("Updates to send: {0}", EVENTS * points.size());

var sessionContext = StoreSessionProxy.createContext(
    metadata.getProperties(),
    null,
    LOGGER.getLogger());
var storeSession = new StoreSessionProxy(NAME);

storeSession.setUp(sessionContext);
storeSession.login(null, null);

for (var event = 1; event <= EVENTS; ++event) {
    var now = DateTime.now();
    var pointValues = new Array(points.size());
    var value = Integer.valueOf(event);
    var index = 0;

    for each (var point in points) {
        pointValues[index++] =
            new PointValue(point, Optional.of(now), null, value);
    }

    storeSession.update(pointValues);
}

storeSession.logout();
serviceActivator.stop();
serviceActivator.destroy();

LOGGER.info("Completed.");

// End.