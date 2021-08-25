/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Association.java 4085 2019-06-16 15:17:12Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.util.concurrent.CountDownLatch;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.tool.Require;

/**
 * Association.
 */
public abstract class Association
{
    /**
     * Constructs an instance.
     *
     * @param localDeviceAddress The local device address.
     * @param remoteDeviceAddress The remote device address.
     * @param remoteEndPoint The remote end point.
     */
    protected Association(
            final short localDeviceAddress,
            final short remoteDeviceAddress,
            @Nonnull final RemoteEndPoint remoteEndPoint)
    {
        _localDeviceAddress = localDeviceAddress;
        _remoteDeviceAddress = remoteDeviceAddress;
        _remoteEndPoint = Require.notNull(remoteEndPoint);

        _applicationLayer = new ApplicationLayer(this);
        _transportFunction = new TransportFunction(this);
        _dataLinkLayer = new DataLinkLayer(this);
    }

    /**
     * Closes.
     */
    public void close()
    {
        _dataLinkLayer.close();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object)
    {
        if (object == this) {
            return true;
        }

        if (object instanceof Association) {
            final Association other = (Association) object;

            return (getLocalDeviceAddress() == other.getLocalDeviceAddress())
                   && (getRemoteDeviceAddress()
                       == other.getRemoteDeviceAddress())
                   && (getRemoteEndPoint() == other.getRemoteEndPoint());
        }

        return false;
    }

    /**
     * Expects a confirm fragment.
     */
    public void expectConfirm()
    {
        _confirmLatch = new CountDownLatch(1);
    }

    /**
     * Gets the application layer.
     *
     * @return The application layer.
     */
    @Nonnull
    @CheckReturnValue
    public final ApplicationLayer getApplicationLayer()
    {
        return _applicationLayer;
    }

    /**
     * Gets the connection.
     *
     * @return The connection (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public final Connection getConnection()
    {
        return _remoteEndPoint.getConnection();
    }

    /**
     * Gets the connection manager.
     *
     * @return The connection manager.
     */
    @Nonnull
    @CheckReturnValue
    public final ConnectionManager getConnectionManager()
    {
        return _remoteEndPoint.getConnectionManager();
    }

    /**
     * Gets the dataLink layer.
     *
     * @return The dataLink layer.
     */
    @Nonnull
    @CheckReturnValue
    public final DataLinkLayer getDataLinkLayer()
    {
        return _dataLinkLayer;
    }

    /**
     * Gets the local device address.
     *
     * @return The local device address.
     */
    @CheckReturnValue
    public final short getLocalDeviceAddress()
    {
        return _localDeviceAddress;
    }

    /**
     * Gets the remote device address.
     *
     * @return The remote device address.
     */
    @CheckReturnValue
    public final short getRemoteDeviceAddress()
    {
        return _remoteDeviceAddress;
    }

    /**
     * Gets the remote end point.
     *
     * @return The remote end point.
     */
    @Nonnull
    @CheckReturnValue
    public final RemoteEndPoint getRemoteEndPoint()
    {
        return _remoteEndPoint;
    }

    /**
     * Gets the transport function.
     *
     * @return The transport function.
     */
    @Nonnull
    @CheckReturnValue
    public final TransportFunction getTransportFunction()
    {
        return _transportFunction;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return Integer
            .hashCode(
                _localDeviceAddress) ^ Integer.hashCode(
                    _remoteDeviceAddress) ^ System.identityHashCode(
                            _remoteEndPoint);
    }

    /**
     * Asks if with outstation.
     *
     * @return True if with outstation.
     */
    @CheckReturnValue
    public abstract boolean isWithOutstation();

    /**
     * Called on receipt of a confirm fragment.
     */
    public void onConfirm()
    {
        final CountDownLatch confirmLatch = _confirmLatch;

        if (confirmLatch != null) {
            confirmLatch.countDown();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "[local " + Integer.toHexString(
            _localDeviceAddress & 0xFFFF) + ", remote " + Integer.toHexString(
                _remoteDeviceAddress & 0xFFFF) + " on "
                + _remoteEndPoint.getRemoteProxyName().orElse(
                    null) + "]";
    }

    /**
     * Waits for a confirm fragment.
     *
     * @throws InterruptedException When interrupted.
     */
    public void waitForConfirm()
        throws InterruptedException
    {
        _confirmLatch.await();
        _confirmLatch = null;
    }

    private final ApplicationLayer _applicationLayer;
    private volatile CountDownLatch _confirmLatch;
    private final DataLinkLayer _dataLinkLayer;
    private final short _localDeviceAddress;
    private final short _remoteDeviceAddress;
    private final RemoteEndPoint _remoteEndPoint;
    private final TransportFunction _transportFunction;
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
