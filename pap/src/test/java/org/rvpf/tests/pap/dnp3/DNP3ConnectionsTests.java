/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3ConnectionsTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.pap.dnp3;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.dnp3.transport.Connection;
import org.rvpf.pap.dnp3.transport.Frame;
import org.rvpf.service.ServiceThread;
import org.rvpf.tests.Tests;

/**
 * DNP3 connections tests.
 */
public abstract class DNP3ConnectionsTests
    extends Tests
    implements ServiceThread.Target
{
    /** {@inheritDoc}
     */
    @Override
    public final void run()
    {
        final ByteBuffer buffer = ByteBuffer.allocate(Frame.MAXIMUM_FRAME_SIZE);

        for (;;) {
            try {
                final byte[] message = _readMessage(_serverConnection, buffer);

                buffer.clear();
                buffer.putShort((short) message.length);
                buffer.put(message);
                buffer.flip();
                _serverConnection.send(buffer);
            } catch (final ClosedChannelException exception) {
                break;
            } catch (final Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    /**
     * Tests connections.
     *
     * @throws Exception On failure.
     */
    public void test()
        throws Exception
    {
        final Connection clientConnection = newMasterConnection();

        getThisLogger()
            .debug(DNP3TestsMessages.MASTER_CONNECTION, clientConnection);
        _serverConnection = newOutstationConnection();
        getThisLogger()
            .debug(DNP3TestsMessages.OUTSTATION_CONNECTION, _serverConnection);

        new ServiceThread(this, "Connections tests").start();

        final ByteBuffer buffer = ByteBuffer.allocate(Frame.MAXIMUM_FRAME_SIZE);
        final Random random = new Random();

        for (int i = 1; i <= _MESSAGE_COUNT; ++i) {
            final int frameSize = Frame.MINIMUM_FRAME_SIZE + random.nextInt(
                Frame.MAXIMUM_FRAME_SIZE - Frame.MINIMUM_FRAME_SIZE);
            final byte[] sentMessage = new byte[frameSize - Short.BYTES];

            random.nextBytes(sentMessage);
            buffer.clear();
            buffer.putShort((short) sentMessage.length);
            buffer.put(sentMessage);
            buffer.flip();

            clientConnection.send(buffer);

            final byte[] receivedMessage = _readMessage(
                clientConnection,
                buffer);

            Require.success(receivedMessage.length == sentMessage.length);
            Require.success(Arrays.equals(receivedMessage, sentMessage));
        }

        clientConnection.close();
        _serverConnection.close();
    }

    /**
     * Gets the tests support instance.
     *
     * @return The tests support instance.
     */
    @Nonnull
    @CheckReturnValue
    protected DNP3TestsSupport getSupport()
    {
        if (_support == null) {
            _support = new DNP3TestsSupport(Optional.empty());
        }

        return _support;
    }

    /**
     * Returns a new master connection.
     *
     * @return The new master connection.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract Connection newMasterConnection()
        throws Exception;

    /**
     * Returns a new outstation connection.
     *
     * @return The new outstation connection.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract Connection newOutstationConnection()
        throws Exception;

    /**
     * Receives from a connection into a buffer.
     *
     * @param connection The connection.
     * @param buffer The buffer.
     *
     * @throws Exception On failure.
     */
    protected void receive(
            @Nonnull final Connection connection,
            @Nonnull final ByteBuffer buffer)
        throws Exception
    {
        connection.receive(buffer);
    }

    private byte[] _readMessage(
            final Connection connection,
            final ByteBuffer buffer)
        throws Exception
    {
        buffer.clear();

        do {
            receive(connection, buffer);
        } while (buffer.position() < Short.BYTES);

        buffer.flip();

        final int messageSize = buffer.getShort();
        final int frameSize = Short.BYTES + messageSize;

        Require.success(frameSize >= Frame.MINIMUM_FRAME_SIZE);
        Require.success(frameSize <= Frame.MAXIMUM_FRAME_SIZE);

        final byte[] message = new byte[messageSize];

        for (int i = 0; i < messageSize; ++i) {
            if (!buffer.hasRemaining()) {
                buffer.clear();
                receive(connection, buffer);
                buffer.flip();
            }

            message[i] = buffer.get();
        }

        return message;
    }

    private static final int _MESSAGE_COUNT = 100;

    private Connection _serverConnection;
    private DNP3TestsSupport _support;
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
