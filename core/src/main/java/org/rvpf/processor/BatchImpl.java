/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BatchImpl.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.processor;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.alert.Signal;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.store.StoreAccessException;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.RecalcTrigger;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.BatchValuesQuery;
import org.rvpf.metadata.processor.Behavior;
import org.rvpf.metadata.processor.MemoryLimitException;

/**
 * Batch implementation.
 */
final class BatchImpl
    implements Batch
{
    /**
     * Constructs an instance.
     *
     * @param batchControl The batch control.
     */
    BatchImpl(@Nonnull final BatchControl batchControl)
    {
        _metadata = batchControl.getMetadata();
        _queriesManager = batchControl.newStoreQueriesManager();
        _batchControl = batchControl;
    }

    /** {@inheritDoc}
     */
    @Override
    public void acceptNotices(
            final Collection<PointValue> notices)
        throws MemoryLimitException
    {
        for (final PointValue notice: notices) {
            final PointEntity point = (PointEntity) notice
                .getPoint()
                .orElse(null);
            boolean noticeAccepted;

            Require.success(!_noticeValuesFrozen);

            _batchControl.traceNotice(notice);

            if (point == null) {
                noticeAccepted = false;
            } else if (notice instanceof RecalcTrigger) {
                noticeAccepted = !point.getInputs().isEmpty();
            } else {
                noticeAccepted = !point.getResults().isEmpty();
            }

            if (!noticeAccepted) {
                _LOGGER.debug(ProcessorMessages.NOTICE_UNEXPECTED, notice);
            }

            if (noticeAccepted && !_batchControl.resynchronizes()) {
                Require.notNull(point);

                final Optional<Sync> sync = point.getSync();

                if (sync.isPresent()) {
                    if (!sync.get().isInSync(notice.getStamp())) {
                        final Message message = new Message(
                            ProcessorMessages.NOTICE_OUT_OF_SYNC,
                            notice);

                        noticeAccepted = false;

                        if (point
                            .getParams()
                            .getBoolean(Point.RESYNCHRONIZED_PARAM)) {
                            _LOGGER.debug(message);
                        } else {
                            _LOGGER.warn(message);
                        }
                    }
                }
            }

            if (noticeAccepted) {
                if (notice.isDeleted()) {
                    forgetInputValue(notice);
                } else if (notice.getValue() != null) {
                    _queriesManager.addNoticeValue(notice);
                }

                noticeAccepted = _acceptInputValue(notice);
                _noticeValues.put(notice, notice);

                if (noticeAccepted) {
                    _LOGGER.trace(ProcessorMessages.NOTICE_ACCEPTED, notice);
                } else {
                    _LOGGER.trace(ProcessorMessages.NOTICE_UPDATED, notice);
                }
            }

            ++_noticeValueCount;

            if (!noticeAccepted) {
                ++_droppedNoticeCount;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void addStoreValuesQuery(final StoreValuesQuery storeQuery)
    {
        _queriesManager.addStoreQuery(storeQuery);
    }

    /** {@inheritDoc}
     */
    @Override
    public void addUpdate(final PointValue update)
    {
        _LOGGER.trace(ProcessorMessages.COMPUTED, update);

        if (_queriesManager.isUpdateNeeded(update)) {
            _updates.add(update);
        } else {
            _LOGGER.trace(ProcessorMessages.REDUNDANT_UPDATE, update);
            ++_droppedUpdateCount;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void forgetInputValue(final PointValue inputValue)
    {
        _inputValues.remove(inputValue);
        _queriesManager.forgetInputValue(inputValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public int getLookUpPass()
    {
        return _lookUpPass;
    }

    /** {@inheritDoc}
     */
    @Override
    public PointValue getPointValue(final BatchValuesQuery batchValuesQuery)
    {
        final TimeInterval interval = batchValuesQuery.getInterval();
        final PointValue returnedValue;

        if (interval.isInstant()) {
            returnedValue = _getPointValueAt(batchValuesQuery);
        } else if (interval.getAfter().isPresent()) {
            returnedValue = _getPointValueNotBefore(batchValuesQuery);
        } else if (interval.getBefore().isPresent()) {
            returnedValue = _getPointValueBefore(batchValuesQuery);
        } else {
            returnedValue = null;
        }

        return (returnedValue != null)? returnedValue: new PointValue(
            batchValuesQuery.getPoint().orElse(null),
            Optional.empty(),
            null,
            null);
    }

    /** {@inheritDoc}
     */
    @Override
    public Collection<PointValue> getPointValues(
            final BatchValuesQuery batchValuesQuery)
    {
        final TimeInterval interval = batchValuesQuery.getInterval();
        Collection<PointValue> values = _inputValues
            .subMap(
                new PointValue(
                    batchValuesQuery.getPoint().orElse(null),
                    Optional.of(interval.getBeginning(true)),
                    null,
                    null),
                new PointValue(
                    batchValuesQuery.getPoint().orElse(null),
                    Optional.of(interval.getEnd(false)),
                    null,
                    null))
            .values();

        if (batchValuesQuery.isNotNull()
                || batchValuesQuery.getSync().isPresent()
                || batchValuesQuery.isPolated()) {
            final Map<PointValue, PointValue> filteredValues = new TreeMap<>(
                POINT_EVENT_COMPARATOR);
            final Iterator<PointValue> iterator = values.iterator();
            final DateTime afterStamp = interval.getEnd(false);
            DateTime nextStamp = (batchValuesQuery.isPolated()
                    && batchValuesQuery.getSync().isPresent())? batchValuesQuery
                        .getSync()
                        .get()
                        .getNextStamp(interval.getBeginning(false))
                        .get(): afterStamp;

            for (;;) {
                final PointValue value = iterator
                    .hasNext()? iterator.next(): null;
                final DateTime valueStamp = (value != null)? value
                    .getStamp(): afterStamp;

                while (nextStamp.isBefore(valueStamp)) {
                    final PointValue key = new PointValue(
                        batchValuesQuery.getPoint().orElse(null),
                        Optional.of(nextStamp),
                        null,
                        null);
                    PointValue synthesized = null;

                    if (batchValuesQuery.isInterpolated()) {
                        synthesized = _interpolatedValues.get(key);
                    }

                    if ((synthesized == null)
                            && batchValuesQuery.isExtrapolated()) {
                        synthesized = _extrapolatedValues.get(key);
                    }

                    if (synthesized == null) {
                        synthesized = _synthesizedValues.get(key);
                    }

                    if (synthesized != null) {
                        filteredValues.put(synthesized, synthesized);
                    }

                    nextStamp = batchValuesQuery
                        .getSync()
                        .get()
                        .getNextStamp()
                        .get();
                }

                if (value == null) {
                    break;
                }

                if ((value.getValue() != null)
                        || !batchValuesQuery.isNotNull()) {
                    if (!batchValuesQuery.getSync().isPresent()
                            || batchValuesQuery.getSync().get().isInSync(
                                valueStamp)) {
                        filteredValues.put(value, value);
                    }
                }
            }

            values = filteredValues.values();
        }

        return values;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<ResultValue> getResultValue(final PointValue pointValue)
    {
        return Optional.ofNullable(_resultValues.get(pointValue));
    }

    /** {@inheritDoc}
     */
    @Override
    public void queueSignal(final String name, final Optional<String> info)
    {
        if (_queuedSignals == null) {
            _queuedSignals = new LinkedList<Signal>();
        }

        _queuedSignals
            .add(
                new Signal(
                    name,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(_metadata.getService().getSourceUUID()),
                    info));
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultValue replaceResultValue(
            ResultValue resultValue,
            final Behavior caller)
    {
        if (!resultValue.isReplaceable()) {
            Logger
                .getInstance(caller.getClass())
                .warn(
                    ProcessorMessages.RESULT_VALUE_CONFLICT,
                    resultValue.getPoint().orElse(null),
                    resultValue.getStamp());

            return null;
        }

        resultValue = caller
            .newResultValue(Optional.of(resultValue.getStamp()));
        _resultValues.put(resultValue, resultValue);

        return resultValue;
    }

    /** {@inheritDoc}
     */
    @Override
    public void scheduleUpdate(final PointValue update)
    {
        _batchControl.scheduleUpdate(update);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setCutoff(final Optional<NormalizedValue> pointValue)
    {
        _batchControl.setCutoff(pointValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultValue setUpResultValue(
            final DateTime stamp,
            final Behavior caller)
    {
        return setUpResultValue(
            caller.newResultValue(Optional.ofNullable(stamp)),
            caller);
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultValue setUpResultValue(
            final ResultValue newResult,
            final Behavior caller)
    {
        final ResultValue oldResult = _resultValues.get(newResult);
        ResultValue result;

        Require.success(!_resultsMapFrozen);

        if (newResult.isReplaceable()
                || (oldResult == null)
                || (newResult.getClass() == oldResult.getClass())) {
            result = oldResult;
        } else {
            if (!oldResult.isReplaceable()) {
                Logger
                    .getInstance(caller.getClass())
                    .warn(
                        ProcessorMessages.RESULT_SET_UP_CONFLICT,
                        caller.getResultPoint());
            }

            result = null;
        }

        if (result == null) {
            if (_batchControl.isResultAllowed(newResult)) {
                final Logger logger = Logger.getInstance(caller.getClass());

                if ((oldResult != null) && oldResult.isFetched()) {
                    newResult.setFetched(true);
                }

                _resultValues.put(newResult, newResult);
                logger
                    .trace(
                        ProcessorMessages.RESULT_SET_UP,
                        caller.getResultPoint(),
                        newResult.getStamp());

                final PointEntity pointEntity = (PointEntity) newResult
                    .getPoint()
                    .get();

                if (pointEntity.getRecalcLatest() >= 0) {
                    _recalcLatestResults.add(newResult);
                }
            } else {
                ++_cutoffResultCount;
            }

            result = newResult;
        }

        return result;
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultValue setUpResultValue(
            final PointValue notice,
            final DateTime stamp,
            final Behavior caller)
    {
        final ResultValue resultValue = setUpResultValue(stamp, caller);

        if (notice.equals(resultValue)) {
            _resultValues.remove(resultValue);
            Logger
                .getInstance(caller.getClass())
                .warn(
                    ProcessorMessages.SELF_TRIGGER_BLOCKED,
                    caller.getResultPoint());

            return null;
        }

        if (!resultValue.isFetched()
                && caller.isResultFetched(notice, resultValue)) {
            resultValue.setFetched(true);
        }

        return resultValue;
    }

    /** {@inheritDoc}
     */
    @Override
    public void setUpdatesFiltered(final boolean updatesFiltered)
    {
        _queriesManager.setUpdatesFiltered(updatesFiltered);
    }

    /**
     * Clears this batch to help the GC recover memory.
     */
    void clear()
    {
        _noticeValues.clear();
        _inputValues.clear();
        _extrapolatedValues.clear();
        _interpolatedValues.clear();
        _synthesizedValues.clear();
        _resultValues.clear();
        _recalcLatestResults.clear();
        _updates.clear();
        _queriesManager.clear();
    }

    /**
     * Drops a result value.
     *
     * @param resultValue The result value to drop.
     */
    void dropResultValue(@Nonnull final ResultValue resultValue)
    {
        Require.success(!_resultsMapFrozen);

        _resultValues.remove(resultValue);
    }

    /**
     * Freezes the results map.
     */
    void freezeResultsMap()
    {
        Require.success(!_resultsMapFrozen);

        _resultsMapFrozen = true;
    }

    /**
     * Gets the count of results discarded by cutoff control.
     *
     * @return The count.
     */
    @CheckReturnValue
    int getCutoffResultCount()
    {
        return _cutoffResultCount;
    }

    /**
     * Gets the dropped notice count.
     *
     * @return The dropped notice count.
     */
    @CheckReturnValue
    int getDroppedNoticeCount()
    {
        return _droppedNoticeCount;
    }

    /**
     * Gets the dropped result count.
     *
     * @return The dropped result count.
     */
    @CheckReturnValue
    int getDroppedResultCount()
    {
        return _droppedResultCount;
    }

    /**
     * Gets the dropped update count.
     *
     * @return The dropped update count.
     */
    @CheckReturnValue
    int getDroppedUpdateCount()
    {
        return _droppedUpdateCount;
    }

    /**
     * Gets the count of notice values received.
     *
     * @return The count.
     */
    @CheckReturnValue
    int getNoticeValueCount()
    {
        return _noticeValueCount;
    }

    /**
     * Gets the notice values.
     *
     * @return The notice values.
     */
    @Nonnull
    @CheckReturnValue
    Collection<PointValue> getNoticeValues()
    {
        return _noticeValues.values();
    }

    /**
     * Gets the queries manager.
     *
     * @return The queries manager.
     */
    @Nonnull
    @CheckReturnValue
    StoreQueriesManager getQueriesManager()
    {
        return _queriesManager;
    }

    /**
     * Gets the 'RecalcLatest' results.
     *
     * @return The 'RecalcLatest' results.
     */
    @Nonnull
    @CheckReturnValue
    Collection<ResultValue> getRecalcLatestResults()
    {
        return _recalcLatestResults;
    }

    /**
     * Gets the count of point values received.
     *
     * @return The count of point values received.
     */
    @CheckReturnValue
    int getReceivedValueCount()
    {
        return _queriesManager.getReceivedValueCount();
    }

    /**
     * Gets the count of results set up.
     *
     * @return The count of results set up.
     */
    @CheckReturnValue
    int getResultValueCount()
    {
        return _resultValues.size();
    }

    /**
     * Gets the result values.
     *
     * @return A collection of the result values.
     */
    @Nonnull
    @CheckReturnValue
    Collection<ResultValue> getResultValues()
    {
        return _resultValues.values();
    }

    /**
     * Gets the count of store queries.
     *
     * @return The count of store queries.
     */
    @CheckReturnValue
    int getStoreQueryCount()
    {
        return _queriesManager.getStoreQueryCount();
    }

    /**
     * Gets the count of sent queries.
     *
     * @return The count of sent queries.
     */
    @CheckReturnValue
    int getStoreQuerySentCount()
    {
        return _queriesManager.getStoreQuerySentCount();
    }

    /**
     * Gets the updates.
     *
     * @return The updates.
     */
    @Nonnull
    @CheckReturnValue
    List<PointValue> getUpdates()
    {
        return _updates;
    }

    /**
     * Sends the pending queries to the stores and gets the responses.
     *
     * @return The number of stores queried.
     *
     * @throws MemoryLimitException When the memory limit is exceeded.
     * @throws InterruptedException When the Service is stopped.
     * @throws StoreAccessException When store access fails.
     */
    @CheckReturnValue
    int processStoreQueries()
        throws MemoryLimitException, InterruptedException, StoreAccessException
    {
        int storeCount = 0;
        boolean repeat;

        do {
            final Collection<StoreValues> responses = _queriesManager
                .getStoreResponses();

            if (storeCount == 0) {
                storeCount = _queriesManager.getStoreCount();
            }

            repeat = false;

            for (final StoreValues response: responses) {
                for (final PointValue pointValue: response) {
                    if (pointValue.isSynthesized()) {
                        _acceptSynthesizedValue(pointValue);
                    } else {
                        _acceptInputValue(pointValue);
                    }
                }

                if (!response.isComplete()) {
                    final StoreValuesQuery.Builder storeQueryBuilder =
                        StoreValuesQuery
                            .newBuilder()
                            .copyFrom(response.createQuery());

                    if (!storeQueryBuilder.restore(_metadata)) {
                        throw new InternalError();
                    }

                    _queriesManager.addStoreQuery(storeQueryBuilder.build());
                    repeat = true;
                }
            }
        } while (repeat);

        ++_lookUpPass;

        return storeCount;
    }

    /**
     * Resets the look up pass number.
     */
    void resetLookUpPass()
    {
        _lookUpPass = 1;
    }

    /**
     * Send queued signals.
     *
     * @throws InterruptedException When interrupted.
     */
    void sendQueuedSignals()
        throws InterruptedException
    {
        if (_queuedSignals != null) {
            for (final Signal signal: _queuedSignals) {
                _metadata
                    .getService()
                    .sendSignal(signal.getName(), signal.getInfo());
            }

            _queuedSignals.clear();
        }
    }

    /**
     * Sets the dropped result count.
     *
     * @param droppedResultValueCount The dropped result count.
     */
    void setDroppedResultValueCount(final int droppedResultValueCount)
    {
        _droppedResultCount += droppedResultValueCount;
    }

    private static PointValue _getPointValueBefore(
            final NavigableMap<PointValue, PointValue> valuesMap,
            final PointValue pointValue)
    {
        final PointValue returnedValue = valuesMap.lowerKey(pointValue);

        return ((returnedValue != null)
                && (returnedValue.getPoint().get()
                    == pointValue.getPoint().get()))? returnedValue: null;
    }

    private static PointValue _getPointValueNotBefore(
            final NavigableMap<PointValue, PointValue> valuesMap,
            final PointValue pointValue)
    {
        final PointValue returnedValue = valuesMap.ceilingKey(pointValue);

        return ((returnedValue != null)
                && (returnedValue.getPoint().get()
                    == pointValue.getPoint().get()))? returnedValue: null;
    }

    private static BatchValuesQuery.Builder _queryBuilder(
            final BatchValuesQuery batchQuery)
    {
        return BatchValuesQuery.newBuilder().copyFrom(batchQuery);
    }

    private boolean _acceptInputValue(
            final PointValue inputValue)
        throws MemoryLimitException
    {
        final boolean accepted = !_inputValues.containsKey(inputValue);

        if (accepted) {
            _batchControl.verifyMemoryLimit(this);
            _inputValues.put(inputValue, inputValue);
        }

        return accepted;
    }

    private boolean _acceptSynthesizedValue(
            final PointValue synthesizedValue)
        throws MemoryLimitException
    {
        final boolean accepted;

        if (synthesizedValue.isInterpolated()) {
            accepted = !_interpolatedValues.containsKey(synthesizedValue);

            if (accepted) {
                _batchControl.verifyMemoryLimit(this);
                _interpolatedValues.put(synthesizedValue, synthesizedValue);
            }
        } else if (synthesizedValue.isExtrapolated()) {
            accepted = !_extrapolatedValues.containsKey(synthesizedValue);

            if (accepted) {
                _batchControl.verifyMemoryLimit(this);
                _extrapolatedValues.put(synthesizedValue, synthesizedValue);
            }
        } else {
            accepted = !_synthesizedValues.containsKey(synthesizedValue);

            if (accepted) {
                _batchControl.verifyMemoryLimit(this);
                _synthesizedValues.put(synthesizedValue, synthesizedValue);
            }
        }

        return accepted;
    }

    private PointValue _getPointValueAt(final BatchValuesQuery batchQuery)
    {
        final PointValue key = new PointValue(
            batchQuery.getPoint().get(),
            Optional.of(batchQuery.getInterval().getBeginning(true)),
            null,
            null);
        PointValue returnedValue;

        returnedValue = _inputValues.get(key);

        if ((returnedValue == null) && batchQuery.isPolated()) {
            if (batchQuery.isInterpolated()) {
                returnedValue = _interpolatedValues.get(key);
            }

            if ((returnedValue == null) && batchQuery.isExtrapolated()) {
                returnedValue = _extrapolatedValues.get(key);
            }

            if (returnedValue == null) {
                returnedValue = _synthesizedValues.get(key);
            }
        }

        if (returnedValue != null) {
            final Optional<Sync> sync = batchQuery.getSync();

            if ((batchQuery.isNotNull() && (returnedValue.getValue() == null))
                    || (sync.isPresent() && !sync.get().isInSync(
                        returnedValue.getStamp()))) {
                returnedValue = null;
            }
        }

        return returnedValue;
    }

    private PointValue _getPointValueBefore(BatchValuesQuery batchQuery)
    {
        final PointValue key = new PointValue(
            batchQuery.getPoint().get(),
            batchQuery.getInterval().getBefore(),
            null,
            null);
        PointValue returnedValue;

        returnedValue = _getPointValueBefore(_inputValues, key);

        if ((returnedValue == null) && batchQuery.isPolated()) {
            if (batchQuery.isInterpolated()) {
                returnedValue = _getPointValueBefore(_interpolatedValues, key);
            }

            if ((returnedValue == null) && batchQuery.isExtrapolated()) {
                returnedValue = _getPointValueBefore(_extrapolatedValues, key);
            }

            if (returnedValue == null) {
                returnedValue = _getPointValueBefore(_synthesizedValues, key);
            }
        }

        if (returnedValue != null) {
            final Optional<Sync> sync = batchQuery.getSync();

            if ((batchQuery.isNotNull() && (returnedValue.getValue() == null))
                    || (sync.isPresent() && !sync.get().isInSync(
                        returnedValue.getStamp()))) {
                batchQuery = _queryBuilder(batchQuery)
                    .setBefore(returnedValue.getStamp())
                    .build();
                returnedValue = _getPointValueBefore(batchQuery);
            }
        }

        return returnedValue;
    }

    private PointValue _getPointValueNotBefore(BatchValuesQuery batchQuery)
    {
        final PointValue key = new PointValue(
            batchQuery.getPoint().get(),
            batchQuery.getInterval().getNotBefore(),
            null,
            null);
        PointValue returnedValue;

        returnedValue = _getPointValueNotBefore(_inputValues, key);

        if ((returnedValue == null) && batchQuery.isPolated()) {
            if (batchQuery.isInterpolated()) {
                returnedValue = _getPointValueNotBefore(
                    _interpolatedValues,
                    key);
            }

            if ((returnedValue == null) && batchQuery.isExtrapolated()) {
                returnedValue = _getPointValueNotBefore(
                    _extrapolatedValues,
                    key);
            }

            if (returnedValue == null) {
                returnedValue = _getPointValueNotBefore(
                    _synthesizedValues,
                    key);
            }
        }

        if (returnedValue != null) {
            final Optional<Sync> sync = batchQuery.getSync();

            if ((batchQuery.isNotNull() && (returnedValue.getValue() == null))
                    || (sync.isPresent() && !sync.get().isInSync(
                        returnedValue.getStamp()))) {
                batchQuery = _queryBuilder(batchQuery)
                    .setAfter(returnedValue.getStamp())
                    .build();
                returnedValue = _getPointValueNotBefore(batchQuery);
            }
        }

        return returnedValue;
    }

    private static final Logger _LOGGER = Logger.getInstance(BatchImpl.class);

    static {
        DISABLED_UPDATE.setPointName("");
        DISABLED_UPDATE.setStamp(DateTime.INVALID);
        DISABLED_UPDATE.freeze();
    }

    private final BatchControl _batchControl;
    private int _cutoffResultCount;
    private int _droppedNoticeCount;
    private int _droppedResultCount;
    private int _droppedUpdateCount;
    private final NavigableMap<PointValue, PointValue> _extrapolatedValues =
        new TreeMap<>(
            POINT_EVENT_COMPARATOR);
    private final NavigableMap<PointValue, PointValue> _inputValues =
        new TreeMap<>(
            POINT_EVENT_COMPARATOR);
    private final NavigableMap<PointValue, PointValue> _interpolatedValues =
        new TreeMap<>(
            POINT_EVENT_COMPARATOR);
    private int _lookUpPass = 1;
    private final Metadata _metadata;
    private int _noticeValueCount;
    private final Map<PointValue, PointValue> _noticeValues =
        new LinkedHashMap<>();
    private boolean _noticeValuesFrozen;
    private final StoreQueriesManager _queriesManager;
    private List<Signal> _queuedSignals;
    private final Collection<ResultValue> _recalcLatestResults =
        new TreeSet<>();
    private final Map<ResultValue, ResultValue> _resultValues =
        new LinkedHashMap<>();
    private boolean _resultsMapFrozen;
    private final NavigableMap<PointValue, PointValue> _synthesizedValues =
        new TreeMap<>(
            POINT_EVENT_COMPARATOR);
    private final List<PointValue> _updates = new LinkedList<>();
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
