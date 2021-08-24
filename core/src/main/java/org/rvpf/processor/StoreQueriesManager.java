/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreQueriesManager.java 4036 2019-05-31 11:19:42Z SFB $
 */

package org.rvpf.processor;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreAccessException;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.value.PointValue;

/**
 * Store queries manager.
 */
final class StoreQueriesManager
{
    /**
     * Constructs an instance.
     *
     * @param cacheManager The cache manager.
     */
    StoreQueriesManager(@Nonnull final CacheManager cacheManager)
    {
        _cacheManager = cacheManager;
    }

    /**
     * Adds a notice value as a query sent.
     *
     * @param noticeValue The notice.
     */
    void addNoticeValue(@Nonnull final PointValue noticeValue)
    {
        if (!_cacheManager.acceptNotice(noticeValue)) {
            final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
                .newBuilder()
                .setPoint(noticeValue.getPoint().get());

            storeQueryBuilder.setNotAfter(noticeValue.getStamp());
            _queriesSent.add(storeQueryBuilder.build());
        }
    }

    /**
     * Adds a store values query for the point's Store.
     *
     * @param storeValuesQuery The store values query.
     */
    void addStoreQuery(@Nonnull final StoreValuesQuery storeValuesQuery)
    {
        final Optional<StoreValues> response = _cacheManager
            .handleQuery(storeValuesQuery);

        if (response.isPresent()) {
            _responses.add(response.get());
        }

        if (!storeValuesQuery.isCancelled()
                && _queriesSent.add(storeValuesQuery)) {
            final Store store = storeValuesQuery
                .getPoint()
                .get()
                .getStore()
                .get();

            if (store.addQuery(storeValuesQuery)) {
                ++_querySentCount;
                _stores.add(store);
            }
        }

        ++_queryCount;
    }

    /**
     * Clears this queries manager to help the GC recover memory.
     */
    void clear()
    {
        _queriesSent.clear();
        _responses.clear();
        _stores.clear();

        _cacheManager.clearCache();
    }

    /**
     * Forgets an input value.
     *
     * <p>This is used by the replication logic.</p>
     *
     * @param inputValue The value to forget.
     */
    void forgetInputValue(@Nonnull final PointValue inputValue)
    {
        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder()
            .setPoint(inputValue.getPoint().get());

        storeQueryBuilder.setNotAfter(inputValue.getStamp());
        _cacheManager.forgetValue(inputValue);
        _queriesSent.remove(storeQueryBuilder.build());
    }

    /**
     * Gets the count of point values received.
     *
     * @return The count.
     */
    @CheckReturnValue
    int getReceivedValueCount()
    {
        return _lastResponseValueCount;
    }

    /**
     * Gets the count of active stores.
     *
     * @return The count.
     */
    @CheckReturnValue
    int getStoreCount()
    {
        return _lastStoreCount;
    }

    /**
     * Gets the count of queries.
     *
     * @return The count.
     */
    @CheckReturnValue
    int getStoreQueryCount()
    {
        return _lastQueryCount;
    }

    /**
     * Gets the count of queries submitted.
     *
     * @return The count.
     */
    @CheckReturnValue
    int getStoreQuerySentCount()
    {
        return _lastQuerySentCount;
    }

    /**
     * Gets the responses from the stores.
     *
     * @return The responses.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws StoreAccessException When store access fails.
     */
    @Nonnull
    @CheckReturnValue
    List<StoreValues> getStoreResponses()
        throws InterruptedException, StoreAccessException
    {
        final List<StoreValues> responses = _responses;

        _lastQueryCount = _queryCount;
        _totalPreparedQueryCount += _queryCount;
        _queryCount = 0;
        _lastQuerySentCount = _querySentCount;
        _totalSentQueryCount += _querySentCount;
        _querySentCount = 0;
        _lastResponseValueCount = 0;

        for (final Store store: _stores) {
            for (;;) {
                final Optional<StoreValues> optionalResponse = store
                    .nextValues();

                if (!optionalResponse.isPresent()) {
                    break;
                }

                final StoreValues response = optionalResponse.get();

                _cacheManager.rememberResponse(response);
                responses.add(response);
                _lastResponseValueCount += response.size();
            }
        }

        _totalResponseValueCount += _lastResponseValueCount;

        _lastStoreCount = _stores.size();
        _stores.clear();
        _responses = new LinkedList<StoreValues>();

        return responses;
    }

    /**
     * Gets the total number of queries prepared.
     *
     * @return Returns the total number of queries prepared.
     */
    @CheckReturnValue
    int getTotalPreparedQueryCount()
    {
        return _totalPreparedQueryCount;
    }

    /**
     * Gets the total number of response values.
     *
     * @return Returns the total number of response values.
     */
    @CheckReturnValue
    int getTotalResponseValueCount()
    {
        return _totalResponseValueCount;
    }

    /**
     * Gets the total number of queries sent.
     *
     * @return Returns the total number of queries sent.
     */
    @CheckReturnValue
    int getTotalSentQueryCount()
    {
        return _totalSentQueryCount;
    }

    /**
     * Asks if an update is needed.
     *
     * @param pointValue The updated point value.
     *
     * @return True if an update is needed.
     */
    @CheckReturnValue
    boolean isUpdateNeeded(@Nonnull final PointValue pointValue)
    {
        return _cacheManager.acceptUpdate(pointValue);
    }

    /**
     * Sets the updates filtered control value.
     *
     * @param updatesFiltered The updates filtered control value.
     */
    void setUpdatesFiltered(final boolean updatesFiltered)
    {
        _cacheManager.setUpdatesFiltered(updatesFiltered);
    }

    private final CacheManager _cacheManager;
    private int _lastQueryCount;
    private int _lastQuerySentCount;
    private int _lastResponseValueCount;
    private int _lastStoreCount;
    private final Set<StoreValuesQuery> _queriesSent = new HashSet<>();
    private int _queryCount;
    private int _querySentCount;
    private List<StoreValues> _responses = new LinkedList<>();
    private final Set<Store> _stores = new HashSet<>();
    private int _totalPreparedQueryCount;
    private int _totalResponseValueCount;
    private int _totalSentQueryCount;
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
