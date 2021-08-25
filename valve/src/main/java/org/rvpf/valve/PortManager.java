/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PortManager.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.valve;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import javax.net.ssl.SSLContext;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.service.ServiceMessages;

/**
 * Port manager.
 *
 * <p>Instances of this class run in their own thread, listening for client
 * connections on specified addresses. These connections are then handled by a
 * {@link ConnectionsManager}.</p>
 */
final class PortManager
    implements Runnable
{
    /**
     * Constructs an instance.
     *
     * @param valveServiceImpl The valve service implementation.
     * @param isControlled True if the port is controlled.
     * @param connectionsLimit The connections limit.
     * @param clientSSLContext The client SSL context.
     * @param clientCertified True for certified clients.
     * @param handshakeTimeout The handshake timeout in milliseconds.
     * @param serverSSLContext The server SSL context.
     */
    PortManager(
            @Nonnull final ValveServiceImpl valveServiceImpl,
            final boolean isControlled,
            final int connectionsLimit,
            @Nonnull final Optional<SSLContext> clientSSLContext,
            final boolean clientCertified,
            final long handshakeTimeout,
            @Nonnull final Optional<SSLContext> serverSSLContext)
    {
        _valveServiceImpl = valveServiceImpl;
        _isControlled = isControlled;
        _connectionsLimit = connectionsLimit;
        _clientSSLContext = clientSSLContext;
        _clientCertified = clientCertified;
        _handshakeTimeout = handshakeTimeout;
        _serverSSLContext = serverSSLContext;
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        boolean terminated = true;

        _LOGGER.debug(
            ServiceMessages.THREAD_STARTED,
            Thread.currentThread().getName());

        try {
            while (!_stopping) {
                // Waits for a ready selection key or a wake up.
                _selector.select();

                // Processes wake ups.
                for (;;) {
                    final Runnable wakeUp = _wakeUps.poll();

                    if (wakeUp == null) {
                        break;
                    }

                    wakeUp.run();
                }

                // Accepts new connections.
                for (final Iterator<SelectionKey> iterator =
                        _selector.selectedKeys().iterator();
                        iterator.hasNext(); ) {
                    final SelectionKey selectionKey = iterator.next();

                    _readyForAccept(
                        (ServerSocketChannel) selectionKey.channel());
                    iterator.remove();
                }
            }

            for (final SelectionKey selectionKey: _selector.keys()) {
                selectionKey.channel().close();
            }

            terminated = false;
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        } finally {
            if (terminated) {
                _LOGGER.warn(
                    ServiceMessages.THREAD_TERMINATED,
                    Thread.currentThread().getName());
            }
        }

        _LOGGER.debug(
            ServiceMessages.THREAD_STOPPED,
            Thread.currentThread().getName());
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    static Logger getLogger()
    {
        return _LOGGER;
    }

    /**
     * Adds a listen address.
     *
     * @param address The listen address.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean addListenAddress(@Nonnull final SocketAddress address)
    {
        final ServerSocketChannel serverChannel;
        final ServerSocket serverSocket;

        try {
            serverChannel = ServerSocketChannel.open();
            serverSocket = serverChannel.socket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(address);
            serverChannel.configureBlocking(false);
        } catch (final IOException exception) {
            getLogger().error(
                ValveMessages.LISTEN_ADD_FAILED_,
                address,
                exception);

            return false;
        }

        if (!serverSocket.isBound()) {
            getLogger().error(ValveMessages.LISTEN_ADD_FAILED, address);

            return false;
        }

        _listenAddresses.add(serverSocket.getLocalSocketAddress());

        final Selector selector = _selector;
        final boolean isControlled = _isControlled;

        _wakeUps.add(
            () -> {
                try {
                    serverChannel.register(selector, SelectionKey.OP_ACCEPT);
                    getLogger().info(
                        ValveMessages.LISTENING_TYPE,
                        ValueConverter.toInteger(isControlled),
                        serverSocket.getLocalSocketAddress());
                } catch (final ClosedChannelException exception) {
                    throw new RuntimeException(exception);
                }
            });
        _selector.wakeup();

        return true;
    }

    /**
     * Called when a connection is closed.
     */
    void connectionClosed()
    {
        _connectionsActive.decrementAndGet();
    }

    /**
     * Gets the handshake timeout.
     *
     * @return The handshake timeout.
     */
    @CheckReturnValue
    long getHandshakeTimeout()
    {
        return _handshakeTimeout;
    }

    /**
     * Gets the listen addresses.
     *
     * <p>For unit tests.</p>
     *
     * @return The listen addresses.
     */
    @Nonnull
    @CheckReturnValue
    Collection<SocketAddress> getListenAddresses()
    {
        return _listenAddresses;
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
        return _valveServiceImpl.getServerAddress();
    }

    /**
     * Gets the valveServiceImpl.
     *
     * @return The valveServiceImpl.
     */
    @Nonnull
    @CheckReturnValue
    ValveServiceImpl getValveServiceImpl()
    {
        return _valveServiceImpl;
    }

    /**
     * Asks if this port manager is paused.
     *
     * @return True if paused.
     */
    @CheckReturnValue
    synchronized boolean isPaused()
    {
        return _connectionsManager == null;
    }

    /**
     * Pauses this port manager.
     */
    synchronized void pause()
    {
        if (_connectionsManager != null) {
            _connectionsManager.updateStats();
            _connectionsManager.stop();
            _connectionsManager = null;
        }
    }

    /**
     * Resumes this port manager.
     */
    synchronized void resume()
    {
        if (_connectionsManager == null) {
            _connectionsManager = new ConnectionsManager(
                this,
                _isControlled,
                _clientSSLContext,
                _clientCertified,
                _serverSSLContext);
            _connectionsManager.start();
        }
    }

    /**
     * Starts this port manager in its own thread.
     */
    void start()
    {
        _stopping = false;

        try {
            _selector = Selector.open();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        _thread = new Thread(
            this,
            "Valve port manager ["
            + (_isControlled? "controlled": "direct") + "]");
        _thread.setUncaughtExceptionHandler(
            Thread.currentThread().getUncaughtExceptionHandler());
        _thread.start();

        getLogger().debug(
            ValveMessages.STARTED_TYPE,
            ValueConverter.toInteger(_isControlled));
    }

    /**
     * Stops the port manager thread.
     */
    void stop()
    {
        _stopping = true;

        pause();

        _selector.wakeup();

        try {
            _thread.join();
        } catch (final InterruptedException exception) {
            throw new Error(exception);    // Should not happen.
        }

        _thread = null;

        try {
            _selector.close();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        _selector = null;

        getLogger().debug(
            ValveMessages.STOPPED_TYPE,
            ValueConverter.toInteger(_isControlled));
    }

    /**
     * Updates the stats.
     *
     * @param stats The stats.
     */
    void updateStats(@Nonnull final ValveStats stats)
    {
        stats.updatePortsStats(
            _connectionsAccepted.getAndSet(0),
            _connectionsRefused.getAndSet(0));

        final ConnectionsManager connectionsManager = _connectionsManager;

        if (connectionsManager != null) {
            connectionsManager.updateStats();
        }
    }

    private synchronized void _readyForAccept(
            @Nonnull final ServerSocketChannel listenChannel)
        throws IOException
    {
        final SocketChannel clientChannel = listenChannel.accept();

        if (clientChannel == null) {
            return;    // False alert.
        }

        final Socket clientSocket = clientChannel.socket();
        final SocketAddress clientLocalAddress =
            clientSocket.getLocalSocketAddress();
        final SocketAddress clientRemoteAddress =
            clientSocket.getRemoteSocketAddress();
        final SocketChannel serverChannel;

        if ((_connectionsManager != null)
                && (_connectionsActive.get() < _connectionsLimit)) {
            // Allows the connection.

            clientSocket.setTcpNoDelay(true);
            clientSocket.setKeepAlive(true);
            _connectionsAccepted.incrementAndGet();
            _connectionsActive.incrementAndGet();
            getLogger().debug(
                (_clientSSLContext != null)
                ? ValveMessages.CONNECTION_REQUESTED
                : ValveMessages.CONNECTION_ACCEPTED,
                _isControlled? ValveMessages.CONTROLLED: ValveMessages.DIRECT,
                clientLocalAddress,
                clientRemoteAddress);
            serverChannel = SocketChannel.open();
        } else {
            // Refuses the connection.

            serverChannel = null;
            _connectionsRefused.incrementAndGet();
            getLogger().debug(
                ValveMessages.CONNECTION_REFUSED,
                _isControlled? ValveMessages.CONTROLLED: ValveMessages.DIRECT,
                clientLocalAddress,
                clientRemoteAddress);
        }

        if (serverChannel != null) {
            // The full duplex flow of data may now begin.

            _connectionsManager.accept(clientChannel, serverChannel);
        } else {
            clientChannel.close();
        }
    }

    private static final Logger _LOGGER = Logger.getInstance(PortManager.class);

    private final boolean _clientCertified;
    private final Optional<SSLContext> _clientSSLContext;
    private final AtomicInteger _connectionsAccepted = new AtomicInteger();
    private final AtomicInteger _connectionsActive = new AtomicInteger();
    private final int _connectionsLimit;
    private ConnectionsManager _connectionsManager;
    private final AtomicInteger _connectionsRefused = new AtomicInteger();
    private final long _handshakeTimeout;
    private final boolean _isControlled;
    private final Queue<SocketAddress> _listenAddresses =
        new ConcurrentLinkedQueue<SocketAddress>();
    private Selector _selector;
    private final Optional<SSLContext> _serverSSLContext;
    private volatile boolean _stopping;
    private Thread _thread;
    private final ValveServiceImpl _valveServiceImpl;
    private final Queue<Runnable> _wakeUps =
        new ConcurrentLinkedQueue<Runnable>();
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
