/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: EthernetIPWrapper.java 4085 2019-06-16 15:17:12Z SFB $
 */

package org.rvpf.pap.cip.transport;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.tool.Require;
import org.rvpf.pap.cip.CIP;

import etherip.types.CIPData;

/**
 * Ethernet/IP wrapper.
 */
public interface EthernetIPWrapper
{
    /**
     * Closes.
     *
     * @throws Exception On failure.
     */
    void close()
        throws Exception;

    /**
     * Connects.
     *
     * @throws Exception On failure.
     */
    void connect()
        throws Exception;

    /**
     * Decodes the status at an index.
     *
     * @param index The index.
     *
     * @return The decoded status.
     */
    @Nonnull
    @CheckReturnValue
    String decodeStatus(int index);

    /**
     * Gets the socket address.
     *
     * @return The socket address.
     */
    @CheckReturnValue
    @Nonnull
    String getAddress();

    /**
     * Gets the status at an index.
     *
     * @param index The index.
     *
     * @return The status.
     */
    @CheckReturnValue
    int getStatus(int index);

    /**
     * Asks if connected.
     *
     * @return True if connected.
     */
    @CheckReturnValue
    boolean isConnected();

    /**
     * Reads tags.
     *
     * @param tags The tags.
     *
     * @return The values.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    CIPData[] readTags(@Nonnull String... tags)
        throws Exception;

    /**
     * Reads tags.
     *
     * @param tags The tags.
     * @param counts The number of elements for each tag.
     *
     * @return The values.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    CIPData[] readTags(
            @Nonnull String[] tags,
            @Nonnull int[] counts)
        throws Exception;

    /**
     * Write tags.
     *
     * @param tags The tags.
     * @param values The tag values.
     *
     * @throws Exception On failure.
     */
    void writeTags(
            @Nonnull String[] tags,
            @Nonnull CIPData[] values)
        throws Exception;

    /**
     * Builder.
     */
    abstract class Builder
    {
        /**
         * Builds an Ethernet/IP wrapper.
         *
         * @return The Ethernet/IP wrapper.
         */
        @Nonnull
        @CheckReturnValue
        public abstract EthernetIPWrapper build();

        /**
         * Sets the address.
         *
         * @param address The address.
         *
         * @return This.
         */
        @Nonnull
        public Builder setAddress(@Nonnull final String address)
        {
            _address = Require.notNull(address);

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
         * Sets the slot.
         *
         * @param slot The slot.
         *
         * @return This.
         */
        @Nonnull
        public Builder setSlot(final int slot)
        {
            _slot = slot;

            return this;
        }

        /**
         * Sets the timeout.
         *
         * @param timeout The timeout.
         *
         * @return This.
         */
        @Nonnull
        public Builder setTimeout(@Nonnull final ElapsedTime timeout)
        {
            _timeout = Require.notNull(timeout);

            return this;
        }

        /**
         * Sets the timeout.
         *
         * @param timeout The timeout in milliseconds.
         *
         * @return This.
         */
        @Nonnull
        public Builder setTimeout(final long timeout)
        {
            _timeout = ElapsedTime.fromMillis(timeout);

            return this;
        }

        /**
         * Gets the address.
         *
         * @return The address.
         */
        @Nonnull
        @CheckReturnValue
        protected String getAddress()
        {
            return Require.notNull(_address);
        }

        /**
         * Gets the port.
         *
         * @return The port.
         */
        @CheckReturnValue
        protected int getPort()
        {
            return _port;
        }

        /**
         * Gets the slot.
         *
         * @return The slot.
         */
        @CheckReturnValue
        protected int getSlot()
        {
            return _slot;
        }

        /**
         * Gets the timeout.
         *
         * @return The timeout.
         */
        @Nonnull
        @CheckReturnValue
        protected ElapsedTime getTimeout()
        {
            return _timeout;
        }

        private String _address;
        private int _port = CIP.DEFAULT_TCP_PORT;
        private int _slot;
        private ElapsedTime _timeout = CIP.DEFAULT_TIMEOUT;
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
