/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: JEWrapperImpl.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.ext.bdb;

import java.io.File;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.store.server.StoreCursor;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.the.bdb.JEWrapper;
import org.rvpf.store.server.the.bdb.JEWrapperException;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentStats;
import com.sleepycat.je.JEVersion;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;
import com.sleepycat.je.Transaction;

/**
 * JE wrapper implementation.
 */
public final class JEWrapperImpl
    implements JEWrapper
{
    /** {@inheritDoc}
     */
    @Override
    public void abort(final Object transaction)
        throws JEWrapperException
    {
        try {
            ((Transaction) transaction).abort();
        } catch (final DatabaseException exception) {
            throw new JEWrapperException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Object beginTransaction()
        throws JEWrapperException
    {
        try {
            return _environment.beginTransaction(null, null);
        } catch (final DatabaseException exception) {
            throw new JEWrapperException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void close()
        throws JEWrapperException
    {
        try {
            if (_versionDatabase != null) {
                _versionDatabase.close();
                _versionDatabase = null;
            }

            if (_database != null) {
                _database.close();
                _database = null;
            }
        } catch (final DatabaseException exception) {
            throw new JEWrapperException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void closeCursor(final Object wrapperCursor)
    {
        ((WrapperCursor) wrapperCursor).close();
    }

    /** {@inheritDoc}
     */
    @Override
    public void commit(final Object transaction)
        throws JEWrapperException
    {
        try {
            ((Transaction) transaction).commit();
        } catch (final DatabaseException exception) {
            throw new JEWrapperException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public long countPointValues(final Object wrapperCursor)
    {
        return ((WrapperCursor) wrapperCursor).count();
    }

    /** {@inheritDoc}
     */
    @Override
    public int delete(
            final Object transaction,
            final VersionedValue pointValue)
        throws JEWrapperException
    {
        final OperationStatus status;

        try {
            status = _database
                .delete(
                    (Transaction) transaction,
                    _converter.getKey(pointValue));
        } catch (final DatabaseException exception) {
            throw new JEWrapperException(exception);
        }

        return (status == OperationStatus.SUCCESS)? 1: 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public Object getCursor(final StoreCursor storeCursor)
    {
        final WrapperCursor wrapperCursor;

        if (_snapshot) {
            wrapperCursor = new WrapperCursor.PointSnapshot(
                storeCursor,
                _database,
                _converter);
        } else if (storeCursor.isPull()) {
            wrapperCursor = new WrapperCursor.Version(
                storeCursor,
                _versionDatabase,
                _converter);
        } else {
            wrapperCursor = new WrapperCursor.PointStamp(
                storeCursor,
                _database,
                _converter);
        }

        return wrapperCursor;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<VersionedValue> nextPointValue(final Object wrapperCursor)
    {
        return ((WrapperCursor) wrapperCursor).next();
    }

    /** {@inheritDoc}
     */
    @Override
    public void open()
        throws JEWrapperException
    {
        final DatabaseConfig databaseConfig = new DatabaseConfig();

        databaseConfig.setTransactional(true);

        try {
            try {
                _database = _environment
                    .openDatabase(null, _databaseName, databaseConfig);
            } catch (final DatabaseNotFoundException exception) {
                databaseConfig.setAllowCreate(true);
                _database = _environment
                    .openDatabase(null, _databaseName, databaseConfig);
                _LOGGER.info(StoreMessages.STORAGE_CREATED, _databaseName);
            }

            if (!_snapshot && !_pullDisabled) {
                _versionDatabase = _secondaryDatabase(
                    VERSION_DATABASE_NAME,
                    _converter.getVersionKeyCreator());
            }
        } catch (final DatabaseException exception) {
            throw new JEWrapperException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void put(
            final Object transaction,
            final VersionedValue pointValue)
        throws JEWrapperException
    {
        final OperationStatus status;

        try {
            status = _database
                .put(
                    (Transaction) transaction,
                    _converter.getKey(pointValue),
                    _converter.getData(pointValue));
        } catch (final DatabaseException exception) {
            throw new JEWrapperException(exception);
        }

        Require.success(status == OperationStatus.SUCCESS);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final File home,
            final boolean snapshot,
            final boolean pullDisabled)
        throws JEWrapperException
    {
        _LOGGER
            .info(
                BDBMessages.BDB_JE_VERSION,
                JEVersion.CURRENT_VERSION.getVersionString());

        _snapshot = snapshot;
        _databaseName = _snapshot
                ? SNAPSHOT_DATABASE_NAME: ARCHIVE_DATABASE_NAME;
        _pullDisabled = pullDisabled;
        _converter = new Converter(_snapshot);

        final EnvironmentConfig environmentConfig = new EnvironmentConfig();

        environmentConfig.setAllowCreate(true).setTransactional(true);

        try {
            _environment = new Environment(home, environmentConfig);
        } catch (final DatabaseException exception) {
            throw new JEWrapperException(exception);
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
        throws JEWrapperException
    {
        close();

        try {
            if (_environment != null) {
                _environment.cleanLog();

                if (_LOGGER.isTraceEnabled()) {
                    final EnvironmentStats stats = _environment.getStats(null);

                    _LOGGER.trace(BDBMessages.BDB_JE_STATS, stats);
                }

                _environment.close();
                _environment = null;
            }
        } catch (final DatabaseException exception) {
            throw new JEWrapperException(exception);
        }
    }

    private SecondaryDatabase _secondaryDatabase(
            @Nonnull final String name,
            @Nonnull final SecondaryKeyCreator keyCreator)
        throws DatabaseException
    {
        final SecondaryConfig secondaryConfig = new SecondaryConfig();

        secondaryConfig.setAllowCreate(true).setTransactional(true);
        secondaryConfig.setKeyCreator(keyCreator);

        return _environment
            .openSecondaryDatabase(null, name, _database, secondaryConfig);
    }

    /** The primary archive database name. */
    public static final String ARCHIVE_DATABASE_NAME = "Archive";

    /** The primary snapshot database name. */
    public static final String SNAPSHOT_DATABASE_NAME = "Snapshot";

    /** The version database name. */
    public static final String VERSION_DATABASE_NAME = "VersionArchive";
    private static final Logger _LOGGER = Logger
        .getInstance(JEWrapperImpl.class);

    private Converter _converter;
    private volatile Database _database;
    private String _databaseName;
    private Environment _environment;
    private boolean _pullDisabled;
    private boolean _snapshot;
    private volatile SecondaryDatabase _versionDatabase;
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
