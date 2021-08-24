/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BaseRMIServerSocketFactory.java 4082 2019-06-14 18:38:55Z SFB $
 */

package org.rvpf.service.rmi;

import java.io.IOException;

import java.net.InetAddress;
import java.net.ServerSocket;

import java.rmi.server.RMIServerSocketFactory;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;

/**
 * Base RMI server socket factory.
 */
public class BaseRMIServerSocketFactory
    implements RMIServerSocketFactory
{
    /**
     * Constructs an instance.
     *
     * @param address The address (may be empty).
     */
    public BaseRMIServerSocketFactory(
            @Nullable final Optional<InetAddress> address)
    {
        _address = address;
    }

    /** {@inheritDoc}
     */
    @Override
    public ServerSocket createServerSocket(final int port)
        throws IOException
    {
        return new ServerSocket(port, 0, getAddress().orElse(null));
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

        return Objects
            .equals(
                ((BaseRMIServerSocketFactory) other).getAddress(),
                getAddress());
    }

    /**
     * Gets the address.
     *
     * @return The optional address.
     */
    @Nonnegative
    @CheckReturnValue
    public final Optional<InetAddress> getAddress()
    {
        return _address;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final Optional<InetAddress> address = getAddress();

        return address.isPresent()? address.get().hashCode(): 0;
    }

    private final Optional<InetAddress> _address;
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
