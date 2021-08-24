/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PrimaryBehavior.java 4066 2019-06-07 20:23:56Z SFB $
 */

package org.rvpf.processor.behavior;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.BatchValuesQuery;
import org.rvpf.processor.ProcessorMessages;

/**
 * Primary behavior.
 *
 * <p>A primary behavior is responsible for the selection of input values for
 * computing a result.</p>
 *
 * <p>There must be one and only one primary behavior for each input (some
 * transforms may supply a default).</p>
 */
public abstract class PrimaryBehavior
    extends AbstractBehavior
{
    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (!super.equals(other)) {
            return false;
        }

        final PrimaryBehavior otherBehavior = (PrimaryBehavior) other;

        if (getRelationSyncEntity().isPresent()) {
            if (!getRelationSyncEntity()
                .equals(otherBehavior.getRelationSyncEntity())) {
                return false;
            }
        } else if (otherBehavior.getRelationSyncEntity().isPresent()) {
            return false;
        }

        if (_afterResultSyncPosition != null) {
            if (!_afterResultSyncPosition
                .equals(otherBehavior._afterResultSyncPosition)) {
                return false;
            }
        } else if (otherBehavior._afterResultSyncPosition != null) {
            return false;
        }

        if (_sinceResultSyncPosition != null) {
            if (!_sinceResultSyncPosition
                .equals(otherBehavior._sinceResultSyncPosition)) {
                return false;
            }
        } else if (otherBehavior._sinceResultSyncPosition != null) {
            return false;
        }

        if (_sinceSyncPosition != null) {
            if (!_sinceSyncPosition.equals(otherBehavior._sinceSyncPosition)) {
                return false;
            }
        } else if (otherBehavior._sinceSyncPosition != null) {
            return false;
        }

        return (getSelectSyncPosition()
                == otherBehavior.getSelectSyncPosition())
               && (isSelectPreviousValue()
                   == otherBehavior.isSelectPreviousValue());
    }

    /**
     * Gets the sync instance.
     *
     * @return The optional sync instance.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<Sync> getSync()
    {
        return Optional.ofNullable(_sync);
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
    public final boolean isPrimary()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean prepareSelect(
            final ResultValue resultValue,
            final Batch batch)
    {
        if (isSynchronized()) {
            return super.prepareSelect(resultValue, batch);
        }

        final boolean done;

        switch (batch.getLookUpPass()) {
            case 1: {
                final TimeInterval interval = _getSelectInterval(resultValue);
                final BatchValuesQuery.Builder batchValuesQueryBuilder =
                    BatchValuesQuery
                        .newBuilder()
                        .setPoint(Optional.of(getInputPoint()));

                batchValuesQueryBuilder.setAt(interval.getNotAfter().get());

                if (batch
                    .getPointValue(batchValuesQueryBuilder.build())
                    .isAbsent()) {
                    final StoreValuesQuery.Builder storeValuesQueryBuilder =
                        StoreValuesQuery
                            .newBuilder()
                            .setPoint(getInputPoint());

                    storeValuesQueryBuilder.setInterval(interval);
                    storeValuesQueryBuilder.setRows(1);
                    storeValuesQueryBuilder
                        .setSync(getRelationSync().orElse(null));
                    batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
                }

                done = !isSelectPreviousValue();

                break;
            }
            case 2: {
                if (isSelectPreviousValue()) {
                    final TimeInterval interval = _getSelectInterval(
                        resultValue);
                    final BatchValuesQuery.Builder batchValuesQueryBuilder =
                        BatchValuesQuery
                            .newBuilder()
                            .setPoint(Optional.of(getInputPoint()));

                    batchValuesQueryBuilder
                        .setBefore(interval.getBefore().get());

                    final PointValue inputValue = batch
                        .getPointValue(batchValuesQueryBuilder.build());
                    final Optional<DateTime> intervalNotBefore = interval
                        .getNotBefore();

                    if ((inputValue.isPresent())
                            && (!intervalNotBefore.isPresent()
                                || inputValue.getStamp().isAfter(
                                        intervalNotBefore.get()))) {
                        final StoreValuesQuery.Builder storeValuesQueryBuilder =
                            StoreValuesQuery
                                .newBuilder()
                                .setPoint(getInputPoint());

                        storeValuesQueryBuilder
                            .setBefore(inputValue.getStamp());
                        batch
                            .addStoreValuesQuery(
                                storeValuesQueryBuilder.build());
                    }
                }

                done = true;

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
        if (isSynchronized()) {
            return super.select(resultValue, batch);
        }

        final TimeInterval interval = _getSelectInterval(resultValue);
        BatchValuesQuery.Builder batchValuesQueryBuilder;
        PointValue inputValue;

        batchValuesQueryBuilder = BatchValuesQuery
            .newBuilder()
            .setPoint(Optional.of(getInputPoint()));
        batchValuesQueryBuilder.setBefore(interval.getBefore().get());
        batchValuesQueryBuilder.setSync(getInputSync());
        inputValue = batch.getPointValue(batchValuesQueryBuilder.build());

        if (isSelectPreviousValue() && inputValue.isPresent()) {
            batchValuesQueryBuilder = BatchValuesQuery
                .newBuilder()
                .setPoint(inputValue.getPoint());
            batchValuesQueryBuilder.setBefore(inputValue.getStamp());
            batchValuesQueryBuilder.setSync(getInputSync());
            inputValue = batch.getPointValue(batchValuesQueryBuilder.build());
        }

        final Optional<DateTime> intervalAfter = interval.getAfter();

        if (inputValue.isPresent()
                && intervalAfter.isPresent()
                && inputValue.getStamp().isNotAfter(intervalAfter.get())) {
            final Point point = inputValue.getPoint().get();

            inputValue = new PointValue(point, Optional.empty(), null, null);
        }

        return addInputToResult(inputValue, resultValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        final PointRelation relation = getRelation().orElse(null);

        if (relation != null) {
            _selectPreviousValue = relation
                .getParams()
                .getBoolean(SELECT_PREVIOUS_VALUE_PARAM, false);

            _selectSyncPosition = relation
                .getParams()
                .getInt(SELECT_SYNC_POSITION_PARAM, 0);

            if (_selectSyncPosition > 0) {
                getThisLogger()
                    .warn(
                        ProcessorMessages.POSITIVE_NOT_SUPPORTED,
                        SELECT_SYNC_POSITION_PARAM);
                _selectSyncPosition = -_selectSyncPosition;
            }

            _sinceSyncPosition = _syncPosition(
                relation,
                SINCE_SYNC_POSITION_PARAM,
                0);

            _sinceResultSyncPosition = _syncPosition(
                relation,
                SINCE_RESULT_SYNC_POSITION_PARAM,
                -1);
            _afterResultSyncPosition = _syncPosition(
                relation,
                AFTER_RESULT_SYNC_POSITION_PARAM,
                -1);

            if (hasAfterResultSyncPosition()
                    && (getAfterResultSyncPosition() == 0)) {
                getThisLogger()
                    .error(
                        ProcessorMessages.ZERO_NOT_SUPPORTED,
                        AFTER_RESULT_SYNC_POSITION_PARAM);

                return false;
            }

            if (((hasSinceSyncPosition()
                    ? 1: 0) + (hasSinceResultSyncPosition()
                        ? 1: 0) + (hasAfterResultSyncPosition()? 1: 0)) > 1) {
                getThisLogger()
                    .error(
                        ProcessorMessages.MUTUALLY_EXCLUSIVE_PARAMS,
                        SINCE_SYNC_POSITION_PARAM,
                        SINCE_RESULT_SYNC_POSITION_PARAM,
                        AFTER_RESULT_SYNC_POSITION_PARAM);

                return false;
            }

            if (hasSinceSyncPosition()
                    && (getSelectSyncPosition() < getSinceSyncPosition())) {
                getThisLogger()
                    .error(ProcessorMessages.INVALID_SELECTION_INTERVAL);

                return false;
            }

            _resultSync = relation.getResultPoint().getSync().orElse(null);

            if (hasSinceResultSyncPosition() || hasAfterResultSyncPosition()) {
                if (_resultSync == null) {
                    getThisLogger()
                        .error(
                            ProcessorMessages.SYNC_RESULT_NEEDED,
                            SINCE_RESULT_SYNC_POSITION_PARAM,
                            AFTER_RESULT_SYNC_POSITION_PARAM);

                    return false;
                }
            }

            _sync = getRelationSync().orElse(null);

            if (_sync == null) {
                if (hasSelectSyncPosition() || hasSinceSyncPosition()) {
                    _sync = relation.getInputPoint().getSync().orElse(null);

                    if (_sync == null) {
                        getThisLogger()
                            .error(
                                ProcessorMessages.SYNC_INPUT_NEEDED,
                                SELECT_SYNC_POSITION_PARAM,
                                AFTER_RESULT_SYNC_POSITION_PARAM,
                                SINCE_SYNC_POSITION_PARAM);

                        return false;
                    }
                } else if (_resultSync != null) {
                    final Sync inputPointSync = relation
                        .getInputPoint()
                        .getSync()
                        .orElse(null);

                    if ((inputPointSync != null)
                            && (!inputPointSync.equals(_resultSync))) {
                        _sync = inputPointSync;
                    }
                }
            }
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean validate()
    {
        if (!super.validate()) {
            return false;
        }

        if (!supportsExtrapolated() && isInputExtrapolated()) {
            getThisLogger()
                .error(
                    ProcessorMessages.EXTRAPOLATION_NOT_SUPPORTED,
                    getName());

            return false;
        }

        if (!supportsInterpolated() && isInputInterpolated()) {
            getThisLogger()
                .error(
                    ProcessorMessages.INTERPOLATION_NOT_SUPPORTED,
                    getName());

            return false;
        }

        return true;
    }

    /**
     * Adds an input for computing the result.
     *
     * @param inputValue The input value.
     * @param resultValue The result value.
     *
     * @return True if the input is accepted.
     */
    @CheckReturnValue
    protected final boolean addInputToResult(
            @Nonnull PointValue inputValue,
            @Nonnull final ResultValue resultValue)
    {
        if ((inputValue.isPresent())
                && !isInputValid(inputValue, resultValue)) {
            final Point point = inputValue.getPoint().get();

            getThisLogger()
                .debug(ProcessorMessages.INVALID_INPUT_DROPPED, inputValue);
            inputValue = new PointValue(point, Optional.empty(), null, null);
        }

        if ((inputValue.isAbsent()) && isInputRequired()) {
            getThisLogger()
                .debug(
                    ProcessorMessages.REQUIRED_MISSING,
                    resultValue.getPoint().get(),
                    resultValue.getStamp(),
                    getInputPoint());

            return false;
        }

        resultValue.addInputValue(inputValue);

        return true;
    }

    /**
     * Gets the 'AfterResultSyncPosition'.
     *
     * @return The 'AfterResultSyncPosition'.
     */
    @CheckReturnValue
    protected final int getAfterResultSyncPosition()
    {
        return _afterResultSyncPosition.intValue();
    }

    /**
     * Gets the input specific sync.
     *
     * @return The optional sync.
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<Sync> getInputSync()
    {
        return Optional
            .ofNullable((getRelationSyncEntity().isPresent())? _sync: null);
    }

    /**
     * Gets the result sync.
     *
     * @return The optional result sync.
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<Sync> getResultSync()
    {
        return Optional.ofNullable(_resultSync);
    }

    /**
     * Gets a time stamp adjusted according to result sync position.
     *
     * @param position The sync position.
     * @param start The start time stamp.
     * @param forward True for a forward adjustment.
     *
     * @return The optional adjusted time stamp.
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<DateTime> getResultSyncStamp(
            final int position,
            @Nonnull final DateTime start,
            final boolean forward)
    {
        DateTime stamp = start;

        for (int i = position; i < 0; ++i) {
            stamp = (forward? _resultSync
                .getNextStamp(stamp): _resultSync.getPreviousStamp(stamp))
                .orElse(null);
        }

        return Optional.ofNullable(stamp);
    }

    /**
     * Gets the 'SelectSyncPosition'.
     *
     * @return The 'SelectSyncPosition'.
     */
    @CheckReturnValue
    protected final int getSelectSyncPosition()
    {
        return _selectSyncPosition;
    }

    /**
     * Gets the 'SinceResultSyncPosition'.
     *
     * @return The 'SinceResultSyncPosition'.
     */
    @CheckReturnValue
    protected final int getSinceResultSyncPosition()
    {
        return _sinceResultSyncPosition.intValue();
    }

    /**
     * Gets the 'SinceSyncPosition'.
     *
     * @return The 'SinceSyncPosition'.
     */
    @CheckReturnValue
    protected final int getSinceSyncPosition()
    {
        return _sinceSyncPosition.intValue();
    }

    /**
     * Gets a time stamp adjusted according to a sync position.
     *
     * @param position The sync position.
     * @param start The start time stamp.
     * @param forward True for a forward adjustment.
     *
     * @return The optional adjusted time stamp.
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<DateTime> getSyncStamp(
            int position,
            @Nonnull final DateTime start,
            final boolean forward)
    {
        DateTime stamp = start;

        if (_sync != null) {
            if (!_sync.isInSync(stamp)) {
                --position;
            }

            for (int i = position; i < 0; ++i) {
                stamp = (forward? _sync
                    .getNextStamp(stamp): _sync.getPreviousStamp(stamp))
                    .orElse(null);
            }
        }

        return Optional.ofNullable(stamp);
    }

    /**
     * Gets the synchronized result stamp.
     *
     * @param inputValue The result value that will be computed.
     * @param batch The current batch context.
     *
     * @return The optional synchronized result stamp.
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<DateTime> getSynchronizedResultStamp(
            @Nonnull final PointValue inputValue,
            @Nonnull final Batch batch)
    {
        final DateTime syncInputStamp = getSyncStamp(
            getSelectSyncPosition(),
            inputValue.getStamp(),
            true)
            .orElse(null);
        DateTime syncResultStamp;

        if (!getResultSync().isPresent()
                || getResultSync().get().isInSync(syncInputStamp)) {
            syncResultStamp = syncInputStamp;

            if (syncInputStamp != inputValue.getStamp()) {
                final PointValue syncInputValue;

                {
                    final BatchValuesQuery.Builder batchValuesQueryBuilder =
                        BatchValuesQuery
                            .newBuilder()
                            .setPoint(Optional.of(getInputPoint()));

                    batchValuesQueryBuilder.setAt(syncInputStamp);
                    syncInputValue = batch
                        .getPointValue(batchValuesQueryBuilder.build());
                }

                if (syncInputValue.isAbsent()) {
                    final PointValue syncResultValue;

                    {
                        final BatchValuesQuery.Builder batchValuesQueryBuilder =
                            BatchValuesQuery
                                .newBuilder()
                                .setPoint(Optional.of(getResultPoint()));

                        batchValuesQueryBuilder.setAt(syncResultStamp);
                        syncResultValue = batch
                            .getPointValue(batchValuesQueryBuilder.build());
                    }

                    if (syncResultValue.isAbsent()) {
                        syncResultStamp = null;
                    }
                }
            }
        } else {
            syncResultStamp = getResultSync()
                .get()
                .getNextStamp(syncInputStamp)
                .orElse(null);

            final BatchValuesQuery.Builder batchValuesQueryBuilder =
                BatchValuesQuery
                    .newBuilder()
                    .setPoint(Optional.of(getResultPoint()));

            batchValuesQueryBuilder.setAt(syncResultStamp);

            if (batch
                .getPointValue(batchValuesQueryBuilder.build())
                .isAbsent()) {
                syncResultStamp = null;
            }
        }

        return Optional.ofNullable(syncResultStamp);
    }

    /**
     * Asks if a 'AfterResultSyncPosition' is specified.
     *
     * @return True if a 'AfterResultSyncPosition' is specified.
     */
    @CheckReturnValue
    protected final boolean hasAfterResultSyncPosition()
    {
        return _afterResultSyncPosition != null;
    }

    /**
     * Asks if a 'SelectSyncPosition' is specified.
     *
     * @return True if a 'SelectSyncPosition' is specified.
     */
    @CheckReturnValue
    protected final boolean hasSelectSyncPosition()
    {
        return _selectSyncPosition < 0;
    }

    /**
     * Asks if a 'SinceResultSyncPosition' is specified.
     *
     * @return True ifa 'SinceResultSyncPosition' is specified.
     */
    @CheckReturnValue
    protected final boolean hasSinceResultSyncPosition()
    {
        return _sinceResultSyncPosition != null;
    }

    /**
     * Asks if a 'SinceSyncPosition' is specified.
     *
     * @return True if a 'SinceSyncPosition' is specified.
     */
    @CheckReturnValue
    protected final boolean hasSinceSyncPosition()
    {
        return _sinceSyncPosition != null;
    }

    /**
     * Asks if the reference time stamp is in sync.
     *
     * @param stamp The reference time stamp.
     *
     * @return True when in sync.
     */
    @CheckReturnValue
    protected final boolean isInSync(@Nonnull final DateTime stamp)
    {
        return (_sync == null) || _sync.isInSync(stamp);
    }

    /**
     * Asks if the previous value should be selected.
     *
     * @return True if the previous value should be selected.
     */
    @CheckReturnValue
    protected final boolean isSelectPreviousValue()
    {
        return _selectPreviousValue;
    }

    /**
     * Asks if this behavior supports extrapolated values.
     *
     * @return True if it supports extrapolated values.
     */
    @CheckReturnValue
    protected boolean supportsExtrapolated()
    {
        return false;
    }

    /**
     * Asks if this behavior supports interpolated values.
     *
     * @return True if it supports interpolated values.
     */
    @CheckReturnValue
    protected boolean supportsInterpolated()
    {
        return false;
    }

    /**
     * Validates that the point is not triggering itself.
     *
     * @return True when validated.
     */
    @CheckReturnValue
    protected final boolean validateNoLoop()
    {
        if (getInputPoint() == getResultPoint()) {
            getThisLogger()
                .error(ProcessorMessages.TRIGGER_LOOP, getInputPoint());

            return false;
        }

        return true;
    }

    /**
     * Validates that the relation is not synchronized.
     *
     * @return True when validated.
     */
    @CheckReturnValue
    protected final boolean validateNotSynchronized()
    {
        if (isSynchronized()) {
            getThisLogger()
                .error(ProcessorMessages.SYNCHRONIZED_BEHAVIOR, getName());

            return false;
        }

        return true;
    }

    private TimeInterval _getSelectInterval(final ResultValue resultValue)
    {
        final DateTime resultStamp = resultValue.getStamp();
        final TimeInterval.Builder intervalBuilder = TimeInterval.newBuilder();

        if (hasSelectSyncPosition()) {
            intervalBuilder
                .setAt(
                    getSyncStamp(getSelectSyncPosition(), resultStamp, false)
                        .get());

            return intervalBuilder.build();
        }

        if ((getInputPoint() == getResultPoint())
                && (!hasSinceResultSyncPosition()
                    || (getSinceResultSyncPosition() == 0))) {
            intervalBuilder.setBefore(resultStamp);
        } else {
            intervalBuilder.setNotAfter(resultStamp);
        }

        if (hasSinceSyncPosition()) {
            intervalBuilder
                .setNotBefore(
                    getSyncStamp(getSinceSyncPosition(), resultStamp, false)
                        .get());
        } else if (hasSinceResultSyncPosition()) {
            intervalBuilder
                .setNotBefore(
                    getResultSyncStamp(
                        getSinceResultSyncPosition(),
                        resultStamp,
                        false)
                        .orElse(null));
        } else if (hasAfterResultSyncPosition()) {
            intervalBuilder
                .setAfter(
                    getResultSyncStamp(
                        getAfterResultSyncPosition(),
                        resultStamp,
                        false)
                        .orElse(null));
        }

        return intervalBuilder.build();
    }

    private Integer _syncPosition(
            final PointRelation relation,
            final String paramName,
            final int defaultValue)
    {
        final Integer syncPosition;

        if (relation.getParams().containsValueKey(paramName)) {
            int position = relation.getParams().getInt(paramName, defaultValue);

            if (position > 0) {
                getThisLogger()
                    .warn(ProcessorMessages.POSITIVE_NOT_SUPPORTED, paramName);
                position = -position;
            }

            syncPosition = Integer.valueOf(position);
        } else {
            syncPosition = null;
        }

        return syncPosition;
    }

    /**
     * Specifies with a negative integer the maximum position in time after
     * which an input value should be selected, going back in the synchronized
     * history of the result point values.
     */
    public static final String AFTER_RESULT_SYNC_POSITION_PARAM =
        "AfterResultSyncPosition";

    /** Specifies a default stamp trim unit for filtering. */
    public static final String FILTER_STAMP_TRIM_UNIT_PARAM =
        Point.FILTER_STAMP_TRIM_UNIT_PARAM;

    /** Specifies a default time limit for filtering. */
    public static final String FILTER_TIME_LIMIT_PARAM =
        Point.FILTER_TIME_LIMIT_PARAM;

    /** Specifies a default time limit for interpolation or extrapolation. */
    public static final String POLATOR_TIME_LIMIT_PARAM =
        Point.POLATOR_TIME_LIMIT_PARAM;

    /** Selects the previous value of the corresponding point. */
    public static final String SELECT_PREVIOUS_VALUE_PARAM =
        "SelectPreviousValue";

    /**
     * Specifies with a negative integer which value should be selected, going
     * back in the synchronized history of the input point values.
     */
    public static final String SELECT_SYNC_POSITION_PARAM =
        "SelectSyncPosition";

    /**
     * Specifies with a zero or negative integer the maximum position in time
     * at which an input value should be selected, going back in the
     * synchronized history of the result point values. A zero value asks for
     * the value of input at the result time stamp.
     */
    public static final String SINCE_RESULT_SYNC_POSITION_PARAM =
        "SinceResultSyncPosition";

    /**
     * Specifies with zero or a negative integer the maximum position at which
     * a value should be selected, going back in the synchronized history of the
     * input point values. A zero value asks for the value of the most recent
     * input position relative to the result time stamp.
     */
    public static final String SINCE_SYNC_POSITION_PARAM = "SinceSyncPosition";

    private Integer _afterResultSyncPosition;
    private Sync _resultSync;
    private boolean _selectPreviousValue;
    private int _selectSyncPosition;
    private Integer _sinceResultSyncPosition;
    private Integer _sinceSyncPosition;
    private Sync _sync;
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
