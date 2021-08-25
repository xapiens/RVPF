/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CIPClient.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.pap.cip;

import java.nio.channels.AsynchronousCloseException;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.exception.ConnectFailedException;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.IdentityHashSet;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.PAPClient;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.cip.transport.ReadTransaction;
import org.rvpf.pap.cip.transport.WriteTransaction;

/**
 * Client.
 */
public class CIPClient
    extends PAPClient.Abstract
{
    /**
     * Constructs an instance
     *
     * @param clientContext The CIP client context.
     */
    public CIPClient(@Nonnull final CIPClientContext clientContext)
    {
        super(clientContext);
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        disconnect();

        rollbackReadRequests();
        rollbackWriteRequests();

        super.close();
    }

    /**
     * Commits point update requests.
     *
     * @return The responses.
     *
     * @throws ServiceNotAvailableException On connection failure.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<WriteTransaction.Response> commitPointUpdateRequests()
        throws ServiceNotAvailableException
    {
        final Collection<WriteTransaction.Response> responses =
            new LinkedList<>();

        synchronized (_outputServerProxies) {
            for (final CIPServerProxy serverProxy: _outputServerProxies) {
                try {
                    for (final WriteTransaction.Response response:
                            serverProxy.commitWriteRequests()) {
                        responses.add(response);
                    }
                } catch (final ServiceNotAvailableException exception) {
                    getThisLogger()
                        .warn(
                            PAPMessages.WRITE_COMMIT_FAILED,
                            serverProxy.getName().orElse(null));

                    throw exception;
                }
            }

            _outputServerProxies.clear();
        }

        return responses;
    }

    /**
     * Commits point value requests.
     *
     * @return The responses.
     *
     * @throws ServiceNotAvailableException On connection failure.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<ReadTransaction.Response> commitPointValueRequests()
        throws ServiceNotAvailableException
    {
        final Collection<ReadTransaction.Response> responses =
            new LinkedList<>();

        synchronized (_inputServerProxies) {
            for (final CIPServerProxy serverProxy: _inputServerProxies) {
                try {
                    for (final ReadTransaction.Response response:
                            serverProxy.commitReadRequests()) {
                        responses.add(response);
                    }
                } catch (final ConnectFailedException exception) {
                    throw exception;
                } catch (final ServiceNotAvailableException exception) {
                    getThisLogger()
                        .warn(
                            PAPMessages.READ_COMMIT_FAILED,
                            serverProxy.getName().orElse(null));

                    throw exception;
                }
            }

            _inputServerProxies.clear();
        }

        return responses;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean connect(final Origin origin)
    {
        final Optional<? extends PAPProxy> serverProxy = getServerProxy(origin);

        if (!serverProxy.isPresent()) {
            return false;
        }

        ((CIPClientContext) getContext()).setConnectionListener(this);

        try {
            ((CIPServerProxy) serverProxy.get()).connect();
        } catch (final ConnectFailedException exception) {
            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void disconnect(final Origin origin)
    {
        final Optional<PAPProxy> serverProxy = forgetServerProxy(origin);

        if (serverProxy.isPresent()) {
            serverProxy.get().disconnect();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public PointValue[] fetchPointValues(
            final Point[] points)
        throws InterruptedException, ServiceNotAvailableException
    {
        for (final Point point: points) {
            final Optional<ReadTransaction.Request> request = requestPointValue(
                point);

            Require.success(request.isPresent());
        }

        final Collection<ReadTransaction.Response> responses;

        try {
            responses = commitPointValueRequests();
        } catch (final ServiceNotAvailableException exception) {
            if (exception.getCause() instanceof AsynchronousCloseException) {
                throw new InterruptedException();
            }

            throw exception;
        }

        final PointValue[] pointValues = new PointValue[points.length];
        final Iterator<ReadTransaction.Response> responsesIterator = responses
            .iterator();

        for (int i = 0; i < pointValues.length; ++i) {
            final ReadTransaction.Response response = responsesIterator.next();

            pointValues[i] = response
                .isSuccess()? response.getPointValue().get(): null;
        }

        return pointValues;
    }

    /** {@inheritDoc}
     */
    @Override
    public void open() {}

    /**
     * Requests a point update.
     *
     * @param pointValue The point value.
     *
     * @return The optional new request.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<WriteTransaction.Request> requestPointUpdate(
            @Nonnull final PointValue pointValue)
    {
        final Point point = pointValue.getPoint().get();
        final Optional<? extends PAPProxy> serverProxy = getServerProxy(
            point.getOrigin().get());

        if (!serverProxy.isPresent()) {
            return Optional.empty();
        }

        synchronized (_outputServerProxies) {
            _outputServerProxies.add((CIPServerProxy) serverProxy.get());
        }

        return Optional
            .of(
                ((CIPServerProxy) serverProxy.get())
                    .addWriteRequest(pointValue));
    }

    /**
     * Requests a point value.
     *
     * @param point The point.
     *
     * @return The optional new request.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<ReadTransaction.Request> requestPointValue(
            @Nonnull final Point point)
    {
        final Optional<? extends PAPProxy> serverProxy = getServerProxy(
            point.getOrigin().get());

        if (!serverProxy.isPresent()) {
            return Optional.empty();
        }

        synchronized (_inputServerProxies) {
            _inputServerProxies.add((CIPServerProxy) serverProxy.get());
        }

        return Optional
            .of(((CIPServerProxy) serverProxy.get()).addReadRequest(point));
    }

    /**
     * Rolls back read requests.
     */
    public void rollbackReadRequests()
    {
        synchronized (_inputServerProxies) {
            for (final CIPServerProxy serverProxy: _inputServerProxies) {
                serverProxy.rollbackReadRequests();
            }

            _inputServerProxies.clear();
        }
    }

    /**
     * Rolls back write requests.
     */
    public void rollbackWriteRequests()
    {
        synchronized (_outputServerProxies) {
            for (final CIPServerProxy serverProxy: _outputServerProxies) {
                serverProxy.rollbackWriteRequests();
            }

            _outputServerProxies.clear();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Exception[] updatePointValues(
            final PointValue[] pointValues)
        throws ServiceNotAvailableException
    {
        final Exception[] exceptions = new Exception[pointValues.length];
        final Map<WriteTransaction.Request, Integer> requests =
            new IdentityHashMap<>();

        for (int i = 0; i < pointValues.length; i++) {
            final Optional<WriteTransaction.Request> request =
                requestPointUpdate(
                    pointValues[i]);

            if (request.isPresent()) {
                requests.put(request.get(), Integer.valueOf(i));
            } else {
                exceptions[i] = new ServiceNotAvailableException();
            }
        }

        for (final WriteTransaction.Response response:
                commitPointUpdateRequests()) {
            final int i = requests.get(response.getRequest()).intValue();

            if (!response.isSuccess()) {
                exceptions[i] = new Exception();

                continue;
            }
        }

        return exceptions;
    }

    private final Set<CIPServerProxy> _inputServerProxies =
        new IdentityHashSet<>();
    private final Set<CIPServerProxy> _outputServerProxies =
        new IdentityHashSet<>();
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
