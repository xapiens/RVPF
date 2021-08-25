/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TCPSocketListener.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.io.IOException;
import java.io.InterruptedIOException;

import java.net.InetSocketAddress;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import org.rvpf.base.logger.Logger;
import org.rvpf.pap.dnp3.DNP3Messages;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * TCP socket listener.
 */
final class TCPSocketListener
    implements ServiceThread.Target
{
    /**
     * Constructs an instance.
     *
     * @param listenAddress The listen address.
     * @param connectionManager The connection manager.
     */
    TCPSocketListener(
            @Nonnull final InetSocketAddress listenAddress,
            @Nonnull final ConnectionManager connectionManager)
    {
        _listenAddress = listenAddress;
        _connectionManager = connectionManager;
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws IOException
    {
        for (;;) {
            try {
                _connectionManager.onSocketAccepted(
                    _serverSocketChannel.accept());
            } catch (final ClosedChannelException exception) {
                break;
            }
        }
    }

    /**
     * Starts.
     *
     * @throws IOException On I/O exception.
     */
    void start()
        throws IOException
    {
        final ServiceThread thread = new ServiceThread(
            this,
            "DNP3 "
            + (_connectionManager.isOnMaster()
               ? "master": "outstation") + " TCP socket listener on "
                   + _listenAddress);

        if (_thread.compareAndSet(null, thread)) {
            _serverSocketChannel = ServerSocketChannel.open();
            _serverSocketChannel.bind(_listenAddress);
            _LOGGER.debug(DNP3Messages.STARTED_LISTENING, _listenAddress);

            _LOGGER.debug(ServiceMessages.STARTING_THREAD, thread.getName());
            thread.start();
        }
    }

    /**
     * Stops.
     *
     * @throws IOException On I/O exception.
     */
    void stop()
        throws IOException
    {
        final Thread thread = _thread.getAndSet(null);

        if (thread != null) {
            _LOGGER.debug(ServiceMessages.STOPPING_THREAD, thread.getName());

            try {
                _serverSocketChannel.close();
                thread.join();
            } catch (final IOException exception) {
                // Ignores.
            } catch (final InterruptedException exception) {
                throw (IOException) new InterruptedIOException().initCause(
                    exception);
            }
        }

        _LOGGER.debug(DNP3Messages.STOPPED_LISTENING, _listenAddress);
    }

    private static final Logger _LOGGER = Logger.getInstance(
        TCPSocketListener.class);

    private final ConnectionManager _connectionManager;
    private final InetSocketAddress _listenAddress;
    private ServerSocketChannel _serverSocketChannel;
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();
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
