/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProcessorStats.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.processor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.Stats;
import org.rvpf.base.StatsOwner;
import org.rvpf.service.ServiceStats;

/**
 * ProcessorStats.
 */
@ThreadSafe
public final class ProcessorStats
    extends ServiceStats
{
    /**
     * Constructs an instance.
     *
     * @param statsOwner The stats owner.
     */
    ProcessorStats(@Nonnull final StatsOwner statsOwner)
    {
        super(statsOwner);
    }

    /** {@inheritDoc}
     */
    @Override
    public void buildText()
    {
        addLine(
            ProcessorMessages.BATCHES_PROCESSED,
            String.valueOf(getBatchesProcessed()));
        addLine(
            ProcessorMessages.NOTICES_STATS,
            String.valueOf(getNoticesReceived()),
            String.valueOf(getNoticesReceived() - getNoticesDropped()));
        addLine(
            ProcessorMessages.NOTICES_RECEPTION_TIME,
            nanosToString(getReceptionTime()));
        addLine(
            ProcessorMessages.QUERIES_STATS,
            String.valueOf(getQueriesPrepared()),
            String.valueOf(getQueriesSent()));

        if (isCacheEnabled()) {
            addLine(
                ProcessorMessages.CACHE_ENTRIES_STATS,
                String.valueOf(getCacheEntriesAdded()),
                String.valueOf(getCacheEntriesRemoved()));
            addLine(
                ProcessorMessages.CACHE_VALUES_STATS,
                String.valueOf(getCacheValuesAdded()),
                String.valueOf(getCacheValuesUpdated()),
                String.valueOf(getCacheValuesRemoved()));
            addLine(
                ProcessorMessages.QUERIED_VALUES_STATS,
                String.valueOf(getQueryValuesReceived()),
                String.valueOf(getCacheHits()));
        } else {
            addLine(
                ProcessorMessages.QUERIED_VALUES_RECEIVED,
                String.valueOf(getQueryValuesReceived()));
        }

        addLine(
            ProcessorMessages.PROCESSING_TIME,
            nanosToString(getProcessingTime()));
        addLine(
            ProcessorMessages.RESULTS_STATS,
            String.valueOf(getResultsPrepared()),
            String.valueOf(getResultsPrepared() - getResultsDropped()),
            String
                .valueOf(
                    getUpdatesSent() + getUpdatesDropped()
                    + getResultsDropped() - getResultsPrepared()));
        addLine(
            ProcessorMessages.UPDATES_STATS,
            String.valueOf(getUpdatesSent()),
            String.valueOf(getUpdatesDropped()));
        addLine(ProcessorMessages.UPDATE_TIME, nanosToString(getUpdateTime()));

        super.buildText();
    }

    /** {@inheritDoc}
     */
    @Override
    public Stats clone()
    {
        final ProcessorStats clone = (ProcessorStats) super.clone();

        clone._batchesProcessed = new AtomicInteger(getBatchesProcessed());
        clone._cacheEntriesAdded = new AtomicInteger(getCacheEntriesAdded());
        clone._cacheEntriesRemoved = new AtomicInteger(
            getCacheEntriesRemoved());
        clone._cacheHits = new AtomicInteger(getCacheHits());
        clone._cacheValuesAdded = new AtomicInteger(getCacheValuesAdded());
        clone._cacheValuesRemoved = new AtomicInteger(getCacheValuesRemoved());
        clone._cacheValuesUpdated = new AtomicInteger(getCacheValuesUpdated());
        clone._noticesDropped = new AtomicInteger(getNoticesDropped());
        clone._noticesReceived = new AtomicInteger(getNoticesReceived());
        clone._processingTime = new AtomicLong(getProcessingTime());
        clone._queriesPrepared = new AtomicInteger(getQueriesPrepared());
        clone._queriesSent = new AtomicInteger(getQueriesSent());
        clone._queryValuesReceived = new AtomicInteger(
            getQueryValuesReceived());
        clone._receptionTime = new AtomicLong(getReceptionTime());
        clone._resultsDropped = new AtomicInteger(getResultsDropped());
        clone._resultsPrepared = new AtomicInteger(getResultsPrepared());
        clone._updatesDropped = new AtomicInteger(getUpdatesDropped());
        clone._updatesSent = new AtomicInteger(getUpdatesSent());
        clone._updateTime = new AtomicLong(getUpdateTime());

        return clone;
    }

    /**
     * Gets the number of batches processed.
     *
     * @return The number of batches processed.
     */
    @CheckReturnValue
    public int getBatchesProcessed()
    {
        return _batchesProcessed.get();
    }

    /**
     * Gets the number of entries added.
     *
     * @return The number of entries added.
     */
    @CheckReturnValue
    public int getCacheEntriesAdded()
    {
        return _cacheEntriesAdded.get();
    }

    /**
     * Gets the number of cache entries removed.
     *
     * @return The number of cache entries removed.
     */
    @CheckReturnValue
    public int getCacheEntriesRemoved()
    {
        return _cacheEntriesRemoved.get();
    }

    /**
     * Gets the number of cache hits.
     *
     * @return The number of cache hits.
     */
    @CheckReturnValue
    public int getCacheHits()
    {
        return _cacheHits.get();
    }

    /**
     * Gets the number of cache values added.
     *
     * @return The number of cache values added.
     */
    @CheckReturnValue
    public int getCacheValuesAdded()
    {
        return _cacheValuesAdded.get();
    }

    /**
     * Gets the number of cache values removed.
     *
     * @return The number of cache values removed.
     */
    @CheckReturnValue
    public int getCacheValuesRemoved()
    {
        return _cacheValuesRemoved.get();
    }

    /**
     * Gets the number of cache values updated.
     *
     * @return The number of cache values updated.
     */
    @CheckReturnValue
    public int getCacheValuesUpdated()
    {
        return _cacheValuesUpdated.get();
    }

    /**
     * Gets the number of notices dropped.
     *
     * @return The number of notices dropped.
     */
    @CheckReturnValue
    public int getNoticesDropped()
    {
        return _noticesDropped.get();
    }

    /**
     * Gets the number of notices received.
     *
     * @return The number of notices received.
     */
    @CheckReturnValue
    public int getNoticesReceived()
    {
        return _noticesReceived.get();
    }

    /**
     * Gets the processing time.
     *
     * @return The processing time.
     */
    @CheckReturnValue
    public long getProcessingTime()
    {
        return _processingTime.get();
    }

    /**
     * Gets the number of queries prepared.
     *
     * @return The number of queries prepared.
     */
    @CheckReturnValue
    public int getQueriesPrepared()
    {
        return _queriesPrepared.get();
    }

    /**
     * Gets the number of queries sent.
     *
     * @return The number of queries sent.
     */
    @CheckReturnValue
    public int getQueriesSent()
    {
        return _queriesSent.get();
    }

    /**
     * Gets the number of queried values received.
     *
     * @return The number of queried values received.
     */
    @CheckReturnValue
    public int getQueryValuesReceived()
    {
        return _queryValuesReceived.get();
    }

    /**
     * Gets the reception time.
     *
     * @return The reception time.
     */
    @CheckReturnValue
    public long getReceptionTime()
    {
        return _receptionTime.get();
    }

    /**
     * Gets the number of results dropped.
     *
     * @return The number of results dropped.
     */
    @CheckReturnValue
    public int getResultsDropped()
    {
        return _resultsDropped.get();
    }

    /**
     * Gets the number of results prepared.
     *
     * @return The number of results prepared.
     */
    @CheckReturnValue
    public int getResultsPrepared()
    {
        return _resultsPrepared.get();
    }

    /**
     * Gets the update time.
     *
     * @return The update time.
     */
    @CheckReturnValue
    public long getUpdateTime()
    {
        return _updateTime.get();
    }

    /**
     * Gets the number of updates dropped.
     *
     * @return The number of updates dropped.
     */
    @CheckReturnValue
    public int getUpdatesDropped()
    {
        return _updatesDropped.get();
    }

    /**
     * Gets the number of updates sent.
     *
     * @return The number of updates sent.
     */
    @CheckReturnValue
    public int getUpdatesSent()
    {
        return _updatesSent.get();
    }

    /**
     * Asks if the cache is enabled.
     *
     * @return The cache enabled indicator.
     */
    @CheckReturnValue
    public boolean isCacheEnabled()
    {
        return _cacheEnabled;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void substract(final Stats snapshot)
    {
        final ProcessorStats stats = (ProcessorStats) snapshot;

        _batchesProcessed.addAndGet(-stats.getBatchesProcessed());
        _cacheEntriesAdded.addAndGet(-stats.getCacheEntriesAdded());
        _cacheEntriesRemoved.addAndGet(-stats.getCacheEntriesRemoved());
        _cacheHits.addAndGet(-stats.getCacheHits());
        _cacheValuesAdded.addAndGet(-stats.getCacheValuesAdded());
        _cacheValuesRemoved.addAndGet(-stats.getCacheValuesRemoved());
        _cacheValuesUpdated.addAndGet(-stats.getCacheValuesUpdated());
        _noticesDropped.addAndGet(-stats.getNoticesDropped());
        _noticesReceived.addAndGet(-stats.getNoticesReceived());
        _processingTime.addAndGet(-stats.getProcessingTime());
        _receptionTime.addAndGet(-stats.getReceptionTime());
        _queriesPrepared.addAndGet(-stats.getQueriesPrepared());
        _queriesSent.addAndGet(-stats.getQueriesSent());
        _queryValuesReceived.addAndGet(-stats.getQueryValuesReceived());
        _resultsDropped.addAndGet(-stats.getResultsDropped());
        _resultsPrepared.addAndGet(-stats.getResultsPrepared());
        _updatesDropped.addAndGet(-stats.getUpdatesDropped());
        _updatesSent.addAndGet(-stats.getUpdatesSent());
        _updateTime.addAndGet(-stats.getUpdateTime());

        super.substract(snapshot);
    }

    /**
     * Adds a batch's stats.
     *
     * @param batch The batch.
     * @param receptionTime The reception time in nanoseconds.
     * @param processingTime The processing time in nanoseconds.
     */
    void addBatch(
            @Nonnull final BatchImpl batch,
            final long receptionTime,
            final long processingTime)
    {
        final StoreQueriesManager queriesManager = batch.getQueriesManager();

        _batchesProcessed.incrementAndGet();

        _noticesReceived.addAndGet(batch.getNoticeValueCount());
        _noticesDropped.addAndGet(batch.getDroppedNoticeCount());

        _queriesPrepared.addAndGet(queriesManager.getTotalPreparedQueryCount());
        _queriesSent.addAndGet(queriesManager.getTotalSentQueryCount());
        _queryValuesReceived
            .addAndGet(queriesManager.getTotalResponseValueCount());

        _resultsPrepared.addAndGet(batch.getResultValueCount());
        _resultsDropped.addAndGet(batch.getDroppedResultCount());
        _updatesDropped.addAndGet(batch.getDroppedUpdateCount());

        _receptionTime.addAndGet(receptionTime);
        _processingTime.addAndGet(processingTime);
    }

    /**
     * Adds update stats.
     *
     * @param updateCount The update count.
     * @param updateTime The update time in nanoseconds.
     */
    void addUpdates(final int updateCount, final long updateTime)
    {
        _updatesSent.addAndGet(updateCount);
        _updateTime.addAndGet(updateTime);
    }

    /**
     * Updates cache entries counters.
     *
     * @param hits Count of cache hits.
     * @param added Count of entries added.
     * @param removed Count of entries removed.
     */
    void updateCacheEntries(final int hits, final int added, final int removed)
    {
        _cacheEnabled = true;
        _cacheHits.addAndGet(hits);
        _cacheEntriesAdded.addAndGet(added);
        _cacheEntriesRemoved.addAndGet(removed);
    }

    /**
     * Updates cache values counters.
     *
     * @param added Count of values added.
     * @param updated Count of values updated.
     * @param removed Count of values removed.
     */
    void updateCacheValues(
            final int added,
            final int updated,
            final int removed)
    {
        _cacheValuesAdded.addAndGet(added);
        _cacheValuesUpdated.addAndGet(updated);
        _cacheValuesRemoved.addAndGet(removed);
    }

    private static final long serialVersionUID = 1L;

    private AtomicInteger _batchesProcessed = new AtomicInteger();
    private volatile boolean _cacheEnabled;
    private AtomicInteger _cacheEntriesAdded = new AtomicInteger();
    private AtomicInteger _cacheEntriesRemoved = new AtomicInteger();
    private AtomicInteger _cacheHits = new AtomicInteger();
    private AtomicInteger _cacheValuesAdded = new AtomicInteger();
    private AtomicInteger _cacheValuesRemoved = new AtomicInteger();
    private AtomicInteger _cacheValuesUpdated = new AtomicInteger();
    private AtomicInteger _noticesDropped = new AtomicInteger();
    private AtomicInteger _noticesReceived = new AtomicInteger();
    private AtomicLong _processingTime = new AtomicLong();
    private AtomicInteger _queriesPrepared = new AtomicInteger();
    private AtomicInteger _queriesSent = new AtomicInteger();
    private AtomicInteger _queryValuesReceived = new AtomicInteger();
    private AtomicLong _receptionTime = new AtomicLong();
    private AtomicInteger _resultsDropped = new AtomicInteger();
    private AtomicInteger _resultsPrepared = new AtomicInteger();
    private AtomicInteger _updatesDropped = new AtomicInteger();
    private AtomicInteger _updatesSent = new AtomicInteger();
    private AtomicLong _updateTime = new AtomicLong();
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
