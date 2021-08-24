/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SummarizesBehavior.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.selector.summarizer;

import java.util.Locale;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.DateTime;
import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.BatchValuesQuery;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.ProcessorServiceAppImpl;
import org.rvpf.processor.engine.rpn.selector.SelectsBehavior;

/**
 * Summarizer behavior.
 */
public final class SummarizesBehavior
    extends SelectsBehavior
{
    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (!super.equals(other)) {
            return false;
        }

        final SummarizesBehavior otherBehavior = (SummarizesBehavior) other;

        return (_resultPosition == otherBehavior._resultPosition)
               && (_reverseInterval == otherBehavior._reverseInterval);
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /** {@inheritDoc}
     */
    @Override
    public Summary newResultValue(final Optional<DateTime> stamp)
    {
        return new Summary(getResultPoint(), stamp, this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean prepareSelect(
            final ResultValue resultValue,
            final Batch batch)
    {
        switch (batch.getLookUpPass()) {
            case 1: {
                if (!(resultValue instanceof Summary)) {
                    final Summary summary = (Summary) batch
                        .replaceResultValue(resultValue, this);
                    final Point summarizesPoint = getInputPoint();
                    final StoreValuesQuery.Builder storeValuesQueryBuilder =
                        StoreValuesQuery
                            .newBuilder()
                            .setPoint(summarizesPoint);

                    switch (_resultPosition) {
                        case BEGINNING: {
                            if (isInSync(summary.getStamp())) {
                                storeValuesQueryBuilder
                                    .setAt(summary.getStamp());
                                batch
                                    .addStoreValuesQuery(
                                        storeValuesQueryBuilder.build());
                                storeValuesQueryBuilder.clear();
                            }

                            storeValuesQueryBuilder
                                .setAfter(summary.getStamp());
                            batch
                                .addStoreValuesQuery(
                                    storeValuesQueryBuilder.build());

                            break;
                        }
                        case MIDDLE:
                        case END: {
                            storeValuesQueryBuilder
                                .setBefore(summary.getStamp());
                            batch
                                .addStoreValuesQuery(
                                    storeValuesQueryBuilder.build());
                            storeValuesQueryBuilder.clear();
                            storeValuesQueryBuilder
                                .setAfter(summary.getStamp());
                            batch
                                .addStoreValuesQuery(
                                    storeValuesQueryBuilder.build());

                            break;
                        }
                        case NEXT: {
                            storeValuesQueryBuilder
                                .setBefore(summary.getStamp());
                            batch
                                .addStoreValuesQuery(
                                    storeValuesQueryBuilder.build());

                            if (isInSync(summary.getStamp())) {
                                storeValuesQueryBuilder.clear();
                                storeValuesQueryBuilder
                                    .setAt(summary.getStamp());
                                batch
                                    .addStoreValuesQuery(
                                        storeValuesQueryBuilder.build());
                            }

                            break;
                        }
                        default: {
                            Require.failure();
                        }
                    }

                    return false;
                }

                return true;
            }
            case 2: {
                if (resultValue instanceof Summary) {
                    setUpSummary((Summary) resultValue, batch);
                }

                return true;
            }
            default: {
                return true;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean prepareTrigger(
            final PointValue noticeValue,
            final Batch batch)
    {
        switch (batch.getLookUpPass()) {
            case 1: {
                final Point summarizesPoint = getInputPoint();
                final StoreValuesQuery.Builder storeValuesQueryBuilder =
                    StoreValuesQuery
                        .newBuilder()
                        .setPoint(summarizesPoint);

                if (isStartStop()) {
                    final Boolean isStart = (Boolean) noticeValue
                        .normalized()
                        .getValue();

                    if (isStart == null) {
                        return true;
                    } else if (isStart.booleanValue()) {
                        if (isRunningInterval()) {
                            storeValuesQueryBuilder
                                .setAfter(noticeValue.getStamp());
                            batch
                                .addStoreValuesQuery(
                                    storeValuesQueryBuilder.build());
                        }

                        return true;
                    }
                } else if (isRunningInterval()) {
                    storeValuesQueryBuilder.setAfter(noticeValue.getStamp());
                    batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
                } else {
                    if (summarizesPoint.isSynced()) {
                        storeValuesQueryBuilder
                            .setAt(_getStopStamp(noticeValue.getStamp()));
                    } else {
                        storeValuesQueryBuilder
                            .setAfter(noticeValue.getStamp());
                    }

                    batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
                }

                storeValuesQueryBuilder.clear();

                if (summarizesPoint.isSynced()) {
                    storeValuesQueryBuilder
                        .setAt(_getStartStamp(noticeValue.getStamp()));
                } else {
                    storeValuesQueryBuilder.setBefore(noticeValue.getStamp());
                }

                batch.addStoreValuesQuery(storeValuesQueryBuilder.build());

                return true;
            }
            default: {
                return true;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean select(final ResultValue resultValue, final Batch batch)
    {
        final boolean success;

        if (resultValue instanceof Summary) {
            final Summary summary = (Summary) resultValue;

            success = addInputToResult(summary.getStopValue().get(), summary);
        } else {
            success = false;
        }

        return success;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        final Optional<PointRelation> relation = getRelation();

        if (relation.isPresent()) {
            final Params relationParams = relation.get().getParams();

            _runningInterval = relationParams
                .getBoolean(RUNNING_INTERVAL_PARAM, false);

            final KeyedGroups processorProperties = metadata
                .getPropertiesGroup(
                    ProcessorServiceAppImpl.PROCESSOR_PROPERTIES);
            final String defaultPositionName = _runningInterval
                ? _ResultPosition.BEGINNING
                    .name(): _ResultPosition.NEXT.name();
            final String positionName = relationParams
                .getString(
                    RESULT_POSITION_PARAM,
                    processorProperties
                        .getString(
                                RESULT_POSITION_PROPERTY,
                                        Optional.of(defaultPositionName)))
                .orElse(null);
            final boolean defaultReverseInterval = processorProperties
                .getBoolean(REVERSE_INTERVAL_PROPERTY);

            try {
                _resultPosition = Enum
                    .valueOf(
                        _ResultPosition.class,
                        positionName.toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException exception) {
                getThisLogger()
                    .warn(ProcessorMessages.RESULT_POSITION, positionName);
                _resultPosition = _runningInterval
                        ? _ResultPosition.BEGINNING: _ResultPosition.NEXT;
            }

            _reverseInterval = relationParams
                .getBoolean(
                    REVERSE_INTERVAL_PARAM,
                    defaultReverseInterval && !_runningInterval);
            _steps = Math.max(relationParams.getInt(STEPS_PARAM, 1), 1);
        } else {
            _resultPosition = _ResultPosition.NEXT;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void trigger(final PointValue noticeValue, final Batch batch)
    {
        final Point summarizesPoint = getInputPoint();
        BatchValuesQuery.Builder batchValuesQueryBuilder;

        // Triggers when the notice stops the interval.

        batchValuesQueryBuilder = BatchValuesQuery
            .newBuilder()
            .setPoint(Optional.of(summarizesPoint));

        if (summarizesPoint.isSynced()) {
            batchValuesQueryBuilder
                .setAt(_getStartStamp(noticeValue.getStamp()));
        } else {
            batchValuesQueryBuilder.setBefore(noticeValue.getStamp());
        }

        final PointValue startValue = batch
            .getPointValue(batchValuesQueryBuilder.build());
        DateTime triggerStamp;

        triggerStamp = getTriggerStamp(startValue, noticeValue);

        if (triggerStamp != null) {
            final Summary summary = (Summary) batch
                .setUpResultValue(noticeValue, triggerStamp, this);

            if (summary != null) {
                summary.setStartValue(startValue);
                summary.setStopValue(noticeValue);
            }
        }

        // Triggers when the notice starts the interval.

        batchValuesQueryBuilder = BatchValuesQuery
            .newBuilder()
            .setPoint(Optional.of(summarizesPoint));

        if (summarizesPoint.isSynced()) {
            batchValuesQueryBuilder
                .setAt(_getStopStamp(noticeValue.getStamp()));
        } else {
            batchValuesQueryBuilder.setAfter(noticeValue.getStamp());
        }

        final PointValue stopValue = batch
            .getPointValue(batchValuesQueryBuilder.build());

        if (isRunningInterval()) {
            if (stopValue.isAbsent()) {
                final Summary summary = (Summary) batch
                    .setUpResultValue(
                        noticeValue,
                        noticeValue.getStamp(),
                        this);

                if (summary != null) {
                    final Optional<Sync> summarizesSync = summarizesPoint
                        .getSync();
                    final PointValue phantomValue = new PointValue(
                        summarizesPoint,
                        summarizesSync.isPresent()? summarizesSync
                            .get()
                            .getNextStamp(
                                noticeValue.getStamp()): Optional
                                        .of(DateTime.END_OF_TIME),
                        null,
                        null);

                    summary.setStartValue(noticeValue);
                    summary.setStopValue(phantomValue);
                }
            }
        } else {
            triggerStamp = getTriggerStamp(noticeValue, stopValue);

            if (triggerStamp != null) {
                final Summary summary = (Summary) batch
                    .setUpResultValue(stopValue, triggerStamp, this);

                if (summary != null) {
                    summary.setStartValue(noticeValue);
                    summary.setStopValue(stopValue);
                }
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean doValidate()
    {
        if (!validateTransform(SummarizerTransform.class)) {
            return false;
        }

        if (isReverseInterval()) {
            if ((_resultPosition == _ResultPosition.BEGINNING)
                    || (_resultPosition == _ResultPosition.END)) {
                getThisLogger()
                    .error(ProcessorMessages.REVERSE_INTERVAL_POSITION);

                return false;
            }
        }

        if (getSteps() > 1) {
            if (_resultPosition != _ResultPosition.NEXT) {
                getThisLogger().error(ProcessorMessages.STEPS_POSITION);

                return false;
            }

            if (!getInputPoint().isSynced()) {
                getThisLogger().error(ProcessorMessages.STEPS_SYNCED);

                return false;
            }

            if (isStartStop()) {
                getThisLogger()
                    .error(
                        ProcessorMessages.START_STOP_INCOMPATIBLE,
                        STEPS_PARAM);

                return false;
            }
        }

        if (isRunningInterval()) {
            if (_resultPosition != _ResultPosition.BEGINNING) {
                getThisLogger()
                    .error(ProcessorMessages.RUNNING_INTERVAL_POSITION);

                return false;
            }
        }

        if (hasSelectSyncPosition()) {
            getThisLogger()
                .error(
                    ProcessorMessages.BEHAVIOR_INCOMPATIBLE,
                    getName(),
                    SELECT_SYNC_POSITION_PARAM);

            return false;
        }

        if (hasSinceSyncPosition()) {
            getThisLogger()
                .error(
                    ProcessorMessages.BEHAVIOR_INCOMPATIBLE,
                    getName(),
                    SINCE_SYNC_POSITION_PARAM);

            return false;
        }

        if (hasSinceResultSyncPosition()) {
            getThisLogger()
                .error(
                    ProcessorMessages.BEHAVIOR_INCOMPATIBLE,
                    getName(),
                    SINCE_RESULT_SYNC_POSITION_PARAM);

            return false;
        }

        if (getRelationSyncEntity().isPresent()) {
            getThisLogger()
                .error(ProcessorMessages.BEHAVIOR_INCOMPATIBLE_SYNC, getName());

            return false;
        }

        return validateNoLoop() | validateNotSynchronized();
    }

    /**
     * Gets the result position.
     *
     * @return The result position.
     */
    @Nonnull
    @CheckReturnValue
    String getResultPosition()
    {
        return _resultPosition.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Gets the number of steps in interval.
     *
     * @return The number of steps.
     */
    @CheckReturnValue
    int getSteps()
    {
        return _steps;
    }

    /**
     * Gets a timestamp for triggering a result computation.
     *
     * @param startValue The start value.
     * @param stopValue The stop value acting as potential trigger.
     *
     * @return The timestamp value if this should trigger, or null.
     */
    @Nullable
    @CheckReturnValue
    DateTime getTriggerStamp(
            @Nonnull final PointValue startValue,
            @Nonnull final PointValue stopValue)
    {
        if (startValue.isAbsent() || stopValue.isAbsent()) {
            return null;
        }

        if (isStartStop()) {
            Boolean value;

            value = (Boolean) startValue.normalized().getValue();

            if ((value == null) || !value.booleanValue()) {
                return null;    // Start value must be present and true;
            }

            value = (Boolean) stopValue.normalized().getValue();

            if ((value == null) || value.booleanValue()) {
                return null;    // Stop value must be present and false.
            }
        } else if (stopValue.getValue() == null) {
            return null;    // A null stop value does not trigger.
        }

        DateTime triggerStamp = null;

        switch (_resultPosition) {
            case BEGINNING: {
                triggerStamp = startValue.getStamp();

                break;
            }
            case MIDDLE: {
                triggerStamp = DateTime
                    .fromRaw(
                        (startValue.getStamp().toRaw()
                         + stopValue.getStamp().toRaw()) / 2);

                break;
            }
            case END: {
                triggerStamp = stopValue.getStamp().before();

                break;
            }
            case NEXT: {
                triggerStamp = stopValue.getStamp();

                break;
            }
            default: {
                Require.failure();
            }
        }

        // When the result has a sync, tries to sync within the interval.
        if (getResultSync().isPresent()
                && !getResultSync().get().isInSync(triggerStamp)) {
            DateTime previousStamp = getResultSync()
                .get()
                .getPreviousStamp(triggerStamp)
                .orElse(null);
            DateTime nextStamp = getResultSync()
                .get()
                .getNextStamp(triggerStamp)
                .orElse(null);

            if (isReverseInterval()) {
                if ((previousStamp != null)
                        && previousStamp.isNotAfter(startValue.getStamp())) {
                    previousStamp = null;
                }

                if ((nextStamp != null)
                        && nextStamp.isAfter(stopValue.getStamp())) {
                    nextStamp = null;
                }
            } else {
                if ((previousStamp != null)
                        && previousStamp.isBefore(startValue.getStamp())) {
                    previousStamp = null;
                }

                if ((nextStamp != null)
                        && nextStamp.isNotBefore(stopValue.getStamp())) {
                    nextStamp = null;
                }
            }

            if (previousStamp == null) {
                triggerStamp = nextStamp;
            } else if (nextStamp == null) {
                triggerStamp = previousStamp;
            } else {
                final int comparison = triggerStamp
                    .sub(previousStamp)
                    .compareTo(nextStamp.sub(triggerStamp));

                if (comparison < 0) {
                    triggerStamp = previousStamp;
                } else if (comparison > 0) {
                    triggerStamp = nextStamp;
                } else {
                    triggerStamp = isReverseInterval()
                            ? previousStamp: nextStamp;
                }
            }
        }

        return triggerStamp;
    }

    /**
     * Asks if the interval is reversed.
     *
     * @return A true value if it is reversed.
     */
    @CheckReturnValue
    boolean isReverseInterval()
    {
        return _reverseInterval;
    }

    /**
     * Asks if the interval is running.
     *
     * @return A true value if it is running.
     */
    @CheckReturnValue
    boolean isRunningInterval()
    {
        return _runningInterval;
    }

    /**
     * Sets up a summary
     *
     * @param summary The summary.
     * @param batch The current batch context.
     *
     * @return A true value on success.
     */
    boolean setUpSummary(final Summary summary, final Batch batch)
    {
        if (!summary.getStartValue().isPresent()) {
            final Point summarizesPoint = getInputPoint();
            final BatchValuesQuery.Builder batchValuesQueryStartBuilder =
                BatchValuesQuery
                    .newBuilder()
                    .setPoint(Optional.of(summarizesPoint));
            final BatchValuesQuery.Builder batchValuesQueryStopBuilder =
                BatchValuesQuery
                    .newBuilder()
                    .setPoint(Optional.of(summarizesPoint));

            switch (_resultPosition) {
                case BEGINNING: {
                    batchValuesQueryStartBuilder.setAt(summary.getStamp());
                    summary
                        .setStartValue(
                            batch
                                .getPointValue(
                                        batchValuesQueryStartBuilder.build()));

                    batchValuesQueryStopBuilder.setAfter(summary.getStamp());
                    summary
                        .setStopValue(
                            batch
                                .getPointValue(
                                        batchValuesQueryStopBuilder.build()));

                    break;
                }
                case MIDDLE:
                case END: {
                    batchValuesQueryStartBuilder.setBefore(summary.getStamp());
                    summary
                        .setStartValue(
                            batch
                                .getPointValue(
                                        batchValuesQueryStartBuilder.build()));

                    batchValuesQueryStopBuilder.setAfter(summary.getStamp());
                    summary
                        .setStopValue(
                            batch
                                .getPointValue(
                                        batchValuesQueryStopBuilder.build()));

                    break;
                }
                case NEXT: {
                    batchValuesQueryStartBuilder.setBefore(summary.getStamp());
                    summary
                        .setStartValue(
                            batch
                                .getPointValue(
                                        batchValuesQueryStartBuilder.build()));

                    batchValuesQueryStopBuilder.setAt(summary.getStamp());
                    summary
                        .setStopValue(
                            batch
                                .getPointValue(
                                        batchValuesQueryStopBuilder.build()));

                    break;
                }
                default: {
                    Require.failure();
                }
            }

            if (!summary.getStartValue().isPresent()) {
                return false;
            }
        }

        return summary.getStopValue().isPresent();
    }

    private DateTime _getStartStamp(final DateTime stopStamp)
    {
        final Sync sync = getInputPoint().getSync().get();

        return sync.getPreviousStamp(stopStamp, getSteps()).orElse(null);
    }

    private DateTime _getStopStamp(final DateTime startStamp)
    {
        final Sync sync = getInputPoint().getSync().get();

        return sync.getNextStamp(startStamp, getSteps()).orElse(null);
    }

    /** Specifies how to stamp the result. */
    public static final String RESULT_POSITION_PARAM = "ResultPosition";

    /** Specifies the default result position. */
    public static final String RESULT_POSITION_PROPERTY =
        "summarizer.result.position";

    /** Reverses the interval: "open to closed" vs "closed to open". */
    public static final String REVERSE_INTERVAL_PARAM = "ReverseInterval";

    /** Reverses the intervals by default. */
    public static final String REVERSE_INTERVAL_PROPERTY =
        "summarizer.interval.reverse";

    /** Triggers running interval results. */
    public static final String RUNNING_INTERVAL_PARAM = "RunningInterval";

    /** The number of steps for one interval. */
    public static final String STEPS_PARAM = "Steps";

    private _ResultPosition _resultPosition;
    private boolean _reverseInterval;
    private boolean _runningInterval;
    private int _steps;

    /**
     * Result position.
     */
    private enum _ResultPosition
    {
        BEGINNING,
        MIDDLE,
        END,
        NEXT;
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
