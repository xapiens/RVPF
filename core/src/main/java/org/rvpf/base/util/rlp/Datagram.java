/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Datagram.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base.util.rlp;

import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.tool.Require;

/**
 * Datagram.
 *
 * <p>Encapsulates an UDP DatagramPacket instance.
 * Used for sending and receiving datagrams.</p>
 */
@NotThreadSafe
class Datagram
{
    /**
     * Constructs an instance.
     */
    Datagram() {}

    /**
     * Adds a byte.
     *
     * @param addedByte The added byte.
     *
     * @return This.
     */
    @Nonnull
    Datagram addByte(final byte addedByte)
    {
        _datagramPacket.getData()[_offset++] = addedByte;
        _datagramPacket.setLength(_offset);

        return this;
    }

    /**
     * Adds a word.
     *
     * @param word The added word.
     *
     * @return This.
     */
    @Nonnull
    Datagram addWord(final short word)
    {
        addByte((byte) (word >> 8));
        addByte((byte) word);

        return this;
    }

    /**
     * Gets the address.
     *
     * @return The address.
     */
    @Nonnull
    @CheckReturnValue
    InetAddress getAddress()
    {
        return Require.notNull(_datagramPacket.getAddress());
    }

    /**
     * Gets the next byte.
     *
     * @return The next byte.
     */
    @CheckReturnValue
    byte getNextByte()
    {
        return _datagramPacket.getData()[_offset++];
    }

    /**
     * Gets the next word.
     *
     * @return The next word.
     */
    @CheckReturnValue
    short getNextWord()
    {
        short word;

        word = (short) (getNextByte() << 8);
        word |= getNextByte() & 0xFF;

        return word;
    }

    /**
     * Gets the offset.
     *
     * @return The offset.
     */
    @CheckReturnValue
    int getOffset()
    {
        return _offset;
    }

    /**
     * Gets the port
     *
     * @return The port.
     */
    @CheckReturnValue
    int getPort()
    {
        final int port = _datagramPacket.getPort();

        Require.success(port > 0);

        return port;
    }

    /**
     * Gets the socket address.
     *
     * @return The socket address.
     */
    @Nonnull
    @CheckReturnValue
    SocketAddress getSocketAddress()
    {
        return _datagramPacket.getSocketAddress();
    }

    /**
     * Receives.
     *
     * @param datagramSocket A datagram socket.
     * @param timeoutMillis The receive timeout in millis (0 means infinite).
     *
     * @return False if the datagram socket has been closed.
     *
     * @throws IOException On failure.
     */
    @CheckReturnValue
    boolean receive(
            @Nonnull final DatagramSocket datagramSocket,
            final int timeoutMillis)
        throws IOException
    {
        reset();

        _datagramPacket.setLength(_datagramPacket.getData().length);

        try {
            datagramSocket.setBroadcast(true);
            datagramSocket.setSoTimeout(timeoutMillis);
            datagramSocket.receive(_datagramPacket);
        } catch (final SocketTimeoutException exception) {
            return false;
        } catch (final SocketException exception) {
            if (!datagramSocket.isClosed()) {
                throw exception;
            }

            return false;
        }

        return true;
    }

    /**
     * Returns the remaining length.
     *
     * @return The remaining length.
     */
    @CheckReturnValue
    int remainingLength()
    {
        return _datagramPacket.getLength() - _offset;
    }

    /**
     * Resets.
     *
     * @return This.
     */
    @Nonnull
    Datagram reset()
    {
        _offset = 0;

        return this;
    }

    /**
     * Sends.
     *
     * @param datagramSocket A datagram socket.
     * @param broadcast True to broadcast.
     *
     * @return False if the datagram socket has been closed.
     *
     * @throws IOException On failure.
     */
    @CheckReturnValue
    boolean send(
            @Nonnull final DatagramSocket datagramSocket,
            final boolean broadcast)
        throws IOException
    {
        try {
            datagramSocket.setBroadcast(broadcast);
            datagramSocket.send(_datagramPacket);
        } catch (final SocketException exception) {
            if (!datagramSocket.isClosed()) {
                throw exception;
            }

            return false;
        }

        reset();

        return true;
    }

    /**
     * Sets the remote address.
     *
     * @param remoteAddress The remote address.
     *
     * @return This.
     */
    @Nonnull
    Datagram setRemoteAddress(@Nonnull final InetAddress remoteAddress)
    {
        _datagramPacket.setAddress(Require.notNull(remoteAddress));

        return this;
    }

    /**
     * Sets the remote port.
     *
     * @param remotePort The remote port.
     *
     * @return This.
     */
    @Nonnull
    Datagram setRemotePort(final int remotePort)
    {
        _datagramPacket.setPort(remotePort);

        return this;
    }

    private final DatagramPacket _datagramPacket = new DatagramPacket(
        new byte[Short.MAX_VALUE - Short.MIN_VALUE],
        0);
    private int _offset;
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
