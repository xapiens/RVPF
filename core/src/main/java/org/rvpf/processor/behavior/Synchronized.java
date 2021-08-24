/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Synchronized.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.processor.behavior;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.PointRelation;
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
 * Synchronized behavior.
 *
 * <p>As a primary behavior, will triggers the computation of a result with the
 * same timestamp.</p>
 *
 * <p>It can also be used as a secondary behavior to limit the supply of values
 * to those with the same timestamp as the result (this is adjusted to
 * accomodate synchronization mismatch).</p>
 *
 * <p>Interpolation and extrapolation are supported.</p>
 */
public final class Synchronized
    extends PrimaryBehavior
{
    /** {@inheritDoc}
     */
    @Override
    public boolean isInputValid(
            final PointValue inputValue,
            final ResultValue resultValue)
    {
        DateTime stamp;

        if (!super.isInputValid(inputValue, resultValue)) {
            return false;
        }

        stamp = getSyncStamp(
            getSelectSyncPosition(),
            inputValue.getStamp(),
            true)
            .orElse(null);

        if (getResultSync().isPresent()
                && !getResultSync().get().isInSync(stamp)) {
            stamp = getResultSync().get().getNextStamp(stamp).orElse(null);
        }

        return (stamp != null) && stamp.equals(resultValue.getStamp());
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isSynchronized()
    {
        return true;
    }

    /** {@inheritDoc}
     *
     * <p>Unless there is already an input value at the result's time adjusted
     * to the sync position, asks for one.</p>
     */
    @Override
    public boolean prepareSelect(
            final ResultValue resultValue,
            final Batch batch)
    {
        switch (batch.getLookUpPass()) {
            case 1: {
                DateTime stamp;

                stamp = resultValue.getStamp();

                if (getResultSync().isPresent()) {
                    final Optional<Sync> sync = getSync();

                    if (sync.isPresent() && !sync.get().isInSync(stamp)) {
                        stamp = sync.get().getPreviousStamp(stamp).get();
                    }
                }

                stamp = getSyncStamp(getSelectSyncPosition(), stamp, false)
                    .orElse(null);

                final BatchValuesQuery.Builder batchValuesQueryBuilder =
                    BatchValuesQuery
                        .newBuilder()
                        .setPoint(Optional.of(getInputPoint()));

                batchValuesQueryBuilder.setAt(stamp);

                if (batch
                    .getPointValue(batchValuesQueryBuilder.build())
                    .isAbsent()) {
                    final StoreValuesQuery.Builder storeValuesQueryBuilder =
                        StoreValuesQuery
                            .newBuilder()
                            .setPoint(getInputPoint());

                    storeValuesQueryBuilder.setAt(stamp);
                    storeValuesQueryBuilder
                        .setInterpolated(isInputInterpolated());
                    storeValuesQueryBuilder
                        .setExtrapolated(isInputExtrapolated());
                    storeValuesQueryBuilder
                        .setPolatorTimeLimit(
                            Optional.ofNullable(_polatorTimeLimit));
                    batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
                }

                break;
            }
            default: {
                break;
            }
        }

        return true;
    }

    /** {@inheritDoc}
     *
     * <p>Asks for a result at the notice's time adjusted to the sync position.
     * When an adjustment is effective, asks for an input value for the same
     * time as the result.</p>
     */
    @Override
    public boolean prepareTrigger(
            final PointValue noticeValue,
            final Batch batch)
    {
        switch (batch.getLookUpPass()) {
            case 1: {
                DateTime stamp = getSyncStamp(
                    getSelectSyncPosition(),
                    noticeValue.getStamp(),
                    true)
                    .orElse(null);

                final boolean inSync = !getResultSync().isPresent()
                        || getResultSync().get().isInSync(stamp);

                if (!inSync) {
                    stamp = getResultSync()
                        .get()
                        .getNextStamp(stamp)
                        .orElse(null);
                }

                final StoreValuesQuery.Builder storeValuesQueryBuilder =
                    StoreValuesQuery
                        .newBuilder();

                storeValuesQueryBuilder.setAt(stamp);

                storeValuesQueryBuilder.setPoint(getResultPoint());
                batch.addStoreValuesQuery(storeValuesQueryBuilder.build());

                if (inSync && (stamp != noticeValue.getStamp())) {
                    storeValuesQueryBuilder.setPoint(getInputPoint());
                    batch.addStoreValuesQuery(storeValuesQueryBuilder.build());

                    storeValuesQueryBuilder.setPoint(getResultPoint());
                    batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
                }

                break;
            }
            default: {
                break;
            }
        }

        return true;
    }

    /** {@inheritDoc}
     *
     * <p>Selects the input value at the result's time adjusted to the sync
     * position.</p>
     */
    @Override
    public boolean select(final ResultValue resultValue, final Batch batch)
    {
        final PointValue inputValue;
        DateTime stamp;

        stamp = resultValue.getStamp();

        if (getResultSync().isPresent()) {
            final Optional<Sync> sync = getSync();

            if (sync.isPresent() && !sync.get().isInSync(stamp)) {
                stamp = sync.get().getPreviousStamp(stamp).get();
            }
        }

        stamp = getSyncStamp(getSelectSyncPosition(), stamp, false)
            .orElse(null);

        final BatchValuesQuery.Builder batchValuesQueryBuilder =
            BatchValuesQuery
                .newBuilder()
                .setPoint(Optional.of(getInputPoint()));

        batchValuesQueryBuilder.setAt(stamp);
        batchValuesQueryBuilder.setInterpolated(isInputInterpolated());
        batchValuesQueryBuilder.setExtrapolated(isInputExtrapolated());
        inputValue = batch.getPointValue(batchValuesQueryBuilder.build());

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

        final Optional<PointRelation> relation = getRelation();

        if (relation.isPresent()) {
            _polatorTimeLimit = relation
                .get()
                .getParams()
                .getElapsed(
                    POLATOR_TIME_LIMIT_PARAM,
                    Optional.empty(),
                    Optional.empty())
                .orElse(null);
        }

        return true;
    }

    /** {@inheritDoc}
     *
     * <p>If there is no time adjustment or if an input value is available at
     * the adjusted time, triggers the result's computation.</p>
     */
    @Override
    public void trigger(final PointValue noticeValue, final Batch batch)
    {
        final Optional<DateTime> resultStamp = getSynchronizedResultStamp(
            noticeValue,
            batch);

        if (resultStamp.isPresent()) {
            batch.setUpResultValue(noticeValue, resultStamp.get(), this);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean doValidate()
    {
        if (isSelectPreviousValue()) {
            getThisLogger()
                .error(
                    ProcessorMessages.BEHAVIOR_INCOMPATIBLE,
                    getName(),
                    SELECT_PREVIOUS_VALUE_PARAM);

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

        if (hasAfterResultSyncPosition()) {
            getThisLogger()
                .error(
                    ProcessorMessages.BEHAVIOR_INCOMPATIBLE,
                    getName(),
                    AFTER_RESULT_SYNC_POSITION_PARAM);

            return false;
        }

        return hasSelectSyncPosition() || validateNoLoop();
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

    private ElapsedTime _polatorTimeLimit;
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
