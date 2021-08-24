/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PooledDataSource.java 4019 2019-05-23 14:14:01Z SFB $
 */

package org.rvpf.store.server.sql;

import java.sql.SQLException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.sql.DataSource;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;

/**
 * Pooled data source.
 */
public interface PooledDataSource
    extends DataSource
{
    /**
     * Adds a custom property.
     *
     * @param name The name of the property.
     * @param value The value of the property.
     */
    void addConnectionProperty(@Nonnull String name, @Nonnull String value);

    /**
     * Close and release all connections that are currently stored in the
     * connection pool associated with our data source.
     *
     * @throws SQLException If a database error occurs
     */
    void close()
        throws SQLException;

    /**
     * Sets the default auto-commit state.
     *
     * @param defaultAutoCommit The default auto-commit state.
     */
    void setDefaultAutoCommit(@Nonnull final Boolean defaultAutoCommit);

    /**
     * Sets the driver class name.
     *
     * @param driverClassName The driver class name.
     */
    void setDriverClassName(@Nonnull String driverClassName);

    /**
     * Sets the password.
     *
     * @param password The password.
     */
    void setPassword(@Nullable String password);

    /**
     * Sets the data source URL.
     *
     * @param url The data source URL.
     */
    void setUrl(@Nonnull String url);

    /**
     * Sets the username.
     *
     * @param username The username.
     */
    void setUsername(@Nonnull String username);

    /** Default pooled data source implementation. */
    ClassDef DEFAULT_IMPL = new ClassDefImpl(
        "org.rvpf.ext.DBCPPooledDataSource");

    /** Pooled data source implementation property. */
    String IMPL_PROPERTY = "pool";
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
