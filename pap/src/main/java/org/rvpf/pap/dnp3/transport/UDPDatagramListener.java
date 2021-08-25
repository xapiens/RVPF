/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: UDPDatagramListener.java 4098 2019-06-25 16:46:46Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.io.IOException;
import java.io.InterruptedIOException;

import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.pap.dnp3.DNP3Messages;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * UDP datagram listener.
 */
public class UDPDatagramListener
    implements ServiceThread.Target
{
    /**
     * Constructs an instance.
     *
     * @param listenAddress The listen address.
     * @param connectionManager The connection manager.
     */
    public UDPDatagramListener(
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
        throws Exception
    {
        final ByteBuffer buffer = ByteBuffer.allocate(MAXIMUM_DATAGRAM_SIZE);
        InetSocketAddress sourceAddress;

        for (;;) {
            try {
                sourceAddress = (InetSocketAddress) _datagramChannel
                    .receive(buffer);
            } catch (final ClosedChannelException exception) {
                break;
            }

            buffer.flip();
            _connectionManager.onDatagramReceived(sourceAddress, buffer);
            buffer.clear();
        }
    }

    /**
     * Gets the channel.
     *
     * @return The channel.
     */
    @Nonnull
    @CheckReturnValue
    DatagramChannel getChannel()
    {
        return Require.notNull(_datagramChannel);
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
               ? "master": "outstation") + " UDP datagram listener on "
                   + _listenAddress);

        if (_thread.compareAndSet(null, thread)) {
            _datagramChannel = DatagramChannel
                .open(StandardProtocolFamily.INET);
            _datagramChannel.bind(_listenAddress);
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
                _datagramChannel.close();
                thread.join();
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            } catch (final InterruptedException exception) {
                throw (IOException) new InterruptedIOException()
                    .initCause(exception);
            }
        }

        _LOGGER.debug(DNP3Messages.STOPPED_LISTENING, _listenAddress);
    }

    /** Maximum datagram size. */
    public static final int MAXIMUM_DATAGRAM_SIZE = 512;

    /**  */

    private static final Logger _LOGGER = Logger
        .getInstance(UDPDatagramListener.class);

    private final ConnectionManager _connectionManager;
    private volatile DatagramChannel _datagramChannel;
    private final InetSocketAddress _listenAddress;
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
