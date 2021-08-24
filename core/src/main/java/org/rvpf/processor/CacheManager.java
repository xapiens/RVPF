/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CacheManager.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.processor;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.base.value.PointValue;
import org.rvpf.content.BooleanContent;
import org.rvpf.metadata.Metadata;

/**
 * Cache manager.
 */
public final class CacheManager
{
    /**
     * Accepts a notice.
     *
     * @param notice The notice.
     *
     * @return False if disabled or not cacheable.
     */
    @CheckReturnValue
    boolean acceptNotice(@Nonnull final PointValue notice)
    {
        final boolean accepted = !_cacheDisabled && notice.isCacheable();

        if (accepted) {
            _updateEntry(notice);
        }

        return accepted;
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
        final boolean changed;

        if (_cacheDisabled || !pointValue.isCacheable()) {
            changed = true;
        } else {
            final CacheEntry cacheEntry = _cache.get(pointValue.getPointUUID());

            if (cacheEntry != null) {
                changed = cacheEntry.acceptUpdate(pointValue);
            } else {
                _updateEntry(pointValue);
                changed = true;
            }
        }

        return changed || !_updatesFiltered;
    }

    /**
     * Clears the cache.
     */
    void clearCache()
    {
        if (!_cacheDisabled) {
            for (final CacheEntry entry: _cache.values()) {
                entry.updateStats(_stats, true);
            }

            _entriesRemoved += _cache.size();
            _cache.clear();
        }
    }

    /**
     * Forgets everything about the availability of a point value.
     *
     * @param pointValue The point value.
     */
    void forgetValue(@Nonnull final PointValue pointValue)
    {
        if (!_cacheDisabled && pointValue.isCacheable()) {
            final CacheEntry cacheEntry = _cache.get(pointValue.getPointUUID());

            if ((cacheEntry != null)
                    && cacheEntry.remove(pointValue)
                    && cacheEntry.isEmpty()) {
                cacheEntry.updateStats(_stats, true);
                _removeEntry(pointValue.getPointUUID());
            }
        }
    }

    /**
     * Gets the cache size.
     *
     * @return The cache size.
     */
    @CheckReturnValue
    int getCacheSize()
    {
        return _cacheSize;
    }

    /**
     * Gets the filter control point.
     *
     * @return The optional filter control point.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Point> getFilterControlPoint()
    {
        return Optional.ofNullable(_cacheDisabled? null: _filterControlPoint);
    }

    /**
     * Handles a store query.
     *
     * @param storeQuery The store query.
     *
     * @return Store values or empty.
     */
    @Nonnull
    @CheckReturnValue
    Optional<StoreValues> handleQuery(
            @Nonnull final StoreValuesQuery storeQuery)
    {
        final Optional<StoreValues> response;

        storeQuery.setCancelled(false);

        if (_cacheDisabled || storeQuery.isPull() || (storeQuery.isSynced())) {
            response = Optional.empty();
        } else {
            final Optional<UUID> pointUUID = storeQuery.getPointUUID();

            if (pointUUID.isPresent()) {
                final CacheEntry cacheEntry = _cache.get(pointUUID.get());

                response = (cacheEntry != null)? cacheEntry
                    .handleQuery(storeQuery): Optional.empty();

                if (response.isPresent()) {
                    _cacheHits += response.get().size();
                }
            } else {
                response = Optional.empty();
            }
        }

        return response;
    }

    /**
     * Remembers a store response.
     *
     * @param response The store response.
     */
    void rememberResponse(@Nonnull final StoreValues response)
    {
        if (!_cacheDisabled) {
            final StoreValuesQuery storeQuery = response.getQuery().get();

            if (storeQuery.isPull()) {
                for (final PointValue pointValue: response) {
                    final UUID pointUUID = pointValue.getPointUUID();
                    CacheEntry cacheEntry;

                    cacheEntry = _cache.get(pointUUID);

                    if (cacheEntry == null) {
                        cacheEntry = _addEntry(pointUUID);
                    }

                    cacheEntry.add(pointValue);
                }
            } else if (!storeQuery.isSynced()) {
                final UUID pointUUID = storeQuery.getPointUUID().orElse(null);

                if (pointUUID != null) {
                    CacheEntry cacheEntry = _cache.get(pointUUID);

                    if (cacheEntry == null) {
                        cacheEntry = _addEntry(pointUUID);
                    }

                    cacheEntry.rememberResponse(response);

                    if (cacheEntry.isEmpty()) {
                        _removeEntry(pointUUID);
                    }
                }
            }
        }
    }

    /**
     * Asks if the processor is dedicated to resynchronization.
     *
     * @return True if the processor is dedicated to resynchronization.
     */
    @CheckReturnValue
    boolean resynchronizes()
    {
        return _resynchronizes;
    }

    /**
     * Sets up this cache manager.
     *
     * @param metadata The metadata.
     * @param filterControlPoint The optional filter control point.
     * @param stats The processor stats.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(
            @Nonnull final Metadata metadata,
            @Nonnull final Optional<Point> filterControlPoint,
            @Nonnull final ProcessorStats stats)
    {
        final KeyedGroups processorProperties = metadata
            .getPropertiesGroup(ProcessorServiceAppImpl.PROCESSOR_PROPERTIES);

        _cacheDisabled = processorProperties
            .getBoolean(CACHE_DISABLED_PROPERTY);

        if (_cacheDisabled) {
            _LOGGER.info(ProcessorMessages.CACHE_DISABLED);
        } else {
            final int cacheBoost = processorProperties
                .getInt(CACHE_BOOST_PROPERTY, DEFAULT_CACHE_BOOST);

            _cacheSize = processorProperties
                .getInt(CACHE_SIZE_PROPERTY, DEFAULT_CACHE_SIZE);
            CacheValue.setBoost(cacheBoost);
            _cache = new LinkedHashMap<>(
                KeyedValues.hashCapacity(_cacheSize),
                KeyedValues.HASH_LOAD_FACTOR,
                true);
            _LOGGER
                .info(ProcessorMessages.CACHE_SIZE, String.valueOf(_cacheSize));
            _LOGGER
                .info(
                    ProcessorMessages.CACHE_BOOST,
                    String.valueOf(cacheBoost));

            _resynchronizes = processorProperties
                .getBoolean(RESYNCHRONIZES_PROPERTY);

            if (_resynchronizes) {
                _LOGGER.info(ProcessorMessages.PROCESSOR_RESYNCHRONIZES);
            }

            if (filterControlPoint.isPresent()) {
                if (!(filterControlPoint
                    .get()
                    .getContent()
                    .orElse(null) instanceof BooleanContent)) {
                    _LOGGER
                        .error(
                            ProcessorMessages.FILTER_CONTROL_CONTENT,
                            filterControlPoint.get());

                    return false;
                }

                _filterControlPoint = filterControlPoint.get();
                _LOGGER
                    .info(
                        ProcessorMessages.FILTER_CONTROL_POINT,
                        _filterControlPoint);
            } else {
                _updatesFiltered = processorProperties
                    .getBoolean(UPDATES_FILTERED_PROPERTY);

                if (_updatesFiltered) {
                    _LOGGER.info(ProcessorMessages.UPDATES_FILTERED);
                }
            }
        }

        _stats = stats;

        return true;
    }

    /**
     * Sets the updates filtered control value.
     *
     * @param updatesFiltered The updates filtered control value.
     */
    void setUpdatesFiltered(final boolean updatesFiltered)
    {
        _updatesFiltered = updatesFiltered;
        _LOGGER
            .info(
                ProcessorMessages.UPDATES_FILTERED_STATE,
                Boolean.valueOf(_updatesFiltered));
    }

    /**
     * Trims the cache.
     */
    void trimCache()
    {
        if (!_cacheDisabled) {
            final Iterator<CacheEntry> iterator = _cache.values().iterator();

            while (_cache.size() > _cacheSize) {
                final CacheEntry cacheEntry = iterator.next();

                cacheEntry.updateStats(_stats, true);
                iterator.remove();
                ++_entriesRemoved;
                CacheEntry.LOGGER.debug(ProcessorMessages.TRIMMED, cacheEntry);
            }

            while (iterator.hasNext()) {
                final CacheEntry cacheEntry = iterator.next();

                cacheEntry.trim();
                cacheEntry.updateStats(_stats, false);
            }

            _stats
                .updateCacheEntries(_cacheHits, _entriesAdded, _entriesRemoved);
            _cacheHits = 0;
            _entriesAdded = 0;
            _entriesRemoved = 0;
        }
    }

    private CacheEntry _addEntry(final UUID pointUUID)
    {
        final CacheEntry cacheEntry = new CacheEntry();

        _cache.put(pointUUID, cacheEntry);
        ++_entriesAdded;

        return cacheEntry;
    }

    private void _removeEntry(final UUID pointUUID)
    {
        _cache.remove(pointUUID);
        ++_entriesRemoved;
    }

    private void _updateEntry(final PointValue pointValue)
    {
        CacheEntry cacheEntry = _cache.get(pointValue.getPointUUID());

        if (cacheEntry == null) {
            cacheEntry = _addEntry(pointValue.getPointUUID());
        }

        cacheEntry.add(pointValue);
    }

    /** The initial hits count for a newly cached value. */
    public static final String CACHE_BOOST_PROPERTY = "cache.boost";

    /** Disables the cache. */
    public static final String CACHE_DISABLED_PROPERTY = "cache.disabled";

    /** The maximum number of points to keep in the cache. */
    public static final String CACHE_SIZE_PROPERTY = "cache.size";

    /** Default cache boost. */
    public static final int DEFAULT_CACHE_BOOST = 10;

    /** Default cache size. */
    public static final int DEFAULT_CACHE_SIZE = 1000;

    /**
     * Specifies that this processor is dedicated to resynchronization. This
     * disables the out of sync notices filter.
     */
    public static final String RESYNCHRONIZES_PROPERTY = "resynchronizes";

    /**
     * Enables the updates filter. This filter avoid sending updates to the
     * Store when the computed value has not changed.
     */
    public static final String UPDATES_FILTERED_PROPERTY =
        "cache.updates.filtered";

    /**  */

    private static final Logger _LOGGER = Logger
        .getInstance(CacheManager.class);

    private Map<UUID, CacheEntry> _cache;
    private boolean _cacheDisabled;
    private int _cacheHits;
    private int _cacheSize;
    private int _entriesAdded;
    private int _entriesRemoved;
    private Point _filterControlPoint;
    private boolean _resynchronizes;
    private ProcessorStats _stats;
    private boolean _updatesFiltered;
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
