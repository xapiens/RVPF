/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CIPProxy.java 4098 2019-06-25 16:46:46Z SFB $
 */

package org.rvpf.pap.cip;

import java.net.InetSocketAddress;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.BaseMessages;
import org.rvpf.base.Origin;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.tool.Require;
import org.rvpf.pap.PAPProxy;

/**
 * CIP proxy.
 */
public abstract class CIPProxy
    extends PAPProxy
{
    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    protected CIPProxy(@Nonnull final CIPProxy other)
    {
        super(other);

        _tcpAddress = other._tcpAddress;
        _tcpPort = other._tcpPort;
    }

    /**
     * Constructs an instance.
     *
     * @param context The context.
     * @param origin The origin representing the remote.
     */
    CIPProxy(@Nonnull final CIPContext context, @Nonnull final Origin origin)
    {
        super(context, origin);
    }

    /** {@inheritDoc}
     */
    @Override
    public CIPContext getContext()
    {
        return (CIPContext) super.getContext();
    }

    /**
     * Gets the TCP address.
     *
     * @return The TCP address.
     */
    @Nonnull
    @CheckReturnValue
    public String getTcpAddress()
    {
        return Require.notNull(_tcpAddress);
    }

    /**
     * Gets the TCP port.
     *
     * @return The TCP port.
     */
    @CheckReturnValue
    public int getTcpPort()
    {
        return _tcpPort;
    }

    /**
     * Sets up this proxy.
     *
     * @param originAttributes The origin attributes.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected boolean setUp(@Nonnull final Attributes originAttributes)
    {
        final String tcpAddressString = originAttributes
            .getString(CIP.TCP_ADDRESS_ATTRIBUTE)
            .get();
        final Optional<InetSocketAddress> tcpSocketAddress = Inet
            .socketAddress(tcpAddressString);

        if (!tcpSocketAddress.isPresent()) {
            getThisLogger().warn(BaseMessages.BAD_ADDRESS, tcpAddressString);

            return false;
        }

        _tcpAddress = tcpSocketAddress.get().getHostString();
        getThisLogger().debug(CIPMessages.TCP_ADDRESS, _tcpAddress);

        _tcpPort = tcpSocketAddress.get().getPort();

        if (_tcpPort <= 0) {
            _tcpPort = originAttributes
                .getInt(CIP.TCP_PORT_ATTRIBUTE, CIP.DEFAULT_TCP_PORT);
        }

        getThisLogger().debug(CIPMessages.TCP_PORT, String.valueOf(_tcpPort));

        return true;
    }

    private String _tcpAddress;
    private int _tcpPort;
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
