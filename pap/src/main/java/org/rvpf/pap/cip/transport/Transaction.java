/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Transaction.java 4085 2019-06-16 15:17:12Z SFB $
 */

package org.rvpf.pap.cip.transport;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.PAPTransaction;
import org.rvpf.pap.cip.CIPServerProxy;

/**
 * Transaction.
 */
abstract class Transaction
    implements PAPTransaction
{
    /**
     * Constructs an instance.
     *
     * @param serverProxy The server proxy.
     */
    protected Transaction(@Nonnull final CIPServerProxy serverProxy)
    {
        _serverProxy = serverProxy;
    }

    /**
     * Gets the exception.
     *
     * @return The optional exception.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Exception> getException()
    {
        return Optional.ofNullable(_exception);
    }

    /**
     * Gets the server proxy.
     *
     * @return The server proxy.
     */
    @Nonnull
    @CheckReturnValue
    CIPServerProxy getServerProxy()
    {
        return _serverProxy;
    }

    /**
     * Sets the exception.
     *
     * @param exception The exception.
     */
    void setException(@Nonnull final Exception exception)
    {
        _exception = Require.notNull(exception);
    }

    private Exception _exception;
    private final CIPServerProxy _serverProxy;

    /**
     * Request.
     */
    public abstract class Request
        implements PAPTransaction.Request
    {
        /**
         * Constructs an instance.
         */
        protected Request() {}
    }


    /**
     * Response.
     */
    public abstract static class Response
        implements PAPTransaction.Response
    {
        /**
         * Constructs an instance.
         *
         * @param request The request.
         */
        protected Response(@Nonnull final Request request)
        {
            _request = Require.notNull(request);
        }

        /**
         * Gets the request.
         *
         * @return The request.
         */
        @Nonnull
        @CheckReturnValue
        public Request getRequest()
        {
            return _request;
        }

        private final Request _request;
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
