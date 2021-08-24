/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ResourceProvider.java 3983 2019-05-14 11:11:45Z SFB $
 */

package org.rvpf.base.util.rlp;

import java.net.InetAddress;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.tool.Inet;

/**
 * Resource provider.
 *
 * <p>Provides resources, listening at a specified UDP address and port.</p>
 */
@Immutable
public class ResourceProvider
{
    /**
     * Constructs an instance (called only by a builder).
     *
     * @param address An UDP address.
     * @param port A UDP port.
     * @param resourceSpecifiers The specifiers for the provided resources.
     */
    ResourceProvider(
            @Nonnull final InetAddress address,
            final int port,
            @Nonnull final ResourceSpecifier[] resourceSpecifiers)
    {
        _address = address;
        _port = port;
        _resourceSpecifiers = resourceSpecifiers;
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
     * Gets the address.
     *
     * @return The address.
     */
    @Nonnull
    @CheckReturnValue
    public InetAddress getAddress()
    {
        return _address;
    }

    /**
     * Gets the port.
     *
     * @return The port.
     */
    @CheckReturnValue
    public int getPort()
    {
        return _port;
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

    private final InetAddress _address;
    private final int _port;
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
         * Builds a resource provider.
         *
         * @return The resource provider.
         */
        @Nonnull
        @CheckReturnValue
        public ResourceProvider build()
        {
            return new ResourceProvider(
                _address.orElse(Inet.getLocalHostAddress()),
                _port,
                (_resourceSpecifiers != null)
                ? _resourceSpecifiers: new ResourceSpecifier[0]);
        }

        /**
         * Imports from a message.
         *
         * @param message The message.
         *
         * @return This.
         */
        @Nonnull
        public Builder importFrom(@Nonnull final RLPMessage message)
        {
            setAddress(message.getRemoteAddress());
            setPort(message.getRemotePort());

            setResourceSpecifier(message.getResourceSpecifiers());

            return this;
        }

        /**
         * Sets the address.
         *
         * @param address The address.
         *
         * @return This.
         */
        @Nonnull
        public Builder setAddress(@Nonnull final Optional<InetAddress> address)
        {
            _address = address;

            return this;
        }

        /**
         * Sets the port.
         *
         * @param port The port.
         *
         * @return This.
         */
        @Nonnull
        public Builder setPort(final int port)
        {
            _port = port;

            return this;
        }

        /**
         * Sets the resource specifiers.
         *
         * @param resourceSpecifiers The resource specifiers.
         */
        public void setResourceSpecifier(
                @Nonnull final ResourceSpecifier[] resourceSpecifiers)
        {
            _resourceSpecifiers = resourceSpecifiers;
        }

        private Optional<InetAddress> _address = Optional.empty();
        private int _port;
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
