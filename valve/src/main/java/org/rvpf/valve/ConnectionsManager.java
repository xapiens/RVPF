/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ConnectionsManager.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.valve;

import java.io.IOException;

import java.net.SocketAddress;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Optional;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import javax.net.ssl.SSLContext;

import org.rvpf.base.logger.Logger;
import org.rvpf.service.ServiceMessages;

/**
 * Connections manager.
 *
 * <p>Instances of this class run in their own thread, maintaining relay
 * connections on specified channels. They create {@link Connection} objects and
 * supply these with events.</p>
 */
final class ConnectionsManager
    implements Runnable
{
    /**
     * Constructs an instance.
     *
     * @param portManager The port manager.
     * @param isControlled True if controlled.
     * @param clientSSLContext The client SSL context.
     * @param clientCertified True for certified client.
     * @param serverSSLContext The server SSL context.
     */
    ConnectionsManager(
            @Nonnull final PortManager portManager,
            final boolean isControlled,
            @Nonnull final Optional<SSLContext> clientSSLContext,
            final boolean clientCertified,
            @Nonnull final Optional<SSLContext> serverSSLContext)
    {
        _portManager = portManager;
        _isControlled = isControlled;
        _clientSSLContext = clientSSLContext;
        _clientCertified = clientCertified;
        _serverSSLContext = serverSSLContext;
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        boolean terminated = true;

        _LOGGER
            .debug(
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

                // Proceeds to the data transfer.
                for (final Iterator<SelectionKey> iterator =
                        _selector.selectedKeys().iterator();
                        iterator.hasNext(); ) {
                    final SelectionKey key = iterator.next();
                    final Connection connection = (Connection) key.attachment();

                    if (!connection.selected(key)) {
                        // The communication in both directions has been
                        // stopped.
                        connection.close();
                    }

                    iterator.remove();
                }
            }

            for (final SelectionKey key: _selector.keys()) {
                if (key.isValid()) {
                    final Connection connection = (Connection) key.attachment();

                    if (connection != null) {
                        connection.close();
                    }
                }
            }

            terminated = false;
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        } finally {
            if (terminated) {
                _LOGGER
                    .warn(
                        ServiceMessages.THREAD_TERMINATED,
                        Thread.currentThread().getName());
            }
        }

        _LOGGER
            .debug(
                ServiceMessages.THREAD_STOPPED,
                Thread.currentThread().getName());
    }

    /**
     * Accepts a new client channel.
     *
     * @param clientChannel The client channel.
     * @param serverChannel The server channel.
     */
    void accept(
            @Nonnull final SocketChannel clientChannel,
            @Nonnull final SocketChannel serverChannel)
    {
        wakeUp(
            () -> {
                final SelectionKey clientKey;
                final SelectionKey serverKey;
                final Connection connection;

                try {
                    clientChannel.configureBlocking(false);
                    clientKey = clientChannel.register(_selector, 0);
                    serverChannel.configureBlocking(false);
                    serverKey = serverChannel.register(_selector, 0);
                } catch (final IOException exception) {
                    throw new RuntimeException(exception);
                }

                connection = new Connection(
                    ConnectionsManager.this,
                    clientKey,
                    serverKey,
                    _isControlled,
                    _portManager.getValveServiceImpl().getConnectionFilter(),
                    _clientSSLContext,
                    _clientCertified,
                    _serverSSLContext);
                clientKey.attach(connection);
                serverKey.attach(connection);
                connection.open();
            });
    }

    /**
     * Called by a connection when it has closed.
     *
     * @param connection The closed connection.
     */
    void closed(@Nonnull final Connection connection)
    {
        connection.updateStats();
        _connectionsClosed.incrementAndGet();
        _portManager.connectionClosed();
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
        return _portManager.getServerAddress();
    }

    /**
     * Gets the stats.
     *
     * @return The stats.
     */
    @Nonnull
    @CheckReturnValue
    ValveStats getStats()
    {
        return _portManager.getValveServiceImpl().getStats();
    }

    /**
     * Schedule an handshake timeout.
     *
     * @param handshakeTimeoutTask The handshake timeout task.
     */
    void scheduleHandshakeTimeout(@Nonnull final TimerTask handshakeTimeoutTask)
    {
        final long handshakeTimeout = _portManager.getHandshakeTimeout();
        final Optional<Timer> timer = _portManager
            .getValveServiceImpl()
            .getTimer();

        if ((handshakeTimeout > 0) && (timer.isPresent())) {
            timer.get().schedule(handshakeTimeoutTask, handshakeTimeout);
        }
    }

    /**
     * Starts this connections manager in its own thread.
     */
    void start()
    {
        _stopping = false;

        try {
            _selector = Selector.open();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        _thread = new Thread(this, "Valve connections manager");
        _thread
            .setUncaughtExceptionHandler(
                Thread.currentThread().getUncaughtExceptionHandler());
        _thread.start();
    }

    /**
     * Stops the connections manager thread.
     */
    void stop()
    {
        _stopping = true;

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
    }

    /**
     * Updates the stats.
     */
    void updateStats()
    {
        final ValveStats stats = getStats();

        if (stats != null) {
            if (_selector != null) {
                SelectionKey[] keys;

                for (;;) {
                    try {
                        keys = _selector.keys().toArray(new SelectionKey[0]);
                    } catch (final ConcurrentModificationException exception) {
                        continue;
                    }

                    break;
                }

                for (final SelectionKey key: keys) {
                    if (key.isValid()) {
                        final Connection connection = (Connection) key
                            .attachment();

                        if (connection != null) {
                            connection.updateStats();
                        }
                    }
                }
            }

            stats.updateConnectionsStats(_connectionsClosed.getAndSet(0));
        }
    }

    /**
     * Wakes up.
     *
     * @param wakeUp The something to run on wake up.
     */
    void wakeUp(@Nonnull final Runnable wakeUp)
    {
        _wakeUps.add(wakeUp);
        _selector.wakeup();
    }

    private static final Logger _LOGGER = Logger
        .getInstance(ConnectionsManager.class);

    private final boolean _clientCertified;
    private final Optional<SSLContext> _clientSSLContext;
    private final AtomicInteger _connectionsClosed = new AtomicInteger();
    private final boolean _isControlled;
    private final PortManager _portManager;
    private Selector _selector;
    private final Optional<SSLContext> _serverSSLContext;
    private volatile boolean _stopping;
    private Thread _thread;
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
