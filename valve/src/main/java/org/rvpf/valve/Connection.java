/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Connection.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.valve;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.Socket;

import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.tool.Require;

/**
 * Connection.
 *
 * <p>Instances of this class implement relayed connections.</p>
 */
public final class Connection
{
    /**
     * Constructs an instance.
     *
     * <p>On return, both directions of the connection are enabled for read.</p>
     *
     * @param connectionsManager The connections manager.
     * @param clientKey The client selection key.
     * @param serverKey The server selection key.
     * @param isControlled True if controlled.
     * @param filter The connection filter.
     * @param clientSSLContext The client SSL context.
     * @param clientCertified True for certified client.
     * @param serverSSLContext The server SSL context.
     */
    Connection(
            @Nonnull final ConnectionsManager connectionsManager,
            @Nonnull final SelectionKey clientKey,
            @Nonnull final SelectionKey serverKey,
            final boolean isControlled,
            @Nonnull final Optional<Filter> filter,
            @Nonnull final Optional<SSLContext> clientSSLContext,
            final boolean clientCertified,
            @Nonnull final Optional<SSLContext> serverSSLContext)
    {
        _connectionsManager = connectionsManager;
        _filter = filter;
        _isControlled = isControlled;

        _clientSSLEngine = _createSSLEngine(clientSSLContext, clientKey);

        if (_clientSSLEngine.isPresent()) {
            _clientSSLEngine.get().setUseClientMode(false);
            _clientSSLEngine.get().setNeedClientAuth(clientCertified);
        }

        _serverSSLEngine = _createSSLEngine(serverSSLContext, serverKey);

        if (_serverSSLEngine.isPresent()) {
            _serverSSLEngine.get().setUseClientMode(true);
        }

        _clientServer = new Direction(
            clientKey,
            serverKey,
            true,
            Optional.empty());
        _serverClient = new Direction(
            serverKey,
            clientKey,
            false,
            Optional.of(_clientServer));
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final Socket clientSocket = ((SocketChannel) _clientServer
            .getInputKey()
            .channel())
            .socket();
        final Socket serverSocket = ((SocketChannel) _clientServer
            .getOutputKey()
            .channel())
            .socket();

        return Message
            .format(
                ValveMessages.CONNECTION_STRING,
                _isControlled()? ValveMessages.CONTROLLED: ValveMessages.DIRECT,
                clientSocket.getLocalSocketAddress(),
                clientSocket.getRemoteSocketAddress(),
                serverSocket.getLocalSocketAddress(),
                serverSocket.getRemoteSocketAddress());
    }

    /**
     * Allocates a network buffer.
     *
     * @param sslEngine The SSL engine.
     * @param selectionKey The selection key.
     * @param messagesEntry The messages entry.
     *
     * @return The network buffer.
     */
    @Nonnull
    @CheckReturnValue
    static ByteBuffer _allocateNetworkBuffer(
            @Nonnull final SSLEngine sslEngine,
            @Nonnull final SelectionKey selectionKey,
            @Nonnull final Messages.Entry messagesEntry)
    {
        final int networkSize = sslEngine.getSession().getPacketBufferSize();

        if (_getDirectionLogger().isTraceEnabled()) {
            final Socket inputSocket = ((SocketChannel) selectionKey.channel())
                .socket();

            _getDirectionLogger()
                .trace(
                    messagesEntry,
                    inputSocket.getLocalSocketAddress(),
                    inputSocket.getRemoteSocketAddress(),
                    String.valueOf(networkSize));
        }

        return ByteBuffer.allocate(networkSize);
    }

    /**
     * Clears an operation for a selection key.
     *
     * @param key The selection key.
     * @param op The operation.
     *
     * @return False if the key has been cancelled.
     */
    static boolean _clear(@Nonnull final SelectionKey key, final int op)
    {
        try {
            key.interestOps(key.interestOps() & ~op);
        } catch (final CancelledKeyException exception) {
            return false;
        }

        return true;
    }

    /**
     * Gets the connection logger.
     *
     * @return The connection logger.
     */
    @Nonnull
    @CheckReturnValue
    static Logger _getConnectionLogger()
    {
        return _CONNECTION_LOGGER;
    }

    /**
     * Gets the direction logger.
     *
     * @return The direction logger.
     */
    @Nonnull
    @CheckReturnValue
    static Logger _getDirectionLogger()
    {
        return _DIRECTION_LOGGER;
    }

    /**
     * Asks if a selection is ready for an operation.
     *
     * @param selectionKey The selection key.
     * @param op The operation.
     *
     * @return True if the selection is ready.
     */
    @CheckReturnValue
    static boolean _isReady(
            @Nonnull final SelectionKey selectionKey,
            final int op)
    {
        return (selectionKey.interestOps() & selectionKey.readyOps() & op) != 0;
    }

    /**
     * Sets an operation for a selection key.
     *
     * @param key The selection key.
     * @param op The operation.
     */
    static void _set(@Nonnull final SelectionKey key, final int op)
    {
        key.interestOps(key.interestOps() | op);
    }

    /**
     * Cancels the handshake timer.
     */
    synchronized void _cancelHandshakeTimeout()
    {
        if (_handshakeTimeout != null) {
            _handshakeTimeout.cancel();
            _handshakeTimeout = null;
        }
    }

    /**
     * Filters a message for a direction.
     *
     * @param direction The direction.
     *
     * @return True if a filter was called.
     */
    @CheckReturnValue
    boolean _filter(@Nonnull final Direction direction)
    {
        if (!_filter.isPresent()) {
            return false;
        }

        if (direction.isFromClient()) {
            _filter.get().onClientData(direction);
        } else {
            _filter.get().onServerData(direction);
        }

        return true;
    }

    /**
     * Gets the client SSL engine.
     *
     * @return The optional client SSL engine.
     */
    @Nonnull
    @CheckReturnValue
    Optional<SSLEngine> _getClientSSLEngine()
    {
        return _clientSSLEngine;
    }

    /**
     * Gets the server SSL engine.
     *
     * @return The optional server SSL engine.
     */
    @Nonnull
    @CheckReturnValue
    Optional<SSLEngine> _getServerSSLEngine()
    {
        return _serverSSLEngine;
    }

    /**
     * Gets the controlled indicator.
     *
     * @return The controlled indicator.
     */
    @CheckReturnValue
    boolean _isControlled()
    {
        return _isControlled;
    }

    /**
     * Gets the server connected indicator.
     *
     * @return The server connected indicator.
     */
    @CheckReturnValue
    boolean _isServerConnected()
    {
        return _isServerConnected;
    }

    /**
     * Closes this connection.
     */
    synchronized void close()
    {
        if (!_closed) {
            final Socket clientSocket = ((SocketChannel) _clientServer
                .getInputKey()
                .channel())
                .socket();

            _closed = true;
            _getConnectionLogger()
                .debug(
                    ValveMessages.CLOSING_CONNECTION,
                    _isControlled()
                    ? ValveMessages.CONTROLLED: ValveMessages.DIRECT,
                    clientSocket.getRemoteSocketAddress());
            _clientServer.close();
            _serverClient.close();

            _connectionsManager.closed(this);
        }
    }

    /**
     * Gets the connections manager.
     *
     * @return The connections manager.
     */
    @Nonnull
    @CheckReturnValue
    ConnectionsManager getConnectionsManager()
    {
        return _connectionsManager;
    }

    /**
     * Opens this connection.
     */
    synchronized void open()
    {
        _clientServer.open();
        _serverClient.open();

        if (_getClientSSLEngine().isPresent()) {
            _handshakeTimeout = new TimerTask()
            {
                @Override
                public void run()
                {
                    _getConnectionLogger()
                        .debug(
                            ValveMessages.SSL_HANDSHAKE_TIMED_OUT,
                            Connection.this);
                    close();
                }
            };
            _connectionsManager.scheduleHandshakeTimeout(_handshakeTimeout);
        }
    }

    /**
     * Performs required actions when a connection's channel is selected.
     *
     * @param selectionKey The selection key.
     *
     * @return False when both directions have been stopped.
     */
    @CheckReturnValue
    synchronized boolean selected(final SelectionKey selectionKey)
    {
        try {
            if (_isReady(selectionKey, SelectionKey.OP_CONNECT)) {
                Require.success(selectionKey == _clientServer.getOutputKey());
                _clientServer.finishConnect();
                _isServerConnected = true;
                _getDirectionLogger()
                    .debug(ValveMessages.SERVER_CONNECTED, this);
                _set(selectionKey, SelectionKey.OP_READ);
                _clientServer.readyForWrite();
                _clientServer.readyForRead();
            }

            if (_isReady(selectionKey, SelectionKey.OP_WRITE)) {
                if (selectionKey == _clientServer.getOutputKey()) {
                    _clientServer.readyForWrite();

                    if (_getClientSSLEngine().isPresent()) {
                        _clientServer.readyForRead();
                    }
                } else {
                    Require
                        .success(
                            selectionKey == _serverClient.getOutputKey());
                    _serverClient.readyForWrite();

                    if (_getServerSSLEngine().isPresent()) {
                        _serverClient.readyForRead();
                    }
                }

                if (!selectionKey.isValid()) {
                    return false;
                }
            }

            if (_isReady(selectionKey, SelectionKey.OP_READ)) {
                if (selectionKey == _clientServer.getInputKey()) {
                    _clientServer.readyForRead();
                } else {
                    Require
                        .success(selectionKey == _serverClient.getInputKey());
                    _serverClient.readyForRead();
                }
            }
        } catch (final CancelledKeyException exception) {
            return false;
        }

        return !(_clientServer.isStopped() && _serverClient.isStopped());
    }

    /**
     * Updates the stats.
     */
    void updateStats()
    {
        _clientServer.updateStats();
        _serverClient.updateStats();
    }

    private static Optional<SSLEngine> _createSSLEngine(
            final Optional<SSLContext> sslContext,
            final SelectionKey selectionKey)
    {
        if (!sslContext.isPresent()) {
            return Optional.empty();
        }

        final SocketChannel channel = (SocketChannel) selectionKey.channel();
        final InetSocketAddress socketAddress = (InetSocketAddress) channel
            .socket()
            .getRemoteSocketAddress();

        return Optional
            .of(
                sslContext
                    .get()
                    .createSSLEngine(
                            socketAddress.getHostName(),
                                    socketAddress.getPort()));
    }

    static final ByteBuffer _NULL_BUFFER = ByteBuffer.allocate(0);
    private static final Logger _CONNECTION_LOGGER = Logger
        .getInstance(Connection.class);
    private static final Logger _DIRECTION_LOGGER = Logger
        .getInstance(Connection.Direction.class);

    private final Optional<SSLEngine> _clientSSLEngine;
    private final Direction _clientServer;
    private boolean _closed;
    private final ConnectionsManager _connectionsManager;
    private final Optional<Filter> _filter;
    private TimerTask _handshakeTimeout;
    private final boolean _isControlled;
    private boolean _isServerConnected;
    private final Direction _serverClient;
    private final Optional<SSLEngine> _serverSSLEngine;

    /**
     * Filter.
     */
    public interface Filter
    {
        /**
         * Called on data received from client.
         *
         * @param direction The client-server direction.
         */
        void onClientData(@Nonnull Direction direction);

        /**
         * Called on data received from server.
         *
         * @param direction The server-client direction.
         */
        void onServerData(@Nonnull Direction direction);
    }


    /**
     * Direction.
     *
     * <p>Instances of this class implement the connection relay in one
     * direction. Each instance of connection has two independent instances of
     * direction.</p>
     */
    public final class Direction
    {
        /**
         * Constructs an instance.
         *
         * <p>On return, this direction is enabled for read.</p>
         *
         * @param inputKey The input selection key.
         * @param outputKey The output selection key.
         * @param isFromClient True if from client.
         * @param alternateDirection The optional alternate direction.
         */
        Direction(
                @Nonnull final SelectionKey inputKey,
                @Nonnull final SelectionKey outputKey,
                final boolean isFromClient,
                @Nonnull final Optional<Direction> alternateDirection)
        {
            _inputKey = inputKey;
            _outputKey = outputKey;
            _isFromClient = isFromClient;

            if (alternateDirection.isPresent()) {
                _alternateDirection = alternateDirection.get();
                _alternateDirection._alternateDirection = this;
            }

            if ((isFromClient? _getClientSSLEngine(): _getServerSSLEngine())
                .isPresent()) {
                _dataBufferPool = DataBufferPool.EXPANDING_BUFFERS;
                _handshaking = true;
            } else {
                _dataBufferPool = DataBufferPool.FIXED_BUFFERS;
            }
        }

        /**
         * Forgets the modified buffer.
         */
        public void forgetModifiedBuffer()
        {
            _modifiedData = null;
        }

        /**
         * Gets the modified data.
         *
         * @return The modified data.
         */
        @Nonnull
        @CheckReturnValue
        public ByteBuffer getModifiedData()
        {
            if (_modifiedData == null) {
                _modifiedData = _dataBufferPool.borrowBuffer();
            }

            return _modifiedData;
        }

        /**
         * Gets the original data.
         *
         * @return The original data.
         */
        @Nonnull
        @CheckReturnValue
        public ByteBuffer getOriginalData()
        {
            if (_originalData == null) {
                _originalData = _dataInput.asReadOnlyBuffer();
                _originalData.flip();
            }

            return _originalData;
        }

        /**
         * Gets the controlled indicator.
         *
         * @return The controlled indicator.
         */
        @CheckReturnValue
        public boolean isControlled()
        {
            return Connection.this._isControlled();
        }

        /**
         * Closes this direction.
         */
        void close()
        {
            _stop();

            if (_inputKey.isValid()) {
                try {
                    _inputKey.channel().close();
                } catch (final IOException exception) {
                    // Ignores.
                }

                if (_getDirectionLogger().isTraceEnabled()) {
                    final Socket inputSocket = ((SocketChannel) _inputKey
                        .channel())
                        .socket();
                    final Socket outputSocket = ((SocketChannel) _outputKey
                        .channel())
                        .socket();

                    _getDirectionLogger()
                        .trace(
                            ValveMessages.DIRECTION_CLOSED,
                            isControlled()
                            ? ValveMessages.CONTROLLED: ValveMessages.DIRECT,
                            inputSocket.getLocalSocketAddress(),
                            inputSocket.getRemoteSocketAddress(),
                            outputSocket.getLocalSocketAddress(),
                            outputSocket.getRemoteSocketAddress());
                }
            }

            _networkInput = null;
            _dataInput = null;
            _dataOutput = null;
            _networkOutput = null;
        }

        /**
         * Called when this direction is ready to finish the connect action.
         */
        void finishConnect()
        {
            if (isStopped()) {
                return;    // Got a stray event.
            }

            if (_clear(_outputKey, SelectionKey.OP_CONNECT)) {
                final SocketChannel serverChannel = (SocketChannel) _outputKey
                    .channel();
                final Socket serverSocket = serverChannel.socket();

                try {
                    serverChannel.finishConnect();
                    serverSocket.setTcpNoDelay(true);
                    serverSocket.setKeepAlive(true);
                } catch (final IOException exception) {
                    _connectFailed(exception);

                    return;
                }
            }
        }

        /**
         * Gets the connection.
         *
         * @return The connection.
         */
        @Nonnull
        @CheckReturnValue
        Connection getConnection()
        {
            return Connection.this;
        }

        /**
         * Gets the input selection key.
         *
         * @return The input selection key.
         */
        @Nonnull
        @CheckReturnValue
        SelectionKey getInputKey()
        {
            return _inputKey;
        }

        /**
         * Gets the output selection key.
         *
         * @return The output selection key.
         */
        @Nonnull
        @CheckReturnValue
        SelectionKey getOutputKey()
        {
            return _outputKey;
        }

        /**
         * Asks if this direction is from client to server.
         *
         * @return True if from client to server.
         */
        @CheckReturnValue
        boolean isFromClient()
        {
            return _isFromClient;
        }

        /**
         * Asks if this direction is stopped.
         *
         * @return True if stopped.
         */
        @CheckReturnValue
        boolean isStopped()
        {
            return _stopped;
        }

        /**
         * Opens this direction.
         */
        void open()
        {
            if (isFromClient()) {
                _set(_inputKey, SelectionKey.OP_READ);

                if (!_getClientSSLEngine().isPresent()) {
                    _connect();
                }
            } else if (_getClientSSLEngine().isPresent()) {
                _set(_outputKey, SelectionKey.OP_WRITE);
            }
        }

        /**
         * Called when this direction is ready for read.
         */
        void readyForRead()
        {
            if (!_clear(_inputKey, SelectionKey.OP_READ)) {
                // The client channel has been cancelled.
                _stopping = true;    // Asks to stop after flushing.
                readyForWrite();
            }

            while (!isStopped()) {
                if (_dataInput == null) {
                    final Optional<SSLEngine> sslEngine = isFromClient()
                        ? _getClientSSLEngine(): _getServerSSLEngine();

                    if (_networkInput == null) {
                        _networkInput = sslEngine
                            .isPresent()? _allocateNetworkBuffer(
                                sslEngine.get(),
                                _inputKey,
                                ValveMessages.SSL_NETWORK_INPUT_SIZE): _dataBufferPool
                                    .borrowBuffer();
                    } else {
                        _networkInput.compact();
                    }

                    if (_read()) {
                        if (sslEngine.isPresent()) {
                            _dataInput = _unwrap(sslEngine.get());

                            if (isStopped()) {
                                return;
                            }
                        } else {
                            _dataInput = _networkInput;
                            _networkInput = null;
                        }

                        if (_dataInput.position() > 0) {    // Got some.
                            _dataInput = _filter(_dataInput);

                            if (_dataInput != null) {
                                readyForWrite();

                                continue;
                            }

                            _set(_inputKey, SelectionKey.OP_READ);
                        } else {    // Nothing available now.
                            _dataBufferPool.returnBuffer(_dataInput);
                            _dataInput = null;
                            _set(_inputKey, SelectionKey.OP_READ);
                        }
                    } else {    // The remote has closed its output.
                        if (sslEngine.isPresent()) {
                            try {
                                sslEngine.get().closeInbound();
                            } catch (final SSLException exception) {
                                final Socket socket = ((SocketChannel) _inputKey
                                    .channel())
                                    .socket();

                                _getDirectionLogger()
                                    .debug(
                                        ValveMessages.SSL_UNEXPECTED_CLOSE,
                                        socket.getRemoteSocketAddress());
                            }
                        } else {
                            _dataBufferPool.returnBuffer(_networkInput);
                            _networkInput = null;
                        }

                        _stopping = true;    // Asks to stop after flushing.
                        readyForWrite();
                    }
                }

                break;
            }
        }

        /**
         * Called when this direction is ready for write.
         */
        void readyForWrite()
        {
            if ((isFromClient() && !_isServerConnected()) || isStopped()) {
                return;    // Too soon or too late.
            }

            if (!_clear(_outputKey, SelectionKey.OP_WRITE)) {
                _stop();    // The server channel has been cancelled.
            }

            for (;;) {
                if (!_isNetworkOutputEmpty()) {
                    _write();    // Tries to flush.
                }

                if (_isNetworkOutputEmpty()) {
                    final Optional<SSLEngine> sslEngine = isFromClient()
                        ? _getServerSSLEngine(): _getClientSSLEngine();

                    if (_dataOutput == null) {
                        _dataOutput = _dataInput;

                        if (_dataOutput != null) {
                            _dataInput = null;
                            _set(_inputKey, SelectionKey.OP_READ);
                            _dataOutput.flip();
                        }
                    }

                    if (sslEngine.isPresent()) {
                        _wrap(sslEngine.get());

                        if (isStopped()) {
                            return;
                        }
                    } else {
                        _networkOutput = _dataOutput;
                        _dataOutput = null;
                    }

                    if (_isNetworkOutputEmpty()) {
                        if (_stopping) {    // Were waiting for the flush.
                            try {
                                ((SocketChannel) _outputKey.channel())
                                    .socket()
                                    .shutdownOutput();
                            } catch (final IOException exception) {
                                // Ignores.
                            }

                            _stopped = true;
                        }

                        break;
                    }
                } else {    // Some output still pending.
                    _set(_outputKey, SelectionKey.OP_WRITE);

                    break;
                }
            }
        }

        /**
         * Updates stats.
         */
        void updateStats()
        {
            final ValveStats stats = getConnectionsManager().getStats();

            if (stats != null) {
                stats.updateConnectionStats(_bytesTransfered.getAndSet(0));
            }
        }

        private void _connect()
        {
            try {
                ((SocketChannel) _outputKey.channel())
                    .connect(getConnectionsManager().getServerAddress());
            } catch (final IOException exception) {
                _connectFailed(exception);

                return;
            }

            _set(_outputKey, SelectionKey.OP_CONNECT);
        }

        private void _connectFailed(final IOException exception)
        {
            final ValveStats stats = getConnectionsManager().getStats();

            if (stats != null) {
                stats.updateConnectionsFailed(1);
            }

            if (_getDirectionLogger().isDebugEnabled()) {
                final Socket clientSocket = ((SocketChannel) _inputKey
                    .channel())
                    .socket();

                _getDirectionLogger()
                    .debug(
                        ValveMessages.SERVER_CONNECT_FAILED,
                        getConnectionsManager().getServerAddress(),
                        clientSocket.getLocalSocketAddress(),
                        clientSocket.getRemoteSocketAddress(),
                        exception.getMessage());
            }

            Connection.this.close();
        }

        private ByteBuffer _filter(ByteBuffer dataBuffer)
        {
            if (Connection.this._filter(this)) {
                _originalData = null;

                if (_modifiedData != null) {
                    dataBuffer = _modifiedData;
                    _modifiedData = null;

                    if (dataBuffer.position() == 0) {
                        _dataBufferPool.returnBuffer(dataBuffer);
                        dataBuffer = null;    // Dropped.
                    }
                }
            }

            return dataBuffer;
        }

        private void _handshakeFinished()
        {
            if (_handshaking) {
                _handshaking = false;

                _cancelHandshakeTimeout();

                if (_getDirectionLogger().isDebugEnabled()) {
                    final Socket inputSocket = ((SocketChannel) _inputKey
                        .channel())
                        .socket();

                    _getDirectionLogger()
                        .debug(
                            isFromClient()
                            ? ValveMessages.SSL_CLIENT_CONNECTION_ACCEPTED
                            : ValveMessages.SSL_SERVER_CONNECTION_ACCEPTED,
                            isControlled()
                            ? ValveMessages.CONTROLLED: ValveMessages.DIRECT,
                            inputSocket.getLocalSocketAddress(),
                            inputSocket.getRemoteSocketAddress());
                }

                if (isFromClient()) {
                    _connect();
                }
            }
        }

        private boolean _isNetworkOutputEmpty()
        {
            return (_networkOutput == null) || !_networkOutput.hasRemaining();
        }

        private boolean _read()
        {
            if (_inputKey.isReadable()) {
                int count;

                try {
                    count = ((SocketChannel) _inputKey.channel())
                        .read(_networkInput);
                } catch (final IOException exception) {
                    count = -1;
                }

                if (count < 0) {    // The remote just closed its output.
                    if (_getDirectionLogger().isTraceEnabled()) {
                        final Socket inputSocket = ((SocketChannel) _inputKey
                            .channel())
                            .socket();

                        _getDirectionLogger()
                            .trace(
                                ValveMessages.INPUT_CLOSED,
                                inputSocket.getLocalSocketAddress(),
                                inputSocket.getRemoteSocketAddress());
                    }

                    return false;
                }

                if (_getDirectionLogger().isTraceEnabled()) {
                    final Socket inputSocket = ((SocketChannel) _inputKey
                        .channel())
                        .socket();

                    _getDirectionLogger()
                        .trace(
                            ValveMessages.READ_COUNT,
                            inputSocket.getLocalSocketAddress(),
                            inputSocket.getRemoteSocketAddress(),
                            String.valueOf(count));
                }
            }

            return true;
        }

        private boolean _returnDataBuffer(final ByteBuffer buffer)
        {
            final boolean hasFixedBuffers = _dataBufferPool
                == DataBufferPool.FIXED_BUFFERS;

            if (hasFixedBuffers) {
                _dataBufferPool.returnBuffer(buffer);
            }

            return hasFixedBuffers;
        }

        private void _stop()
        {
            // Disables read/write events for this direction.

            _clear(_inputKey, SelectionKey.OP_READ);
            _clear(_outputKey, SelectionKey.OP_WRITE);

            // Asks to ignore stray events.

            _stopped = true;
        }

        private ByteBuffer _unwrap(final SSLEngine sslEngine)
        {
            ByteBuffer dataBuffer;

            _networkInput.flip();

            switch (sslEngine.getHandshakeStatus()) {
                case NEED_UNWRAP: {
                    dataBuffer = _dataBufferPool.borrowBuffer();

                    break;
                }
                case FINISHED:
                case NOT_HANDSHAKING: {
                    dataBuffer = _dataBufferPool.borrowBuffer();

                    break;
                }
                case NEED_TASK:
                case NEED_WRAP: {
                    _alternateDirection.readyForWrite();

                    return _NULL_BUFFER;
                }
                default: {
                    throw Require.failure();
                }
            }

            while (_networkInput.hasRemaining()) {
                final SSLEngineResult result;

                try {
                    result = sslEngine.unwrap(_networkInput, dataBuffer);
                } catch (final SSLException exception) {
                    _getDirectionLogger()
                        .warn(
                            ValveMessages.SSL_UNWRAP_FAILED,
                            exception.getMessage());
                    _stop();

                    break;
                }

                if (_getDirectionLogger().isTraceEnabled()) {
                    final Socket inputSocket = ((SocketChannel) _inputKey
                        .channel())
                        .socket();

                    _getDirectionLogger()
                        .trace(
                            ValveMessages.SSL_UNWRAP_RESULT,
                            inputSocket.getLocalSocketAddress(),
                            inputSocket.getRemoteSocketAddress(),
                            result.toString().replace('\n', ' '));
                }

                switch (result.getStatus()) {
                    case OK:
                    case CLOSED: {
                        switch (result.getHandshakeStatus()) {
                            case NEED_UNWRAP: {
                                continue;
                            }
                            case FINISHED: {
                                _handshakeFinished();

                                continue;
                            }
                            default: {
                                break;
                            }
                        }

                        _set(_inputKey, SelectionKey.OP_WRITE);

                        break;
                    }
                    case BUFFER_UNDERFLOW: {
                        if (sslEngine.getSession().getPacketBufferSize()
                                > _networkInput.capacity()) {
                            final ByteBuffer networkInput =
                                _allocateNetworkBuffer(
                                    sslEngine,
                                    _inputKey,
                                    ValveMessages.SSL_NETWORK_INPUT_SIZE);

                            networkInput.put(_networkInput);
                            _networkInput = networkInput;
                            _networkInput.flip();
                        } else {
                            Require
                                .success(
                                    _networkInput.position()
                                    < _networkInput.capacity());
                        }

                        _dataBufferPool.returnBuffer(dataBuffer);
                        dataBuffer = _NULL_BUFFER;

                        break;
                    }
                    case BUFFER_OVERFLOW: {
                        Require.success(dataBuffer != _NULL_BUFFER);

                        final int dataSize = sslEngine
                            .getSession()
                            .getApplicationBufferSize();

                        Require.success(dataSize > dataBuffer.capacity());
                        _dataBufferPool.useBufferSize(dataSize);

                        final ByteBuffer newBuffer = _dataBufferPool
                            .borrowBuffer();

                        dataBuffer.flip();
                        newBuffer.put(dataBuffer);
                        dataBuffer = newBuffer;

                        if (_getDirectionLogger().isTraceEnabled()) {
                            final Socket inputSocket =
                                ((SocketChannel) _inputKey
                                    .channel())
                                    .socket();

                            _getDirectionLogger()
                                .trace(
                                    ValveMessages.SSL_DATA_SIZE,
                                    inputSocket.getLocalSocketAddress(),
                                    inputSocket.getRemoteSocketAddress(),
                                    String.valueOf(dataSize));
                        }

                        continue;
                    }
                    default: {
                        throw Require.failure();
                    }
                }

                break;
            }

            return dataBuffer;
        }

        private void _wrap(final SSLEngine sslEngine)
        {
            final ByteBuffer dataOutput;

            switch (sslEngine.getHandshakeStatus()) {
                case NEED_WRAP: {
                    dataOutput = _NULL_BUFFER;

                    break;
                }
                case FINISHED:
                case NOT_HANDSHAKING: {
                    if (_dataOutput == null) {
                        return;
                    }

                    dataOutput = _dataOutput;
                    _dataOutput = null;

                    break;
                }
                case NEED_TASK: {
                    DelegatedTaskExecutor.delegateTasks(sslEngine, this);

                    return;
                }
                case NEED_UNWRAP: {
                    _alternateDirection.readyForRead();

                    return;
                }
                default: {
                    throw Require.failure();
                }
            }

            if (_networkOutput == null) {
                _networkOutput = _allocateNetworkBuffer(
                    sslEngine,
                    _outputKey,
                    ValveMessages.SSL_NETWORK_OUTPUT_SIZE);
            } else {
                _networkOutput.compact();
            }

            for (;;) {
                final SSLEngineResult result;

                try {
                    result = sslEngine.wrap(dataOutput, _networkOutput);
                } catch (final SSLException exception) {
                    _getDirectionLogger()
                        .warn(
                            ValveMessages.SSL_WRAP_FAILED,
                            exception.getMessage());
                    _stop();

                    return;
                }

                if (_getDirectionLogger().isTraceEnabled()) {
                    final Socket outputSocket = ((SocketChannel) _outputKey
                        .channel())
                        .socket();

                    _getDirectionLogger()
                        .trace(
                            ValveMessages.SSL_WRAP_RESULT,
                            outputSocket.getLocalSocketAddress(),
                            outputSocket.getRemoteSocketAddress(),
                            result.toString().replace('\n', ' '));
                }

                switch (result.getStatus()) {
                    case OK:
                    case CLOSED: {
                        _networkOutput.flip();

                        switch (result.getHandshakeStatus()) {
                            case NEED_WRAP:
                            case NEED_TASK: {
                                _set(_outputKey, SelectionKey.OP_WRITE);

                                break;
                            }
                            case FINISHED: {
                                _alternateDirection._handshakeFinished();

                                break;
                            }
                            default: {
                                break;
                            }
                        }

                        return;
                    }
                    case BUFFER_UNDERFLOW: {
                        throw Require.failure();
                    }
                    case BUFFER_OVERFLOW: {
                        final ByteBuffer networkOutput = _allocateNetworkBuffer(
                            sslEngine,
                            _outputKey,
                            ValveMessages.SSL_NETWORK_OUTPUT_SIZE);

                        Require
                            .success(
                                networkOutput.capacity()
                                > _networkOutput.capacity());

                        _networkOutput.flip();
                        networkOutput.put(_networkOutput);
                        _networkOutput = networkOutput;

                        break;
                    }
                    default: {
                        Require.failure();
                    }
                }
            }
        }

        private void _write()
        {
            if (_outputKey.isWritable()) {
                int count;

                try {
                    count = ((SocketChannel) _outputKey.channel())
                        .write(_networkOutput);
                } catch (final IOException exception) {
                    count = -1;
                }

                if (count < 0) {    // The remote just closed its input.
                    if (_getDirectionLogger().isTraceEnabled()) {
                        final Socket outputSocket = ((SocketChannel) _outputKey
                            .channel())
                            .socket();

                        _getDirectionLogger()
                            .trace(
                                ValveMessages.OUTPUT_CLOSED,
                                outputSocket.getLocalSocketAddress(),
                                outputSocket.getRemoteSocketAddress());
                    }

                    _stop();
                } else {
                    if (_getDirectionLogger().isTraceEnabled()) {
                        final Socket outputSocket = ((SocketChannel) _outputKey
                            .channel())
                            .socket();

                        _getDirectionLogger()
                            .trace(
                                ValveMessages.WRITE_COUNT,
                                outputSocket.getLocalSocketAddress(),
                                outputSocket.getRemoteSocketAddress(),
                                String.valueOf(count));
                    }

                    _bytesTransfered.addAndGet(count);

                    if (_networkOutput.hasRemaining()) {    // Not done yet.
                        _set(_outputKey, SelectionKey.OP_WRITE);
                    } else if (_alternateDirection
                        ._returnDataBuffer(_networkOutput)) {
                        _networkOutput = null;
                    }
                }
            } else {
                _set(_outputKey, SelectionKey.OP_WRITE);
            }
        }

        private Direction _alternateDirection;
        private final AtomicLong _bytesTransfered = new AtomicLong();
        private final DataBufferPool _dataBufferPool;
        private ByteBuffer _dataInput;
        private ByteBuffer _dataOutput;
        private boolean _handshaking;
        private final SelectionKey _inputKey;
        private final boolean _isFromClient;
        private ByteBuffer _modifiedData;
        private ByteBuffer _networkInput;
        private ByteBuffer _networkOutput;
        private ByteBuffer _originalData;
        private final SelectionKey _outputKey;
        private boolean _stopped;
        private boolean _stopping;
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
