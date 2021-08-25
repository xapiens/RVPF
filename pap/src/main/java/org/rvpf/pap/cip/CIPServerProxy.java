/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CIPServerProxy.java 4084 2019-06-15 18:32:47Z SFB $
 */

package org.rvpf.pap.cip;

import java.net.InetSocketAddress;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.exception.ConnectFailedException;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.cip.transport.ClientConnection;
import org.rvpf.pap.cip.transport.ReadTransaction;
import org.rvpf.pap.cip.transport.WriteTransaction;

/**
 * CIP server proxy.
 */
public class CIPServerProxy
    extends CIPProxy
{
    /**
     * Constructs an instance.
     *
     * @param context The context.
     * @param origin The origin representing the remote.
     */
    CIPServerProxy(
            @Nonnull final CIPContext context,
            @Nonnull final Origin origin)
    {
        super(context, origin);
    }

    private CIPServerProxy(final CIPServerProxy other)
    {
        super(other);

        _slot = other._slot;
        _timeout = other._timeout;
    }

    /**
     * Adds a read request for a point.
     *
     * @param point The point.
     *
     * @return The new read request.
     */
    @Nonnull
    @CheckReturnValue
    public ReadTransaction.Request addReadRequest(@Nonnull final Point point)
    {
        return _readTransaction.addRequest(point);
    }

    /**
     * Adds write request for a point value.
     *
     * @param pointValue The point value.
     *
     * @return The new write request.
     */
    @Nonnull
    @CheckReturnValue
    public WriteTransaction.Request addWriteRequest(
            @Nonnull final PointValue pointValue)
    {
        return _writeTransaction.addRequest(pointValue);
    }

    /**
     * Commits read requests.
     *
     * @return The responses.
     *
     * @throws ServiceNotAvailableException On connection failure.
     */
    @Nonnull
    @CheckReturnValue
    public ReadTransaction.Response[] commitReadRequests()
        throws ServiceNotAvailableException
    {
        try {
            return _readTransaction.commit();
        } catch (final ServiceNotAvailableException exception) {
            disconnect();

            throw exception;
        }
    }

    /**
     * Commits write requests.
     *
     * @return The responses.
     *
     * @throws ServiceNotAvailableException On connection failure.
     */
    @Nonnull
    @CheckReturnValue
    public WriteTransaction.Response[] commitWriteRequests()
        throws ServiceNotAvailableException
    {
        try {
            return _writeTransaction.commit();
        } catch (final ServiceNotAvailableException exception) {
            disconnect();

            throw exception;
        }
    }

    /**
     * Connects.
     *
     * @return A connection.
     *
     * @throws ConnectFailedException On failure.
     */
    @Nonnull
    @CheckReturnValue
    public ClientConnection connect()
        throws ConnectFailedException
    {
        synchronized (_connectMutex) {
            if (_connection == null) {
                final ClientConnection connection = new ClientConnection(
                    getTcpAddress(),
                    getTcpPort(),
                    _slot,
                    _timeout,
                    this,
                    getConnectionListener());

                try {
                    connection.open();
                } catch (final ConnectFailedException exception) {
                    getThisLogger()
                        .warn(
                            PAPMessages.SERVER_CONNECTION_FAILED,
                            new InetSocketAddress(
                                getTcpAddress(),
                                getTcpPort()));

                    throw exception;
                }

                _connection = connection;
            }
        }

        return _connection;
    }

    /** {@inheritDoc}
     */
    @Override
    public PAPProxy copy()
    {
        return new CIPServerProxy(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public void disconnect()
    {
        synchronized (_connectMutex) {
            final ClientConnection connection = _connection;

            if (connection != null) {
                _connection = null;
                connection.close();

                super.disconnect();
            }
        }
    }

    /**
     * Rolls back read requests.
     */
    public void rollbackReadRequests()
    {
        _readTransaction.rollback();
    }

    /**
     * Rolls back write requests.
     */
    public void rollbackWriteRequests()
    {
        _writeTransaction.rollback();
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final Attributes originAttributes)
    {
        if (!super.setUp(originAttributes)) {
            return false;
        }

        _slot = originAttributes.getInt(CIP.SLOT_ATTRIBUTE, 0);
        getThisLogger().debug(CIPMessages.SLOT, String.valueOf(_slot));

        final ElapsedTime timeout = originAttributes
            .getElapsed(
                CIP.TIMEOUT_ATTRIBUTE,
                Optional.of(CIP.DEFAULT_TIMEOUT),
                Optional.of(ElapsedTime.INFINITY))
            .get();

        getThisLogger().debug(CIPMessages.TIMEOUT, timeout);
        _timeout = timeout.toMillis();

        return true;
    }

    private final Object _connectMutex = new Object();
    private ClientConnection _connection;
    private final ReadTransaction _readTransaction = new ReadTransaction(this);
    private int _slot;
    private long _timeout;
    private final WriteTransaction _writeTransaction = new WriteTransaction(
        this);
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
