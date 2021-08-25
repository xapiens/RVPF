/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: EtherIPWrapper.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.cip.transport;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import etherip.EtherNetIP;

import etherip.types.CIPData;

/**
 * EtherIP wrapper.
 */
public final class EtherIPWrapper
    implements EthernetIPWrapper
{
    /**
     * Constructs an instance.
     *
     * @param address The address.
     * @param port The port.
     * @param slot The slot.
     * @param timeout The timeout.
     */
    EtherIPWrapper(
            @Nonnull final String address,
            final int port,
            final int slot,
            final long timeout)
    {
        _address = address;
        _etherNetIP = new EtherNetIP(address, slot);
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
    public void close()
        throws Exception
    {
        _etherNetIP.close();
    }

    /** {@inheritDoc}
     */
    @Override
    public void connect()
        throws Exception
    {
        _etherNetIP.connectTcp();
    }

    /** {@inheritDoc}
     */
    @Override
    public String decodeStatus(final int index)
    {
        return _etherNetIP.decodeStatus(index);
    }

    /** {@inheritDoc}
     */
    @Override
    public String getAddress()
    {
        return _address;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getStatus(final int index)
    {
        return _etherNetIP.getStatus(index);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isConnected()
    {
        return _etherNetIP.isConnected();
    }

    /** {@inheritDoc}
     */
    @Override
    public CIPData[] readTags(final String... tags)
        throws Exception
    {
        return _etherNetIP.readTags(tags);
    }

    /** {@inheritDoc}
     */
    @Override
    public CIPData[] readTags(
            final String[] tags,
            final int[] counts)
        throws Exception
    {
        return _etherNetIP.readTags(tags, counts);
    }

    /** {@inheritDoc}
     */
    @Override
    public void writeTags(
            final String[] tags,
            final CIPData[] values)
        throws Exception
    {
        _etherNetIP.writeTags(tags, values);
    }

    private final String _address;
    private final EtherNetIP _etherNetIP;

    /**
     * Builder.
     */
    public static final class Builder
        extends EthernetIPWrapper.Builder
    {
        /**
         * Constructs an instance.
         */
        Builder() {}

        /** {@inheritDoc}
         */
        @Override
        public EtherIPWrapper build()
        {
            return new EtherIPWrapper(
                getAddress(),
                getPort(),
                getSlot(),
                getTimeout().toMillis());
        }
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
