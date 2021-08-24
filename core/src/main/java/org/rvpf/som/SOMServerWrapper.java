/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMServerWrapper.java 4021 2019-05-24 13:01:15Z SFB $
 */

package org.rvpf.som;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * SOM server wrapper.
 */
public abstract class SOMServerWrapper
{
    /**
     * Closes the server.
     */
    public final void closeServer()
    {
        getServer().close();
    }

    /**
     * Gets the server name.
     *
     * @return The server name.
     */
    @Nonnull
    @CheckReturnValue
    public final String getServerName()
    {
        return getServer().getName();
    }

    /**
     * Asks if the server is closed.
     *
     * @return True ifthe server is closed.
     */
    @CheckReturnValue
    public final boolean isServerClosed()
    {
        return getServer().isClosed();
    }

    /**
     * Tears down the server.
     */
    public final void tearDownServer()
    {
        getServer().tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getServer().getURI().toString();
    }

    /**
     * Gets the server.
     *
     * @return The server.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract SOMServerImpl getServer();
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
