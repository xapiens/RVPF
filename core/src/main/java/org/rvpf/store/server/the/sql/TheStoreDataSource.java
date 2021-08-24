/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TheStoreDataSource.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the.sql;

import java.io.File;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.ClassDef;
import org.rvpf.store.server.sql.StoreConnection;
import org.rvpf.store.server.sql.StoreDataSource;
import org.rvpf.store.server.the.sql.dialect.DialectSupport;

/**
 * TheStore data source.
 */
public final class TheStoreDataSource
    extends StoreDataSource
{
    /**
     * Constructs an instance.
     *
     * @param support The dialect support.
     */
    public TheStoreDataSource(@Nonnull final DialectSupport support)
    {
        _support = support;
    }

    /** {@inheritDoc}
     */
    @Override
    public TheStoreConnection getConnection()
        throws SQLException
    {
        return (TheStoreConnection) super.getConnection();
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getDefaultConnectionOptions()
    {
        return _support.getDefaultConnectionOptions();
    }

    /** {@inheritDoc}
     */
    @Override
    protected Optional<String> getDefaultConnectionPassword()
    {
        return _support.getDefaultConnectionPassword();
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean getDefaultConnectionShared()
    {
        return _support.getDefaultConnectionShared();
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getDefaultConnectionURL(
            final File storeDataDir,
            final String storeEntityName)
    {
        return _support.getDefaultConnectionURL(storeDataDir, storeEntityName);
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getDefaultConnectionUser()
    {
        return _support.getDefaultConnectionUser();
    }

    /** {@inheritDoc}
     */
    @Override
    protected TheStoreConnection newStoreConnection(
            final Connection connection,
            final boolean shared)
    {
        return new TheStoreConnection(connection, shared, _support);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void onFirstConnection(final StoreConnection storeConnection)
    {
        _support.onFirstConnection((TheStoreConnection) storeConnection);
    }

    /** {@inheritDoc}
     */
    @Override
    protected Class<? extends Driver> registerDriver(
            final ClassDef driverClassDef)
    {
        return _support.registerDriver(driverClassDef);
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
