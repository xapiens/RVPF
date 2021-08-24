/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SecureRMIServerSocketFactory.java 4082 2019-06-14 18:38:55Z SFB $
 */

package org.rvpf.service.rmi;

import java.io.IOException;

import java.net.InetAddress;
import java.net.ServerSocket;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;

/**
 * Secure RMI server socket factory.
 */
public class SecureRMIServerSocketFactory
    extends BaseRMIServerSocketFactory
{
    /**
     * Constructs an instance.
     *
     * @param address The address (may be empty).
     * @param securityContext The security context.
     */
    SecureRMIServerSocketFactory(
            @Nonnull final Optional<InetAddress> address,
            @Nonnull final SessionSecurityContext securityContext)
    {
        super(address);

        _securityContext = Require.notNull(securityContext);
    }

    /**
     * Creates a server socket on the specified port.
     *
     * @param port The port number.
     *
     * @return The server socket on the specified port.
     *
     * @throws IOException If an I/O error occurs during server socket creation.
     */
    @Override
    public ServerSocket createServerSocket(final int port)
        throws IOException
    {
        return _securityContext
            .getSSLServerSocketFactory()
            .createServerSocket(port, 0, getAddress().orElse(null));
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if (!super.equals(other)) {
            return false;
        }

        return _securityContext
               == ((SecureRMIServerSocketFactory) other)._securityContext;
    }

    /** {@inheritDoc}
     */
    @Override
    public final int hashCode()
    {
        return _securityContext.hashCode();
    }

    private final SessionSecurityContext _securityContext;
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
