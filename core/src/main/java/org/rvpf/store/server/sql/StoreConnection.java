/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreConnection.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.store.server.sql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import org.rvpf.base.tool.Require;

/**
 * Store connection.
 */
public class StoreConnection
{
    /**
     * Creates an instance.
     *
     * @param connection The actual Connection.
     * @param shared True if this connection is shared.
     */
    public StoreConnection(
            @Nonnull final Connection connection,
            final boolean shared)
    {
        _connection = connection;
        _shared = shared;
        _databaseMetaData = null;
    }

    /**
     * Closes the actual Connection.
     */
    public final void close()
    {
        if (!_shared) {
            tearDown();
        }
    }

    /**
     * Commits.
     */
    public final void commit()
    {
        try {
            _connection.commit();
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Creates a statement.
     *
     * @return A new Statement.
     */
    @Nonnull
    @CheckReturnValue
    public final Statement createStatement()
    {
        try {
            return _connection.createStatement();
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Gets the database metadata.
     *
     * @return The database metadata.
     */
    @Nonnull
    @CheckReturnValue
    public final DatabaseMetaData getDatabaseMetaData()
    {
        if (_databaseMetaData == null) {
            try {
                _databaseMetaData = Require.notNull(_connection.getMetaData());
            } catch (final SQLException exception) {
                throw new RuntimeException(exception);
            }
        }

        return _databaseMetaData;
    }

    /**
     * Asks if the connected database has the specified table.
     *
     * @param catalogName The catalog name.
     * @param schemaName The schema name.
     * @param tableName The table name.
     *
     * @return True if it has that table.
     */
    @CheckReturnValue
    public final boolean hasTable(
            @Nonnull final Optional<String> catalogName,
            @Nonnull final String schemaName,
            @Nonnull String tableName)
    {
        final boolean hasTable;

        try {
            final DatabaseMetaData metaData = getDatabaseMetaData();
            final ResultSet resultSet;

            if (metaData.storesUpperCaseIdentifiers()) {
                tableName = tableName.toUpperCase(Locale.ROOT);
            } else if (metaData.storesLowerCaseIdentifiers()) {
                tableName = tableName.toLowerCase(Locale.ROOT);
            }

            resultSet = metaData
                .getTables(
                    catalogName.orElse(null),
                    schemaName,
                    tableName,
                    null);
            hasTable = resultSet.next();
            resultSet.close();
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }

        return hasTable;
    }

    /**
     * Locks.
     */
    @SuppressWarnings({"LockAcquiredButNotSafelyReleased"})
    public void lock()
    {
        _lock.lock();
    }

    /**
     * Prepares a SQL statement.
     *
     * @param sql The SQL for the statement.
     *
     * @return A new prepared statement.
     */
    @Nonnull
    @CheckReturnValue
    public final PreparedStatement prepareStatement(
            @Nonnull final CharSequence sql)
    {
        try {
            return _connection.prepareStatement(sql.toString());
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Rolls back.
     */
    public final void rollback()
    {
        try {
            _connection.rollback();
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Tears down the connection.
     */
    public void tearDown()
    {
        if (_connection != null) {
            try {
                if (!_connection.isClosed()) {
                    _connection.close();
                }
            } catch (final SQLException exception) {
                throw new RuntimeException(exception);
            }

            _connection = null;
        }
    }

    /**
     * Unlocks.
     */
    public void unlock()
    {
        _lock.unlock();
    }

    private Connection _connection;
    private DatabaseMetaData _databaseMetaData;
    private final Lock _lock = new ReentrantLock();
    private final boolean _shared;
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
