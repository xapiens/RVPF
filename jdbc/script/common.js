// $Id: common.js 3186 2016-09-23 01:07:19Z SFB $

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

var Version = JavaType('org.rvpf.base.util.Version');
var Logger = JavaType('org.rvpf.base.logger.StringLogger');

var LOGGER = Logger.getInstance('org.rvpf.' + NAME);

Version.logSystemInfo(NAME, true);

function fail(message, code) {
    print(message);
    quit(code);
}

// End.
