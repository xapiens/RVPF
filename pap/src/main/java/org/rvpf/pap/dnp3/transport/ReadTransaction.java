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

import org.rvpf.base.Point;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.PAPReadTransaction;
import org.rvpf.pap.PAPTransaction;
import org.rvpf.pap.dnp3.DNP3OutstationProxy;

/**
 * Read transaction.
 */
public final class ReadTransaction
    extends Transaction
{
    /**
     * Constructs an instance.
     *
     * @param outstationProxy The outstation proxy.
     */
    ReadTransaction(@Nonnull final DNP3OutstationProxy outstationProxy)
    {
        super(outstationProxy);
    }

    /**
     * Adds a request.
     *
     * @param point A point.
     *
     * @return The new request.
     */
    @Nonnull
    Request addRequest(@Nonnull final Point point)
    {
        final Request request = new Request(point);

        _requests.add(request);

        return request;
    }

    /**
     * Adds a response.
     *
     * @param request The request.
     * @param pointValue The optional point value.
     */
    void addResponse(
            @Nonnull final Request request,
            @Nonnull final Optional<PointValue> pointValue)
    {
        _responses.add(new Response(request, pointValue));
    }

    /**
     * Commits
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
        try {
//            if (!_requests.isEmpty()) {
//                final Connection connection = getServerProxy().connect();
//
//                connection.sendReadRequests(this);
//            }

            return _responses.toArray(new Response[_responses.size()]);
        } finally {
            rollback();
        }
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
     * Rolls back.
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
        implements PAPReadTransaction.Request
    {
        /**
         * Constructs an instance.
         *
         * @param point The point.
         */
        Request(@Nonnull final Point point)
        {
            _point = Require.notNull(point);
        }

        /** {@inheritDoc}
         */
        @Override
        public Point getPoint()
        {
            return _point;
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

        private final Point _point;
    }


    /**
     * Response.
     */
    public final class Response
        extends Transaction.Response
        implements PAPReadTransaction.Response
    {
        /**
         * Constructs an instance.
         *
         * @param request The request.
         * @param pointValue The optional point value.
         */
        Response(
                @Nonnull final Request request,
                @Nonnull final Optional<PointValue> pointValue)
        {
            super(request);

            _pointValue = pointValue;
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<PointValue> getPointValue()
        {
            return _pointValue;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isSuccess()
        {
            return _pointValue.isPresent() && !getException().isPresent();
        }

        final Optional<PointValue> _pointValue;
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
