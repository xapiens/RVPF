/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TCPConnection.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.io.IOException;

import java.net.InetSocketAddress;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

import javax.annotation.Nonnull;

/**
 * TCP connection.
 */
public final class TCPConnection
    extends Connection
{
    /**
     * Constructs an instance.
     *
     * @param localEndPoint The local end point.
     * @param remoteEndPoint The remote end point.
     * @param channel The TCP socket channel.
     */
    public TCPConnection(
            @Nonnull final LocalEndPoint localEndPoint,
            @Nonnull final RemoteEndPoint remoteEndPoint,
            @Nonnull final SocketChannel channel)
    {
        super(localEndPoint, remoteEndPoint);

        _socketChannel = channel;

        try {
            _localAddress = (InetSocketAddress) _socketChannel
                .getLocalAddress();
            _remoteAddress = (InetSocketAddress) _socketChannel
                .getRemoteAddress();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void doClose()
        throws IOException
    {
        _socketChannel.close();

        super.doClose();
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
        try {
            _socketChannel.read(buffer);
        } catch (final AsynchronousCloseException exception) {
            throw new ClosedChannelException();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doSend(final ByteBuffer buffer)
        throws IOException
    {
        try {
            _socketChannel.write(buffer);
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

    private final InetSocketAddress _localAddress;
    private final InetSocketAddress _remoteAddress;
    private final SocketChannel _socketChannel;
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
