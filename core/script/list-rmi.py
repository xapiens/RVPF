# $Id: list-rmi.py 3873 2019-01-19 15:05:33Z SFB $

from java.lang import System
import sys, os

_NAME = os.path.splitext(os.path.split(sys.argv[0])[1])[0]
System.setProperty('rvpf.log.prefix', _NAME)

from java.rmi import ConnectException
from java.rmi.registry import LocateRegistry

from javax.management import ObjectName

from org.rvpf.base.logger import Logger
from org.rvpf.base.rmi import RegistryEntry
from org.rvpf.document.loader import ConfigDocumentLoader
from org.rvpf.jmx import Agent, AgentSessionFactory, AgentSessionProxy
from org.rvpf.service.rmi import ServiceRegistry

try: True
except:
    True = 1
    False = not True

_LOGGER = Logger.getInstance('org.rvpf.' + _NAME)
_RVPF_DOMAIN = "org.rvpf"


def _main(argv):

    argv = argv[1:]
    if len(argv) != 0: return "Usage: %s" % _NAME

    config = ConfigDocumentLoader.loadConfig("", None, None)
    if config is None: raise _Failed("Failed to load config!")
    if not ServiceRegistry.setUp(config.getProperties()):
        raise _Failed("Failed to set up the service registry!")

    registryPort = ServiceRegistry.getRegistryPort()
    if registryPort < 0: raise _Failed("Registry port unknown!")

    registry = LocateRegistry.getRegistry("127.0.0.1", registryPort)
    try: names = registry.list()
    except ConnectException:
        return "Failed to access a registry on port %i" % registryPort

    loginInfo = Agent.getLoginInfo(config)

    agentSessionProxyBuilder = AgentSessionProxy.newBuilder()

    print "RMI object registry on local port %i" % registryPort
    for name in names:
        interfaces = registry.lookup(name).getClass().interfaces
        interface = interfaces[-1].simpleName

        print "    %s: %s" % (name, interface)
        if interface == AgentSessionFactory.simpleName:
            registryEntryBuilder = RegistryEntry.newBuilder()
            registryEntryBuilder.setName(name)
            registryEntryBuilder.setDefaultPrefix(Agent.REGISTRY_PREFIX)
            registryEntryBuilder.setDefaultRegistryAddress(ServiceRegistry.getRegistryAddress())
            registryEntryBuilder.setDefaultRegistryPort(ServiceRegistry.getRegistryPort())

            agentSessionProxyBuilder.setRegistryEntry(registryEntryBuilder.build())
            agentSessionProxyBuilder.setClientName(_NAME)
            agentSessionProxyBuilder.setClientLogger(_LOGGER)
            agentSessionProxyBuilder.setLoginInfo(loginInfo)

            agentSessionProxy = agentSessionProxyBuilder.build()
            agentSessionProxy.connect()

            for objectName in agentSessionProxy.objectNames:
                if objectName.getDomain() == _RVPF_DOMAIN:
                    className = agentSessionProxy.getObjectClassName(objectName).split('.')[-1]
                    print "        %s: %s" % (objectName, className)

            agentSessionProxy.disconnect()


if __name__ == '__main__': sys.exit(_main(sys.argv))


# End.
