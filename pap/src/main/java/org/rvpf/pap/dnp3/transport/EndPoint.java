/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: EndPoint.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;

/**
 * End point.
 */
public abstract class EndPoint
{
    /**
     * Constructs an instance.
     *
     * @param connectionManager The connection manager.
     */
    protected EndPoint(@Nonnull final ConnectionManager connectionManager)
    {
        _connectionManager = connectionManager;
    }

    /**
     * Gets the connection manager.
     *
     * @return The connection manager.
     */
    @Nonnull
    @CheckReturnValue
    public ConnectionManager getConnectionManager()
    {
        final ConnectionManager connectionManager = _connectionManager;

        Require.notNull(connectionManager);

        return connectionManager;
    }

    /**
     * Asks if this end point is on a master.
     *
     * @return True if this end point is on a master.
     */
    @CheckReturnValue
    public boolean isOnMaster()
    {
        return getConnectionManager().isOnMaster();
    }

    /**
     * Asks if this end point is on an outstation.
     *
     * @return True if this end point is on an outstation.
     */
    @CheckReturnValue
    public boolean isOnOutstation()
    {
        return getConnectionManager().isOnOutstation();
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
        Logger logger = _logger;

        if (logger == null) {
            logger = Logger.getInstance(getClass());
            _logger = logger;
        }

        return logger;
    }

    /**
     * Closes.
     */
    void close()
    {
        _connectionManager = null;
    }

    private volatile ConnectionManager _connectionManager;
    private volatile Logger _logger;
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
