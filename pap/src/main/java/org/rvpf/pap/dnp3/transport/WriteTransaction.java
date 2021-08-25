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

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.PAPTransaction;
import org.rvpf.pap.PAPWriteTransaction;
import org.rvpf.pap.dnp3.DNP3OutstationProxy;

/**
 * Write transaction.
 */
public final class WriteTransaction
    extends Transaction
{
    /**
     * Constructs an instance.
     *
     * @param outstationProxy The outstation proxy.
     */
    WriteTransaction(@Nonnull final DNP3OutstationProxy outstationProxy)
    {
        super(outstationProxy);
    }

    /**
     * Adds a request.
     *
     * @param pointValue A point value.
     *
     * @return The new request.
     */
    @Nonnull
    Request addRequest(@Nonnull final PointValue pointValue)
    {
        final Request request = new Request(pointValue);

        _requests.add(request);

        return request;
    }

    /**
     * Adds a response.
     *
     * @param request The request.
     * @param success True on success.
     */
    void addResponse(@Nonnull final Request request, final boolean success)
    {
        _responses.add(new Response(request, success));
    }

    /**
     * Commits.
     *
     * @return The responses.
     *
     * @throws ServiceNotAvailableException On connection failure.
     */
    @Nonnull
    @CheckReturnValue
    Response[] commit()    // ...
        throws ServiceNotAvailableException
    {
//        if (!_requests.isEmpty()) {
//            final Connection connection = getServerProxy().connect();
//
//            connection.sendWriteRequests(this);
//        }

        final Response[] responses = _responses
            .toArray(new Response[_responses.size()]);

        rollback();

        return responses;
    }

    /**
     * Gets the requests.
     *
     * @return The requests.
     */
    @Nonnull
    @CheckReturnValue
    Collection<Request> getRequests()
    {
        return _requests;
    }

    /**
     * Asks if the transaction is empty.
     *
     * @return True if empty.
     */
    @CheckReturnValue
    boolean isEmpty()
    {
        return _requests.isEmpty();
    }

    /**
     * Rools back.
     */
    void rollback()
    {
        _requests.clear();
        _responses.clear();
    }

    private final Collection<Request> _requests = new LinkedList<>();
    private final Collection<Response> _responses = new LinkedList<>();

    /**
     * Request.
     */
    public final class Request
        extends Transaction.Request
        implements PAPWriteTransaction.Request
    {
        /**
         * Constructs an instance.
         *
         * @param pointValue The point value.
         */
        Request(@Nonnull final PointValue pointValue)
        {
            _pointValue = pointValue;
        }

        /**
         * Gets the point value.
         *
         * @return The point value.
         */
        @Nonnull
        @CheckReturnValue
        public PointValue getPointValue()
        {
            return _pointValue;
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<? extends PAPTransaction.Response> getResponse()
            throws InterruptedException
        {
            return Optional.empty();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean waitForResponse()
            throws InterruptedException
        {
            return false;
        }

        private final PointValue _pointValue;
    }


    /**
     * Response.
     */
    public final class Response
        extends Transaction.Response
        implements PAPWriteTransaction.Response
    {
        /**
         * Constructs an instance.
         *
         * @param request The request.
         * @param success True on success.
         */
        Response(@Nonnull final Request request, final boolean success)
        {
            super(request);

            _success = success;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isSuccess()
        {
            return _success && !getException().isPresent();
        }

        private final boolean _success;
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
