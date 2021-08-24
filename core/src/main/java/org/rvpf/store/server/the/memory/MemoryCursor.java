/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MemoryCursor.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the.memory;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.store.server.StoreCursor;

/**
 * Memory cursor.
 */
abstract class MemoryCursor
{
    /**
     * Constructs an instance.
     *
     * @param storeCursor A store cursor.
     */
    MemoryCursor(@Nonnull final StoreCursor storeCursor)
    {
        final Optional<DateTime> after = storeCursor.getAfter();
        final Optional<DateTime> before = storeCursor.getBefore();

        _after = after.isPresent()? after.get(): DateTime.BEGINNING_OF_TIME;
        _before = before.isPresent()? before.get(): DateTime.END_OF_TIME;

        _nullIgnored = storeCursor.isNullIgnored();
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
     * Counts values.
     *
     * @return The values count.
     */
    @CheckReturnValue
    abstract long count();

    /**
     * Returns the next value;
     *
     * @return The next value (maybe empty).
     */
    @Nonnull
    @CheckReturnValue
    abstract Optional<VersionedValue> next();

    private final DateTime _after;
    private final DateTime _before;
    private final boolean _nullIgnored;

    /**
     * Snapshot wrapper cursor.
     */
    static final class Snapshot
        extends MemoryCursor
    {
        /**
         * Constructs an instance.
         *
         * @param storeCursor A store cursor.
         * @param snapshotMap The snapshot map.
         */
        Snapshot(
                @Nonnull final StoreCursor storeCursor,
                @Nonnull final Map<UUID, VersionedValue> snapshotMap)
        {
            super(storeCursor);

            _snapshotMap = snapshotMap;
            _pointUUID = storeCursor.getPointUUID().orElse(null);
        }

        /** {@inheritDoc}
         */
        @Override
        long count()
        {
            return next().isPresent()? 1: 0;
        }

        /** {@inheritDoc}
         */
        @Override
        Optional<VersionedValue> next()
        {
            if (_done) {
                return Optional.empty();
            }

            VersionedValue pointValue = _snapshotMap.get(_pointUUID);

            if (pointValue != null) {
                final DateTime stamp = pointValue.getStamp();

                if (stamp.isBefore(_getBefore())
                        && stamp.isAfter(_getAfter())) {
                    if (_isNullIgnored() && (pointValue.getValue() == null)) {
                        pointValue = null;
                    }
                } else {
                    pointValue = null;
                }
            }

            _done = true;

            return Optional.ofNullable(pointValue);
        }

        private boolean _done;
        private final UUID _pointUUID;
        private final Map<UUID, VersionedValue> _snapshotMap;
    }


    /**
     * Stamp wrapper cursor.
     */
    static final class Stamp
        extends MemoryCursor
    {
        /**
         * Constructs an instance.
         *
         * @param storeCursor A store cursor.
         * @param pointValuesMap The point values map.
         */
        Stamp(
                @Nonnull final StoreCursor storeCursor,
                @Nonnull final NavigableMap<DateTime,
                VersionedValue> pointValuesMap)
        {
            super(storeCursor);

            _map = pointValuesMap
                .subMap(_getAfter(), false, _getBefore(), false);
            _reverse = storeCursor.isReverse();
        }

        /** {@inheritDoc}
         */
        @Override
        long count()
        {
            long count;

            if (_isNullIgnored()) {
                count = 0;

                for (final VersionedValue pointValue: _map.values()) {
                    if (pointValue.getValue() != null) {
                        ++count;
                    }
                }
            } else {
                count = _map.size();
            }

            return count;
        }

        /** {@inheritDoc}
         */
        @Override
        Optional<VersionedValue> next()
        {
            if (_iterator == null) {
                final Map<DateTime, VersionedValue> map = _reverse? _map
                    .descendingMap(): _map;

                _iterator = map.values().iterator();
            }

            VersionedValue pointValue = null;

            while (_iterator.hasNext()) {
                pointValue = _iterator.next();

                if (!_isNullIgnored() || (pointValue.getValue() != null)) {
                    break;
                }

                pointValue = null;
            }

            return Optional.ofNullable(pointValue);
        }

        private Iterator<VersionedValue> _iterator;
        private final NavigableMap<DateTime, VersionedValue> _map;
        private final boolean _reverse;
    }


    /**
     * Version wrapper cursor.
     */
    static final class Version
        extends MemoryCursor
    {
        /**
         * Constructs an instance.
         *
         * @param storeCursor A store cursor.
         * @param versionsMap The versions map.
         */
        Version(
                @Nonnull final StoreCursor storeCursor,
                @Nonnull final NavigableMap<DateTime,
                VersionedValue> versionsMap)
        {
            super(storeCursor);

            _map = versionsMap.subMap(_getAfter(), false, _getBefore(), false);
            _reverse = storeCursor.isReverse();
            _pointUUID = storeCursor.getPointUUID().orElse(null);
        }

        /** {@inheritDoc}
         */
        @Override
        long count()
        {
            long count;

            if ((_pointUUID != null) || _isNullIgnored()) {
                count = 0;

                for (final VersionedValue pointValue: _map.values()) {
                    if ((_pointUUID != null)
                            && !_pointUUID.equals(pointValue.getPointUUID())) {
                        continue;
                    }

                    if (_isNullIgnored() && (pointValue.getValue() == null)) {
                        continue;
                    }

                    ++count;
                }
            } else {
                count = _map.size();
            }

            return count;
        }

        /** {@inheritDoc}
         */
        @Override
        Optional<VersionedValue> next()
        {
            if (_iterator == null) {
                final Map<DateTime, VersionedValue> map = _reverse? _map
                    .descendingMap(): _map;

                _iterator = map.values().iterator();
            }

            VersionedValue pointValue = null;

            while (_iterator.hasNext()) {
                pointValue = _iterator.next();

                if ((_pointUUID == null)
                        || _pointUUID.equals(pointValue.getPointUUID())) {
                    if (!_isNullIgnored() || (pointValue.getValue() != null)) {
                        break;
                    }
                }

                pointValue = null;
            }

            return Optional.ofNullable(pointValue);
        }

        private Iterator<VersionedValue> _iterator;
        private final NavigableMap<DateTime, VersionedValue> _map;
        private final UUID _pointUUID;
        private final boolean _reverse;
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
