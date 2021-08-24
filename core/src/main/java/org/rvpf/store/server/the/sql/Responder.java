/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Responder.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the.sql;

import java.io.Serializable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.Points;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.store.server.StoreCursor;
import org.rvpf.store.server.StoreMessages;

/**
 * Responder.
 */
public final class Responder
    implements StoreCursor.Responder
{
    /**
     * Constructs an instance.
     *
     * @param connection The connection to use for database services.
     * @param points The point from which to get the point definitions.
     * @param backEndLimit A limit on the number of values allowed.
     * @param statementsLimit A limit on the number of cached statements.
     */
    public Responder(
            @Nonnull final TheStoreConnection connection,
            @Nonnull final Points points,
            final int backEndLimit,
            final int statementsLimit)
    {
        _connection = connection;
        _points = points;
        _backEndLimit = backEndLimit;
        _statementsLimit = statementsLimit;
        _statementsPool = new LinkedHashMap<>(
            KeyedValues.hashCapacity(_statementsLimit),
            KeyedValues.HASH_LOAD_FACTOR,
            true);
    }

    /**
     * Closes this.
     */
    public void close()
    {
        if (_closed.compareAndSet(false, true)) {
            reset(Optional.empty());

            try {
                for (final PreparedStatement statement:
                        _statementsPool.values()) {
                    statement.close();
                }

                _statementsPool.clear();
            } catch (final SQLException exception) {
                throw new RuntimeException(exception);
            }

            _connection.close();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public long count()
    {
        try {
            return _resultSet.getLong(1);
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }
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
        return _connection;
    }

    /** {@inheritDoc}
     */
    @Override
    public int limit()
    {
        return _backEndLimit;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<VersionedValue> next()
    {
        final UUID uuid;
        final DateTime stamp;
        final DateTime version;
        final Serializable state;
        final Serializable value;

        try {
            if (!_resultSet.next()) {
                return Optional.empty();
            }
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }

        try {
            int index = 0;
            byte[] bytes;

            uuid = UUID.fromBytes(_resultSet.getBytes(++index));
            stamp = DateTime.fromRaw(_resultSet.getLong(++index));
            version = DateTime.fromRaw(_resultSet.getLong(++index));
            bytes = _resultSet.getBytes(++index);
            state = (bytes == null)? null: Externalizer.internalize(bytes);
            bytes = _resultSet.getBytes(++index);
            value = (bytes == null)? null: Externalizer.internalize(bytes);
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }

        VersionedValue pointValue = VersionedValue.Factory
            .restore(uuid, Optional.of(stamp), version, state, value);

        if (_point != null) {
            pointValue = (VersionedValue) pointValue.restore(_point);
        } else {
            pointValue = (VersionedValue) pointValue.restore(_points);
        }

        if (_pull) {
            if ((_version != null) && version.isNotAfter(_version)) {
                throw new RuntimeException(
                    "Non increasing version value at: " + pointValue);
            }

            _version = version;
        }

        return Optional.of(pointValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public void reset(final Optional<StoreCursor> storeCursor)
    {
        if (_resultSet != null) {
            try {
                _resultSet.close();
            } catch (final SQLException exception) {
                throw new RuntimeException(exception);
            }

            _resultSet = null;
        }

        _point = null;

        if (storeCursor.isPresent()) {
            final Integer type = Integer
                .valueOf(storeCursor.get().refreshType());
            PreparedStatement statement = _statementsPool.get(type);

            if (statement == null) {
                while (_statementsPool.size() >= _statementsLimit) {
                    final Iterator<PreparedStatement> iterator = _statementsPool
                        .values()
                        .iterator();

                    statement = iterator.next();

                    try {
                        statement.close();
                    } catch (final SQLException exception) {
                        throw new RuntimeException(exception);
                    }

                    iterator.remove();
                }

                statement = _connection
                    .prepareStatement(
                        _connection
                            .getSupport()
                            .getSelectSQL(storeCursor.get()));
                _statementsPool.put(type, statement);
            }

            _LOGGER
                .trace(
                    StoreMessages.USING_SQL_FOR,
                    Integer.toHexString(type.intValue()));

            _point = storeCursor.get().getPoint().orElse(null);

            try {
                _connection
                    .getSupport()
                    .setSelectStatement(statement, storeCursor.get());

                _resultSet = statement.executeQuery();

                statement.clearParameters();

                if (storeCursor.get().isCount()) {
                    _resultSet.next();
                }
            } catch (final SQLException exception) {
                throw new RuntimeException(exception);
            }

            _pull = storeCursor.get().isPull();
            _version = null;
        }
    }

    /**
     * Gets the last use time.
     *
     * @return The optional last use time.
     */
    @Nonnull
    @CheckReturnValue
    Optional<DateTime> getLastUse()
    {
        return Optional.ofNullable(_lastUse);
    }

    /**
     * Sets the last use time.
     *
     * @param lastUse The last use time.
     */
    void setLastUse(@Nonnull final DateTime lastUse)
    {
        _lastUse = lastUse;
    }

    private static final Logger _LOGGER = Logger.getInstance(Responder.class);

    private final int _backEndLimit;
    private final AtomicBoolean _closed = new AtomicBoolean();
    private final TheStoreConnection _connection;
    private DateTime _lastUse;
    private Point _point;
    private final Points _points;
    private boolean _pull;
    private ResultSet _resultSet;
    private final int _statementsLimit;
    private final Map<Integer, PreparedStatement> _statementsPool;
    private DateTime _version;
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
