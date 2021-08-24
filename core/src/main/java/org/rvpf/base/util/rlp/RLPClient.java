/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RLPClient.java 3892 2019-02-13 13:24:20Z SFB $
 */

package org.rvpf.base.util.rlp;

import java.io.IOException;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import java.nio.channels.InterruptedByTimeoutException;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Inet;

/**
 * RLP client.
 *
 * <p>Represents a process that need to locate resources.</p>
 */
@ThreadSafe
public class RLPClient
{
    /**
     * Constructs an instance (called only by a builder).
     *
     * @param localAddress The local address.
     * @param localPort The local port.
     *
     * @throws SocketException On failure.
     */
    RLPClient(
            @Nonnull final InetAddress localAddress,
            final int localPort)
        throws SocketException
    {
        _datagramSocket = new DatagramSocket(localPort, localAddress);
    }

    /**
     * Returns a new builder.
     *
     * @return The new builder.
     */
    @Nonnull
    @CheckReturnValue
    public static Builder newBuilder()
    {
        return new Builder();
    }

    /**
     * Closes.
     */
    public void close()
    {
        _datagramSocket.close();
    }

    /**
     * Do you provide.
     *
     * @param resourceProvider A resource provider.
     * @param resourceSpecifiers The resource specifiers.
     * @param local True for local resource providers.
     * @param timeout The timeout interval.
     *
     * @return The resource providers
     *         (empty optional on timeout or socket closed).
     *
     * @throws IOException On failure.
     * @throws InterruptedByTimeoutException When interrupted by timeout.
     */
    @Nonnull
    @CheckReturnValue
    public synchronized Optional<ResourceSpecifier[]> doYouProvide(
            @Nonnull final ResourceProvider resourceProvider,
            @Nonnull final ResourceSpecifier[] resourceSpecifiers,
            final boolean local,
            @Nonnull final ElapsedTime timeout)
        throws IOException, InterruptedByTimeoutException
    {
        final Datagram datagram = new Datagram()
            .setRemoteAddress(resourceProvider.getAddress())
            .setRemotePort(resourceProvider.getPort());
        final RLPMessage sentMessage = newMessage(
            MessageType.DO_YOU_PROVIDE,
            resourceSpecifiers,
            local);

        sentMessage.exportTo(datagram);

        if (!datagram.send(_datagramSocket, false)) {
            return Optional.empty();
        }

        _LOGGER
            .trace(
                RLPMessages.SENT_MESSAGE,
                _datagramSocket.getLocalSocketAddress(),
                datagram.getSocketAddress(),
                sentMessage);

        for (;;) {
            if (!datagram.receive(_datagramSocket, _intMillis(timeout))) {
                return Optional.empty();
            }

            final RLPMessage receivedMessage = RLPMessage
                .newBuilder()
                .importFrom(datagram)
                .build();

            _LOGGER
                .trace(
                    RLPMessages.RECEIVED_MESSAGE,
                    _datagramSocket.getLocalSocketAddress(),
                    datagram.getSocketAddress(),
                    receivedMessage);

            if ((receivedMessage.getMessageID() == sentMessage.getMessageID())
                    && (receivedMessage.getMessageType()
                        == MessageType.I_PROVIDE)) {
                return Optional.of(receivedMessage.getResourceSpecifiers());
            }
        }
    }

    /**
     * Who provides.
     *
     * <p>This method waits a maximum amount of time to receive at least one
     * response. After each response, it will also wait a minimum amount of time
     * to get an other.</p>
     *
     * @param resourceProvider A resource provider (broadcast).
     * @param resourceSpecifiers The resource specifiers.
     * @param local True for local resource providers.
     * @param maximumWait The maximum time to wait.
     * @param minimumWait The minimum time to wait.
     *
     * @return The resource providers (null on timeout or socket closed).
     *
     * @throws IOException On failure.
     */
    @Nonnull
    @CheckReturnValue
    public synchronized Optional<ResourceProvider[]> whoProvides(
            @Nonnull final ResourceProvider resourceProvider,
            @Nonnull final ResourceSpecifier[] resourceSpecifiers,
            final boolean local,
            @Nonnull final ElapsedTime maximumWait,
            @Nonnull final ElapsedTime minimumWait)
        throws IOException
    {
        final Datagram datagram = new Datagram()
            .setRemoteAddress(resourceProvider.getAddress())
            .setRemotePort(resourceProvider.getPort());
        final RLPMessage sentMessage = newMessage(
            MessageType.WHO_PROVIDES,
            resourceSpecifiers,
            local);

        sentMessage.exportTo(datagram);

        if (!datagram.send(_datagramSocket, true)) {
            return Optional.empty();
        }

        _LOGGER
            .trace(
                RLPMessages.SENT_MESSAGE,
                _datagramSocket.getLocalSocketAddress(),
                datagram.getSocketAddress(),
                sentMessage);

        final List<ResourceProvider> resourceProviders = new LinkedList<>();
        int wait = _intMillis(maximumWait);

        while (datagram.receive(_datagramSocket, wait)) {
            final RLPMessage receivedMessage = RLPMessage
                .newBuilder()
                .importFrom(datagram)
                .build();

            _LOGGER
                .trace(
                    RLPMessages.RECEIVED_MESSAGE,
                    _datagramSocket.getLocalSocketAddress(),
                    datagram.getSocketAddress(),
                    receivedMessage);

            if ((receivedMessage.getMessageID() == sentMessage.getMessageID())
                    && (receivedMessage.getMessageType()
                        == MessageType.I_PROVIDE)) {
                resourceProviders
                    .add(
                        ResourceProvider
                            .newBuilder()
                            .importFrom(receivedMessage)
                            .build());
            }

            wait = _intMillis(minimumWait);

            if (wait == 0) {
                break;    // First reply only.
            }
        }

        return Optional
            .of(
                resourceProviders
                    .toArray(new ResourceProvider[resourceProviders.size()]));
    }

    private static int _intMillis(final ElapsedTime elapsedTime)
    {
        final long millis = elapsedTime.toMillis();

        return (millis <= Integer.MAX_VALUE)? (int) millis: 0;
    }

    private static RLPMessage newMessage(
            final MessageType messageType,
            final ResourceSpecifier[] resourceSpecifiers,
            final boolean local)
    {
        return RLPMessage
            .newBuilder()
            .setMessageType(messageType)
            .setResourceSpecifiers(resourceSpecifiers)
            .setLocal(local)
            .build();
    }

    private static final Logger _LOGGER = Logger.getInstance(RLPClient.class);

    private final DatagramSocket _datagramSocket;

    /**
     * Builder.
     */
    @NotThreadSafe
    public static final class Builder
    {
        /**
         * Constructs an instance.
         */
        Builder() {}

        /**
         * Builds a Client.
         *
         * @return The Client.
         *
         * @throws SocketException On failure.
         */
        @Nonnull
        @CheckReturnValue
        public RLPClient build()
            throws SocketException
        {
            return new RLPClient(
                _localAddress.orElse(Inet.getLocalHostAddress()),
                _localPort);
        }

        /**
         * Sets the local address.
         *
         * @param localAddress The local address.
         *
         * @return This.
         */
        @Nonnull
        public Builder setLocalAddress(
                @Nonnull final Optional<InetAddress> localAddress)
        {
            _localAddress = localAddress;

            return this;
        }

        /**
         * Sets the local port.
         *
         * @param localPort The local port.
         *
         * @return This.
         */
        @Nonnull
        public Builder setLocalPort(final int localPort)
        {
            _localPort = localPort;

            return this;
        }

        private Optional<InetAddress> _localAddress = Optional.empty();
        private int _localPort;
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
