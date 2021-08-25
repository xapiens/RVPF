/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: UDPConnection.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.io.IOException;

import java.net.InetSocketAddress;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Nonnull;

/**
 * UDP connection.
 */
public final class UDPConnection
    extends Connection
{
    /**
     * Constructs an instance.
     *
     * @param localEndPoint The local end point.
     * @param remoteEndPoint The remote end point.
     * @param datagramChannel The UDP datagram channel.
     * @param remoteAddress The remote address.
     */
    public UDPConnection(
            @Nonnull final LocalEndPoint localEndPoint,
            @Nonnull final RemoteEndPoint remoteEndPoint,
            @Nonnull final DatagramChannel datagramChannel,
            @Nonnull final InetSocketAddress remoteAddress)
    {
        super(localEndPoint, remoteEndPoint);

        _datagramChannel = datagramChannel;

        try {
            _localAddress = (InetSocketAddress) _datagramChannel
                .getLocalAddress();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        _remoteAddress = remoteAddress;
    }

    /** {@inheritDoc}
     */
    @Override
    public void doClose()
        throws IOException
    {
        super.doClose();

        _datagramChannel.close();
    }

    /**
     * Called when a new datagram is received.
     *
     * @param buffer The new datagram.
     */
    public void onDatagramReceived(@Nonnull final ByteBuffer buffer)
    {
        final byte[] datagram = new byte[buffer.remaining()];

        if (datagram.length > 0) {
            buffer.get(datagram);
            _datagrams.add(datagram);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return _localAddress.toString() + " > " + _remoteAddress.toString();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doReceive(final ByteBuffer buffer)
        throws IOException
    {
        synchronized (_receiveMutex) {
            if (_datagram == null) {
                try {
                    _datagram = ByteBuffer.wrap(_datagrams.take());
                } catch (final InterruptedException exception) {
                    throw new ClosedChannelException();
                }
            }

            do {
                buffer.put(_datagram.get());

                if (!_datagram.hasRemaining()) {
                    _datagram = null;

                    break;
                }
            } while (buffer.hasRemaining());
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doSend(final ByteBuffer buffer)
        throws IOException
    {
        try {
            _datagramChannel.send(buffer, _remoteAddress);
        } catch (final AsynchronousCloseException exception) {
            throw new ClosedChannelException();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getName()
    {
        return _localAddress.toString();
    }

    private ByteBuffer _datagram;
    private final DatagramChannel _datagramChannel;
    private final BlockingQueue<byte[]> _datagrams =
        new LinkedBlockingQueue<>();
    private final InetSocketAddress _localAddress;
    private final Object _receiveMutex = new Object();
    private final InetSocketAddress _remoteAddress;
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
