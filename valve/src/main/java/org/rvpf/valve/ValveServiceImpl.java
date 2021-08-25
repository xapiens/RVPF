/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ValveServiceImpl.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.valve;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.URL;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.StatsOwner;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.MemoryLogger;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.security.KeyStoreConfig;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.security.ServerSecurityContext;
import org.rvpf.base.security.TrustStoreConfig;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.service.ServiceActivatorBase;
import org.rvpf.service.ServiceBaseImpl;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceStats;

/**
 * Valve service implementation.
 *
 * <p>Instances of this class are created by the
 * {@linkplain org.rvpf.valve.ValveServiceActivator Valve service activator}.
 * </p>
 *
 * <p>They will listen on a 'direct port' and relay everything to the 'server
 * address'. They will also listen on a 'controlled port' and relay to
 * the'server address' only if a connection has been successfully established on
 * the 'control port'.</p>
 *
 * <p>This service is configured with values from a properties file. By default,
 * it looks for all resources named {@value #DEFAULT_ENV_PATH}, but that may be
 * changed by specifying a URL (absolute or relative) in a system property named
 * {@value #ENV_PATH_PROPERTY}.</p>
 */
public final class ValveServiceImpl
    extends ServiceBaseImpl
{
    /**
     * Constructs an instance.
     *
     * @param serviceActivatorBase The service activator base owning this.
     */
    public ValveServiceImpl(
            @Nonnull final ServiceActivatorBase serviceActivatorBase)
    {
        setServiceActivatorBase(serviceActivatorBase);
    }

    /**
     * Creates a security context.
     *
     * @param env The environment properties.
     * @param forServer True for a server.
     * @param certifiedProperty The certified property.
     * @param keystoreProperty The keystore property.
     * @param keystoreTypeProperty The keystore type property.
     * @param keystoreProviderProperty The keystore provider property.
     * @param keystorePasswordProperty The keystore password property.
     * @param keyPasswordProperty The key password property.
     * @param truststoreProperty The truststore property.
     * @param truststoreTypeProperty The truststore type property.
     * @param truststoreProviderProperty The truststore provider property.
     * @param logger A logger.
     *
     * @return The security context.
     *
     * @throws IOException When appropriate.
     */
    public static Optional<SecurityContext> createSecurityContext(
            @Nonnull final Properties env,
            final boolean forServer,
            @Nonnull final String certifiedProperty,
            @Nonnull final String keystoreProperty,
            @Nonnull final String keystoreTypeProperty,
            @Nonnull final String keystoreProviderProperty,
            @Nonnull final String keystorePasswordProperty,
            @Nonnull final String keyPasswordProperty,
            @Nonnull final String truststoreProperty,
            @Nonnull final String truststoreTypeProperty,
            @Nonnull final String truststoreProviderProperty,
            @Nonnull final Logger logger)
        throws IOException
    {
        final Optional<String> keystore = Optional
            .ofNullable(env.getProperty(keystoreProperty));
        final Optional<String> truststore = Optional
            .ofNullable(env.getProperty(truststoreProperty));
        final SecurityContext securityContext;

        if (keystore.isPresent() || truststore.isPresent()) {
            Require.success(keystore.isPresent() || !forServer);

            securityContext = forServer? new ServerSecurityContext(
                logger): new SecurityContext(logger);

            securityContext
                .setCertified(_getBooleanProperty(env, certifiedProperty));

            final KeyStoreConfig keyStoreConfig = securityContext
                .getKeyStoreConfig();

            keyStoreConfig.setPath(keystore);
            keyStoreConfig
                .setType(
                    Optional.ofNullable(env.getProperty(keystoreTypeProperty)));
            keyStoreConfig
                .setProvider(
                    Optional
                        .ofNullable(env.getProperty(keystoreProviderProperty)));

            final char[] password = env
                .getProperty(keystorePasswordProperty, "")
                .toCharArray();

            keyStoreConfig.setPassword(Optional.of(password));

            final char[] keyPassword = env
                .getProperty(keyPasswordProperty, "")
                .toCharArray();

            keyStoreConfig.setKeyPassword(Optional.of(keyPassword));

            final TrustStoreConfig trustStoreConfig = securityContext
                .getTrustStoreConfig();

            trustStoreConfig.setPath(truststore);

            final String passwordType = env.getProperty(truststoreTypeProperty);

            trustStoreConfig.setType(Optional.ofNullable(passwordType));

            final String truststoreProvider = env
                .getProperty(truststoreProviderProperty);

            trustStoreConfig
                .setProvider(Optional.ofNullable(truststoreProvider));
        } else {
            securityContext = null;
        }

        return Optional.ofNullable(securityContext);
    }

    /**
     * Returns the environment properties path.
     *
     * @return The environment properties path.
     */
    @Nonnull
    @CheckReturnValue
    public static String envPath()
    {
        return System.getProperty(ENV_PATH_PROPERTY, DEFAULT_ENV_PATH);
    }

    /**
     * Closes the server connections.
     */
    public void close()
    {
        if (isOpened()) {
            getThisLogger()
                .info(ServiceMessages.STOPPING_SERVICE, getServiceName());
            updateStats();

            _closeLatch.countDown();

            if (_controlPort != null) {
                _controlPort.close();
                _controlPort = null;
            }

            if (_controlledPortManager != null) {
                _controlledPortManager.stop();
                _controlledPortManager = null;
            }

            if (_directPortManager != null) {
                _directPortManager.stop();
                _directPortManager = null;
            }

            Require.ignored(join());

            DelegatedTaskExecutor.shutdown();

            if (getStats() != null) {
                logStats(false);
            }

            tearDown();
        }
    }

    /**
     * Gets the controlled addresses.
     *
     * <p>For framework tests.</p>
     *
     * @return The controlled addresses.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<SocketAddress> getControlledAddresses()
    {
        return (_controlledPortManager != null)? _controlledPortManager
            .getListenAddresses(): new LinkedList<SocketAddress>();
    }

    /**
     * Gets the direct addresses.
     *
     * <p>For framework tests.</p>
     *
     * @return The direct addresses.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<SocketAddress> getDirectAddresses()
    {
        return (_directPortManager != null)? _directPortManager
            .getListenAddresses(): Collections.<SocketAddress>emptyList();
    }

    /**
     * Gets the environment properties.
     *
     * <p>For framework tests.</p>
     *
     * @return The environment properties.
     */
    @Nonnull
    @CheckReturnValue
    public Properties getEnv()
    {
        return Require.notNull(_env);
    }

    /** {@inheritDoc}
     */
    @Override
    public ValveStats getStats()
    {
        return (ValveStats) super.getStats();
    }

    /**
     * Asks if the server connections are closed.
     *
     * @return True if the server connections are closed.
     */
    @CheckReturnValue
    public boolean isClosed()
    {
        return !isOpened();
    }

    /**
     * Asks if the server connections are opened.
     *
     * @return True if the server connections are opened.
     */
    @CheckReturnValue
    public boolean isOpened()
    {
        return _closeLatch.getCount() > 0;
    }

    /**
     * Asks if the controlled ports are paused.
     *
     * @return True if paused.
     */
    @CheckReturnValue
    public boolean isPaused()
    {
        return (_controlledPortManager != null)
               && _controlledPortManager.isPaused();
    }

    /**
     * Opens the server connections.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean open()
    {
        getThisLogger()
            .info(
                ServiceMessages.STARTING_SERVICE,
                getServiceName(),
                getVersion().getImplementationVersion());

        // Loads the configuration properties.

        if (!_loadEnv()) {
            return false;
        }

        // Initializes the stats.

        if (!setUp()) {
            return false;
        }

        getStats()
            .setLogEnabled(
                _getBooleanProperty(_env, STATS_LOG_ENABLED_PROPERTY));

        // Configures the minimum buffer size.

        final int bufferSize = _getIntProperty(
            _env,
            BUFFER_SIZE_PROPERTY,
            DEFAULT_BUFFER_SIZE);

        if (getThisLogger().isDebugEnabled()) {
            getThisLogger()
                .debug(ValveMessages.BUFFER_SIZE, String.valueOf(bufferSize));
        }

        DataBufferPool.FIXED_BUFFERS.useBufferSize(bufferSize);
        DataBufferPool.EXPANDING_BUFFERS.useBufferSize(bufferSize);

        // Configures the filter.

        final String filterClass = _env.getProperty(FILTER_CLASS_PROPERTY);

        if (filterClass != null) {
            getThisLogger().debug(ValveMessages.FILTER_CLASS, filterClass);

            try {
                _connectionFilter = Optional
                    .of(
                        (Connection.Filter) Class
                            .forName(filterClass)
                            .newInstance());
            } catch (final RuntimeException exception) {
                throw exception;
            } catch (final Exception exception) {
                throw new RuntimeException(exception);
            }
        } else {
            _connectionFilter = Optional.empty();
        }

        // Gets the server.

        _serverAddress = _getSocketAddress(
            _env,
            SERVER_ADDRESS_PROPERTY,
            getThisLogger());

        if (_serverAddress == null) {
            return false;
        }

        getThisLogger().info(ValveMessages.SERVER_ADDRESS, _serverAddress);

        // Creates the SSL context.

        final Optional<SecurityContext> serverSecurityContext;
        final SSLContext serverSSLContext;

        try {
            serverSecurityContext = createSecurityContext(
                _env,
                false,
                SERVER_CERTIFIED_PROPERTY,
                SERVER_KEYSTORE_PROPERTY,
                SERVER_KEYSTORE_TYPE_PROPERTY,
                SERVER_KEYSTORE_PROVIDER_PROPERTY,
                SERVER_KEYSTORE_PASSWORD_PROPERTY,
                SERVER_KEY_PASSWORD_PROPERTY,
                SERVER_TRUSTSTORE_PROPERTY,
                SERVER_TRUSTSTORE_TYPE_PROPERTY,
                SERVER_TRUSTSTORE_PROVIDER_PROPERTY,
                getThisLogger());
            serverSSLContext = (serverSecurityContext
                .isPresent())? serverSecurityContext
                    .get()
                    .createSSLContext(): null;
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        // Creates and enables the direct port relay to the server.

        if (!_setUpDirectPortRelay(serverSSLContext)) {
            return false;
        }

        // Creates and enables the controlled port relay to the server.

        if (_env.containsKey(CONTROLLED_ADDRESSES_PROPERTY)
                || _env.containsKey(CONTROL_ADDRESS_PROPERTY)) {
            if (!_setUpControlledPortRelay(serverSSLContext)) {
                return false;
            }
        }

        // Ensures that something has been configured.

        if ((_directPortManager == null) && (_controlledPortManager == null)) {
            getThisLogger().error(ValveMessages.NO_SERVICE_PORT);

            return false;
        }

        // Starts the service thread.

        startThread();

        return true;
    }

    /**
     * Sets the properties supplied by the MBean mechanism.
     *
     * @param properties The properties.
     */
    public void putProperties(@Nonnull final Properties properties)
    {
        _env = new Properties();
        _env.putAll(properties);
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        MemoryLogger
            .getInstance()
            .activate(
                Optional
                    .ofNullable(
                            _getElapsedProperty(
                                    _env,
                                            MEMORY_LOG_INTERVAL_PROPERTY,
                                            null)));
        startTimer();
        scheduleMidnightLogger();

        getThisLogger().info(ServiceMessages.SERVICE_RUNNING, getServiceName());

        try {
            if (_controlPort != null) {
                if (_controlPort.isInverted()) {
                    _controlledPortManager.resume();
                }

                _controlPort.listen();
            } else {
                _closeLatch.await();
            }
        } catch (final InterruptedException exception) {
            getThisLogger().warn(ServiceMessages.INTERRUPTED);
            Thread.currentThread().interrupt();
            fail();
        } finally {
            stopTimer();
            MemoryLogger.getInstance().deactivate();
        }
    }

    /**
     * Sets the environment properties file path.
     *
     * @param envPath The environment properties file path.
     */
    public void setEnvPath(@Nonnull final String envPath)
    {
        _envPath = envPath;
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateStats()
    {
        if (_controlPort != null) {
            _controlPort.updateStats(getStats());
        }

        if (_directPortManager != null) {
            _directPortManager.updateStats(getStats());
        }

        if (_controlledPortManager != null) {
            _controlledPortManager.updateStats(getStats());
        }
    }

    /**
     * Waits for control.
     *
     * <p>For framework tests.</p>
     *
     * @param control The value to wait for.
     *
     * @throws InterruptedException When interrupted.
     */
    public void waitForControl(
            final boolean control)
        throws InterruptedException
    {
        if (_controlPort != null) {
            _controlPort.waitForControl(control);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected ServiceStats createStats(final StatsOwner statsOwner)
    {
        return new ValveStats(statsOwner);
    }

    /**
     * Gets the connection filter.
     *
     * @return The connection filter.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Connection.Filter> getConnectionFilter()
    {
        return _connectionFilter;
    }

    /**
     * Gets the server address.
     *
     * @return The server address.
     */
    @Nonnull
    @CheckReturnValue
    SocketAddress getServerAddress()
    {
        return Require.notNull(_serverAddress);
    }

    private static ServerSocket _controlPortServerSocket(
            final Properties env,
            final Logger logger)
        throws IOException
    {
        final Optional<SecurityContext> securityContext = createSecurityContext(
            env,
            true,
            CONTROL_CERTIFIED_PROPERTY,
            CONTROL_KEYSTORE_PROPERTY,
            CONTROL_KEYSTORE_TYPE_PROPERTY,
            CONTROL_KEYSTORE_PROVIDER_PROPERTY,
            CONTROL_KEYSTORE_PASSWORD_PROPERTY,
            CONTROL_KEY_PASSWORD_PROPERTY,
            CONTROL_TRUSTSTORE_PROPERTY,
            CONTROL_TRUSTSTORE_TYPE_PROPERTY,
            CONTROL_TRUSTSTORE_PROVIDER_PROPERTY,
            logger);
        final ServerSocket serverSocket;

        if (securityContext.isPresent()) {
            serverSocket = securityContext
                .get()
                .createSSLContext()
                .getServerSocketFactory()
                .createServerSocket();
            ((SSLServerSocket) serverSocket)
                .setNeedClientAuth(securityContext.get().isCertified());
        } else {
            serverSocket = ServerSocketFactory
                .getDefault()
                .createServerSocket();
        }

        return serverSocket;
    }

    private static boolean _getBooleanProperty(
            final Properties env,
            final String property)
    {
        return ValueConverter
            .convertToBoolean(
                ServiceMessages.PROPERTY_TYPE.toString(),
                property,
                Optional.ofNullable(env.getProperty(property)),
                false);
    }

    private static int _getIntProperty(
            final Properties env,
            final String property,
            final int defaultValue)
    {
        return ValueConverter
            .convertToInteger(
                ServiceMessages.PROPERTY_TYPE.toString(),
                property,
                Optional.ofNullable(env.getProperty(property)),
                Optional.of(Integer.valueOf(defaultValue)))
            .get()
            .intValue();
    }

    private static InetSocketAddress _getSocketAddress(
            final Properties env,
            final String property,
            final Logger logger)
    {
        final String addressString = env.getProperty(property);

        if (addressString == null) {
            logger.error(ValveMessages.MISSING_PROPERTY, property);

            return null;
        }

        final Optional<InetSocketAddress> socketAddress = Inet
            .socketAddress(addressString);

        if (!socketAddress.isPresent()) {
            logger.error(ValveMessages.BAD_ADDRESS, addressString);
        }

        return socketAddress.orElse(null);
    }

    private static List<SocketAddress> _getSocketAddresses(
            final Properties env,
            final String property,
            final boolean required,
            final Logger logger,
            final Messages.Entry messagesEntry)
    {
        final String addressesString = env.getProperty(property);
        final List<SocketAddress> addresses = new LinkedList<SocketAddress>();

        if (addressesString != null) {
            final String[] addressStrings = ValueConverter
                .splitFields(addressesString.trim());

            if (addressStrings.length > 0) {
                for (final String addressString: addressStrings) {
                    final Optional<InetSocketAddress> address = Inet
                        .socketAddress(addressesString);

                    if (!address.isPresent()) {
                        logger.error(ValveMessages.BAD_ADDRESS, addressString);
                        addresses.clear();

                        break;
                    }

                    addresses.add(address.get());
                    logger.info(messagesEntry, address.get());
                }
            } else if (required) {
                logger.error(ValveMessages.EMPTY_PROPERTY, property);
            }
        } else if (required) {
            logger.error(ValveMessages.MISSING_PROPERTY, property);
        }

        return addresses;
    }

    private ElapsedTime _getElapsedProperty(
            final Properties env,
            final String property,
            final ElapsedTime defaultValue)
    {
        ElapsedTime elapsedTime;

        try {
            elapsedTime = ElapsedTime
                .fromString(Optional.ofNullable(env.getProperty(property)))
                .orElse(defaultValue);
        } catch (final IllegalArgumentException exception) {
            getThisLogger()
                .warn(BaseMessages.ILLEGAL_ARGUMENT, exception.getMessage());
            elapsedTime = defaultValue;
        }

        return elapsedTime;
    }

    private boolean _loadEnv()
    {
        try {
            final Enumeration<URL> resources = Thread
                .currentThread()
                .getContextClassLoader()
                .getResources(_envPath);

            if (!resources.hasMoreElements()) {
                throw new FileNotFoundException();
            }

            while (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                final Properties properties = new Properties();
                final InputStream stream;

                getThisLogger().debug(ValveMessages.LOADING_PROPERTIES, url);
                stream = url.openStream();
                properties.load(stream);
                stream.close();

                // Previously loaded properties have precedence.

                properties.putAll(_env);
                _env = properties;
            }
        } catch (final IOException exception) {
            getThisLogger()
                .error(
                    ValveMessages.PROPERTIES_LOAD_FAILED,
                    _envPath,
                    exception);

            return false;
        }

        return true;
    }

    private boolean _setUpControlledPortRelay(final SSLContext serverSSLContext)
    {
        final List<SocketAddress> addresses = _getSocketAddresses(
            _env,
            CONTROLLED_ADDRESSES_PROPERTY,
            true,
            getThisLogger(),
            ValveMessages.CONTROLLED_ADDRESS);

        if (addresses.isEmpty()) {
            return false;
        }

        final Optional<SSLContext> controlledSSLContext;
        final boolean controlledCertified;

        try {
            final Optional<SecurityContext> controlledSecurityContext =
                createSecurityContext(
                    _env,
                    true,
                    CONTROLLED_CERTIFIED_PROPERTY,
                    CONTROLLED_KEYSTORE_PROPERTY,
                    CONTROLLED_KEYSTORE_TYPE_PROPERTY,
                    CONTROLLED_KEYSTORE_PROVIDER_PROPERTY,
                    CONTROLLED_KEYSTORE_PASSWORD_PROPERTY,
                    CONTROLLED_KEY_PASSWORD_PROPERTY,
                    CONTROLLED_TRUSTSTORE_PROPERTY,
                    CONTROLLED_TRUSTSTORE_TYPE_PROPERTY,
                    CONTROLLED_TRUSTSTORE_PROVIDER_PROPERTY,
                    getThisLogger());

            if (controlledSecurityContext.isPresent()) {
                controlledSSLContext = Optional
                    .of(controlledSecurityContext.get().createSSLContext());
                controlledCertified = controlledSecurityContext
                    .get()
                    .isCertified();
            } else {
                controlledSSLContext = Optional.empty();
                controlledCertified = false;
            }
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        final int controlledConnectionsLimit = _getIntProperty(
            _env,
            CONTROLLED_CONNECTIONS_LIMIT_PROPERTY,
            Integer.MAX_VALUE);
        final ElapsedTime controlledHandshakeTimeout = _getElapsedProperty(
            _env,
            CONTROLLED_HANDSHAKE_TIMEOUT_PROPERTY,
            DEFAULT_HANDSHAKE_TIMEOUT);

        if (controlledConnectionsLimit < Integer.MAX_VALUE) {
            getThisLogger()
                .debug(
                    ValveMessages.CONTROLLED_CONNECTIONS_LIMIT,
                    String.valueOf(controlledConnectionsLimit));
        }

        if (controlledSSLContext.isPresent()) {
            getThisLogger()
                .debug(
                    ValveMessages.CONTROLLED_HANDSHAKE_TIMEOUT,
                    controlledHandshakeTimeout);
        }

        _controlledPortManager = new PortManager(
            this,
            true,
            controlledConnectionsLimit,
            controlledSSLContext,
            controlledCertified,
            controlledHandshakeTimeout.toMillis(),
            Optional.ofNullable(serverSSLContext));
        _controlledPortManager.start();

        for (final SocketAddress address: addresses) {
            if (!_controlledPortManager.addListenAddress(address)) {
                return false;
            }
        }

        // Opens the control port and starts listening.

        final SocketAddress controlAddress = _getSocketAddress(
            _env,
            CONTROL_ADDRESS_PROPERTY,
            getThisLogger());
        final ServerSocket controlPortServerSocket;

        if (controlAddress == null) {
            return false;
        }

        getThisLogger().info(ValveMessages.CONTROL_ADDRESS, controlAddress);

        try {
            controlPortServerSocket = _controlPortServerSocket(
                _env,
                getThisLogger());
        } catch (final IOException exception) {
            getThisLogger()
                .error(ValveMessages.SERVER_SOCKET, exception.getMessage());

            return false;
        }

        final ElapsedTime controlHandshakeTimeout = _getElapsedProperty(
            _env,
            CONTROL_HANDSHAKE_TIMEOUT_PROPERTY,
            DEFAULT_HANDSHAKE_TIMEOUT);
        final boolean controlInverted = _getBooleanProperty(
            _env,
            CONTROL_INVERTED_PROPERTY);

        if (controlPortServerSocket instanceof SSLServerSocket) {
            getThisLogger()
                .debug(
                    ValveMessages.CONTROL_HANDSHAKE_TIMEOUT,
                    controlHandshakeTimeout);
        }

        if (controlInverted) {
            getThisLogger().info(ValveMessages.CONTROL_INVERTED);
        }

        _controlPort = new ControlPort();

        if (!_controlPort
            .open(
                controlPortServerSocket,
                controlAddress,
                _controlledPortManager,
                (int) controlHandshakeTimeout.toMillis(),
                controlInverted)) {
            return false;
        }

        return true;
    }

    private boolean _setUpDirectPortRelay(final SSLContext serverSSLContext)
    {
        final List<SocketAddress> addresses = _getSocketAddresses(
            _env,
            DIRECT_ADDRESSES_PROPERTY,
            false,
            getThisLogger(),
            ValveMessages.DIRECT_ADDRESS);

        if (addresses.isEmpty()) {
            return true;
        }

        final Optional<SSLContext> directSSLContext;
        final boolean directCertified;

        try {
            final Optional<SecurityContext> directSecurityContext =
                createSecurityContext(
                    _env,
                    true,
                    DIRECT_CERTIFIED_PROPERTY,
                    DIRECT_KEYSTORE_PROPERTY,
                    DIRECT_KEYSTORE_TYPE_PROPERTY,
                    DIRECT_KEYSTORE_PROVIDER_PROPERTY,
                    DIRECT_KEYSTORE_PASSWORD_PROPERTY,
                    DIRECT_KEY_PASSWORD_PROPERTY,
                    DIRECT_TRUSTSTORE_PROPERTY,
                    DIRECT_TRUSTSTORE_TYPE_PROPERTY,
                    DIRECT_TRUSTSTORE_PROVIDER_PROPERTY,
                    getThisLogger());

            if (directSecurityContext.isPresent()) {
                directSSLContext = Optional
                    .of(directSecurityContext.get().createSSLContext());
                directCertified = directSecurityContext.get().isCertified();
            } else {
                directSSLContext = Optional.empty();
                directCertified = false;
            }
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        final int directConnectionsLimit = _getIntProperty(
            _env,
            DIRECT_CONNECTIONS_LIMIT_PROPERTY,
            Integer.MAX_VALUE);
        final ElapsedTime directHandshakeTimeout = _getElapsedProperty(
            _env,
            DIRECT_HANDSHAKE_TIMEOUT_PROPERTY,
            DEFAULT_HANDSHAKE_TIMEOUT);

        if (directConnectionsLimit < Integer.MAX_VALUE) {
            getThisLogger()
                .debug(
                    ValveMessages.DIRECT_CONNECTIONS_LIMIT,
                    String.valueOf(directConnectionsLimit));
        }

        if (directSSLContext.isPresent()) {
            getThisLogger()
                .debug(
                    ValveMessages.DIRECT_HANDSHAKE_TIMEOUT,
                    directHandshakeTimeout);
        }

        _directPortManager = new PortManager(
            this,
            false,
            _getIntProperty(
                _env,
                DIRECT_CONNECTIONS_LIMIT_PROPERTY,
                Integer.MAX_VALUE),
            directSSLContext,
            directCertified,
            directHandshakeTimeout.toMillis(),
            Optional.ofNullable(serverSSLContext));
        _directPortManager.start();

        for (final SocketAddress address: addresses) {
            if (!_directPortManager.addListenAddress(address)) {
                return false;
            }
        }

        _directPortManager.resume();

        return true;
    }

    /** The buffer size property. */
    public static final String BUFFER_SIZE_PROPERTY = "buffer.size";

    /** Controlled addresses property. */
    public static final String CONTROLLED_ADDRESSES_PROPERTY =
        "controlled.addresses";

    /** Controlled certified property. */
    public static final String CONTROLLED_CERTIFIED_PROPERTY =
        "controlled.certified";

    /** Controlled connections limit property. */
    public static final String CONTROLLED_CONNECTIONS_LIMIT_PROPERTY =
        "controlled.connections.limit";

    /** Controlled handshake timeout property. */
    public static final String CONTROLLED_HANDSHAKE_TIMEOUT_PROPERTY =
        "controlled.handshake.timeout";

    /** Controlled keystore password property. */
    public static final String CONTROLLED_KEYSTORE_PASSWORD_PROPERTY =
        "controlled.keystore.password";

    /** Controlled keystore property. */
    public static final String CONTROLLED_KEYSTORE_PROPERTY =
        "controlled.keystore";

    /** Controlled keystore provider property. */
    public static final String CONTROLLED_KEYSTORE_PROVIDER_PROPERTY =
        "controlled.keystore.provider";

    /** Controlled keystore type property. */
    public static final String CONTROLLED_KEYSTORE_TYPE_PROPERTY =
        "controlled.keystore.type";

    /** Controlled key password property. */
    public static final String CONTROLLED_KEY_PASSWORD_PROPERTY =
        "controlled.key.password";

    /** Controlled truststore property. */
    public static final String CONTROLLED_TRUSTSTORE_PROPERTY =
        "controlled.truststore";

    /** Controlled truststore provider property. */
    public static final String CONTROLLED_TRUSTSTORE_PROVIDER_PROPERTY =
        "controlled.truststore.provider";

    /** Controlled truststore type. */
    public static final String CONTROLLED_TRUSTSTORE_TYPE_PROPERTY =
        "controlled.truststore.type";

    /** Control address property. */
    public static final String CONTROL_ADDRESS_PROPERTY = "control.address";

    /** Control certified property. */
    public static final String CONTROL_CERTIFIED_PROPERTY = "control.certified";

    /** Control timeout while waiting for handshake. */
    public static final String CONTROL_HANDSHAKE_TIMEOUT_PROPERTY =
        "control.handshake.timeout";

    /** Inverted control property. */
    public static final String CONTROL_INVERTED_PROPERTY = "control.inverted";

    /** Control keystore password property. */
    public static final String CONTROL_KEYSTORE_PASSWORD_PROPERTY =
        "control.keystore.password";

    /** Control keystore property. */
    public static final String CONTROL_KEYSTORE_PROPERTY = "control.keystore";

    /** Control keystore provider property. */
    public static final String CONTROL_KEYSTORE_PROVIDER_PROPERTY =
        "control.keystore.provider";

    /** Control keystore type property. */
    public static final String CONTROL_KEYSTORE_TYPE_PROPERTY =
        "control.keystore.type";

    /** Control key password property. */
    public static final String CONTROL_KEY_PASSWORD_PROPERTY =
        "control.key.password";

    /** Control truststore property. */
    public static final String CONTROL_TRUSTSTORE_PROPERTY =
        "control.truststore";

    /** Control truststore provider property. */
    public static final String CONTROL_TRUSTSTORE_PROVIDER_PROPERTY =
        "control.truststore.provider";

    /** Control truststore type. */
    public static final String CONTROL_TRUSTSTORE_TYPE_PROPERTY =
        "control.truststore.type";

    /** Default buffer size. */
    public static final int DEFAULT_BUFFER_SIZE = 2048;

    /** Default environment properties path. */
    public static final String DEFAULT_ENV_PATH = "rvpf-valve.properties";

    /** Default handshake timeout. */
    public static final ElapsedTime DEFAULT_HANDSHAKE_TIMEOUT = ElapsedTime
        .fromMillis(60000);

    /** Direct addresses property. */
    public static final String DIRECT_ADDRESSES_PROPERTY = "direct.addresses";

    /** Direct certified property. */
    public static final String DIRECT_CERTIFIED_PROPERTY = "direct.certified";

    /** Direct connections limit property. */
    public static final String DIRECT_CONNECTIONS_LIMIT_PROPERTY =
        "direct.connections.limit";

    /** Direct handshake timeout property. */
    public static final String DIRECT_HANDSHAKE_TIMEOUT_PROPERTY =
        "direct.handshake.timeout";

    /** Direct keystore password property. */
    public static final String DIRECT_KEYSTORE_PASSWORD_PROPERTY =
        "direct.keystore.password";

    /** Direct keystore property. */
    public static final String DIRECT_KEYSTORE_PROPERTY = "direct.keystore";

    /** Direct keystore provider property. */
    public static final String DIRECT_KEYSTORE_PROVIDER_PROPERTY =
        "direct.keystore.provider";

    /** Direct keystore type property. */
    public static final String DIRECT_KEYSTORE_TYPE_PROPERTY =
        "direct.keystore.type";

    /** Direct key password property. */
    public static final String DIRECT_KEY_PASSWORD_PROPERTY =
        "direct.key.password";

    /** Direct truststore property. */
    public static final String DIRECT_TRUSTSTORE_PROPERTY = "direct.truststore";

    /** Direct truststore provider property. */
    public static final String DIRECT_TRUSTSTORE_PROVIDER_PROPERTY =
        "direct.truststore.provider";

    /** Direct truststore type. */
    public static final String DIRECT_TRUSTSTORE_TYPE_PROPERTY =
        "direct.truststore.type";

    /** Environment properties path. */
    public static final String ENV_PATH_PROPERTY = "rvpf.valve.properties";

    /** Filter class property. */
    public static final String FILTER_CLASS_PROPERTY = "filter.class";

    /** Memory log interval property. */
    public static final String MEMORY_LOG_INTERVAL_PROPERTY =
        "memory.log.interval";

    /** Server address. */
    public static final String SERVER_ADDRESS_PROPERTY = "server.address";

    /** Server certified property. */
    public static final String SERVER_CERTIFIED_PROPERTY = "server.certified";

    /** Server keystore password property. */
    public static final String SERVER_KEYSTORE_PASSWORD_PROPERTY =
        "server.keystore.password";

    /** Server keystore property. */
    public static final String SERVER_KEYSTORE_PROPERTY = "server.keystore";

    /** Server keystore provider property. */
    public static final String SERVER_KEYSTORE_PROVIDER_PROPERTY =
        "server.keystore.provider";

    /** Server keystore type property. */
    public static final String SERVER_KEYSTORE_TYPE_PROPERTY =
        "server.keystore.type";

    /** Server key password property. */
    public static final String SERVER_KEY_PASSWORD_PROPERTY =
        "server.key.password";

    /** Server truststore property. */
    public static final String SERVER_TRUSTSTORE_PROPERTY = "server.truststore";

    /** Server truststore provider property. */
    public static final String SERVER_TRUSTSTORE_PROVIDER_PROPERTY =
        "server.truststore.provider";

    /** Server truststore type. */
    public static final String SERVER_TRUSTSTORE_TYPE_PROPERTY =
        "server.truststore.type";

    /** Stats log enabled property. */
    public static final String STATS_LOG_ENABLED_PROPERTY = "stats.log.enabled";

    private final CountDownLatch _closeLatch = new CountDownLatch(1);
    private Optional<Connection.Filter> _connectionFilter;
    private ControlPort _controlPort;
    private PortManager _controlledPortManager;
    private PortManager _directPortManager;
    private Properties _env;
    private String _envPath;
    private InetSocketAddress _serverAddress;
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
