/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3DataLinkLayerTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.pap.dnp3;

import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

import org.rvpf.base.tool.Require;
import org.rvpf.config.Config;
import org.rvpf.pap.dnp3.transport.Association;
import org.rvpf.pap.dnp3.transport.Connection;
import org.rvpf.pap.dnp3.transport.DataLinkLayer;
import org.rvpf.pap.dnp3.transport.Frame;
import org.rvpf.service.ServiceThread;
import org.rvpf.tests.service.MetadataServiceTests;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * DNP3 data link layer tests.
 */
public class DNP3DataLinkLayerTests
    extends MetadataServiceTests
    implements ServiceThread.Target
{
    /**
     * Constructs an instance.
     *
     * @throws Exception On failure.
     */
    public DNP3DataLinkLayerTests()
        throws Exception
    {
        _outstationSocketChannel = ServerSocketChannel.open();
        _outstationSocketChannel.bind(_tcpSocketAddress);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void run()
        throws Exception
    {
        final ByteBuffer buffer = ByteBuffer.allocate(Frame.MAXIMUM_DATA_SIZE);

        try {
            Require
                .success(
                    _outstationDataLink
                        .isLinkActive(getTimeout(DEFAULT_TIMEOUT)));
            Thread.sleep(1);
        } catch (final IOException|InterruptedException exception) {
            throw new RuntimeException(exception);
        }

        for (;;) {
            try {
                final byte[] message = _readMessage(
                    _outstationDataLink,
                    buffer);

                Thread.sleep(1);
                buffer.clear();
                buffer.putShort((short) message.length);
                buffer.put(message);
                buffer.flip();
                _outstationDataLink.send(buffer);
                Thread.sleep(1);
            } catch (final ClosedChannelException exception) {
                break;
            }
        }
    }

    /**
     * Sets up this.
     */
    @BeforeClass
    public void setUp()
    {
        setSystemProperty(Config.RVPF_PROPERTIES, _TESTS_PROPERTIES);
        _messageCount = getConfig()
            .getIntValue(MESSAGE_COUNT_PROPERTY, DEFAULT_MESSAGE_COUNT);
    }

    /**
     * Tests data link.
     *
     * @throws Exception On failure.
     */
    @Test
    public final void test()
        throws Exception
    {
        setSystemProperty(Config.RVPF_PROPERTIES, _TESTS_PROPERTIES);

        final DNP3TestsSupport support = new DNP3TestsSupport(
            Optional.of(getMetadata()));
        final Connection masterConnection = support
            .newTCPConnection(true, SocketChannel.open(_tcpSocketAddress));

        getThisLogger()
            .debug(DNP3TestsMessages.MASTER_CONNECTION, masterConnection);

        final Connection outstationConnection = support
            .newTCPConnection(false, _outstationSocketChannel.accept());

        getThisLogger()
            .debug(
                DNP3TestsMessages.OUTSTATION_CONNECTION,
                outstationConnection);

        outstationConnection.activate();
        masterConnection.activate();

        final Association outstationMasterAssociation = outstationConnection
            .getRemoteEndPoint()
            .getAssociation((short) 0, (short) 0);

        _outstationDataLink = outstationMasterAssociation.getDataLinkLayer();

        final Association masterOutstationAssociation = masterConnection
            .getRemoteEndPoint()
            .getAssociation((short) 0, (short) 0);
        final DataLinkLayer masterDataLink = masterOutstationAssociation
            .getDataLinkLayer();
        final ServiceThread serviceThread = new ServiceThread(
            this,
            "Data link tests");

        serviceThread.start();

        final ByteBuffer buffer = ByteBuffer.allocate(Frame.MAXIMUM_DATA_SIZE);
        final Random random = new Random();

        Require
            .success(
                masterDataLink.isLinkActive(getTimeout(DEFAULT_TIMEOUT)));

        for (int i = 1; i <= _messageCount; ++i) {
            final int segmentSize = Short.BYTES + random.nextInt(
                Frame.MAXIMUM_DATA_SIZE - Short.BYTES);
            final byte[] sentMessage = new byte[segmentSize - Short.BYTES];

            random.nextBytes(sentMessage);
            buffer.clear();
            buffer.putShort((short) sentMessage.length);
            buffer.put(sentMessage);
            buffer.flip();

            masterDataLink.send(buffer);
            Thread.sleep(1);

            final byte[] receivedMessage = _readMessage(masterDataLink, buffer);

            Require.success(receivedMessage.length == sentMessage.length);
            Require.success(Arrays.equals(receivedMessage, sentMessage));
            Thread.sleep(1);
        }

        Require
            .success(
                masterDataLink.isLinkActive(getTimeout(DEFAULT_TIMEOUT)));
        Thread.sleep(1);
        Require
            .success(
                masterDataLink.isLinkActive(getTimeout(DEFAULT_TIMEOUT)));

        masterConnection.close();
        outstationConnection.close();

        Require
            .ignored(
                serviceThread
                    .interruptAndJoin(
                            getThisLogger(),
                                    getTimeout(DEFAULT_TIMEOUT)));
    }

    private static byte[] _readMessage(
            final DataLinkLayer dataLink,
            final ByteBuffer buffer)
        throws Exception
    {
        buffer.clear();

        do {
            dataLink.receive(buffer);
        } while (buffer.position() < Short.BYTES);

        buffer.flip();

        final int messageSize = buffer.getShort();
        final int segmentSize = Short.BYTES + messageSize;

        Require.success(segmentSize >= 1);
        Require.success(segmentSize <= Frame.MAXIMUM_DATA_SIZE);

        final byte[] message = new byte[messageSize];

        for (int i = 0; i < messageSize; ++i) {
            if (!buffer.hasRemaining()) {
                buffer.clear();
                dataLink.receive(buffer);
                buffer.flip();
            }

            message[i] = buffer.get();
        }

        return message;
    }

    /** Default message count. */
    public static final int DEFAULT_MESSAGE_COUNT = 100;

    /** Default message count. */
    public static final String MESSAGE_COUNT_PROPERTY =
        "tests.dataLink.messages";

    /**  */

    private static final String _TESTS_PROPERTIES = "rvpf-dnp3.properties";

    private int _messageCount;
    private DataLinkLayer _outstationDataLink;
    private ServerSocketChannel _outstationSocketChannel;
    private final InetSocketAddress _tcpSocketAddress = new InetSocketAddress(
        InetAddress.getLoopbackAddress(),
        allocateTCPPort());
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
