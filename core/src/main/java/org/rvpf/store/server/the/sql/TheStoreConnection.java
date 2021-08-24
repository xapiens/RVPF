/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TheStoreConnection.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the.sql;

import java.sql.Connection;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import org.rvpf.base.tool.Require;
import org.rvpf.store.server.sql.StoreConnection;
import org.rvpf.store.server.the.sql.dialect.DialectSupport;

/**
 * Store connection.
 */
public final class TheStoreConnection
    extends StoreConnection
{
    /**
     * Creates an instance.
     *
     * @param connection The actual Connection.
     * @param shared True if this connection is shared.
     * @param support The DialectSupport instance.
     */
    TheStoreConnection(
            @Nonnull final Connection connection,
            final boolean shared,
            @Nonnull final DialectSupport support)
    {
        super(connection, shared);

        _support = Require.notNull(support);
    }

    /**
     * Gets the dialect support.
     *
     * @return The dialect support.
     */
    @Nonnull
    @CheckReturnValue
    public DialectSupport getSupport()
    {
        return _support;
    }

    private final DialectSupport _support;
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
