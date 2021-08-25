/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Connection.java 4086 2019-06-16 16:36:41Z SFB $
 */

package org.rvpf.pap.modbus.transport;

import java.io.IOException;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.PAPConnection;
import org.rvpf.pap.PAPConnectionListener;
import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.pap.modbus.ModbusProxy;

/**
 * Connection.
 */
public abstract class Connection
    extends PAPConnection.Abstract
{
    /**
     * Constructs an instance.
     *
     * @param transport The transport object.
     * @param remoteProxy The remote proxy.
     * @param listener An optional listener.
     */
    Connection(
            @Nonnull final Transport transport,
            @Nonnull final ModbusProxy remoteProxy,
            @Nonnull final Optional<PAPConnectionListener> listener)
    {
        _transport = Require.notNull(transport);
        _remoteProxy = Require.notNull(remoteProxy);
        _listener = Require.notNull(listener);
    }

    /**
     * Gets the remote address.
     *
     * @return The remote address.
     */
    @Nonnull
    @CheckReturnValue
    public final String getRemoteAddress()
    {
        return _transport.getRemoteAddress();
    }

    /**
     * Gets the remote name.
     *
     * @return The optional remote name.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<String> getRemoteName()
    {
        return getRemoteProxy().getOrigin().getName();
    }

    /**
     * Gets the remote proxy.
     *
     * @return The remote proxy.
     */
    @Nonnull
    @CheckReturnValue
    public ModbusProxy getRemoteProxy()
    {
        return _remoteProxy;
    }

    /**
     * Starts.
     */
    public abstract void start();

    /**
     * Stops.
     */
    public void stop()
    {
        _remoteProxy.forgetConnection();
    }

    /** {@inheritDoc}
     */
    @Override
    protected final void doClose()
        throws IOException
    {
        getThisLogger()
            .debug(
                ModbusMessages.CLOSING_CONNECTION,
                _transport.getRemoteAddress());

        _transport.close();
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getName()
    {
        return getRemoteAddress();
    }

    /**
     * Gets the listener.
     *
     * @return The optional listener.
     */
    @Nonnull
    @CheckReturnValue
    Optional<PAPConnectionListener> getListener()
    {
        return _listener;
    }

    /**
     * Gets the transport.
     *
     * @return The transport.
     */
    @Nonnull
    @CheckReturnValue
    Transport getTransport()
    {
        return _transport;
    }

    private final Optional<PAPConnectionListener> _listener;
    private final ModbusProxy _remoteProxy;
    private final Transport _transport;
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
