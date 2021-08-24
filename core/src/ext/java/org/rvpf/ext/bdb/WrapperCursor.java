/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: WrapperCursor.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.ext.bdb;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.store.server.StoreCursor;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryCursor;
import com.sleepycat.je.SecondaryDatabase;

/**
 * Wrapper cursor.
 */
abstract class WrapperCursor
{
    /**
     * Constructs an instance.
     *
     * @param storeCursor A store cursor.
     * @param converter The converter.
     */
    WrapperCursor(
            @Nonnull final StoreCursor storeCursor,
            @Nonnull final Converter converter)
    {
        _pointUUID = storeCursor.getPointUUID().orElse(null);

        final Optional<DateTime> after = storeCursor.getAfter();
        final Optional<DateTime> before = storeCursor.getBefore();

        _after = after.isPresent()? after.get(): DateTime.BEGINNING_OF_TIME;
        _before = before.isPresent()? before.get(): DateTime.END_OF_TIME;

        _nullIgnored = storeCursor.isNullIgnored();

        _converter = converter;
    }

    /**
     * Gets the after stamp.
     *
     * @return The after stamp.
     */
    @Nonnull
    @CheckReturnValue
    final DateTime _getAfter()
    {
        return _after;
    }

    /**
     * Gets the before stamp.
     *
     * @return The before stamp.
     */
    @Nonnull
    @CheckReturnValue
    final DateTime _getBefore()
    {
        return _before;
    }

    /**
     * Gets the converter.
     *
     * @return The converter.
     */
    @Nonnull
    @CheckReturnValue
    final Converter _getConverter()
    {
        return _converter;
    }

    /**
     * Gets the point UUID.
     *
     * @return The optional point UUID.
     */
    @Nonnull
    @CheckReturnValue
    final Optional<UUID> _getPointUUID()
    {
        return Optional.ofNullable(_pointUUID);
    }

    /**
     * Gets the null ignored indicator.
     *
     * @return The null ignored indicator.
     */
    @CheckReturnValue
    final boolean _isNullIgnored()
    {
        return _nullIgnored;
    }

    /**
     * Closes.
     */
    abstract void close();

    /**
     * Counts values.
     *
     * @return The values count.
     */
    @CheckReturnValue
    abstract long count();

    /**
     * Returns the next value.
     *
     * @return The next value (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    abstract Optional<VersionedValue> next();

    private final DateTime _after;
    private final DateTime _before;
    private final Converter _converter;
    private final boolean _nullIgnored;
    private final UUID _pointUUID;

    /**
     * Point snapshot wrapper cursor.
     */
    static final class PointSnapshot
        extends WrapperCursor
    {
        /**
         * Constructs an instance.
         *
         * @param storeCursor A store cursor.
         * @param database The database.
         * @param converter The converter.
         */
        PointSnapshot(
                @Nonnull final StoreCursor storeCursor,
                @Nonnull final Database database,
                @Nonnull final Converter converter)
        {
            super(storeCursor, converter);

            _database = database;
        }

        /** {@inheritDoc}
         */
        @Override
        void close() {}

        /** {@inheritDoc}
         */
        @Override
        long count()
        {
            final DatabaseEntry data = new DatabaseEntry();
            final OperationStatus status;
            long count = 0;

            data.setPartial(_isNullIgnored());

            try {
                final DatabaseEntry key = _getConverter()
                    .newKey(_getPointUUID().get());

                status = _database.get(null, key, data, null);
            } catch (final DatabaseException exception) {
                throw new RuntimeException(exception);
            }

            if (status == OperationStatus.SUCCESS) {
                final DateTime stamp = _getConverter().getDataStamp(data);

                if (stamp.isBefore(_getBefore())
                        && stamp.isAfter(_getAfter())) {
                    if (!(_isNullIgnored()
                            && _getConverter().isValueNull(data))) {
                        ++count;
                    }
                }
            }

            return count;
        }

        /** {@inheritDoc}
         */
        @Override
        Optional<VersionedValue> next()
        {
            if (_done) {
                return Optional.empty();
            }

            final DatabaseEntry data = new DatabaseEntry();
            final DatabaseEntry key;
            final VersionedValue pointValue;
            final OperationStatus status;

            try {
                key = _getConverter().newKey(_getPointUUID().get());
                status = _database.get(null, key, data, null);
            } catch (final DatabaseException exception) {
                throw new RuntimeException(exception);
            }

            if (status == OperationStatus.SUCCESS) {
                final DateTime stamp = _getConverter().getDataStamp(data);

                if (stamp.isBefore(_getBefore())
                        && stamp.isAfter(_getAfter())) {
                    if (_isNullIgnored() && _getConverter().isValueNull(data)) {
                        pointValue = null;
                    } else {
                        pointValue = _getConverter().getPointValue(key, data);
                    }
                } else {
                    pointValue = null;
                }
            } else {
                pointValue = null;
            }

            _done = true;

            return Optional.ofNullable(pointValue);
        }

        private final Database _database;
        private boolean _done;
    }


    /**
     * PointStamp wrapper cursor.
     */
    static final class PointStamp
        extends WrapperCursor
    {
        /**
         * Constructs an instance.
         *
         * @param storeCursor A store cursor.
         * @param database The database.
         * @param converter The converter.
         */
        PointStamp(
                @Nonnull final StoreCursor storeCursor,
                @Nonnull final Database database,
                @Nonnull final Converter converter)
        {
            super(storeCursor, converter);

            _isInstant = storeCursor.isInstant();
            _isReverse = storeCursor.isReverse();
            _database = database;
        }

        /** {@inheritDoc}
         */
        @Override
        void close()
        {
            if (_cursor != null) {
                try {
                    _cursor.close();
                } catch (final DatabaseException exception) {
                    throw new RuntimeException(exception);
                }

                _cursor = null;
            }
        }

        /** {@inheritDoc}
         */
        @Override
        long count()
        {
            final DatabaseEntry data = new DatabaseEntry();
            long count = 0;
            DatabaseEntry key;
            OperationStatus status;

            data.setPartial(_isNullIgnored());

            for (;;) {
                try {
                    if (_cursor == null) {
                        if (_isInstant) {
                            key = _getConverter()
                                .newKey(
                                    _getPointUUID().get(),
                                    _getAfter().after());
                            status = _database.get(null, key, data, null);
                        } else {
                            _cursor = _database.openCursor(null, null);
                            key = _getConverter()
                                .newKey(
                                    _getPointUUID().get(),
                                    _getAfter().after());
                            status = _cursor.getSearchKeyRange(key, data, null);
                        }
                    } else {
                        key = new DatabaseEntry();
                        status = _cursor.getNext(key, data, null);
                    }
                } catch (final DatabaseException exception) {
                    throw new RuntimeException(exception);
                }

                if ((status == OperationStatus.SUCCESS)
                        && _getConverter().hasSameUUID(
                            key,
                            _getPointUUID().get())) {
                    if (_getConverter()
                        .getKeyStamp(key)
                        .isBefore(_getBefore())) {
                        if (!(_isNullIgnored()
                                && _getConverter().isValueNull(data))) {
                            ++count;
                        }

                        if (_isInstant) {
                            break;
                        }
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }

            return count;
        }

        /** {@inheritDoc}
         */
        @Override
        Optional<VersionedValue> next()
        {
            final DatabaseEntry data = new DatabaseEntry();
            final DatabaseEntry key;
            final VersionedValue pointValue;
            OperationStatus status;

            try {
                if (_cursor == null) {
                    if (_isInstant) {
                        key = _getConverter()
                            .newKey(_getPointUUID().get(), _getAfter().after());
                        status = _database.get(null, key, data, null);
                    } else {
                        _cursor = _database.openCursor(null, null);
                        key = _getConverter()
                            .newKey(
                                _getPointUUID().get(),
                                _isReverse? _getBefore(): _getAfter().after());
                        status = _cursor.getSearchKeyRange(key, data, null);

                        if (_isReverse) {
                            if (status == OperationStatus.SUCCESS) {
                                status = _cursor.getPrev(key, data, null);
                            } else {
                                status = _cursor.getLast(key, data, null);
                            }
                        }
                    }
                } else {
                    key = new DatabaseEntry();
                    status = _isReverse? _cursor
                        .getPrev(
                            key,
                            data,
                            null): _cursor.getNext(key, data, null);
                }
            } catch (final DatabaseException exception) {
                throw new RuntimeException(exception);
            }

            if ((status == OperationStatus.SUCCESS)
                    && _getConverter().hasSameUUID(
                        key,
                        _getPointUUID().get())) {
                final DateTime stamp = _getConverter().getKeyStamp(key);

                if (_isReverse? stamp
                    .isAfter(_getAfter()): stamp.isBefore(_getBefore())) {
                    pointValue = _getConverter().getPointValue(key, data);
                } else {
                    pointValue = null;
                }
            } else {
                pointValue = null;
            }

            return Optional.ofNullable(pointValue);
        }

        private Cursor _cursor;
        private final Database _database;
        private final boolean _isInstant;
        private final boolean _isReverse;
    }


    /**
     * Version wrapper cursor.
     */
    static final class Version
        extends WrapperCursor
    {
        /**
         * Constructs an instance.
         *
         * @param storeCursor A store cursor.
         * @param database The database.
         * @param converter The converter.
         */
        Version(
                @Nonnull final StoreCursor storeCursor,
                @Nonnull final SecondaryDatabase database,
                @Nonnull final Converter converter)
        {
            super(storeCursor, converter);

            _database = database;

            if (_getPointUUID().isPresent()) {
                try {
                    _cursor = _database.openCursor(null, null);

                    if (!storeCursor.isCount()) {
                        _pointValue = _firstPointValue(
                            _cursor,
                            _getPointUUID().get());

                        if (storeCursor.isIncludeDeleted()
                                && !_getPointUUID().get().isDeleted()) {
                            _deletedCursor = _database.openCursor(null, null);
                            _deletedValue = _firstPointValue(
                                _deletedCursor,
                                _getPointUUID().get().deleted());
                        }
                    }
                } catch (final DatabaseException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        void close()
        {
            try {
                if (_cursor != null) {
                    _cursor.close();
                    _cursor = null;
                }

                if (_deletedCursor != null) {
                    _deletedCursor.close();
                    _deletedCursor = null;
                }
            } catch (final DatabaseException exception) {
                throw new RuntimeException(exception);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        long count()
        {
            final DatabaseEntry data = new DatabaseEntry();
            final DatabaseEntry key = new DatabaseEntry();
            long count = 0;
            OperationStatus status;

            data.setPartial(_isNullIgnored());

            try {
                _cursor = _database.openCursor(null, null);
                status = _cursor
                    .getSearchKeyRange(
                        _getConverter().newKey(_getAfter().after()),
                        key,
                        data,
                        null);

                while ((status == OperationStatus.SUCCESS)
                        && _getConverter().getKeyStamp(
                            key).isBefore(_getBefore())) {
                    final boolean ignored = (_getPointUUID().isPresent())
                            && !Converter.getUUID(
                                key).equals(_getPointUUID().get());

                    if (!(ignored
                            || (_isNullIgnored() && _getConverter().isValueNull(
                                data)))) {
                        ++count;
                    }

                    status = _cursor
                        .getNext(new DatabaseEntry(), key, data, null);
                }
            } catch (final DatabaseException exception) {
                throw new RuntimeException(exception);
            }

            return count;
        }

        /** {@inheritDoc}
         */
        @Override
        Optional<VersionedValue> next()
        {
            final VersionedValue pointValue;

            if (_getPointUUID().isPresent()) {
                if (_pointValue != null) {
                    if (_deletedValue != null) {
                        if (_pointValue
                            .getVersion()
                            .isBefore(_deletedValue.getVersion())) {
                            pointValue = _pointValue;
                            _pointValue = _nextPointValue(
                                _cursor,
                                _getPointUUID().get());
                        } else {
                            pointValue = _deletedValue;
                            _deletedValue = _nextPointValue(
                                _deletedCursor,
                                _getPointUUID().get().deleted());
                        }
                    } else {
                        pointValue = _pointValue;
                        _pointValue = _nextPointValue(
                            _cursor,
                            _getPointUUID().get());
                    }
                } else if (_deletedValue != null) {
                    pointValue = _deletedValue;
                    _deletedValue = _nextPointValue(
                        _deletedCursor,
                        _getPointUUID().get().deleted());
                } else {
                    pointValue = null;
                }
            } else {
                final DatabaseEntry data = new DatabaseEntry();
                final DatabaseEntry key = new DatabaseEntry();
                final OperationStatus status;

                try {
                    if (_cursor == null) {
                        _cursor = _database.openCursor(null, null);
                        status = _cursor
                            .getSearchKeyRange(
                                _getConverter().newKey(_getAfter().after()),
                                key,
                                data,
                                null);
                    } else {
                        status = _cursor
                            .getNext(new DatabaseEntry(), key, data, null);
                    }
                } catch (final DatabaseException exception) {
                    throw new RuntimeException(exception);
                }

                if ((status == OperationStatus.SUCCESS)
                        && _getConverter().getVersion(
                            data).isBefore(_getBefore())) {
                    pointValue = _getConverter().getPointValue(key, data);
                } else {
                    pointValue = null;
                }
            }

            return Optional.ofNullable(pointValue);
        }

        private VersionedValue _firstPointValue(
                final SecondaryCursor cursor,
                final UUID uuid)
        {
            final DatabaseEntry data = new DatabaseEntry();
            final DatabaseEntry key = new DatabaseEntry();
            OperationStatus status;

            try {
                status = cursor
                    .getSearchKeyRange(
                        _getConverter().newKey(_getAfter().after()),
                        key,
                        data,
                        null);

                while ((status == OperationStatus.SUCCESS)
                        && !Converter.getUUID(
                            key).equals(_getPointUUID().get())) {
                    status = cursor
                        .getNext(new DatabaseEntry(), key, data, null);
                }
            } catch (final DatabaseException exception) {
                throw new RuntimeException(exception);
            }

            return _pointValue(status, uuid, key, data);
        }

        private VersionedValue _nextPointValue(
                final SecondaryCursor cursor,
                final UUID uuid)
        {
            final DatabaseEntry data = new DatabaseEntry();
            final DatabaseEntry key = new DatabaseEntry();
            OperationStatus status;

            try {
                do {
                    status = cursor
                        .getNext(new DatabaseEntry(), key, data, null);
                } while ((status == OperationStatus.SUCCESS)
                         && !Converter.getUUID(
                                 key).equals(_getPointUUID().get()));
            } catch (final DatabaseException exception) {
                throw new RuntimeException(exception);
            }

            return _pointValue(status, uuid, key, data);
        }

        private VersionedValue _pointValue(
                final OperationStatus status,
                final UUID uuid,
                final DatabaseEntry key,
                final DatabaseEntry data)
        {
            return ((status == OperationStatus.SUCCESS)
                    && _getConverter().hasSameUUID(key, uuid)
                    && _getConverter().getVersion(
                        data).isBefore(
                                _getBefore()))? _getConverter()
                                        .getPointValue(key, data): null;
        }

        private SecondaryCursor _cursor;
        private final SecondaryDatabase _database;
        private SecondaryCursor _deletedCursor;
        private VersionedValue _deletedValue;
        private VersionedValue _pointValue;
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
