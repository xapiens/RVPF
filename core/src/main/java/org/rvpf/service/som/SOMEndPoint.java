/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMEndPoint.java 4021 2019-05-24 13:01:15Z SFB $
 */

package org.rvpf.service.som;

import java.net.URI;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.tool.Require;
import org.rvpf.som.SOMServerImpl;

/**
 * SOM object.
 */
public interface SOMEndPoint
{
    /**
     * Closes.
     */
    void close();

    /**
     * Gets the exception.
     *
     * @return The exception (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    Optional<SessionException> getException();

    /**
     * Gets the SOM name.
     *
     * @return The SOM name.
     */
    @Nonnull
    @CheckReturnValue
    String getSOMName();

    /**
     * Gets the server.
     *
     * @return The server implementation (empty when client).
     */
    @Nonnull
    @CheckReturnValue
    Optional<? extends SOMServerImpl> getServer();

    /**
     * Gets the server URI.
     *
     * @return The server URI (empty when server).
     */
    @Nonnull
    @CheckReturnValue
    Optional<URI> getServerURI();

    /**
     * Asks if closed.
     *
     * @return True if closed.
     */
    @CheckReturnValue
    boolean isClosed();

    /**
     * Asks if open.
     *
     * @return True if open.
     */
    @CheckReturnValue
    boolean isOpen();

    /**
     * Asks if remote.
     *
     * @return True if remote.
     */
    @CheckReturnValue
    boolean isRemote();

    /**
     * Asks if server.
     *
     * @return True if server.
     */
    @CheckReturnValue
    boolean isServer();

    /**
     * Opens.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean open();

    /**
     * Tears down.
     */
    void tearDown();

    /**
     * Abstract.
     */
    abstract class Abstract
        implements SOMEndPoint
    {
        /** {@inheritDoc}
         */
        @Override
        public Optional<SessionException> getException()
        {
            return Optional.ofNullable(_exception);
        }

        /**
         * Clears the exception.
         */
        protected void clearException()
        {
            _exception = null;
        }

        /**
         * Sets the exception.
         *
         * @param exception The exception.
         */
        protected void setException(@Nonnull final SessionException exception)
        {
            _exception = Require.notNull(exception);
        }

        private SessionException _exception;
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
