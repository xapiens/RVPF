// $Id: common.js 3535 2017-07-23 14:48:16Z SFB $

var File = Java.type('java.io.File');
var NAME = new File(FILE).getName().replace('.js', '');
var System = Java.type('java.lang.System');
System.setProperty('rvpf.log.prefix', NAME)

var ServiceClassLoader = Java.type('org.rvpf.service.ServiceClassLoader');

var classLoader = ServiceClassLoader.getInstance();

classLoader.activate();

function JavaType(className) {
    return classLoader.loadClass(className).static;
}

function getVersion(className) {
    return new (JavaType(className))();
}

var Logger = Java.type('org.rvpf.base.logger.StringLogger');

var LOGGER = Logger.getInstance('org.rvpf.' + NAME);

getVersion('org.rvpf.pap.PAPVersion').logSystemInfo(NAME, true);

function fail(message, code) {
    print(message);
    exit(code);
}

// End.
