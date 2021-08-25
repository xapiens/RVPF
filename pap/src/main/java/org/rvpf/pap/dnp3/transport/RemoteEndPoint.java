/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RemoteEndPoint.java 4098 2019-06-25 16:46:46Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.io.IOException;

import java.net.InetSocketAddress;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.dnp3.DNP3Messages;
import org.rvpf.pap.dnp3.DNP3Proxy;

/**
 * Remote end point.
 */
public class RemoteEndPoint
    extends EndPoint
{
    /**
     * Constructs an instance.
     *
     * @param connectionManager The connection manager.
     * @param remoteProxy The remote proxy.
     */
    public RemoteEndPoint(
            @Nonnull final ConnectionManager connectionManager,
            @Nonnull final DNP3Proxy remoteProxy)
    {
        super(connectionManager);

        _remoteProxy = remoteProxy;
    }

    /** {@inheritDoc}
     */
    @Override
    public final void close()
    {
        for (final Association association: _associations.values()) {
            association.close();
        }

        _associations.clear();

        super.close();
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean equals(final Object object)
    {
        if (this == object) {
            return true;
        }

        if ((object == null) || (getClass() != object.getClass())) {
            return false;
        }

        return getRemoteProxyName()
            .equals(((RemoteEndPoint) object).getRemoteProxyName());
    }

    /**
     * Gets an association for a local and remote address.
     *
     * @param localDeviceAddress The local device address.
     * @param remoteDeviceAddress The remote device address.
     *
     * @return The association.
     */
    @Nonnull
    @CheckReturnValue
    public final Association getAssociation(
            @Nonnull final short localDeviceAddress,
            @Nonnull final short remoteDeviceAddress)
    {
        final Integer associationKey = Integer
            .valueOf(
                (localDeviceAddress << 16) | (remoteDeviceAddress & 0xFFFF));
        Association association = _associations.get(associationKey);

        if (association == null) {
            if (isOnMaster()) {
                association = new MasterOutstationAssociation(
                    localDeviceAddress,
                    remoteDeviceAddress,
                    this);
            } else if (isOnOutstation()) {
                association = new OutstationMasterAssociation(
                    localDeviceAddress,
                    remoteDeviceAddress,
                    this);
            } else {
                throw Require.failure();
            }

            _associations.put(associationKey, association);
        }

        return association;
    }

    /**
     * Gets the connect timeout.
     *
     * @return The connect timeout.
     */
    @CheckReturnValue
    public final int getConnectTimeout()
    {
        return _remoteProxy.getConnectTimeout();
    }

    /**
     * Gets the connection.
     *
     * @return The connection (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public Connection getConnection()
    {
        return getConnectionManager().getConnection(this);
    }

    /**
     * Gets the maximum fragment size.
     *
     * @return The maximum fragment size.
     */
    @CheckReturnValue
    public final int getMaxFragmentSize()
    {
        return _remoteProxy.getMaxFragmentSize();
    }

    /**
     * Gets the remote proxy.
     *
     * @return The remote proxy.
     */
    @Nonnull
    @CheckReturnValue
    public final DNP3Proxy getRemoteProxy()
    {
        return _remoteProxy;
    }

    /**
     * Gets the remote proxy name.
     *
     * @return The optional remote proxy name.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<String> getRemoteProxyName()
    {
        return _remoteProxy.getName();
    }

    /**
     * Gets the reply timeout.
     *
     * @return The reply timeout.
     */
    @CheckReturnValue
    public final long getReplyTimeout()
    {
        return _remoteProxy.getReplyTimeout();
    }

    /**
     * Gets the serial port name.
     *
     * @return The serial port name (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    public final String getSerialPortName()
    {
        return _remoteProxy.getSerialPortName();
    }

    /**
     * Gets the serial port speed.
     *
     * @return The serial port speed.
     */
    @CheckReturnValue
    public final int getSerialPortSpeed()
    {
        return _remoteProxy.getSerialPortSpeed();
    }

    /**
     * Gets the TCP socket addresses.
     *
     * @return The socket TCP addresses.
     */
    @Nonnull
    @CheckReturnValue
    public final List<InetSocketAddress> getTCPSocketAddresses()
    {
        return _remoteProxy.getTCPSocketAddresses();
    }

    /**
     * Gets the UDP socket addresses.
     *
     * @return The socket UDP addresses.
     */
    @Nonnull
    @CheckReturnValue
    public final List<InetSocketAddress> getUDPSocketAddresses()
    {
        return _remoteProxy.getUDPSocketAddresses();
    }

    /** {@inheritDoc}
     */
    @Override
    public final int hashCode()
    {
        return getRemoteProxyName().hashCode();
    }

    /**
     * Called when a frame is received.
     *
     * @param frame The frame.
     *
     * @throws IOException On I/O exception.
     */
    public void onFrameReceived(@Nonnull final Frame frame)
        throws IOException
    {
        if (getConnectionManager().onFrameReceived(frame, this)) {
            final Frame.Header frameHeader = frame.getHeader();
            final Association association = getAssociation(
                frameHeader.getDestination(),
                frameHeader.getSource());
            final DataLinkLayer dataLinkLayer = association.getDataLinkLayer();

            dataLinkLayer.accept(frame);
        } else if (getThisLogger().isTraceEnabled()) {
            final Frame.Header frameHeader = frame.getHeader();

            getThisLogger()
                .trace(
                    DNP3Messages.IGNORED_FRAME,
                    frameHeader.getFunctionCode(),
                    _remoteProxy,
                    Integer.toHexString(frameHeader.getSource() & 0xFFFF),
                    Integer.toHexString(frameHeader.getDestination() & 0xFFFF));
        }
    }

    private final Map<Integer, Association> _associations =
        new ConcurrentHashMap<>();
    private final DNP3Proxy _remoteProxy;
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
