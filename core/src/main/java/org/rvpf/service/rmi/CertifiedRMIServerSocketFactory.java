/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CertifiedRMIServerSocketFactory.java 4082 2019-06-14 18:38:55Z SFB $
 */

package org.rvpf.service.rmi;

import java.io.IOException;

import java.net.InetAddress;
import java.net.ServerSocket;

import java.util.Optional;

import javax.annotation.Nonnull;

import javax.net.ssl.SSLServerSocket;

/**
 * Certified RMI server socket factory.
 */
final class CertifiedRMIServerSocketFactory
    extends SecureRMIServerSocketFactory
{
    /**
     * Constructs an instance.
     *
     * @param address The address (may be empty).
     * @param securityContext The security context.
     */
    CertifiedRMIServerSocketFactory(
            @Nonnull final Optional<InetAddress> address,
            @Nonnull final SessionSecurityContext securityContext)
    {
        super(address, securityContext);
    }

    /** {@inheritDoc}
     */
    @Override
    public ServerSocket createServerSocket(final int port)
        throws IOException
    {
        final SSLServerSocket socket = (SSLServerSocket) super
            .createServerSocket(port);

        socket.setNeedClientAuth(true);

        return socket;
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
