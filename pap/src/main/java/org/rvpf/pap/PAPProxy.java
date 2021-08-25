/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPProxy.java 4086 2019-06-16 16:36:41Z SFB $
 */

package org.rvpf.pap;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;

/**
 * PAP proxy.
 */
public abstract class PAPProxy
{
    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    protected PAPProxy(@Nonnull final PAPProxy other)
    {
        _context = other._context;
        _origin = other._origin;
        _logger = other._logger;
        _connectionStatePoint = other._connectionStatePoint;
    }

    /**
     * Constructs an instance.
     *
     * @param context The context.
     * @param origin The origin representing the remote.
     */
    protected PAPProxy(
            @Nonnull final PAPContext context,
            @Nonnull final Origin origin)
    {
        _context = Require.notNull(context);
        _origin = Require.notNull(origin);
        _logger = Logger.getInstance(getClass());
    }

    /**
     * Creates a copy of this.
     *
     * @return The copy.
     */
    @Nonnull
    @CheckReturnValue
    public abstract PAPProxy copy();

    /**
     * Disconnects.
     */
    public void disconnect()
    {
        _connectionListener = null;
    }

    /**
     * Gets the connection listener.
     *
     * @return The connection istener.
     */
    @Nonnull
    @CheckReturnValue
    public PAPConnectionListener getConnectionListener()
    {
        return Require.notNull(_connectionListener);
    }

    /**
     * Gets the connection state point.
     *
     * @return The connection state point (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<Point> getConnectionStatePoint()
    {
        return Optional.ofNullable(_connectionStatePoint);
    }

    /**
     * Gets the context.
     *
     * @return The context.
     */
    @Nonnull
    @CheckReturnValue
    public PAPContext getContext()
    {
        return _context;
    }

    /**
     * Gets the origin name.
     *
     * @return The optional origin name.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<String> getName()
    {
        return _origin.getName();
    }

    /**
     * Gets the origin representing the remote.
     *
     * @return The origin representing the remote.
     */
    @Nonnull
    @CheckReturnValue
    public final Origin getOrigin()
    {
        return _origin;
    }

    /**
     * Sets the connection listener.
     *
     * @param connectionListener The connection listener.
     */
    public final void setConnectionListener(
            @Nonnull final PAPConnectionListener connectionListener)
    {
        _connectionListener = Require.notNull(connectionListener);
    }

    /**
     * Sets the connection state point.
     *
     * @param connectionStatePoint The connection state point.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public final boolean setConnectionStatePoint(
            @Nonnull final Point connectionStatePoint)
    {
        if (_connectionStatePoint == null) {
            _connectionStatePoint = connectionStatePoint;
        } else {
            getThisLogger()
                .warn(PAPMessages.MULTIPLE_CONNECTION_STATE, getOrigin());

            return false;
        }

        return true;
    }

    /**
     * Asks if this proxy can be configured with a wildcard address.
     *
     * @return True if a wildcard address is supported.
     */
    @CheckReturnValue
    public boolean supportsWildcardAddress()
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "Node " + getName().orElse(null);
    }

    /**
     * Gets the logger for this instance.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        return _logger;
    }

    private PAPConnectionListener _connectionListener;
    private Point _connectionStatePoint;
    private final PAPContext _context;
    private final Logger _logger;
    private final Origin _origin;

    /**
     * Responder.
     */
    public interface Responder
    {
        /**
         * Selects point values.
         *
         * @param points The requested points.
         *
         * @return The point values.
         *
         * @throws InterruptedException When interrupted.
         * @throws ServiceNotAvailableException When the service is not
         *                                      available.
         */
        @Nonnull
        @CheckReturnValue
        PointValue[] select(
                @Nonnull Point[] points)
            throws InterruptedException, ServiceNotAvailableException;
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
