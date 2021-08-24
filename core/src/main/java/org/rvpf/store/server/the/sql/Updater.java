/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Updater.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.TimeInterval;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.store.server.StoreMessages;

/**
 * Updater.
 */
public final class Updater
{
    /**
     * Constructs an instance.
     *
     * @param connection The connection to use for database services.
     * @param dropDeleted The drop deleted indicator.
     */
    public Updater(
            @Nonnull final TheStoreConnection connection,
            final boolean dropDeleted)
    {
        _connection = connection;
        _dropDeleted = dropDeleted;
    }

    /**
     * Closes.
     */
    public void close()
    {
        if (_connection != null) {
            try {
                if (_deleteStatement != null) {
                    _deleteStatement.close();
                    _deleteStatement = null;
                }

                if (_insertStatement != null) {
                    _insertStatement.close();
                    _insertStatement = null;
                }

                if (_updateStatement != null) {
                    _updateStatement.close();
                    _updateStatement = null;
                }
            } catch (final SQLException exception) {
                throw new RuntimeException(exception);
            }

            _connection.close();
            _connection = null;
        }
    }

    /**
     * Commits.
     */
    public void commit()
    {
        _connection.commit();
    }

    /**
     * Deletes a point value.
     *
     * @param versionedValue The point value to delete.
     *
     * @return True if there was a value to delete.
     */
    @CheckReturnValue
    public int delete(@Nonnull final VersionedValue versionedValue)
    {
        final VersionedValue.Deleted deletedValue =
            (VersionedValue.Deleted) ((versionedValue
                instanceof VersionedValue.Deleted)? versionedValue
                    : new VersionedValue.Deleted(
                            versionedValue));
        final UUID pointUUID = deletedValue.getPointUUID();
        final long stamp = deletedValue.getStamp().toRaw();
        final int deleted = _delete(
            pointUUID,
            stamp,
            stamp,
            versionedValue instanceof VersionedValue.Purged);

        if (deleted > 0) {
            _LOGGER
                .trace(
                    StoreMessages.ROW_DELETED,
                    versionedValue.isDeleted()? new VersionedValue(
                        versionedValue): versionedValue);

            if (!(_dropDeleted
                    || (versionedValue instanceof VersionedValue.Purged))) {
                _update(deletedValue);
            }
        }

        return deleted;
    }

    /**
     * Gets the store connection.
     *
     * @return The store connection.
     */
    @Nonnull
    @CheckReturnValue
    public TheStoreConnection getConnection()
    {
        return Require.notNull(_connection);
    }

    /**
     * Purges values of a point.
     *
     * @param pointUUID The UUID of the point.
     * @param timeInterval The purge interval.
     *
     * @return The number of values purged.
     */
    @CheckReturnValue
    public int purge(
            @Nonnull final UUID pointUUID,
            @Nonnull final TimeInterval timeInterval)
    {
        final int purged = _delete(
            pointUUID,
            timeInterval.getNotBefore().get().toRaw(),
            timeInterval.getNotAfter().get().toRaw(),
            true);

        if (purged > 0) {
            _LOGGER.trace(StoreMessages.ROWS_DELETED, String.valueOf(purged));
        }

        return purged;
    }

    /**
     * Rolls back.
     */
    public void rollback()
    {
        _connection.rollback();
    }

    /**
     * Updates a value.
     *
     * @param versionedValue The new Point value.
     */
    public void update(@Nonnull final VersionedValue versionedValue)
    {
        _update(versionedValue);
    }

    private int _delete(
            final UUID pointUUID,
            final long startStamp,
            final long finishStamp,
            final boolean dropDeleted)
    {
        final PreparedStatement deleteStatement = _getDeleteStatement(
            startStamp,
            finishStamp);
        int deleted;

        try {
            _connection
                .getSupport()
                .setDeleteStatement(
                    deleteStatement,
                    pointUUID.toBytes(),
                    startStamp,
                    finishStamp);
            deleteStatement.execute();
            deleted = deleteStatement.getUpdateCount();

            if (dropDeleted && !pointUUID.isDeleted()) {
                _connection
                    .getSupport()
                    .setDeleteStatement(
                        deleteStatement,
                        pointUUID.deleted().toBytes(),
                        startStamp,
                        finishStamp);
                deleteStatement.execute();
                deleted += deleteStatement.getUpdateCount();
            }
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }

        return deleted;
    }

    private PreparedStatement _getDeleteStatement(
            final long startStamp,
            final long finishStamp)
    {
        if (startStamp < finishStamp) {
            if (_purgeStatement == null) {
                _purgeStatement = _connection
                    .prepareStatement(
                        _connection.getSupport().getDeleteSQL(true));
            }

            return _purgeStatement;
        }

        if (_deleteStatement == null) {
            _deleteStatement = _connection
                .prepareStatement(_connection.getSupport().getDeleteSQL(false));
        }

        return _deleteStatement;

    }

    private PreparedStatement _getInsertStatement()
    {
        if (_insertStatement == null) {
            _insertStatement = _connection
                .prepareStatement(_connection.getSupport().getInsertSQL());
        }

        return _insertStatement;
    }

    private PreparedStatement _getUpdateStatement()
    {
        if (_updateStatement == null) {
            _updateStatement = _connection
                .prepareStatement(_connection.getSupport().getUpdateSQL());
        }

        return _updateStatement;
    }

    private void _update(final VersionedValue versionedValue)
    {
        final UUID uuid = versionedValue.getPointUUID();
        final byte[] point = versionedValue
            .isDeleted()? uuid.deleted().toBytes(): uuid.toBytes();
        final long stamp = versionedValue.getStamp().toRaw();
        final long version = versionedValue.getVersion().toRaw();
        final byte[] state = Externalizer
            .externalize(versionedValue.getState());
        final byte[] value = Externalizer
            .externalize(versionedValue.getValue());
        final boolean inserted;
        PreparedStatement statement;

        try {
            statement = _getUpdateStatement();
            _connection
                .getSupport()
                .setUpdateStatement(
                    statement,
                    point,
                    stamp,
                    version,
                    state,
                    value);
            statement.execute();
            inserted = statement.getUpdateCount() == 0;

            if (inserted) {
                statement = _getInsertStatement();
                _connection
                    .getSupport()
                    .setInsertStatement(
                        statement,
                        point,
                        stamp,
                        version,
                        state,
                        value);
                statement.execute();
            }
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }

        _LOGGER
            .trace(
                (inserted
                 ? StoreMessages.ROW_INSERTED: StoreMessages.ROW_REPLACED),
                versionedValue);
    }

    private static final Logger _LOGGER = Logger.getInstance(Updater.class);

    private TheStoreConnection _connection;
    private PreparedStatement _deleteStatement;
    private final boolean _dropDeleted;
    private PreparedStatement _insertStatement;
    private PreparedStatement _purgeStatement;
    private PreparedStatement _updateStatement;
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
