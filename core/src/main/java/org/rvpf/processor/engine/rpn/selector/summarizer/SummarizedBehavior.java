/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SummarizedBehavior.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.processor.engine.rpn.selector.summarizer;

import java.util.Collection;
import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointInput;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.BatchValuesQuery;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.selector.SelectedBehavior;
import org.rvpf.service.ServiceMessages;

/**
 * Summarized behavior.
 */
public final class SummarizedBehavior
    extends SelectedBehavior
{
    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (!super.equals(other)) {
            return false;
        }

        final SummarizedBehavior otherBehavior = (SummarizedBehavior) other;

        return (_fetchPreviousEndValue == otherBehavior._fetchPreviousEndValue)
               && (_fetchPreviousValue == otherBehavior._fetchPreviousValue)
               && (_includeNullSteps == otherBehavior._includeNullSteps);
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
        return _summarizesBehavior.newResultValue(stamp);
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
                return false;    // Lets the Summarizes Behavior get its values.
            }
            case 2: {
                if (resultValue instanceof Summary) {
                    final Summary summary = (Summary) resultValue;

                    if (!_summarizesBehavior.setUpSummary(summary, batch)) {
                        return true;
                    }

                    final Point summarizedPoint = getInputPoint();
                    final StoreValuesQuery.Builder storeValuesQueryBuilder =
                        StoreValuesQuery
                            .newBuilder()
                            .setPoint(summarizedPoint);

                    storeValuesQueryBuilder
                        .setInterval(
                            _getSelectInterval(
                                summary.getStartStamp(),
                                summary.getStopStamp()));
                    storeValuesQueryBuilder
                        .setSync(getRelationSync().orElse(null));
                    storeValuesQueryBuilder
                        .setInterpolated(isInputInterpolated());
                    storeValuesQueryBuilder
                        .setExtrapolated(isInputExtrapolated());
                    batch.addStoreValuesQuery(storeValuesQueryBuilder.build());

                    if (_fetchPreviousEndValue || _fetchPreviousValue) {
                        storeValuesQueryBuilder.clear();

                        if (_summarizesBehavior.isReverseInterval()) {
                            storeValuesQueryBuilder
                                .setNotAfter(summary.getStartStamp().get());
                        } else {
                            storeValuesQueryBuilder
                                .setBefore(summary.getStartStamp().get());
                        }

                        storeValuesQueryBuilder.setNotNull(!_includeNullSteps);
                        storeValuesQueryBuilder
                            .setSync(getRelationSync().orElse(null));
                        storeValuesQueryBuilder
                            .setInterpolated(isInputInterpolated());
                        storeValuesQueryBuilder
                            .setExtrapolated(isInputExtrapolated());
                        batch
                            .addStoreValuesQuery(
                                storeValuesQueryBuilder.build());

                        if (_fetchPreviousEndValue) {
                            storeValuesQueryBuilder
                                .setPoint(_getIntervalPoint(summary))
                                .clear();
                            storeValuesQueryBuilder
                                .setBefore(summary.getStartStamp().get());
                            batch
                                .addStoreValuesQuery(
                                    storeValuesQueryBuilder.build());
                        }
                    }
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
        final boolean done;

        switch (batch.getLookUpPass()) {
            case 1: {
                done = _prepareTriggerPass1(noticeValue, batch);

                break;
            }
            case 2: {
                done = _prepareTriggerPass2(noticeValue, batch);

                break;
            }
            default: {
                done = true;

                break;
            }
        }

        return done;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean select(final ResultValue resultValue, final Batch batch)
    {
        if (resultValue instanceof Summary) {
            final Summary summary = (Summary) resultValue;
            final Optional<DateTime> startStamp = summary.getStartStamp();
            final Optional<DateTime> stopStamp = summary.getStopStamp();

            if (!startStamp.isPresent() || !stopStamp.isPresent()) {
                getThisLogger()
                    .warn(ProcessorMessages.UNEXPECTED_TRIGGER, resultValue);

                return false;
            }

            if (_fetchPreviousEndValue || _fetchPreviousValue) {
                BatchValuesQuery.Builder batchValuesQueryBuilder =
                    BatchValuesQuery
                        .newBuilder()
                        .setPoint(Optional.of(getInputPoint()));

                if (_summarizesBehavior.isReverseInterval()) {
                    batchValuesQueryBuilder.setBefore(startStamp.get());
                } else {
                    batchValuesQueryBuilder.setNotAfter(startStamp.get());
                }

                batchValuesQueryBuilder.setNotNull(!_includeNullSteps);
                batchValuesQueryBuilder.setSync(getInputSync());
                batchValuesQueryBuilder.setInterpolated(isInputInterpolated());
                batchValuesQueryBuilder.setExtrapolated(isInputExtrapolated());

                final PointValue inputValue = batch
                    .getPointValue(batchValuesQueryBuilder.build());

                if (inputValue.isAbsent()) {
                    return false;
                }

                if (_fetchPreviousEndValue) {
                    batchValuesQueryBuilder = BatchValuesQuery
                        .newBuilder()
                        .setPoint(
                            Optional.ofNullable(_getIntervalPoint(summary)));
                    batchValuesQueryBuilder.setBefore(startStamp.get());

                    final PointValue previousStartValue = batch
                        .getPointValue(batchValuesQueryBuilder.build());
                    final DateTime limit = previousStartValue
                        .isAbsent()? DateTime.END_OF_TIME: (_summarizesBehavior
                            .isReverseInterval()? previousStartValue
                                .getStamp()
                                .after(): previousStartValue.getStamp());

                    if (inputValue.getStamp().isBefore(limit)) {
                        return false;
                    }
                }

                if (!addInputToResult(inputValue, resultValue)) {
                    return false;
                }
            }

            final BatchValuesQuery.Builder batchValuesQueryBuilder =
                BatchValuesQuery
                    .newBuilder()
                    .setPoint(Optional.of(getInputPoint()));

            batchValuesQueryBuilder
                .setInterval(_getSelectInterval(startStamp, stopStamp));
            batchValuesQueryBuilder.setNotNull(!_includeNullSteps);
            batchValuesQueryBuilder.setSync(getInputSync());
            batchValuesQueryBuilder.setInterpolated(isInputInterpolated());
            batchValuesQueryBuilder.setExtrapolated(isInputExtrapolated());

            final Collection<PointValue> inputValues = batch
                .getPointValues(batchValuesQueryBuilder.build());

            for (final PointValue inputValue: inputValues) {
                if (!addInputToResult(inputValue, resultValue)) {
                    return false;
                }
            }
        } else {
            getThisLogger()
                .warn(ProcessorMessages.UNEXPECTED_RESULT_SET_UP, resultValue);

            return false;
        }

        return true;
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
            final Optional<PointInput> summarizesRelation = getInput(
                SummarizesBehavior.class,
                false);

            if (!summarizesRelation.isPresent()) {
                return false;
            }

            _summarizesBehavior = (SummarizesBehavior) summarizesRelation
                .get()
                .getPrimaryBehavior()
                .orElse(null);

            final Params relationParams = relation.get().getParams();

            _includeNullSteps = relationParams
                .getBoolean(INCLUDE_NULLS_PARAM, false);
            _fetchPreviousEndValue = relationParams
                .getBoolean(FETCH_PREVIOUS_END_VALUE_PARAM, false);
            _fetchPreviousValue = relationParams
                .getBoolean(FETCH_PREVIOUS_VALUE_PARAM, false);
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void trigger(final PointValue noticeValue, final Batch batch)
    {
        final DateTime noticeStamp = noticeValue.getStamp();
        final Point summarizesPoint = _summarizesBehavior.getInputPoint();
        BatchValuesQuery.Builder batchValuesQueryBuilder;
        PointValue nextSummarizesValue;

        if (_summarizesBehavior.isReverseInterval()) {
            batchValuesQueryBuilder = BatchValuesQuery
                .newBuilder()
                .setPoint(Optional.of(summarizesPoint));
            batchValuesQueryBuilder.setNotBefore(noticeStamp);
            nextSummarizesValue = batch
                .getPointValue(batchValuesQueryBuilder.build());
        } else {
            batchValuesQueryBuilder = BatchValuesQuery
                .newBuilder()
                .setPoint(Optional.of(summarizesPoint));
            batchValuesQueryBuilder.setAfter(noticeStamp);
            nextSummarizesValue = batch
                .getPointValue(batchValuesQueryBuilder.build());
        }

        if (nextSummarizesValue.isAbsent()
                && !_summarizesBehavior.isRunningInterval()) {
            return;
        }

        final TimeInterval interval = _getTriggerInterval(noticeStamp);

        if (interval != null) {
            final Collection<PointValue> summarizesValues;

            batchValuesQueryBuilder = BatchValuesQuery
                .newBuilder()
                .setPoint(Optional.of(summarizesPoint));
            batchValuesQueryBuilder.setInterval(interval);
            summarizesValues = batch
                .getPointValues(batchValuesQueryBuilder.build());

            for (final PointValue summarizesValue: summarizesValues) {
                _setUpResult(summarizesValue, batch);
            }
        } else {
            if (nextSummarizesValue.isAbsent()) {    // Running interval.
                final PointValue previousSummarizes;

                batchValuesQueryBuilder = BatchValuesQuery
                    .newBuilder()
                    .setPoint(Optional.of(summarizesPoint));
                batchValuesQueryBuilder.setNotAfter(noticeValue.getStamp());
                previousSummarizes = batch
                    .getPointValue(batchValuesQueryBuilder.build());

                final DateTime resultStamp = (previousSummarizes
                    .getValue() != null)? previousSummarizes.getStamp(): null;

                if (resultStamp != null) {
                    final Optional<Sync> sync = summarizesPoint.getSync();
                    final DateTime endStamp = sync
                        .isPresent()? sync
                            .get()
                            .getNextStamp(previousSummarizes.getStamp())
                            .get(): DateTime.END_OF_TIME;

                    if (noticeStamp.isBefore(endStamp)) {
                        final Summary summary = (Summary) batch
                            .setUpResultValue(resultStamp, this);

                        if (summary != null) {
                            summary.setStartValue(previousSummarizes);
                            summary
                                .setStopValue(
                                    new PointValue(
                                        summarizesPoint,
                                        Optional.of(endStamp),
                                        null,
                                        null));
                        }
                    }
                }
            } else {
                _setUpResult(nextSummarizesValue, batch);
            }

            if (_fetchPreviousEndValue || _fetchPreviousValue) {
                final PointValue nextNoticeValue;

                batchValuesQueryBuilder = BatchValuesQuery
                    .newBuilder()
                    .setPoint(Optional.of(getInputPoint()));
                batchValuesQueryBuilder.setAfter(noticeStamp);
                nextNoticeValue = batch
                    .getPointValue(batchValuesQueryBuilder.build());

                boolean nextNoticePresent = nextNoticeValue.isPresent();

                if (_summarizesBehavior.isReverseInterval()) {
                    batchValuesQueryBuilder = BatchValuesQuery
                        .newBuilder()
                        .setPoint(Optional.of(summarizesPoint));
                    batchValuesQueryBuilder.setNotBefore(noticeStamp);
                    nextSummarizesValue = batch
                        .getPointValue(batchValuesQueryBuilder.build());

                    if (nextNoticePresent
                            && nextSummarizesValue.isPresent()
                            && nextNoticeValue.getStamp().isAfter(
                                nextSummarizesValue.getStamp())) {
                        nextNoticePresent = false;
                    }
                } else {
                    batchValuesQueryBuilder = BatchValuesQuery
                        .newBuilder()
                        .setPoint(Optional.of(summarizesPoint));
                    batchValuesQueryBuilder.setAfter(noticeStamp);
                    nextSummarizesValue = batch
                        .getPointValue(batchValuesQueryBuilder.build());

                    if (nextNoticePresent
                            && nextSummarizesValue.isPresent()
                            && nextNoticeValue.getStamp().isNotBefore(
                                nextSummarizesValue.getStamp())) {
                        nextNoticePresent = false;
                    }
                }

                if (_fetchPreviousEndValue) {
                    if (nextSummarizesValue.isPresent()) {
                        _setUpResult(nextSummarizesValue, batch);

                        if (!nextNoticePresent) {
                            batchValuesQueryBuilder = BatchValuesQuery
                                .newBuilder()
                                .setPoint(Optional.of(summarizesPoint));
                            batchValuesQueryBuilder
                                .setAfter(nextSummarizesValue.getStamp());
                            nextSummarizesValue = batch
                                .getPointValue(batchValuesQueryBuilder.build());

                            if (nextSummarizesValue.isPresent()) {
                                _setUpResult(nextSummarizesValue, batch);
                            }
                        }
                    }
                } else if (nextSummarizesValue.isPresent()) {
                    final TimeInterval.Builder intervalBuilder = TimeInterval
                        .newBuilder();

                    if (_summarizesBehavior.isReverseInterval()) {
                        intervalBuilder
                            .setAfter(nextSummarizesValue.getStamp());
                    } else {
                        intervalBuilder
                            .setNotBefore(nextSummarizesValue.getStamp());
                    }

                    intervalBuilder
                        .setBefore(
                            nextNoticePresent
                            ? nextNoticeValue.getStamp(): DateTime.END_OF_TIME);

                    if (intervalBuilder.isValid()) {
                        final Collection<PointValue> pointValues;

                        batchValuesQueryBuilder = BatchValuesQuery
                            .newBuilder()
                            .setPoint(Optional.of(getResultPoint()));
                        batchValuesQueryBuilder.setInterval(intervalBuilder);
                        pointValues = batch
                            .getPointValues(batchValuesQueryBuilder.build());

                        for (final PointValue pointValue: pointValues) {
                            batch
                                .setUpResultValue(
                                    super
                                        .newResultValue(
                                                Optional
                                                        .ofNullable(
                                                                pointValue.getStamp())),
                                    this);
                        }
                    }

                    if (nextNoticePresent) {
                        final PointValue pointValue;

                        batchValuesQueryBuilder = BatchValuesQuery
                            .newBuilder()
                            .setPoint(Optional.of(getResultPoint()));
                        batchValuesQueryBuilder
                            .setNotBefore(nextNoticeValue.getStamp());
                        pointValue = batch
                            .getPointValue(batchValuesQueryBuilder.build());

                        if (pointValue.isPresent()) {
                            batch
                                .setUpResultValue(
                                    super
                                        .newResultValue(
                                                Optional
                                                        .ofNullable(
                                                                pointValue.getStamp())),
                                    this);
                        }
                    }
                }
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean doValidate()
    {
        if (getResultPoint().getInputs().size() < 2) {
            getThisLogger()
                .error(ProcessorMessages.BEHAVIOR_GE_2_INPUTS, getName());

            return false;
        }

        if (getRelations(SummarizesBehavior.class).size() != 1) {
            getThisLogger()
                .error(ProcessorMessages.BEHAVIOR_NEEDS_SELECTOR, getName());

            return false;
        }

        if (hasSelectSyncPosition()) {
            getThisLogger()
                .error(
                    ProcessorMessages.BEHAVIOR_INCOMPATIBLE,
                    getName(),
                    SELECT_SYNC_POSITION_PARAM);

            return false;
        }

        if (hasAfterResultSyncPosition()) {
            getThisLogger()
                .error(
                    ProcessorMessages.BEHAVIOR_INCOMPATIBLE,
                    getName(),
                    AFTER_RESULT_SYNC_POSITION_PARAM);

            return false;
        }

        if (_fetchPreviousEndValue || _fetchPreviousValue) {
            if (hasSinceSyncPosition()) {
                getThisLogger()
                    .error(
                        ServiceMessages.PARAM_CONFLICT,
                        FETCH_PREVIOUS_END_VALUE_PARAM,
                        SINCE_SYNC_POSITION_PARAM);

                return false;
            }

            if (hasSinceResultSyncPosition()) {
                getThisLogger()
                    .error(
                        ServiceMessages.PARAM_CONFLICT,
                        FETCH_PREVIOUS_END_VALUE_PARAM,
                        SINCE_RESULT_SYNC_POSITION_PARAM);

                return false;
            }

            if (_fetchPreviousEndValue && _fetchPreviousValue) {
                getThisLogger()
                    .error(
                        ServiceMessages.PARAM_CONFLICT,
                        FETCH_PREVIOUS_END_VALUE_PARAM,
                        FETCH_PREVIOUS_VALUE_PARAM);

                return false;
            }
        }

        if (_fetchPreviousEndValue && _summarizesBehavior.isStartStop()) {
            getThisLogger()
                .error(
                    ProcessorMessages.START_STOP_INCOMPATIBLE,
                    FETCH_PREVIOUS_END_VALUE_PARAM);

            return false;
        }

        return validateNoLoop() | validateNotSynchronized();
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean supportsExtrapolated()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean supportsInterpolated()
    {
        return true;
    }

    private static Point _getIntervalPoint(final Summary summary)
    {
        final Optional<PointValue> startValue = summary.getStartValue();

        return startValue.isPresent()? startValue.get().getPoint().get(): null;
    }

    private TimeInterval _getSelectInterval(
            Optional<DateTime> startStamp,
            final Optional<DateTime> stopStamp)
    {
        final TimeInterval.Builder intervalBuilder = TimeInterval.newBuilder();

        if (hasSinceSyncPosition()) {
            startStamp = getSyncStamp(
                getSinceSyncPosition(),
                stopStamp.get(),
                false);
        } else if (hasSinceResultSyncPosition()) {
            startStamp = getResultSyncStamp(
                getSinceResultSyncPosition(),
                stopStamp.get(),
                false);
        }

        if (_summarizesBehavior.isReverseInterval()) {
            if (hasSinceSyncPosition()) {
                intervalBuilder.setNotBefore(startStamp.get());
            } else {
                intervalBuilder.setAfter(startStamp.get());
            }

            intervalBuilder.setNotAfter(stopStamp.get());
        } else {
            intervalBuilder.setNotBefore(startStamp.get());
            intervalBuilder.setBefore(stopStamp.get());
        }

        return intervalBuilder.build();
    }

    private TimeInterval _getTriggerInterval(final DateTime noticeStamp)
    {
        final DateTime startStamp;
        final DateTime stopStamp;

        if (hasSinceSyncPosition()) {
            startStamp = noticeStamp;
            stopStamp = getSyncStamp(getSinceSyncPosition(), startStamp, true)
                .orElse(null);
        } else if (hasSinceResultSyncPosition()) {
            startStamp = getResultSync()
                .get()
                .isInSync(
                    noticeStamp)? noticeStamp: getResultSyncStamp(
                        -1,
                        noticeStamp,
                        true)
                        .get();
            stopStamp = getResultSyncStamp(
                getSinceResultSyncPosition(),
                startStamp,
                true)
                .get();
        } else {
            return null;
        }

        final TimeInterval.Builder intervalBuilder = TimeInterval.newBuilder();

        if (_summarizesBehavior.isReverseInterval()) {
            intervalBuilder.setAfter(startStamp);
            intervalBuilder.setNotAfter(stopStamp);
        } else {
            intervalBuilder.setNotBefore(startStamp);
            intervalBuilder.setBefore(stopStamp);
        }

        return intervalBuilder.build();
    }

    private boolean _prepareTriggerPass1(
            final PointValue noticeValue,
            final Batch batch)
    {
        final DateTime noticeStamp = noticeValue.getStamp();
        final StoreValuesQuery.Builder storeValuesQueryBuilder =
            StoreValuesQuery
                .newBuilder()
                .setPoint(_summarizesBehavior.getInputPoint());

        if (_summarizesBehavior.isReverseInterval()) {
            storeValuesQueryBuilder.setNotBefore(noticeStamp);
        } else {
            storeValuesQueryBuilder.setAfter(noticeStamp);
        }

        batch.addStoreValuesQuery(storeValuesQueryBuilder.build());

        if (_fetchPreviousEndValue) {
            storeValuesQueryBuilder.setPoint(getInputPoint()).clear();
            storeValuesQueryBuilder.setAfter(noticeStamp);
            batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
        }

        return false;
    }

    private boolean _prepareTriggerPass2(
            final PointValue noticeValue,
            final Batch batch)
    {
        final Point summarizesPoint = _summarizesBehavior.getInputPoint();
        final DateTime noticeStamp = noticeValue.getStamp();
        final TimeInterval interval;
        final boolean reverseInterval = _summarizesBehavior.isReverseInterval();
        BatchValuesQuery.Builder batchValuesQueryBuilder;
        PointValue nextSummarizesValue;

        batchValuesQueryBuilder = BatchValuesQuery
            .newBuilder()
            .setPoint(Optional.of(summarizesPoint));

        if (reverseInterval) {
            batchValuesQueryBuilder.setNotBefore(noticeStamp);
        } else {
            batchValuesQueryBuilder.setAfter(noticeStamp);
        }

        nextSummarizesValue = batch
            .getPointValue(batchValuesQueryBuilder.build());

        final StoreValuesQuery.Builder storeValuesQueryBuilder =
            StoreValuesQuery
                .newBuilder()
                .setPoint(summarizesPoint);

        if (nextSummarizesValue.isAbsent()) {
            if (_summarizesBehavior.isRunningInterval()) {
                storeValuesQueryBuilder.setNotAfter(noticeStamp);
                batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
            }

            return true;
        }

        interval = _getTriggerInterval(noticeStamp);

        if (interval != null) {
            storeValuesQueryBuilder.setInterval(interval);
            batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
        } else {
            if (reverseInterval) {
                storeValuesQueryBuilder.setBefore(noticeStamp);
            } else {
                storeValuesQueryBuilder.setNotAfter(noticeStamp);
            }

            batch.addStoreValuesQuery(storeValuesQueryBuilder.build());

            if (_fetchPreviousEndValue || _fetchPreviousValue) {
                final PointValue nextNoticeValue;

                batchValuesQueryBuilder = BatchValuesQuery
                    .newBuilder()
                    .setPoint(Optional.of(getInputPoint()));
                batchValuesQueryBuilder.setAfter(noticeStamp);
                nextNoticeValue = batch
                    .getPointValue(batchValuesQueryBuilder.build());

                boolean nextNoticePresent = nextNoticeValue.isPresent();

                if (reverseInterval) {
                    batchValuesQueryBuilder = BatchValuesQuery
                        .newBuilder()
                        .setPoint(Optional.of(summarizesPoint));
                    batchValuesQueryBuilder.setNotBefore(noticeStamp);
                    nextSummarizesValue = batch
                        .getPointValue(batchValuesQueryBuilder.build());

                    if (nextNoticePresent
                            && nextSummarizesValue.isPresent()
                            && nextNoticeValue.getStamp().isAfter(
                                nextSummarizesValue.getStamp())) {
                        nextNoticePresent = false;
                    }
                } else {
                    batchValuesQueryBuilder = BatchValuesQuery
                        .newBuilder()
                        .setPoint(Optional.of(summarizesPoint));
                    batchValuesQueryBuilder.setAfter(noticeStamp);
                    nextSummarizesValue = batch
                        .getPointValue(batchValuesQueryBuilder.build());

                    if (nextNoticePresent
                            && nextSummarizesValue.isPresent()
                            && nextNoticeValue.getStamp().isNotBefore(
                                nextSummarizesValue.getStamp())) {
                        nextNoticePresent = false;
                    }
                }

                if (_fetchPreviousEndValue) {
                    if (!nextNoticePresent && nextSummarizesValue.isPresent()) {
                        storeValuesQueryBuilder.clear();
                        storeValuesQueryBuilder
                            .setAfter(nextSummarizesValue.getStamp());
                        batch
                            .addStoreValuesQuery(
                                storeValuesQueryBuilder.build());
                    }
                } else if (nextSummarizesValue.isPresent()) {
                    storeValuesQueryBuilder.setPoint(getResultPoint()).clear();

                    if (reverseInterval) {
                        storeValuesQueryBuilder
                            .setAfter(nextSummarizesValue.getStamp());
                    } else {
                        storeValuesQueryBuilder
                            .setNotBefore(nextSummarizesValue.getStamp());
                    }

                    storeValuesQueryBuilder
                        .setBefore(
                            nextNoticePresent
                            ? nextNoticeValue.getStamp(): DateTime.END_OF_TIME);

                    if (storeValuesQueryBuilder.isIntervalValid()) {
                        batch
                            .addStoreValuesQuery(
                                storeValuesQueryBuilder.build());
                    }

                    if (nextNoticePresent) {
                        storeValuesQueryBuilder
                            .setPoint(getResultPoint())
                            .clear();
                        storeValuesQueryBuilder
                            .setNotBefore(nextNoticeValue.getStamp());
                        batch
                            .addStoreValuesQuery(
                                storeValuesQueryBuilder.build());
                    }
                }
            }
        }

        return true;
    }

    private void _setUpResult(
            final PointValue nextSummarizes,
            final Batch batch)
    {
        final BatchValuesQuery.Builder batchValuesQueryBuilder =
            BatchValuesQuery
                .newBuilder()
                .setPoint(nextSummarizes.getPoint());

        batchValuesQueryBuilder.setBefore(nextSummarizes.getStamp());

        final PointValue previousSummarizes = batch
            .getPointValue(batchValuesQueryBuilder.build());
        final DateTime triggerStamp = _summarizesBehavior
            .getTriggerStamp(previousSummarizes, nextSummarizes);
        final Summary summary;

        if (triggerStamp != null) {
            summary = (Summary) batch
                .setUpResultValue(nextSummarizes, triggerStamp, this);

            if (summary != null) {
                summary.setStartValue(previousSummarizes);
                summary.setStopValue(nextSummarizes);
            }
        }
    }

    /**
     * The end value from the previous interval should be fetched and included
     * as the first value.
     */
    public static final String FETCH_PREVIOUS_END_VALUE_PARAM =
        "FetchPreviousEndValue";

    /**
     * The last value before the current interval should be fetched and
     * included as the first value.
     */
    public static final String FETCH_PREVIOUS_VALUE_PARAM =
        "FetchPreviousValue";

    /** Null values should be included. The default is to exclude them. */
    public static final String INCLUDE_NULLS_PARAM = "IncludeNulls";

    private boolean _fetchPreviousEndValue;
    private boolean _fetchPreviousValue;
    private boolean _includeNullSteps;
    private SummarizesBehavior _summarizesBehavior;
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
