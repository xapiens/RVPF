/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RLPMessage.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base.util.rlp;

import java.net.InetAddress;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Require;

/**
 * RLP message.
 */
@Immutable
class RLPMessage
{
    /**
     * Constructs an instance (called only by a builder).
     *
     * @param messageType The message type.
     * @param local True if local.
     * @param messageID The message ID.
     * @param resourceSpecifiers The resource specifiers.
     * @param remoteAddress The remote address (empty if unknown).
     * @param remotePort The remote port (0 if unknown).
     */
    RLPMessage(
            @Nonnull final MessageType messageType,
            final boolean local,
            final short messageID,
            @Nonnull final ResourceSpecifier[] resourceSpecifiers,
            @Nonnull final Optional<InetAddress> remoteAddress,
            final int remotePort)
    {
        _messageType = Require.notNull(messageType);
        _local = local;
        _messageID = messageID;
        _resourceSpecifiers = resourceSpecifiers;
        _remoteAddress = remoteAddress;
        _remotePort = remotePort;
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
     * Exports this message.
     *
     * @param datagram The destination datagram.
     */
    public void exportTo(@Nonnull final Datagram datagram)
    {
        datagram
            .reset()
            .addByte((byte) _messageType.ordinal())
            .addByte((byte) (_local? LOCAL_MASK: 0))
            .addWord(_messageID);

        for (final ResourceSpecifier resourceSpecifier: _resourceSpecifiers) {
            final byte[] resourceIdentifier = resourceSpecifier.getIdentifier();

            datagram.addByte((byte) resourceSpecifier.getProtocol());
            datagram.addByte((byte) resourceIdentifier.length);

            for (final byte resourceID: resourceIdentifier) {
                datagram.addByte(resourceID);
            }
        }
    }

    /**
     * Gets the message ID.
     *
     * @return The message ID.
     */
    @CheckReturnValue
    public short getMessageID()
    {
        return _messageID;
    }

    /**
     * Gets the messageType.
     *
     * @return The messageType.
     */
    @Nonnull
    @CheckReturnValue
    public MessageType getMessageType()
    {
        return _messageType;
    }

    /**
     * Gets the remoteAddress.
     *
     * @return The remoteAddress.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<InetAddress> getRemoteAddress()
    {
        return _remoteAddress;
    }

    /**
     * Gets the remotePort.
     *
     * @return The remotePort.
     */
    @CheckReturnValue
    public int getRemotePort()
    {
        return _remotePort;
    }

    /**
     * Gets the resource specifiers.
     *
     * @return The resource specifiers.
     */
    @Nonnull
    @CheckReturnValue
    public ResourceSpecifier[] getResourceSpecifiers()
    {
        return _resourceSpecifiers;
    }

    /**
     * Asks if is local.
     *
     * @return True when is local.
     */
    @CheckReturnValue
    public boolean isLocal()
    {
        return _local;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder stringBuilder = new StringBuilder(
            _messageType.name());

        if (isLocal()) {
            stringBuilder.append(" (local)");
        }

        stringBuilder.append(" ID=");
        stringBuilder.append(_messageID & 0xFFFF);
        stringBuilder.append(" {");

        for (final ResourceSpecifier resourceSpecifier: _resourceSpecifiers) {
            final byte[] resourceIdentifier = resourceSpecifier.getIdentifier();

            stringBuilder.append("[");
            stringBuilder.append(resourceSpecifier.getProtocol());

            if (resourceIdentifier.length > 0) {
                stringBuilder.append(",");

                for (final byte identifierByte: resourceIdentifier) {
                    stringBuilder.append(" ");

                    final String hexDigits = Integer
                        .toHexString(identifierByte & 0xFF);

                    if (hexDigits.length() < 2) {
                        stringBuilder.append('0');
                    }

                    stringBuilder.append(hexDigits);
                }
            }

            stringBuilder.append("]");
        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }

    static final int LOCAL_MASK = 0xA0;

    private final boolean _local;
    private final short _messageID;
    private final MessageType _messageType;
    private final Optional<InetAddress> _remoteAddress;
    private final int _remotePort;
    private final ResourceSpecifier[] _resourceSpecifiers;

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
         * Builds a message.
         *
         * @return The message.
         */
        @Nonnull
        @CheckReturnValue
        public RLPMessage build()
        {
            final RLPMessage message = new RLPMessage(
                _messageType,
                _local,
                (_messageID != 0)? _messageID: (short) _RANDOM.nextInt(),
                (_resourceSpecifiers != null)
                ? _resourceSpecifiers: new ResourceSpecifier[0],
                _remoteAddress,
                _remotePort);

            _messageID = 0;

            return message;
        }

        /**
         * Imports from a datagram.
         *
         * @param datagram The datagram.
         *
         * @return This.
         *
         * @throws IllegalArgumentException On bad datagram.
         */
        @Nonnull
        public Builder importFrom(
                @Nonnull final Datagram datagram)
            throws IllegalArgumentException
        {
            if (datagram.remainingLength() < 4) {
                throw new IllegalArgumentException(
                    Message.format(
                        RLPMessages.DATAGRAM_TOO_SHORT,
                        String.valueOf(datagram.remainingLength())));
            }

            final byte typeByte = datagram.getNextByte();
            final MessageType messageType;

            try {
                messageType = MessageType.instance(typeByte);
            } catch (final Require.FailureException exception) {
                throw new IllegalArgumentException(
                    Message.format(
                        RLPMessages.UNKNOWN_MESSAGE_TYPE,
                        String.valueOf(typeByte & 0xFF)));
            }

            setMessageType(messageType);
            setLocal((datagram.getNextByte() & LOCAL_MASK) != 0);
            setMessageID(datagram.getNextWord());

            final List<ResourceSpecifier> resourceSpecifiers =
                new LinkedList<>();

            while (datagram.remainingLength() > 0) {
                resourceSpecifiers
                    .add(
                        ResourceSpecifier
                            .newBuilder()
                            .importFrom(datagram)
                            .build());
            }

            setResourceSpecifiers(
                resourceSpecifiers
                    .toArray(new ResourceSpecifier[resourceSpecifiers.size()]));

            setRemoteAddress(Optional.of(datagram.getAddress()));
            setRemotePort(datagram.getPort());

            return this;
        }

        /**
         * Sets local.
         *
         * @param local True if local.
         *
         * @return This.
         */
        @Nonnull
        public Builder setLocal(final boolean local)
        {
            _local = local;

            return this;
        }

        /**
         * Sets the message ID.
         *
         * @param messageID The message ID.
         *
         * @return This.
         */
        @Nonnull
        public Builder setMessageID(final short messageID)
        {
            _messageID = messageID;

            return this;
        }

        /**
         * Sets the message type.
         *
         * @param messageType The message type.
         *
         * @return This.
         */
        @Nonnull
        public Builder setMessageType(@Nonnull final MessageType messageType)
        {
            _messageType = messageType;

            return this;
        }

        /**
         * Sets the remote address.
         *
         * @param remoteAddress The remote address.
         *
         * @return This.
         */
        @Nonnull
        public Builder setRemoteAddress(
                @Nonnull final Optional<InetAddress> remoteAddress)
        {
            _remoteAddress = remoteAddress;

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
        public Builder setRemotePort(final int remotePort)
        {
            _remotePort = remotePort;

            return this;
        }

        /**
         * Sets the resource specifiers.
         *
         * @param resourceSpecifiers The resource specifiers.
         *
         * @return This.
         */
        @Nonnull
        public Builder setResourceSpecifiers(
                @Nonnull final ResourceSpecifier[] resourceSpecifiers)
        {
            _resourceSpecifiers = resourceSpecifiers;

            return this;
        }

        private static final Random _RANDOM = new Random();

        private boolean _local;
        private short _messageID;
        private MessageType _messageType;
        private Optional<InetAddress> _remoteAddress;
        private int _remotePort;
        private ResourceSpecifier[] _resourceSpecifiers;
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
