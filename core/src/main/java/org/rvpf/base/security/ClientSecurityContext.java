/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClientSecurityContext.java 3987 2019-05-15 19:41:07Z SFB $
 */

package org.rvpf.base.security;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import javax.net.SocketFactory;
import javax.net.ssl.SSLException;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;

/**
 * Secure client context.
 *
 * <p>Holds a client context by keeping a reference to a server URI and by
 * generating an identifying UUID.</p>
 *
 * <p>The generated UUID is used to establish a relation between RMI server
 * sessions and the client's context for socket creation. This is needed because
 * the framework allows configurations where mutiple services are running within
 * the same process, each with its own client / server properties.</p>
 */
@ThreadSafe
public final class ClientSecurityContext
    extends SecurityContext
{
    /**
     * Constructs an instance.
     *
     * @param logger The logger instance to use.
     */
    public ClientSecurityContext(@Nonnull final Logger logger)
    {
        super(logger);
    }

    /**
     * Gets the SSL socket factory.
     *
     * @return The SSL socket factory.
     *
     * @throws SSLException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    public synchronized SocketFactory getSSLSocketFactory()
        throws SSLException
    {
        if (_socketFactory == null) {
            _socketFactory = createSSLContext().getSocketFactory();
        }

        return _socketFactory;
    }

    /** {@inheritDoc}
     */
    @Override
    protected synchronized void useDefaults()
        throws SSLException
    {
        if (_socketFactory == null) {
            super.useDefaults();

            if (!getTrustStoreConfig().getPath().isPresent()
                    && !getKeyStoreConfig().getPath().isPresent()) {
                throw new SSLException(Message.format(BaseMessages.NO_SECURE));
            }
        }
    }

    @GuardedBy("this")
    private SocketFactory _socketFactory;
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
