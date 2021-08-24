/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SessionSecurityContext.java 4082 2019-06-14 18:38:55Z SFB $
 */

package org.rvpf.service.rmi;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLException;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.security.ServerSecurityContext;
import org.rvpf.base.tool.Require;

/**
 * Session Factory Context.
 */
@ThreadSafe
public final class SessionSecurityContext
    extends ServerSecurityContext
{
    /**
     * Constructs an instance.
     *
     * @param logger The logger instance to use.
     */
    SessionSecurityContext(@Nonnull final Logger logger)
    {
        super(logger);
    }

    /** {@inheritDoc}
     */
    @GuardedBy("this")
    @Override
    protected void useDefaults()
        throws SSLException
    {
        super.useDefaults();

        if (getKeyStoreConfig().getPath() == null) {
            throw new SSLException("Not configured for secure operation");
        }
    }

    /**
     * Gets the SSL server socket factory.
     *
     * @return The SSL server socket factory.
     *
     * @throws SSLException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    synchronized ServerSocketFactory getSSLServerSocketFactory()
        throws SSLException
    {
        if (_socketFactory == null) {
            _socketFactory = Require
                .notNull(createSSLContext().getServerSocketFactory());
        }

        return _socketFactory;
    }

    private ServerSocketFactory _socketFactory;
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
