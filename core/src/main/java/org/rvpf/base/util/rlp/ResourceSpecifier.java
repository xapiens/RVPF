/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ResourceSpecifier.java 3902 2019-02-20 22:30:12Z SFB $
 */

package org.rvpf.base.util.rlp;

import java.nio.charset.StandardCharsets;

import java.util.Arrays;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.logger.Message;

/**
 * Resource specifier.
 *
 * <p>Represents a resource for a specified protocol and identifier.</p>
 */
@Immutable
public class ResourceSpecifier
{
    /**
     * Constructs an instance (called only by a builder).
     *
     * @param protocol The protocol to access the resource.
     * @param identifier The resource identifier.
     *
     */
    ResourceSpecifier(final int protocol, @Nonnull final byte[] identifier)
    {
        _protocol = protocol;
        _identifier = identifier;
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

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if ((other == null) || (getClass() != other.getClass())) {
            return false;
        }

        final ResourceSpecifier otherSpecifier = (ResourceSpecifier) other;

        return (otherSpecifier.getProtocol() == getProtocol())
               && Arrays.equals(
                   otherSpecifier.getIdentifier(),
                   getIdentifier());
    }

    /**
     * Gets the identifier.
     *
     * @return The identifier.
     */
    @Nonnull
    @CheckReturnValue
    public byte[] getIdentifier()
    {
        return _identifier;
    }

    /**
     * Gets the protocol.
     *
     * @return The protocol.
     */
    @CheckReturnValue
    public int getProtocol()
    {
        return _protocol;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return Integer.hashCode(_protocol) ^ Arrays.hashCode(_identifier);
    }

    private final byte[] _identifier;
    private final int _protocol;

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
         * Builds a resource specifier.
         *
         * @return The resource specifier.
         */
        @Nonnull
        @CheckReturnValue
        public ResourceSpecifier build()
        {
            final byte[] identifier = (_identifier != null)
                ? _identifier: new byte[0];

            return new ResourceSpecifier(_protocol, identifier);
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
            setProtocol(datagram.getNextByte());

            final byte[] identifier = (datagram
                .remainingLength() > 0)? new byte[datagram.getNextByte() & 0xFF]
                    : null;

            if ((identifier == null)
                    || (datagram.remainingLength() < identifier.length)) {
                datagram.reset();

                throw new IllegalArgumentException(
                    Message.format(
                        RLPMessages.DATAGRAM_TOO_SHORT,
                        String.valueOf(datagram.remainingLength())));
            }

            for (int i = 0; i < identifier.length; ++i) {
                identifier[i] = datagram.getNextByte();
            }

            setIdentifier(identifier);

            return this;
        }

        /**
         * Sets the identifier.
         *
         * @param identifier The identifier.
         *
         * @return This.
         */
        @Nonnull
        public Builder setIdentifier(@Nonnull final byte[] identifier)
        {
            _identifier = identifier;

            return this;
        }

        /**
         * Sets the identifier.
         *
         * @param identifier The identifier.
         *
         * @return This.
         */
        @Nonnull
        public Builder setIdentifier(@Nonnull final String identifier)
        {
            return setIdentifier(identifier.getBytes(StandardCharsets.UTF_8));
        }

        /**
         * Sets the protocol.
         *
         * @param protocol The protocol.
         *
         * @return This.
         */
        @Nonnull
        public Builder setProtocol(final int protocol)
        {
            _protocol = protocol;

            return this;
        }

        /**
         * Sets the protocol.
         *
         * @param protocol The protocol.
         *
         * @return This.
         */
        @Nonnull
        public Builder setProtocol(@Nonnull final Protocol protocol)
        {
            return setProtocol(protocol.code());
        }

        private byte[] _identifier;
        private int _protocol;
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
