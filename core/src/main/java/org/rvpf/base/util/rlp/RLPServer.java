/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RLPServer.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base.util.rlp;

import java.io.IOException;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * RLP server.
 *
 * <p>Represents a process that responds to resource location requests.</p>
 */
@NotThreadSafe
public class RLPServer
    implements ServiceThread.Target
{
    /**
     * Constructs an instance.
     *
     * @param resourceSpecifiers Resource specifiers.
     * @param localAddress The local address.
     * @param localPort The local port.
     */
    RLPServer(
            @Nonnull final Set<ResourceSpecifier> resourceSpecifiers,
            @Nonnull final Optional<InetAddress> localAddress,
            final int localPort)
    {
        _resourceSpecifiers = resourceSpecifiers;
        _localAddress = localAddress;
        _localPort = localPort;
        _thread = new ServiceThread(this, "RLP Server");
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
     * Gets the local address.
     *
     * @return The local address.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<InetAddress> getLocalAddress()
    {
        return _localAddress;
    }

    /**
     * Gets the local port.
     *
     * @return The local port.
     */
    @CheckReturnValue
    public int getLocalPort()
    {
        return _localPort;
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws Exception
    {
        try {
            final Datagram datagram = new Datagram();
            final RLPMessage.Builder messageBuilder = RLPMessage.newBuilder();

            for (;;) {
                if (!datagram.receive(_datagramSocket, 0)) {
                    break;
                }

                try {
                    messageBuilder.importFrom(datagram);
                } catch (final IllegalArgumentException exception) {
                    _LOGGER
                        .debug(
                            RLPMessages.BAD_DATAGRAM,
                            exception.getMessage());

                    continue;    // Ignores.
                }

                final RLPMessage receivedMessage = messageBuilder.build();

                _LOGGER
                    .trace(
                        RLPMessages.RECEIVED_MESSAGE,
                        _datagramSocket.getLocalSocketAddress(),
                        datagram.getSocketAddress(),
                        receivedMessage);

                switch (receivedMessage.getMessageType()) {
                    case DO_YOU_PROVIDE:

                        _processProviderRequest(
                            datagram,
                            receivedMessage,
                            true);

                        break;

                    case DOES_ANYONE_PROVIDE:

                        break;    // Not supported.

                    case I_PROVIDE:

                        break;    // Ignored.

                    case THEY_PROVIDE:

                        break;    // Ignored.

                    case WHO_ANYWHERE_PROVIDES:

                        break;    // Not supported.

                    case WHO_PROVIDES:

                        _processProviderRequest(
                            datagram,
                            receivedMessage,
                            false);

                        break;

                    default:
                        Require.failure();
                }
            }
        } finally {
            _datagramSocket.close();
        }
    }

    /**
     * Starts.
     *
     * @throws SocketException On failure.
     */
    public final void start()
        throws SocketException
    {
        _LOGGER.debug(ServiceMessages.STARTING_THREAD, _thread.getName());

        _datagramSocket = new DatagramSocket(
            _localPort,
            _localAddress.orElse(null));
        _thread.start();
    }

    /**
     * Stops.
     *
     * @param joinTimeout The join timeout.
     */
    public final void stop(final long joinTimeout)
    {
        _LOGGER.debug(ServiceMessages.STOPPING_THREAD, _thread.getName());

        _datagramSocket.close();
        Require.ignored(_thread.join(_LOGGER, joinTimeout));
    }

    private void _processProviderRequest(
            final Datagram datagram,
            final RLPMessage receivedMessage,
            final boolean replyRequired)
        throws IOException
    {
        final List<ResourceSpecifier> resourceSpecifiers = new ArrayList<>(
            receivedMessage.getResourceSpecifiers().length);

        for (final ResourceSpecifier resourceSpecifier:
                receivedMessage.getResourceSpecifiers()) {
            if (_resourceSpecifiers.contains(resourceSpecifier)) {
                resourceSpecifiers.add(resourceSpecifier);
            }
        }

        if ((resourceSpecifiers.size() == 0) && !replyRequired) {
            return;
        }

        final RLPMessage.Builder messageBuilder = RLPMessage.newBuilder();
        final RLPMessage sentMessage = messageBuilder
            .setMessageType(MessageType.I_PROVIDE)
            .setMessageID(receivedMessage.getMessageID())
            .setLocal(false)
            .setResourceSpecifiers(
                resourceSpecifiers
                    .toArray(new ResourceSpecifier[resourceSpecifiers.size()]))
            .build();

        sentMessage.exportTo(datagram);

        if (!datagram.send(_datagramSocket, false)) {
            return;
        }

        _LOGGER
            .trace(
                RLPMessages.SENT_MESSAGE,
                _datagramSocket.getLocalSocketAddress(),
                datagram.getSocketAddress(),
                sentMessage);

    }

    /**
     * UDP port.
     */
    public static final int UDP_PORT = 39;

    /**  */

    private static final Logger _LOGGER = Logger.getInstance(RLPServer.class);

    private DatagramSocket _datagramSocket;
    private final Optional<InetAddress> _localAddress;
    private final int _localPort;
    private final Set<ResourceSpecifier> _resourceSpecifiers;
    private final ServiceThread _thread;

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
         * Adds resource specifiers.
         *
         * @param resourceSpecifiers The resource specifiers.
         *
         * @return This.
         */
        public Builder addResourceSpecifier(
                @Nonnull final ResourceSpecifier... resourceSpecifiers)
        {
            for (final ResourceSpecifier resourceSpecifier:
                    resourceSpecifiers) {
                _resourceSpecifiers.add(resourceSpecifier);
            }

            return this;
        }

        /**
         * Builds a Server.
         *
         * @return The Server.
         */
        @Nonnull
        @CheckReturnValue
        public RLPServer build()
        {
            return new RLPServer(
                _resourceSpecifiers,
                _localAddress,
                (_localPort > 0)? _localPort: UDP_PORT);
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
         * @param localPort The port.
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
        private final Set<ResourceSpecifier> _resourceSpecifiers =
            new HashSet<>();
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
