/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CacheEntry.java 4079 2019-06-12 13:40:21Z SFB $
 */

package org.rvpf.processor;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;

/**
 * Cache entry.
 *
 * <p>Except at creation or just before removal, a cache entry must contain at
 * least one value. All values refer to the same point.</p>
 */
final class CacheEntry
{
    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return _values.firstEntry().getValue().getPointValue().pointString();
    }

    /**
     * Accepts an update.
     *
     * @param pointValue The update.
     *
     * @return True if the value has changed.
     */
    @CheckReturnValue
    boolean acceptUpdate(@Nonnull final PointValue pointValue)
    {
        final DateTime stamp = pointValue.getStamp();
        final boolean changed;
        CacheValue cacheValue;

        cacheValue = _getValue(stamp);

        if (cacheValue != null) {
            final PointValue cachedValue = cacheValue.getPointValue();

            changed = (pointValue.getClass() != cachedValue.getClass())
                      || !pointValue.sameValueAs(cachedValue);

            if (changed) {
                cacheValue.setPointValue(pointValue);
                ++_valuesUpdated;
                CacheValue.LOGGER.trace(ProcessorMessages.UPDATED, cacheValue);
            }
        } else {
            cacheValue = _createValue(
                pointValue,
                Optional.empty(),
                Optional.empty(),
                false);
            changed = true;
        }

        cacheValue.hit();

        return changed;
    }

    /**
     * Adds a value.
     *
     * @param pointValue The point value.
     */
    void add(@Nonnull final PointValue pointValue)
    {
        Require.notNull(pointValue.getStamp());

        _add(pointValue, Optional.empty(), Optional.empty(), false);
    }

    /**
     * Handles a store values query.
     *
     * @param storeQuery The store values query.
     *
     * @return A store values instance or empty.
     */
    @Nonnull
    @CheckReturnValue
    Optional<StoreValues> handleQuery(
            @Nonnull final StoreValuesQuery storeQuery)
    {
        final StoreValues response = new StoreValues(storeQuery);
        final boolean notNull = storeQuery.isNotNull();
        final TimeInterval queryInterval = storeQuery.getInterval();
        DateTime queryAfter = queryInterval.getAfter().orElse(null);
        DateTime queryBefore = queryInterval.getBefore().orElse(null);

        if (queryAfter == null) {
            queryAfter = DateTime.BEGINNING_OF_TIME;
        }

        if (queryBefore == null) {
            queryBefore = DateTime.END_OF_TIME;
        }

        if (!storeQuery.isMultiple()) {
            if (storeQuery.isReverse()) {
                final CacheValue cacheValue = _getValueBefore(queryBefore);

                if ((cacheValue != null)
                        && cacheValue.getStamp().isAfter(queryAfter)) {
                    final DateTime cacheBefore;

                    if (notNull || !cacheValue.isNullIgnored()) {
                        cacheBefore = cacheValue.getBefore().get();
                    } else {
                        cacheBefore = cacheValue.getStamp().after();
                    }

                    if (cacheBefore.isNotBefore(queryBefore)) {
                        if (_cacheHit(cacheValue, notNull, response)) {
                            _cancelQuery(storeQuery);
                        }
                    }
                }
            } else if (storeQuery.getInterval().isInstant()) {
                final CacheValue cacheValue = _getValueAfter(queryAfter);

                if ((cacheValue != null)
                        && (cacheValue.getStamp().equals(queryAfter.after()))) {
                    if (_cacheHit(cacheValue, notNull, response)) {
                        _cancelQuery(storeQuery);
                    }
                }
            } else {
                final CacheValue cacheValue = _getValueAfter(queryAfter);

                if ((cacheValue != null)
                        && cacheValue.getStamp().isBefore(queryBefore)) {
                    final DateTime cacheAfter;

                    if (notNull || !cacheValue.isNullIgnored()) {
                        cacheAfter = cacheValue.getAfter().get();
                    } else {
                        cacheAfter = cacheValue.getStamp().before();
                    }

                    if (cacheAfter.isNotAfter(queryAfter)) {
                        if (_cacheHit(cacheValue, notNull, response)) {
                            _cancelQuery(storeQuery);
                        }
                    }
                }
            }
        }

        return (!response.isEmpty())? Optional.of(response): Optional.empty();
    }

    /**
     * Asks if the number of values in this entry is none.
     *
     * @return True if the number of values in this entry is none.
     */
    @CheckReturnValue
    boolean isEmpty()
    {
        return _values.isEmpty();
    }

    /**
     * Remembers a store response.
     *
     * @param response The store response.
     */
    void rememberResponse(@Nonnull final StoreValues response)
    {
        final StoreValuesQuery storeQuery = response.getQuery().get();
        final boolean reverse = storeQuery.isReverse();
        final boolean nullIgnored = storeQuery.isNotNull();
        DateTime queryAfter;
        DateTime queryBefore;
        PointValue previousValue = null;
        DateTime after;
        DateTime before;

        queryAfter = storeQuery.getInterval().getAfter().orElse(null);

        if (queryAfter == null) {
            queryAfter = DateTime.BEGINNING_OF_TIME;
        }

        queryBefore = storeQuery.getInterval().getBefore().orElse(null);

        if (queryBefore == null) {
            queryBefore = DateTime.END_OF_TIME;
        }

        if (reverse) {
            after = null;
            before = queryBefore;
        } else {
            after = queryAfter;
            before = null;
        }

        try {
            for (final PointValue pointValue: response) {
                final DateTime stamp = pointValue.getStamp();

                if (previousValue != null) {
                    if (reverse) {
                        after = stamp;
                    } else {
                        before = stamp;
                    }

                    _add(
                        previousValue,
                        Optional.ofNullable(after),
                        Optional.ofNullable(before),
                        nullIgnored);

                    if (reverse) {
                        before = previousValue.getStamp();
                    } else {
                        after = previousValue.getStamp();
                    }
                }

                previousValue = pointValue;
            }

            if (previousValue != null) {
                if (storeQuery.isAll() && response.isComplete()) {
                    if (reverse) {
                        after = queryAfter;
                    } else {
                        before = queryBefore;
                    }
                } else if (reverse) {
                    after = null;
                } else {
                    before = null;
                }

                _add(
                    previousValue,
                    Optional.ofNullable(after),
                    Optional.ofNullable(before),
                    nullIgnored);
            }
        } catch (final AssertionError exception) {
            LOGGER.error(ProcessorMessages.CHECK_QUERY, storeQuery);

            for (final PointValue responseValue: response) {
                LOGGER
                    .error(
                        ProcessorMessages.CHECK_RESPONSE_VALUE,
                        responseValue);
            }

            LOGGER.error(ProcessorMessages.CHECK_PREVIOUS_VALUE, previousValue);
            LOGGER.error(ProcessorMessages.CHECK_AFTER_BEFORE, after, before);

            throw exception;
        }
    }

    /**
     * Removes a value.
     *
     * @param pointValue The point value.
     *
     * @return True if it was present.
     */
    @CheckReturnValue
    boolean remove(@Nonnull final PointValue pointValue)
    {
        final DateTime stamp = pointValue.getStamp();
        final CacheValue cacheValue = _getValue(stamp);

        if (cacheValue != null) {
            _values.remove(stamp);
            ++_valuesRemoved;
            CacheValue.LOGGER.trace(ProcessorMessages.REMOVED, cacheValue);
        }

        return cacheValue != null;
    }

    /**
     * Trims the entry.
     */
    void trim()
    {
        final Iterator<CacheValue> iterator = _values.values().iterator();

        for (;;) {
            final CacheValue cacheValue = iterator.next();

            if (!iterator.hasNext()) {
                break;
            }

            if (cacheValue.hits() == 0) {
                iterator.remove();
                ++_valuesRemoved;
                CacheValue.LOGGER.trace(ProcessorMessages.TRIMMED, cacheValue);
            }
        }
    }

    /**
     * Updates the processor stats.
     *
     * @param stats The processor stats.
     * @param removing True if this entry is being removed.
     */
    void updateStats(
            @Nonnull final ProcessorStats stats,
            final boolean removing)
    {
        if (removing) {
            _valuesRemoved += _values.size();
        }

        stats.updateCacheValues(_valuesAdded, _valuesUpdated, _valuesRemoved);
        _valuesAdded = 0;
        _valuesUpdated = 0;
        _valuesRemoved = 0;
    }

    private static boolean _cacheHit(
            final CacheValue cacheValue,
            final boolean nullIgnored,
            final StoreValues response)
    {
        final boolean confirmed = !nullIgnored || !cacheValue.isNull();

        if (confirmed) {
            response.add(cacheValue.getPointValue());
            cacheValue.hit();
            LOGGER
                .trace(ProcessorMessages.CACHE_HIT, cacheValue.getPointValue());
        }

        return confirmed;
    }

    private static void _cancelQuery(final StoreValuesQuery storeQuery)
    {
        storeQuery.setCancelled(true);
        LOGGER.trace(ProcessorMessages.CANCELLED_QUERY, storeQuery);
    }

    private void _add(
            final PointValue pointValue,
            final Optional<DateTime> after,
            final Optional<DateTime> before,
            final boolean nullsIgnored)
    {
        CacheValue cacheValue;

        cacheValue = _values.get(pointValue.getStamp());

        if (cacheValue == null) {
            cacheValue = _createValue(pointValue, after, before, nullsIgnored);
        } else {
            cacheValue.setPointValue(pointValue);
            cacheValue.setAfter(after);
            cacheValue.setBefore(before);
            cacheValue.setNullIgnored(nullsIgnored);
            ++_valuesUpdated;
            CacheValue.LOGGER.trace(ProcessorMessages.REFRESHED, cacheValue);
        }

        cacheValue.hit();
    }

    private CacheValue _createValue(
            final PointValue pointValue,
            final Optional<DateTime> after,
            final Optional<DateTime> before,
            final boolean nullsIgnored)
    {
        final CacheValue cacheValue = new CacheValue(
            pointValue,
            after,
            before,
            nullsIgnored);

        _values.put(pointValue.getStamp(), cacheValue);
        ++_valuesAdded;
        CacheValue.LOGGER.trace(ProcessorMessages.ADDED, cacheValue);

        return cacheValue;
    }

    private CacheValue _getValue(final DateTime stamp)
    {
        return _values.get(stamp);
    }

    private CacheValue _getValueAfter(final DateTime stamp)
    {
        final Map.Entry<DateTime, CacheValue> entry = _values
            .higherEntry(stamp);

        return (entry != null)? entry.getValue(): null;
    }

    private CacheValue _getValueBefore(final DateTime stamp)
    {
        final Map.Entry<DateTime, CacheValue> entry = _values.lowerEntry(stamp);

        return (entry != null)? entry.getValue(): null;
    }

    /** Logger shared with {@link CacheManager}. */
    static final Logger LOGGER = Logger.getInstance(CacheEntry.class);

    private final NavigableMap<DateTime, CacheValue> _values =
        new TreeMap<DateTime, CacheValue>();
    private int _valuesAdded;
    private int _valuesRemoved;
    private int _valuesUpdated;
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
