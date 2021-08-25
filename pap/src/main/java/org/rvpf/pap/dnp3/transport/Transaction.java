/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.pap.dnp3.transport;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.PAPTransaction;
import org.rvpf.pap.dnp3.DNP3OutstationProxy;

/**
 * Transaction.
 */
abstract class Transaction
    implements PAPTransaction
{
    /**
     * Constructs an instance.
     *
     * @param outstationProxy The outstation proxy.
     */
    protected Transaction(@Nonnull final DNP3OutstationProxy outstationProxy)
    {
        _outstationProxy = outstationProxy;
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
     * Gets the outstation proxy.
     *
     * @return The outstation proxy.
     */
    @Nonnull
    @CheckReturnValue
    DNP3OutstationProxy getOutstationProxy()
    {
        return _outstationProxy;
    }

    /**
     * Sets the exception.
     *
     * @param exception The exception.
     */
    void setException(@Nonnull final Exception exception)
    {
        _exception = exception;
    }

    private Exception _exception;
    private final DNP3OutstationProxy _outstationProxy;

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
