/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceRegistry.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.service.rmi;

import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.ConnectException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.rmi.RegistryConfig;
import org.rvpf.base.rmi.RegistryEntry;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.SnoozeAlarm;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.service.ServiceClassLoader;
import org.rvpf.service.ServiceMessages;

/**
 * Service registry.
 *
 * <h1>Notes</h1>
 *
 * <ul>
 *   <li>A private registry (specified by the 'private' property) does not use
 *     the RMI infrastructure. Instead, the registered servers are called
 *     directly. Since the serialization/deserialization process is bypassed,
 *     the servers must take appropriate precautions when handling objects seen
 *     by the clients. The {@link ServiceRegistry#isPrivate()} method may be
 *     used by the servers to detect that situation.</li>
 *   <li>A stealth registry (when the registry port is not specified) uses an
 *     allocated port.</li>
 *   <li>A protected registry (specified by the 'protected' property) does not
 *     accept registrations from outside its JVM. A private or stealth registry
 *     is implicitly protected.</li>
 *   <li>A registry is local when it is located in the current JVM.</li>
 *   <li>A registry is shared (allowed by the 'shared' property) when it has
 *     been created in an other JVM on the local system.</li>
 * </ul>
 */
public final class ServiceRegistry
{
    /**
     * Constructs an instance.
     *
     * @param isPrivate True if the registry is private.
     * @param address The registry address.
     * @param port The registry port.
     * @param isProtected True if the registry is protected.
     * @param isShared True if the registry is shared.
     */
    private ServiceRegistry(
            final boolean isPrivate,
            final InetAddress address,
            final int port,
            final boolean isProtected,
            final boolean isShared)
    {
        _private = isPrivate;
        _address = _private? Optional.empty(): Optional.ofNullable(address);
        _stealth = port == 0;
        _port = _private? -1: (_stealth? Inet.allocateTCPPort(): port);
        _protected = isProtected || _private || _stealth;
        _shared = isShared;
    }

    /**
     * Gets the singleton instance.
     *
     * @return The singleton instance.
     */
    @Nonnull
    @CheckReturnValue
    public static synchronized ServiceRegistry getInstance()
    {
        return Require.notNull(_instance);
    }

    /**
     * Gets the registry address.
     *
     * @return The registry address (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<InetAddress> getRegistryAddress()
    {
        final ServiceRegistry instance = _instance;

        return (instance != null)? instance._address: Optional.empty();
    }

    /**
     * Gets the registry port.
     *
     * @return The registry port (-1 if unknown).
     */
    @CheckReturnValue
    public static int getRegistryPort()
    {
        final ServiceRegistry instance = _instance;

        return (instance != null)? instance._port: -1;
    }

    /**
     * Asks if the registry is local (located in the current JVM).
     *
     * @return True if the registry is local.
     */
    @CheckReturnValue
    public static boolean isLocal()
    {
        return getInstance()._created;
    }

    /**
     * Asks if the registry is private.
     *
     * @return True if the registry is private.
     */
    @CheckReturnValue
    public static boolean isPrivate()
    {
        return getInstance()._private;
    }

    /**
     * Asks if the registry is protected.
     *
     * @return True if the registry is protected.
     */
    @CheckReturnValue
    public static boolean isProtected()
    {
        return getInstance()._protected;
    }

    /**
     * Asks if the registry is shared.
     *
     * @return True if the registry is shared.
     */
    @CheckReturnValue
    public static boolean isShared()
    {
        return getInstance()._shared;
    }

    /**
     * Asks if the registry is stealth.
     *
     * @return True if the registry is stealth.
     */
    @CheckReturnValue
    public static boolean isStealth()
    {
        return getInstance()._stealth;
    }

    /**
     * Purges the registry entries.
     */
    public static void purge()
    {
        final ServiceRegistry instance = _instance;

        if (instance != null) {
            instance._purge();
        }
    }

    /**
     * Sets up the singleton instance.
     *
     * @param configProperties The configuration properties.
     *
     * @return A true value on success.
     */
    @CheckReturnValue
    public static synchronized boolean setUp(
            @Nonnull final KeyedGroups configProperties)
    {
        final RegistryConfig registryConfig = new RegistryConfig(
            configProperties);
        final Optional<InetSocketAddress> registrySocketAddress = registryConfig
            .getRegistrySocketAddress();

        if (!registrySocketAddress.isPresent()) {
            return false;
        }

        final int registryPort = registrySocketAddress.get().getPort();
        final InetAddress registryAddress = registrySocketAddress
            .get()
            .getAddress();

        if (_instance == null) {
            final ServiceRegistry serviceRegistry = new ServiceRegistry(
                registryConfig.isRegistryPrivate(),
                registryAddress,
                registryPort,
                registryConfig.isRegistryReadOnly(),
                registryConfig.isRegistryShared());

            if (serviceRegistry._setUp()) {
                _instance = serviceRegistry;
            } else {
                return false;
            }
        } else if (!_instance._private
                   && (registryPort > 0)
                   && (registryPort != _instance._port)) {
            _LOGGER
                .error(
                    BaseMessages.RMI_REGISTRY_CONFLICT,
                    String.valueOf(registryPort),
                    String.valueOf(_instance._port));

            return false;
        }

        return true;
    }

    /**
     * Gets the RMI registry.
     *
     * @return The RMI registry.
     */
    @Nonnull
    @CheckReturnValue
    public Registry getRMIRegistry()
    {
        return Require.notNull(_registry);
    }

    /**
     * Asks if a name is registered.
     *
     * @param name The name.
     *
     * @return A true value if registered.
     *
     * @throws RemoteException When communication with the registry fails.
     */
    @CheckReturnValue
    public boolean isRegistered(final String name)
        throws RemoteException
    {
        try {
            _registry.lookup(name);
        } catch (final NotBoundException exception) {
            return false;
        }

        return true;
    }

    /**
     * Registers.
     *
     * @param sessionFactory The session factory.
     * @param name The name.
     * @param logger A logger.
     *
     * @return A true value on success.
     */
    @CheckReturnValue
    public boolean register(
            @Nonnull final SessionFactory sessionFactory,
            @Nonnull final String name,
            @Nonnull final Logger logger)
    {
        if (_exports.containsKey(name)) {
            logger.error(ServiceMessages.RMI_ALREADY_EXPORTED, name);

            return false;
        }

        final Remote serverStub;

        try {
            serverStub = sessionFactory.export();
        } catch (final RemoteException exception) {
            logger.error(exception, ServiceMessages.RMI_EXPORT_FAILED, name);

            return false;
        }

        _exports.put(name, sessionFactory);

        try {
            try {
                _registry.bind(name, serverStub);
            } catch (final AlreadyBoundException exception) {
                logger.warn(ServiceMessages.REBINDING_RMI, name);
                _registry.rebind(name, serverStub);
            } catch (final ServerException exception) {
                if (exception.getCause() instanceof AccessException) {
                    logger.error(ServiceMessages.RMI_REGISTRY_PROTECTED);

                    return false;
                }

                throw exception;
            }
        } catch (final Exception exception) {
            logger.error(exception, ServiceMessages.RMI_BIND_FAILED, name);

            return false;
        }

        return true;
    }

    /**
     * Registers a RMI server.
     *
     * @param server The server object.
     * @param serverURI The server URI.
     * @param logger A logger.
     *
     * @return The server name (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public String registerServer(
            @Nonnull final SessionFactory server,
            @Nonnull final URI serverURI,
            @Nonnull final Logger logger)
    {
        if (serverURI.isAbsolute() && !"rmi".equals(serverURI.getScheme())) {
            logger
                .error(
                    BaseMessages.SCHEME_NOT_SUPPORTED,
                    serverURI.getScheme());

            return null;
        }

        if (!Inet.isOnLocalHost(serverURI)) {
            logger.warn(BaseMessages.HOST_NOT_LOCAL, serverURI.getHost());
        }

        final int serverPort = serverURI.getPort();

        if ((serverPort >= 0)
                && (serverPort != ServiceRegistry.getRegistryPort())) {
            logger
                .error(
                    BaseMessages.RMI_REGISTRY_CONFLICT,
                    String.valueOf(serverURI.getPort()),
                    String.valueOf(ServiceRegistry.getRegistryPort()));

            return null;
        }

        String serverName = serverURI.getPath();

        if (serverName == null) {
            throw new IllegalArgumentException(
                Message.format(ServiceMessages.SERVER_IDENTIFICATION));
        }

        if (serverName.startsWith("/")) {
            serverName = serverName.substring(1);
        }

        if (serverName.isEmpty()) {
            throw new IllegalArgumentException(
                Message.format(ServiceMessages.SERVER_IDENTIFICATION));
        }

        if (!register(server, serverName, logger)) {
            return null;
        }

        logger.info(ServiceMessages.REGISTERED_RMI, serverName);

        return serverName;
    }

    /**
     * Unregisters.
     *
     * @param name The name.
     * @param logger A logger.
     */
    public void unregister(
            @Nonnull final String name,
            @Nonnull final Logger logger)
    {
        if (name != null) {
            final SessionFactory sessionFactory;

            try {
                _registry.unbind(name);
                logger.info(ServiceMessages.UNREGISTERED_RMI, name);
            } catch (final RemoteException exception) {
                logger
                    .debug(ServiceMessages.RMI_UNBIND_FAILED, name, exception);
            } catch (final NotBoundException exception) {
                logger.warn(ServiceMessages.RMI_NOT_BOUND, name);
            }

            sessionFactory = _exports.get(name);

            if (sessionFactory != null) {
                try {
                    sessionFactory.unexport();
                } catch (final NoSuchObjectException exception) {
                    logger.warn(ServiceMessages.RMI_NOT_EXPORTED, name);
                }

                _exports.remove(name);
            } else {
                logger.warn(ServiceMessages.RMI_NOT_REGISTERED, name);
            }
        }
    }

    private void _purge()
    {
        for (final String exportedName: _exports.keySet()) {
            _LOGGER.warn(ServiceMessages.REGISTRY_EXPORT_PURGED, exportedName);
            unregister(exportedName, _LOGGER);
        }

        if (_created) {
            try {
                for (final String registeredName: _registry.list()) {
                    _LOGGER
                        .warn(
                            ServiceMessages.REGISTRY_ENTRY_PURGED,
                            registeredName);
                    _registry.unbind(registeredName);
                }
            } catch (final Exception exception) {
                _LOGGER
                    .error(
                        exception,
                        BaseMessages.VERBATIM,
                        exception.getMessage());
            }
        }
    }

    private boolean _setUp()
    {
        if (_private) {
            _registry = new _Private();
            _LOGGER.info(ServiceMessages.CREATED_RMI_REGISTRY_PRIVATE);
            RegistryEntry.setRegistry(_registry, true);

            return true;
        }

        synchronized (RMISocketFactory.class) {    // JVM wide.
            if (RMISocketFactory.getSocketFactory() == null) {
                try {
                    RMISocketFactory.setSocketFactory(new SocketFactory());
                } catch (final IOException exception) {
                    throw new InternalError(exception);    // Should not happen.
                }
            }
        }

        boolean loggedWaiting = false;
        SnoozeAlarm snoozeAlarm = null;

        for (;;) {
            try {
                final Optional<String> savedLogID = Logger.currentLogID();
                final Optional<ServiceClassLoader> savedClassLoader =
                    ServiceClassLoader
                        .hideInstance();

                try {
                    Logger.restoreLogID(Optional.empty());
                    _registry = LocateRegistry
                        .createRegistry(
                            _port,
                            null,
                            _protected? new ReadOnlyRegistrySocketFactory(
                                _address): new BaseRMIServerSocketFactory(
                                        _address));
                } finally {
                    ServiceClassLoader.restoreInstance(savedClassLoader);
                    Logger.restoreLogID(savedLogID);
                }

                if (!_address.get().isAnyLocalAddress()) {
                    System
                        .setProperty(
                            _SERVER_HOSTNAME_PROPERTY,
                            _address.get().getHostAddress());
                }

                _created = true;
            } catch (final RemoteException remoteException) {
                if (!_shared) {
                    _LOGGER
                        .error(
                            ServiceMessages.RMI_REGISTRY_CREATE_FAILED,
                            String.valueOf(_port),
                            remoteException.getCause());

                    return false;
                }

                try {
                    final Registry registry = LocateRegistry
                        .getRegistry(_address.get().getHostAddress(), _port);

                    registry.list();
                    _registry = registry;
                } catch (final ConnectException exception) {
                    if (!loggedWaiting) {
                        _LOGGER.info(ServiceMessages.RMI_REGISTRY_WAIT);
                        loggedWaiting = true;
                    }

                    if (snoozeAlarm == null) {
                        snoozeAlarm = new SnoozeAlarm(
                            Optional.of(ServiceRegistry.class));
                    }

                    try {
                        snoozeAlarm.snooze(_REGISTRY_WAIT);
                    } catch (final InterruptedException interruptedException) {
                        throw new RuntimeException(interruptedException);
                    }

                    continue;
                } catch (final RemoteException exception) {
                    _LOGGER
                        .error(
                            exception,
                            ServiceMessages.RMI_REGISTRY_FAILED,
                            String.valueOf(_port));
                }
            }

            break;
        }

        final InetSocketAddress registrySocketAddress = new InetSocketAddress(
            _address.orElse(null),
            _port);

        if (_created) {
            _LOGGER
                .info(
                    _stealth
                    ? ServiceMessages.CREATED_RMI_REGISTRY_STEALTH
                    : (_protected
                       ? ServiceMessages.CREATED_RMI_REGISTRY_PROTECTED
                       : ServiceMessages.CREATED_RMI_REGISTRY),
                    registrySocketAddress);
        } else {
            _LOGGER
                .info(ServiceMessages.GOT_RMI_REGISTRY, registrySocketAddress);
        }

        RegistryEntry.setRegistry(_registry, false);

        return true;
    }

    private static final Logger _LOGGER = Logger
        .getInstance(ServiceRegistry.class);
    private static final ElapsedTime _REGISTRY_WAIT = ElapsedTime
        .fromMillis(1000);
    private static final String _SERVER_HOSTNAME_PROPERTY =
        "java.rmi.server.hostname";
    private static ServiceRegistry _instance;

    private final Optional<InetAddress> _address;
    private boolean _created;
    private final Map<String, SessionFactory> _exports =
        new ConcurrentHashMap<>();
    private final int _port;
    private final boolean _private;
    private final boolean _protected;
    private Registry _registry;
    private final boolean _shared;
    private final boolean _stealth;

    /**
     * Not local server socket.
     */
    private static final class NotLocalServerSocket
        extends ServerSocket
    {
        /**
         * Constructs an instance.
         *
         * @param port The port.
         * @param bindAddr The bind address.
         *
         * @throws IOException On I/O exception.
         */
        public NotLocalServerSocket(
                final int port,
                @Nonnull final Optional<InetAddress> bindAddr)
            throws IOException
        {
            super(port, 0, bindAddr.orElse(null));
        }

        /** {@inheritDoc}
         */
        @Override
        public Socket accept()
            throws IOException
        {
            final Socket socket = new NotLocalSocket();

            implAccept(socket);

            return socket;
        }
    }


    /**
     * Not local socket.
     */
    private static final class NotLocalSocket
        extends Socket
    {
        /**
         * Constructs an instance.
         */
        NotLocalSocket() {}

        /** {@inheritDoc}
         */
        @Override
        public InetAddress getInetAddress()
        {
            return null;
        }
    }


    /**
     * Read-only registry socket factory.
     */
    private static final class ReadOnlyRegistrySocketFactory
        extends BaseRMIServerSocketFactory
    {
        /**
         * Constructs an instance.
         *
         * @param address The address (may be empty).
         */
        ReadOnlyRegistrySocketFactory(
                @Nonnull final Optional<InetAddress> address)
        {
            super(address);
        }

        /** {@inheritDoc}
         */
        @Override
        public ServerSocket createServerSocket(
                final int port)
            throws IOException
        {
            return new NotLocalServerSocket(port, getAddress());
        }
    }


    /**
     * Socket factory.
     */
    private static final class SocketFactory
        extends RMISocketFactory
    {
        /**
         * Constructs an instance.
         */
        SocketFactory() {}

        /** {@inheritDoc}
         */
        @Override
        public ServerSocket createServerSocket(
                final int port)
            throws IOException
        {
            return new ServerSocket(port, 0, getRegistryAddress().orElse(null));
        }

        /** {@inheritDoc}
         */
        @Override
        public Socket createSocket(
                final String host,
                final int port)
            throws IOException
        {
            final Socket socket = new Socket(host, port);

            socket.setSoLinger(false, 0);

            return socket;
        }
    }


    private static final class _Private
        implements Registry
    {
        /**
         * Constructs an instance.
         */
        _Private() {}

        /** {@inheritDoc}
         */
        @Override
        public void bind(
                final String name,
                final Remote remote)
            throws AlreadyBoundException
        {
            final String key = _key(name);

            Require.notNull(remote);

            if (_bindings.containsKey(key)) {
                throw new AlreadyBoundException();
            }

            _bindings.put(key, remote);
        }

        /** {@inheritDoc}
         */
        @Override
        public String[] list()
            throws RemoteException, AccessException
        {
            final Set<String> keys = _bindings.keySet();

            return keys.toArray(new String[keys.size()]);
        }

        /** {@inheritDoc}
         */
        @Override
        public Remote lookup(final String name)
            throws NotBoundException
        {
            final String key = _key(name);
            final Remote remote = _bindings.get(key);

            if (remote == null) {
                throw new NotBoundException(key);
            }

            return remote;
        }

        /** {@inheritDoc}
         */
        @Override
        public void rebind(
                final String name,
                final Remote remote)
            throws RemoteException, AccessException
        {
            Require.notNull(remote);
            _bindings.put(_key(name), remote);
        }

        /** {@inheritDoc}
         */
        @Override
        public void unbind(final String name)
            throws NotBoundException
        {
            if (_bindings.remove(_key(name)) == null) {
                throw new NotBoundException();
            }
        }

        private static String _key(final String name)
        {
            String key = URI.create(name).getPath();

            if (key.startsWith("/")) {
                key = key.substring(1);
            }

            return key;
        }

        private final Map<String, Remote> _bindings = new HashMap<>();
    }
}

/* This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * version 2.1 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA
 */
