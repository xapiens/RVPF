/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MessagesPortServerModule.java 4080 2019-06-12 20:21:38Z SFB $
 */

package org.rvpf.forwarder.input;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

import java.nio.charset.StandardCharsets;

import java.rmi.AccessException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.security.Identity;
import org.rvpf.base.security.Realm;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.security.ServerSecurityContext;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.base.xml.streamer.Streamer;
import org.rvpf.forwarder.BatchControl;
import org.rvpf.forwarder.ForwarderMessages;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * Messages port server module.
 */
public final class MessagesPortServerModule
    extends InputModule
{
    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        final Collection<_Receiver> receivers;

        synchronized (_receivers) {
            receivers = new ArrayList<>(_receivers);
        }

        for (final _Receiver receiver: receivers) {
            receiver._close();
        }

        if (_streamer != null) {
            _streamer.tearDown();
            _streamer = null;
        }

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups moduleProperties)
    {
        final String addressString = moduleProperties
            .getString(ADDRESS_PROPERTY)
            .orElse(null);

        if (addressString == null) {
            getThisLogger()
                .error(BaseMessages.MISSING_PROPERTY, ADDRESS_PROPERTY);

            return false;
        }

        getThisLogger().info(ForwarderMessages.ADDRESS, addressString);

        final Optional<InetSocketAddress> socketAddress = Inet
            .socketAddress(addressString);

        if (!socketAddress.isPresent()) {
            getThisLogger().error(BaseMessages.BAD_ADDRESS, addressString);

            return false;
        }

        if (!Inet.isOnLocalHost(socketAddress.get().getAddress())) {
            getThisLogger()
                .warn(
                    ServiceMessages.LISTEN_ADDRESS_NOT_LOCAL,
                    socketAddress.get().getAddress());

            return false;
        }

        final ServerSecurityContext securityContext = new ServerSecurityContext(
            getThisLogger());
        final KeyedGroups securityProperties = moduleProperties
            .getGroup(SecurityContext.SECURITY_PROPERTIES);

        if (!securityContext.setUp(getConfigProperties(), securityProperties)) {
            return false;
        }

        final boolean secure = securityContext.isCertified()
                || securityContext.isSecure()
                || (!securityProperties.isEmpty())
                || !socketAddress.get().getAddress().isLoopbackAddress();
        final ServerSocketFactory factory;

        if (secure) {
            try {
                securityContext.checkForSecureOperation();
            } catch (final SSLException exception) {
                getThisLogger()
                    .error(BaseMessages.VERBATIM, exception.getMessage());

                return false;
            }

            try {
                factory = securityContext
                    .createSSLContext()
                    .getServerSocketFactory();
            } catch (final SSLException exception) {
                getThisLogger()
                    .error(
                        exception,
                        ServiceMessages.SSL_CREATE_FAILED,
                        exception.getMessage());

                return false;
            }
        } else {
            factory = ServerSocketFactory.getDefault();
        }

        final ServerSocket serverSocket;

        try {
            serverSocket = factory.createServerSocket();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        if (secure) {
            ((SSLServerSocket) serverSocket)
                .setNeedClientAuth(securityContext.isCertified());
        }

        _roles = moduleProperties.getStrings(ROLE_PROPERTY);

        if (_roles.length > 0) {
            for (final String role: _roles) {
                getThisLogger().info(ForwarderMessages.AUTHORIZED_ROLE, role);
            }

            final KeyedGroups realmProperties = securityContext
                .getRealmProperties();

            if (realmProperties.isMissing()) {
                getThisLogger().error(ServiceMessages.ROLES_REQUIRE_REALM);

                return false;
            }

            if (!_realm.setUp(realmProperties, securityContext)) {
                return false;
            }
        }

        _streamer = Streamer.newInstance();

        if (!_streamer
            .setUp(
                Optional.of(getConfigProperties()),
                Optional.of(moduleProperties))) {
            return false;
        }

        setInput(
            new _PortListener(
                serverSocket,
                socketAddress.get(),
                secure
                ? (securityContext.isCertified()
                   ? BaseMessages.CONNECTION_CERTIFIED
                           .toString(): BaseMessages.CONNECTION_SECURE.toString()): BaseMessages.CONNECTION_LOCAL
                                   .toString(),
                getBatchControl().getWait()));

        return super.setUp(moduleProperties);
    }

    /**
     * Gets the queue.
     *
     * @return The queue.
     */
    @Nonnull
    @CheckReturnValue
    BlockingQueue<List<Serializable>> _getQueue()
    {
        return _queue;
    }

    /**
     * Gets the users roles.
     *
     * @return The users roles.
     */
    @Nonnull
    @CheckReturnValue
    Realm _getRealm()
    {
        return Require.notNull(_realm);
    }

    /**
     * Gets the receivers.
     *
     * @return The receivers.
     */
    @Nonnull
    @CheckReturnValue
    Set<_Receiver> _getReceivers()
    {
        return _receivers;
    }

    /**
     * Gets the roles.
     *
     * @return The roles.
     */
    @Nonnull
    @CheckReturnValue
    String[] _getRoles()
    {
        return _roles;
    }

    /**
     * Gets the streamer.
     *
     * @return The streamer.
     */
    @Nonnull
    @CheckReturnValue
    Streamer _getStreamer()
    {
        return Require.notNull(_streamer);
    }

    /** Specifies on which address to listen. */
    public static final String ADDRESS_PROPERTY = "address";

    /** Client attribute. */
    public static final String CLIENT_ATTRIBUTE = "client";

    /** Flush attribute. */
    public static final String FLUSH_ATTRIBUTE = "flush";

    /** Flush element name. */
    public static final String FLUSH_ELEMENT = "flush";

    /** ID attribute. */
    public static final String ID_ATTRIBUTE = "id";

    /** Login element name. */
    public static final String LOGIN_ELEMENT = "login";

    /** Messages element name. */
    public static final String MESSAGES_ELEMENT = "messages";

    /** Password attribute. */
    public static final String PASSWORD_ATTRIBUTE = "password";

    /** A security role allowing access to the service. */
    public static final String ROLE_PROPERTY = "role";

    /** User attribute. */
    public static final String USER_ATTRIBUTE = "user";

    private final BlockingQueue<List<Serializable>> _queue =
        new SynchronousQueue<>();
    private final Realm _realm = new Realm();
    private final Set<_Receiver> _receivers = new HashSet<>();
    private volatile String[] _roles;
    private volatile Streamer _streamer;

    /**
     * Port listener.
     */
    private final class _PortListener
        extends AbstractInput
        implements ServiceThread.Target
    {
        /**
         * Constructs an instance.
         *
         * @param serverSocket The server socket.
         * @param socketAddress The socket address
         * @param comment A message comment.
         * @param batchWait The optional batch wait time.
         */
        _PortListener(
                @Nonnull final ServerSocket serverSocket,
                @Nonnull final SocketAddress socketAddress,
                @Nonnull final String comment,
                @Nonnull final Optional<ElapsedTime> batchWait)
        {
            _serverSocket = serverSocket;
            _socketAddress = socketAddress;
            _comment = comment;
            _batchWait = batchWait;
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            if (!_serverSocket.isClosed()) {
                final ServiceThread thread = _thread.getAndSet(null);

                if (thread != null) {
                    getThisLogger()
                        .debug(
                            ServiceMessages.STOPPING_THREAD,
                            thread.getName());

                    try {
                        _serverSocket.close();
                    } catch (final IOException exception) {
                        throw new RuntimeException(exception);
                    }

                    Require
                        .ignored(
                            thread
                                .join(_logger, getService().getJoinTimeout()));
                }
            }
        }

        /** {@inheritDoc}
         */
        @SuppressWarnings({"NotifyWithoutCorrespondingWait"})
        @Override
        public boolean commit()
        {
            final List<Serializable> messages = _messages;

            synchronized (messages) {
                messages.clear();
                messages.notifyAll();
            }

            _messages = null;

            return super.commit();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getDisplayName()
        {
            return "Streamed messages port";
        }

        /** {@inheritDoc}
         */
        @Override
        public String getSourceName()
        {
            return _socketAddress.toString();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<Serializable[]> input(
                final BatchControl batchControl)
            throws InterruptedException
        {
            for (;;) {
                _messages = _getQueue().take();

                if (!_messages.isEmpty()) {
                    break;
                }

                commit();
            }

            return Optional
                .of(_messages.toArray(new Serializable[_messages.size()]));
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return _serverSocket.isClosed();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isReliable()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean open()
        {
            final ServiceThread thread = new ServiceThread(
                this,
                "XML input port (listener on "
                + _serverSocket.getLocalSocketAddress() + ")");

            if (_thread.compareAndSet(null, thread)) {
                getThisLogger()
                    .debug(ServiceMessages.STARTING_THREAD, thread.getName());
                thread.start();
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void run()
        {
            _logger
                .debug(
                    ForwarderMessages.LISTENING_ON_PORT,
                    _serverSocket.getLocalSocketAddress(),
                    _comment);

            for (;;) {
                final Socket socket;

                try {
                    socket = _serverSocket.accept();

                    if (socket instanceof SSLSocket) {
                        try {
                            ((SSLSocket) socket).startHandshake();
                            socket.setKeepAlive(true);
                        } catch (final IOException exception) {
                            _logger
                                .warn(
                                    ForwarderMessages.BAD_CONNECTION,
                                    _serverSocket.getLocalSocketAddress(),
                                    exception.getMessage());
                            socket.close();

                            continue;
                        }
                    }
                } catch (final SocketException exception) {
                    break;
                } catch (final IOException exception) {
                    throw new RuntimeException(exception);
                }

                _logger
                    .debug(
                        () -> new Message(
                            ForwarderMessages.CONNECTION_ACCEPTED,
                            _comment,
                            socket.getRemoteSocketAddress()));

                if (!new _Receiver(_batchWait)._open(socket)) {
                    try {
                        socket.close();
                    } catch (final IOException exception) {
                        // Ignores.
                    }

                    _logger
                        .debug(
                            () -> new Message(
                                ForwarderMessages.CONNECTION_CANCELLED,
                                socket.getRemoteSocketAddress()));
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(final KeyedGroups moduleProperties)
        {
            if (!super.setUp(moduleProperties)) {
                return false;
            }

            try {
                _serverSocket.bind(_socketAddress);
            } catch (final IOException exception) {
                _logger
                    .error(
                        ServiceMessages.BIND_FAILED_,
                        _socketAddress,
                        exception);

                return false;
            }

            if (!_serverSocket.isBound()) {
                _logger.error(ServiceMessages.BIND_FAILED, _socketAddress);

                return false;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            close();

            super.tearDown();
        }

        private final Optional<ElapsedTime> _batchWait;
        private final String _comment;
        private final Logger _logger = Logger.getInstance(getClass());
        private List<Serializable> _messages;
        private final ServerSocket _serverSocket;
        private final SocketAddress _socketAddress;
        private final AtomicReference<ServiceThread> _thread =
            new AtomicReference<>();
    }


    /**
     * Receiver.
     */
    private final class _Receiver
        implements ServiceThread.Target
    {
        /**
         * Constructs an instance.
         *
         * @param batchWait The batch wait time.
         */
        _Receiver(@Nonnull final Optional<ElapsedTime> batchWait)
        {
            _setClient(Optional.empty());    // Initializes the logger.
            _setAuthorized(_getRoles().length == 0);

            _document
                .setElementHandler(
                    '/' + MESSAGES_ELEMENT,
                    Optional.of(new _MessagesHandler()));
            _document
                .setElementHandler(
                    '/' + LOGIN_ELEMENT,
                    Optional.of(new _LoginHandler()));
            _document
                .setElementHandler(
                    '/' + FLUSH_ELEMENT,
                    Optional.of(new _FlushHandler()));
            _batchWait = batchWait;
        }

        /**
         * Gets the batch wait.
         *
         * @return The optional batch wait.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<ElapsedTime> getBatchWait()
        {
            return _batchWait;
        }

        /** {@inheritDoc}
         */
        @Override
        public void run()
        {
            try {
                final Reader reader = _reader;

                for (;;) {
                    try {
                        _document.parse(reader);
                        reader.reset();
                    } catch (final XMLDocument.ParseException exception) {
                        if (reader.markSupported()) {
                            throw exception;
                        }

                        break;
                    }
                }

                _close();
            } catch (final Exception exception) {
                _failed(exception);
                _close();
            }
        }

        /**
         * Adds a message.
         *
         * @param message The message.
         */
        void _addMessage(@Nonnull final Serializable message)
        {
            _getLogger().trace(ForwarderMessages.DECODED_VALUE, message);

            synchronized (this) {
                if (_messages.size() >= getBatchControl().getLimit()) {
                    _flush(ForwarderMessages.FLUSH_BATCH_LIMIT, false);
                }

                _messages.add(message);
            }

            getTraces().add(_client, message);
        }

        /**
         * Checks if authorized.
         *
         * @throws XMLDocument.ParseException When not authorized.
         */
        void _checkAuthorized()
            throws XMLDocument.ParseException
        {
            if (!_authorized) {
                throw new XMLDocument.ParseException(
                    new AccessException(
                        Message.format(ForwarderMessages.LOGIN_NEEDED)));
            }
        }

        /**
         * Closes this.
         */
        synchronized void _close()
        {
            final _Monitor monitor = _monitor.getAndSet(null);

            if (monitor != null) {
                final Thread monitorThread = monitor.getThread();

                getThisLogger()
                    .debug(
                        ServiceMessages.STOPPING_THREAD,
                        monitorThread.getName());
                monitorThread.interrupt();

                if (!_messages.isEmpty()) {
                    _flush(ForwarderMessages.FLUSH_CLOSE, true);
                }

                if (!_socket.isClosed()) {
                    final SocketAddress socketAddress = _socket
                        .getRemoteSocketAddress();

                    try {
                        _output.close();
                        _input.close();
                        _socket.close();
                    } catch (final IOException exception) {
                        _getLogger()
                            .warn(
                                ForwarderMessages.CONNECTION_CLOSE_FAILED,
                                socketAddress,
                                exception.getMessage());

                        return;
                    }

                    _getLogger()
                        .debug(
                            ForwarderMessages.CONNECTION_CLOSED,
                            socketAddress);
                }
            }
        }

        /**
         * Responds with 'done'.
         *
         * <p>NOTE. Some clients may depend on the exact representation
         * generated here to avoid the overhead or complexity of full XML
         * parsing.</p>
         */
        void _done()
        {
            final StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("<done");

            if ((_reference != null) && (_reference.length() > 0)) {
                stringBuilder.append(" ref='");
                stringBuilder.append(_reference);
                stringBuilder.append("'");
                _reference = null;
            }

            stringBuilder.append("/>\n");

            _respond(stringBuilder);
        }

        /**
         * Responds with 'failed'.
         *
         * <p>NOTE. Some clients may depend on the exact representation
         * generated here to avoid the overhead or complexity of full XML
         * parsing.</p>
         *
         * @param cause The cause of failure.
         */
        void _failed(@Nonnull final Exception cause)
        {
            final StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("<failed");

            if ((_reference != null) && (_reference.length() > 0)) {
                stringBuilder.append(" ref='");
                stringBuilder.append(_reference);
                stringBuilder.append("'");
            }

            stringBuilder.append(" exception='");
            stringBuilder.append(cause.getClass().getName());
            stringBuilder.append("'>");

            if (cause.getMessage() != null) {
                stringBuilder
                    .append(XMLElement.escape(cause.getMessage().trim(), '\''));
            }

            stringBuilder.append("</failed>\n");

            _respond(stringBuilder);
        }

        /**
         * Flushes the accumulated messages.
         *
         * @param reason The reason for the flush.
         * @param wait Wait for output completed.
         */
        void _flush(@Nonnull final Messages.Entry reason, final boolean wait)
        {
            final List<Serializable> messages;

            _getLogger().debug(ForwarderMessages.FLUSHING_MESSAGES, reason);

            synchronized (this) {
                if (_messages.isEmpty()) {
                    _getLogger().debug(ForwarderMessages.FLUSHED_NOTHING);

                    return;
                }

                messages = _messages;
                _messages = new LinkedList<>();
            }

            final int count = messages.size();

            try {
                _getQueue().put(messages);
                _getLogger().trace(ForwarderMessages.QUEUED_MESSAGES);
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();

                throw new RuntimeException(exception);
            }

            getTraces().commit();
            _getLogger()
                .debug(
                    ForwarderMessages.FLUSHED_MESSAGES,
                    String.valueOf(count),
                    Integer.valueOf(count));

            if (wait) {
                synchronized (messages) {
                    while (!messages.isEmpty()) {
                        try {
                            messages.wait();
                        } catch (final InterruptedException exception) {
                            Thread.currentThread().interrupt();

                            throw new RuntimeException(exception);
                        }
                    }
                }
            }
        }

        /**
         * Gets the logger.
         *
         * @return The logger.
         */
        @Nonnull
        @CheckReturnValue
        Logger _getLogger()
        {
            return _logger;
        }

        /**
         * Gets the time.
         *
         * @return The time.
         */
        @CheckReturnValue
        long _getTime()
        {
            return _time;
        }

        /**
         * Keeps the reference to an element.
         *
         * @param element The element.
         */
        void _keepReference(@Nonnull final XMLElement element)
        {
            _reference = element
                .getAttributeValue(ID_ATTRIBUTE, Optional.empty())
                .orElse(null);
        }

        /**
         * Opens this.
         *
         * @param socket The conversation socket.
         *
         * @return True on success.
         */
        @CheckReturnValue
        boolean _open(@Nonnull final Socket socket)
        {
            _messages = new LinkedList<>();

            synchronized (this) {
                _socket = socket;

                try {
                    _input = socket.getInputStream();
                    _output = socket.getOutputStream();

                    final XMLDocument.ElementReader reader =
                        new XMLDocument.ElementReader(
                            _input);

                    _document.setRootHandler(Optional.of(reader));
                    _reader = reader;
                } catch (final IOException exception) {
                    _getLogger()
                        .error(
                            ForwarderMessages.GET_STREAM_FAILED,
                            socket.getRemoteSocketAddress(),
                            exception.getMessage());

                    return false;
                }

                final _Monitor monitor = new _Monitor(
                    socket.getRemoteSocketAddress());

                if (_monitor.compareAndSet(null, monitor)) {
                    final ServiceThread monitorThread = monitor.getThread();

                    getThisLogger()
                        .debug(
                            ServiceMessages.STARTING_THREAD,
                            monitorThread.getName());
                    monitorThread.start();
                }
            }

            final ServiceThread receiverThread = new ServiceThread(
                this,
                "Streamed messages port (receiver on "
                + socket.getRemoteSocketAddress() + ")");

            if (_receiverThread.compareAndSet(null, receiverThread)) {
                synchronized (_getReceivers()) {
                    _getReceivers().add(this);
                }

                getThisLogger()
                    .debug(
                        ServiceMessages.STARTING_THREAD,
                        receiverThread.getName());
                receiverThread.start();
            }

            return true;
        }

        /**
         * Sets the authorized indicator.
         *
         * @param authorized The authorized incicator.
         */
        void _setAuthorized(final boolean authorized)
        {
            _authorized = authorized;
        }

        /**
         * Sets the client.
         *
         * @param client The optional client.
         */
        void _setClient(@Nonnull final Optional<String> client)
        {
            _client = client;
            _logger = Logger.getInstance(getClass());

            if (_client.isPresent()) {
                _logger.debug(ForwarderMessages.CLIENT, _client.get());
                _logger = Logger
                    .getInstance(getClass().getName() + ':' + _client.get());
            }
        }

        /**
         * Sets the time.
         *
         * @param time The time.
         */
        void _setTime(final long time)
        {
            _time = time;
        }

        @CheckReturnValue
        private boolean _respond(@Nonnull final StringBuilder response)
        {
            final byte[] bytes = response
                .toString()
                .getBytes(StandardCharsets.UTF_8);

            try {
                _output.write(bytes);
            } catch (final IOException exception) {
                _getLogger()
                    .warn(
                        ForwarderMessages.FAILED_TO_RESPOND,
                        exception.getMessage());

                return false;
            }

            return true;
        }

        private boolean _authorized;
        private final Optional<ElapsedTime> _batchWait;
        private Optional<String> _client;
        private final XMLDocument _document = new XMLDocument();
        private InputStream _input;
        private Logger _logger;
        private volatile List<Serializable> _messages;
        private final AtomicReference<_Monitor> _monitor =
            new AtomicReference<>();
        private volatile OutputStream _output;
        private volatile Reader _reader;
        private final AtomicReference<ServiceThread> _receiverThread =
            new AtomicReference<>();
        private String _reference;
        private volatile Socket _socket;
        private volatile long _time;

        /**
         * Flush handler.
         */
        private final class _FlushHandler
            implements XMLElement.Handler
        {
            /**
             * Constructs an instance.
             */
            _FlushHandler() {}

            /** {@inheritDoc}
             */
            @Override
            public XMLElement onElementEnd(final XMLElement element)
            {
                _flush(ForwarderMessages.FLUSH_REQUEST, true);
                _done();

                return element;
            }

            /** {@inheritDoc}
             */
            @Override
            public XMLElement onElementStart(
                    final XMLElement element)
                throws XMLDocument.ParseException
            {
                _checkAuthorized();
                _keepReference(element);

                return element;
            }
        }


        /**
         * Login handler.
         */
        private final class _LoginHandler
            implements XMLElement.Handler
        {
            /**
             * Constructs an instance.
             */
            _LoginHandler() {}

            /** {@inheritDoc}
             */
            @Override
            public XMLElement onElementEnd(
                    final XMLElement element)
                throws XMLDocument.ParseException
            {
                _setClient(
                    element
                        .getAttributeValue(CLIENT_ATTRIBUTE, Optional.empty()));

                final Optional<String> identifier = element
                    .getAttributeValue(USER_ATTRIBUTE, Optional.empty());
                final Optional<String> password = element
                    .getAttributeValue(PASSWORD_ATTRIBUTE, Optional.of(""));

                try {
                    if ((!identifier.isPresent())
                            || identifier.get().trim().isEmpty()) {
                        final String message = Message
                            .format(
                                ForwarderMessages.USER_ATTRIBUTE,
                                USER_ATTRIBUTE);

                        _getLogger().warn(BaseMessages.VERBATIM, message);

                        throw new AccessException(message);
                    }

                    final String[] roles = _getRoles();

                    if (roles.length > 0) {
                        final Optional<Identity> identity = _getRealm()
                            .authenticate(
                                identifier.get(),
                                password.get().toCharArray());

                        _setAuthorized(false);

                        if (!identity.isPresent()) {
                            final String message = Message
                                .format(
                                    ForwarderMessages.AUTHENTICATION_FAILED,
                                    identifier.get());

                            _getLogger().warn(BaseMessages.VERBATIM, message);

                            throw new AccessException(message);
                        }

                        if (!identity.get().isInRoles(roles)) {
                            final String message = Message
                                .format(
                                    ForwarderMessages.NOT_AUTHORIZED,
                                    identifier.get());

                            _getLogger().warn(BaseMessages.VERBATIM, message);

                            throw new AccessException(message);
                        }

                        _setAuthorized(true);
                        _getLogger()
                            .debug(
                                ForwarderMessages.AUTHORIZED,
                                identifier.get());
                    }
                } catch (final AccessException exception) {
                    throw new XMLDocument.ParseException(exception);
                }

                _done();

                return element;
            }

            /** {@inheritDoc}
             */
            @Override
            public XMLElement onElementStart(final XMLElement element)
            {
                _keepReference(element);

                return element;
            }
        }


        /**
         * Messages handler.
         */
        private final class _MessagesHandler
            extends XMLElement
            implements XMLElement.Handler
        {
            /**
             * Constructs an instance.
             */
            _MessagesHandler()
            {
                super(MESSAGES_ELEMENT);
            }

            /** {@inheritDoc}
             */
            @Override
            public void addChild(final XMLElement element)
            {
                if (getChildCount() >= getBatchControl().getLimit()) {
                    _queue();
                }

                super.addChild(element);
            }

            /** {@inheritDoc}
             */
            @Override
            public XMLElement copy()
            {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc}
             */
            @Override
            public XMLElement onElementEnd(
                    final XMLElement element)
                throws XMLDocument.ParseException
            {
                _queue();

                if (ValueConverter
                    .convertToBoolean(
                        ServiceMessages.ATTRIBUTE_TYPE.toString(),
                        FLUSH_ATTRIBUTE,
                        element
                            .getAttributeValue(
                                    FLUSH_ATTRIBUTE,
                                            Optional.empty()),
                        false)) {
                    _flush(ForwarderMessages.FLUSH_REQUEST, true);
                }

                _done();

                return element;
            }

            /** {@inheritDoc}
             *
             * @throws XMLDocument.ParseException
             */
            @Override
            public XMLElement onElementStart(
                    final XMLElement element)
                throws XMLDocument.ParseException
            {
                _checkAuthorized();
                _keepReference(element);

                setAttribute(
                    FLUSH_ATTRIBUTE,
                    element
                        .getAttributeValue(FLUSH_ATTRIBUTE, Optional.empty()));

                return this;
            }

            private void _queue()
            {
                if (getChildCount() > 0) {
                    final Streamer.Input streamerInput = _getStreamer()
                        .newInput(this);

                    for (final Serializable message: streamerInput) {
                        _addMessage(message);
                    }

                    streamerInput.close();
                    disownChildren();
                }
            }
        }


        /**
         * Monitor.
         */
        private final class _Monitor
            implements ServiceThread.Target
        {
            /**
             * Constructs an instance.
             *
             * @param socketAddress The socket address.
             */
            _Monitor(final SocketAddress socketAddress)
            {
                _socketAddress = socketAddress;
            }

            /** {@inheritDoc}
             */
            @Override
            public synchronized void run()
                throws InterruptedException
            {
                final Optional<ElapsedTime> batchWait = getBatchWait();

                for (;;) {
                    if (_getTime() == 0) {
                        wait();
                    } else {
                        final long elapsedMillis = System
                            .currentTimeMillis() - _getTime();

                        if ((elapsedMillis < 0)
                                || !batchWait.isPresent()
                                || (elapsedMillis
                                    >= batchWait.get().toMillis())) {
                            _flush(ForwarderMessages.FLUSH_TIME_LIMIT, true);
                            _setTime(0);
                        } else {
                            wait(batchWait.get().toMillis() - elapsedMillis);
                        }
                    }
                }
            }

            /**
             * Gets the thread.
             *
             * @return The thread.
             */
            @Nonnull
            @CheckReturnValue
            ServiceThread getThread()
            {
                ServiceThread thread = _thread.get();

                if (thread == null) {
                    thread = new ServiceThread(
                        this,
                        "Streamed messages port monitor (" + _socketAddress
                        + ")");

                    if (_thread.compareAndSet(null, thread)) {
                        thread
                            .setUncaughtExceptionHandler(
                                Thread
                                    .currentThread()
                                    .getUncaughtExceptionHandler());
                    }
                }

                return thread;
            }

            private final SocketAddress _socketAddress;
            private final AtomicReference<ServiceThread> _thread =
                new AtomicReference<ServiceThread>();
        }
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
