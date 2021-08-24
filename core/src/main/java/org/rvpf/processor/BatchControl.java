/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BatchControl.java 4079 2019-06-12 13:40:21Z SFB $
 */

package org.rvpf.processor;

import java.io.File;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.Traces;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.processor.MemoryLimitException;
import org.rvpf.service.Service;

/**
 * Batch control.
 */
public final class BatchControl
{
    /**
     * Returns a new batch instance.
     *
     * @return The new batch instance.
     */
    @Nonnull
    @CheckReturnValue
    public BatchImpl newBatch()
    {
        _memoryUsed = 0;

        return new BatchImpl(this);
    }

    /**
     * Sets up this batch factory.
     *
     * @param metadata The metadata.
     * @param cutoffControl The optional cutoff control object.
     * @param cacheManager The cache manager.
     * @param configProperties The config properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean setUp(
            @Nonnull final Metadata metadata,
            @Nonnull final Optional<CutoffControl> cutoffControl,
            @Nonnull final CacheManager cacheManager,
            @Nonnull final KeyedGroups configProperties)
    {
        final int totalMemoryLow;
        final int totalMemoryMaximum;
        final int totalMemoryHigh;
        final long maxMemory = Runtime.getRuntime().maxMemory();
        final KeyedGroups processorProperties = metadata
            .getPropertiesGroup(ProcessorServiceAppImpl.PROCESSOR_PROPERTIES);

        _metadata = metadata;
        _cutoffControl = cutoffControl.orElse(null);
        _cacheManager = cacheManager;

        _batchLimit = processorProperties
            .getInt(BATCH_INITIAL_PROPERTY, DEFAULT_BATCH_INITIAL);
        _batchLimitMaximum = processorProperties
            .getInt(BATCH_MAXIMUM_PROPERTY, DEFAULT_BATCH_MAXIMUM);

        _actualMemoryLow = processorProperties
            .getInt(ACTUAL_MEMORY_LOW_PROPERTY, DEFAULT_ACTUAL_MEMORY_LOW);
        totalMemoryLow = processorProperties
            .getInt(TOTAL_MEMORY_LOW_PROPERTY, DEFAULT_TOTAL_MEMORY_LOW);
        totalMemoryHigh = processorProperties
            .getInt(TOTAL_MEMORY_HIGH_PROPERTY, DEFAULT_TOTAL_MEMORY_HIGH);
        totalMemoryMaximum = processorProperties
            .getInt(
                TOTAL_MEMORY_MAXIMUM_PROPERTY,
                DEFAULT_TOTAL_MEMORY_MAXIMUM);

        if (_LOGGER.isDebugEnabled()) {
            _LOGGER
                .debug(
                    ProcessorMessages.BATCH_LIMIT,
                    String.valueOf(_batchLimit),
                    String.valueOf(_batchLimitMaximum));
            _LOGGER
                .debug(
                    ProcessorMessages.MEMORY_LIMIT,
                    String.valueOf(totalMemoryLow),
                    String.valueOf(_actualMemoryLow),
                    String.valueOf(totalMemoryHigh),
                    String.valueOf(totalMemoryMaximum));
        }

        _totalMemoryLow = (long) (maxMemory * (totalMemoryLow / 100.0));
        _totalMemoryLimit = (long) (maxMemory * (totalMemoryMaximum / 100.0));
        _totalMemoryHigh = (long) (maxMemory * (totalMemoryHigh / 100.0));

        if (metadata.hasService()) {
            final Service service = metadata.getService();
            final UUID sourceUUID = service.getSourceUUID();
            final File dataDir = service.getDataDir();
            final KeyedGroups tracesProperties = configProperties
                .getGroup(Traces.TRACES_PROPERTIES);

            Require
                .ignored(
                    _receivedTraces
                        .setUp(
                                dataDir,
                                        tracesProperties,
                                        sourceUUID,
                                        Optional.of(RECEIVED_TRACES)));
            Require
                .ignored(
                    _sentTraces
                        .setUp(
                                dataDir,
                                        tracesProperties,
                                        sourceUUID,
                                        Optional.of(SENT_TRACES)));
        }

        return true;
    }

    /**
     * Commits.
     */
    void commit()
    {
        if (_receivedTraces != null) {
            _receivedTraces.commit();
        }

        if (_sentTraces != null) {
            _sentTraces.commit();
        }
    }

    /**
     * Gets the batch limit on the number of notices.
     *
     * @return The limit.
     */
    @CheckReturnValue
    int getBatchLimit()
    {
        return _batchLimit;
    }

    /**
     * Gets the cache manager.
     *
     * @return The cache manager.
     */
    @Nonnull
    @CheckReturnValue
    CacheManager getCacheManager()
    {
        return _cacheManager;
    }

    /**
     * Gets the wait time until the next update is due.
     *
     * @return Negative is forever, 0 is no wait, other is milliseconds.
     */
    @CheckReturnValue
    long getDueUpdateWait()
    {
        return _scheduledUpdates
            .isEmpty()? -1: Math
                .max(
                    0,
                    _scheduledUpdates
                            .firstKey()
                            .sub(DateTime.now())
                            .toMillis());
    }

    /**
     * Gets the due updates.
     *
     * @return The due updates.
     */
    @Nonnull
    @CheckReturnValue
    Collection<PointValue> getDueUpdates()
    {
        final NavigableMap<DateTime, PointValue> dueUpdatesMap =
            _scheduledUpdates
                .headMap(DateTime.now(), true);
        final Collection<PointValue> dueUpdates = new ArrayList<>(
            dueUpdatesMap.size());

        do {
            final Entry<DateTime, PointValue> dueUpdateEntry = dueUpdatesMap
                .pollFirstEntry();

            if (dueUpdateEntry == null) {
                break;
            }

            final PointValue dueUpdate = dueUpdateEntry.getValue();

            dueUpdates.add(dueUpdate);
            _LOGGER.trace(ProcessorMessages.UPDATE_DUE, dueUpdate);
            Require.ignored(_cacheManager.acceptUpdate(dueUpdate));
        } while (dueUpdates.size() < _batchLimit);

        return dueUpdates;
    }

    /**
     * Gets the metadata.
     *
     * @return Then metadata.
     */
    @Nonnull
    @CheckReturnValue
    Metadata getMetadata()
    {
        return _metadata;
    }

    /**
     * Asks if a result value holder is allowed.
     *
     * @param newResult The new result value holder.
     *
     * @return True if the new result is allowed.
     */
    @CheckReturnValue
    boolean isResultAllowed(@Nonnull final ResultValue newResult)
    {
        return (_cutoffControl == null) || _cutoffControl.verify(newResult);
    }

    /**
     * Returns a new store queries manager.
     *
     * @return The new store queries manager.
     */
    @Nonnull
    @CheckReturnValue
    StoreQueriesManager newStoreQueriesManager()
    {
        return new StoreQueriesManager(_cacheManager);
    }

    /**
     * Asks if the processor is dedicated to resynchronization.
     *
     * @return True if it is.
     */
    @CheckReturnValue
    boolean resynchronizes()
    {
        return _cacheManager.resynchronizes();
    }

    /**
     * Rollbacks.
     */
    void rollback()
    {
        if (_receivedTraces != null) {
            _receivedTraces.rollback();
        }

        if (_sentTraces != null) {
            _sentTraces.rollback();
        }
    }

    /**
     * Schedules an update.
     *
     * @param update The update.
     */
    void scheduleUpdate(@Nonnull final PointValue update)
    {
        _scheduledUpdates.put(update.getStamp(), update);
        _LOGGER.trace(ProcessorMessages.UPDATE_SCHEDULED, update);
    }

    /**
     * Sets the batch limit.
     *
     * @param batchLimit The new batch limit.
     */
    void setBatchLimit(final int batchLimit)
    {
        if (_LOGGER.isDebugEnabled()) {
            if (batchLimit < _batchLimit) {
                _LOGGER
                    .debug(
                        ProcessorMessages.BATCH_LIMIT_REDUCED,
                        String.valueOf(_batchLimit));
            } else if (batchLimit > _batchLimit) {
                _LOGGER
                    .debug(
                        ProcessorMessages.BATCH_LIMIT_INCREASED,
                        String.valueOf(batchLimit));
            }
        }

        _batchLimit = batchLimit;
    }

    /**
     * Sets the cutoff to the point value.
     *
     * <p>An empty point value clears the cutoff.</p>
     *
     * @param pointValue The point value holding the time (clock content).
     */
    void setCutoff(@Nonnull final Optional<NormalizedValue> pointValue)
    {
        _cutoffControl.use(pointValue);
    }

    /**
     * Tears down what has been set up.
     */
    void tearDown()
    {
        if (_sentTraces != null) {
            _sentTraces.tearDown();
        }

        if (_receivedTraces != null) {
            _receivedTraces.tearDown();
        }
    }

    /**
     * Traces a notice.
     *
     * @param notice The notice.
     */
    void traceNotice(@Nonnull final PointValue notice)
    {
        if (_receivedTraces != null) {
            _receivedTraces.add(notice);
        }
    }

    /**
     * Traces an update.
     *
     * @param update The update.
     */
    void traceUpdate(@Nonnull final PointValue update)
    {
        if (_sentTraces != null) {
            _sentTraces.add(update);
        }
    }

    /**
     * Updates memory limits.
     *
     * @param batchSize The current batch size.
     */
    void updateMemoryLimits(final int batchSize)
    {
        if ((_memoryUsed > _totalMemoryHigh) && (_batchLimit > 1)) {
            setBatchLimit(Math.max(batchSize / 2, 1));
        } else if ((_memoryUsed > 0) && (batchSize >= _batchLimit)) {
            final long memoryLow = Math
                .max(
                    (Runtime.getRuntime().totalMemory() * _actualMemoryLow)
                    / 100,
                    _totalMemoryLow);

            if ((_memoryUsed < memoryLow)
                    && (_batchLimit < _batchLimitMaximum)) {
                setBatchLimit(Math.min(_batchLimit * 2, _batchLimitMaximum));
            }
        }

        _cacheManager.trimCache();
    }

    /**
     * Verifies the memory limits for a batch.
     *
     * @param batch The batch.
     *
     * @throws MemoryLimitException When appropriate.
     */
    void verifyMemoryLimit(
            @Nonnull final BatchImpl batch)
        throws MemoryLimitException
    {
        final Runtime runtime = Runtime.getRuntime();
        final long memoryUsed = runtime.totalMemory() - runtime.freeMemory();

        if (memoryUsed > _totalMemoryLimit) {
            _cacheManager.clearCache();
            _batchLimit = Math.max(batch.getNoticeValues().size() / 10, 1);
            _memoryUsed = 0;

            throw new MemoryLimitException();
        }

        if (memoryUsed > _memoryUsed) {
            _memoryUsed = memoryUsed;
        }
    }

    /**
     * The minimum percentage of the JVM actual memory that should be used by a
     * full batch. When underused, the next batch will be allowed 2 times more
     * notices, up to '{@value #BATCH_MAXIMUM_PROPERTY}'.
     */
    public static final String ACTUAL_MEMORY_LOW_PROPERTY =
        "memory.actual.use.low";

    /** The initial number of notices to accept before processing them. */
    public static final String BATCH_INITIAL_PROPERTY = "batch.limit.initial";

    /** The maximum number of notices to accept before processing them. */
    public static final String BATCH_MAXIMUM_PROPERTY = "batch.limit.maximum";

    /** Default value for '{@value #ACTUAL_MEMORY_LOW_PROPERTY}'. */
    public static final int DEFAULT_ACTUAL_MEMORY_LOW = 25;

    /** Default value for '{@value #BATCH_INITIAL_PROPERTY}'. */
    public static final int DEFAULT_BATCH_INITIAL = 1000;

    /** Default value for '{@value #BATCH_MAXIMUM_PROPERTY}'. */
    public static final int DEFAULT_BATCH_MAXIMUM = 5000;

    /** Default value for '{@value #TOTAL_MEMORY_HIGH_PROPERTY}'. */
    public static final int DEFAULT_TOTAL_MEMORY_HIGH = 50;

    /** Default value for '{@value #TOTAL_MEMORY_LOW_PROPERTY}'. */
    public static final int DEFAULT_TOTAL_MEMORY_LOW = 5;

    /** Default value for '{@value #TOTAL_MEMORY_MAXIMUM_PROPERTY}'. */
    public static final int DEFAULT_TOTAL_MEMORY_MAXIMUM = 75;

    /** Received values trace subdirectory. */
    public static final String RECEIVED_TRACES = "received";

    /** Sent values trace subdirectory. */
    public static final String SENT_TRACES = "sent";

    /**
     * The maximum percentage of the JVM total memory that should be used be a
     * batch. When exceeded, the next batch will be allowed 2 times less
     * notices.
     */
    public static final String TOTAL_MEMORY_HIGH_PROPERTY =
        "memory.total.use.high";

    /**
     * The minimum percentage of the JVM total memory that should be used by a
     * full batch. When underused, the next batch will be allowed 2 times more
     * notices, up to '{@value #BATCH_MAXIMUM_PROPERTY}'.
     */
    public static final String TOTAL_MEMORY_LOW_PROPERTY =
        "memory.total.use.low";

    /**
     * The maximum percentage of the JVM total memory that can be used by a
     * batch. When exceeded, the batch will be cancelled and the next one will
     * be allowed 10 times less notices (but at least 1). If the cancelled batch
     * only had one notice in it, that notice will be dropped, since its
     * processing requires too much resources.
     */
    public static final String TOTAL_MEMORY_MAXIMUM_PROPERTY =
        "memory.total.use.maximum";

    /**  */

    private static final Logger _LOGGER = Logger
        .getInstance(BatchControl.class);

    private int _actualMemoryLow;
    private int _batchLimit;
    private int _batchLimitMaximum;
    private CacheManager _cacheManager;
    private CutoffControl _cutoffControl;
    private long _memoryUsed;
    private Metadata _metadata;
    private final Traces _receivedTraces = new Traces();
    private final NavigableMap<DateTime, PointValue> _scheduledUpdates =
        new TreeMap<>();
    private final Traces _sentTraces = new Traces();
    private long _totalMemoryHigh;
    private long _totalMemoryLimit;
    private long _totalMemoryLow;
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
