/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AlwaysTriggers.java 4036 2019-05-31 11:19:42Z SFB $
 */

package org.rvpf.processor.behavior;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.BatchValuesQuery;
import org.rvpf.processor.ProcessorMessages;

/**
 * Always triggers behavior.
 *
 * <p>Always triggers the computation of a result with the same timestamp as
 * the notice. It also retriggers the computation of subsequent dependents up
 * to the next appearance of itself.</p>
 */
public final class AlwaysTriggers
    extends PrimaryBehavior
{
    /** {@inheritDoc}
     */
    @Override
    public boolean isResultFetched(
            final PointValue noticeValue,
            final ResultValue resultValue)
    {
        return !isSynchronized()
               || super.isResultFetched(noticeValue, resultValue);
    }

    /** {@inheritDoc}
     *
     * <p>On the first pass, asks for the next notice.</p>
     *
     * <p>On the second pass, if there is a next notice, asks for all the
     * results between this notice and the next. Otherwise, asks for all the
     * results at or after the notice.</p>
     */
    @Override
    public boolean prepareTrigger(
            final PointValue noticeValue,
            final Batch batch)
    {
        final boolean done;

        if (isSynchronized()) {
            done = super.prepareTrigger(noticeValue, batch);
        } else {
            switch (batch.getLookUpPass()) {
                case 1: {
                    final StoreValuesQuery.Builder storeValuesQueryBuilder =
                        StoreValuesQuery
                            .newBuilder()
                            .setPoint(noticeValue.getPoint().get());

                    storeValuesQueryBuilder.setAfter(noticeValue.getStamp());
                    batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
                    done = false;

                    break;
                }
                case 2: {
                    final PointValue nextNoticeValue;
                    final BatchValuesQuery.Builder batchValuesQueryBuilder =
                        BatchValuesQuery
                            .newBuilder()
                            .setPoint(noticeValue.getPoint());

                    batchValuesQueryBuilder.setAfter(noticeValue.getStamp());
                    nextNoticeValue = batch
                        .getPointValue(batchValuesQueryBuilder.build());

                    final StoreValuesQuery.Builder storeValuesQueryBuilder =
                        StoreValuesQuery
                            .newBuilder()
                            .setPoint(getResultPoint());

                    storeValuesQueryBuilder
                        .setNotBefore(noticeValue.getStamp());
                    storeValuesQueryBuilder
                        .setBefore(
                            nextNoticeValue.isPresent()? nextNoticeValue
                                .getStamp(): DateTime.END_OF_TIME);
                    batch.addStoreValuesQuery(storeValuesQueryBuilder.build());

                    done = true;

                    break;
                }
                default: {
                    done = true;

                    break;
                }
            }
        }

        return done;
    }

    /** {@inheritDoc}
     *
     * <p>Sets up a result at the notice's time.</p>
     *
     * <p>Also retriggers the computation of all the results btween this notice
     * and the next, defaulting to the end of time.</p>
     */
    @Override
    public void trigger(final PointValue noticeValue, final Batch batch)
    {
        if (isSynchronized()) {
            super.trigger(noticeValue, batch);
        } else {
            final PointValue nextNoticeValue;
            BatchValuesQuery.Builder batchValuesQueryBuilder;

            batchValuesQueryBuilder = BatchValuesQuery
                .newBuilder()
                .setPoint(noticeValue.getPoint());
            batchValuesQueryBuilder.setAfter(noticeValue.getStamp());
            nextNoticeValue = batch
                .getPointValue(batchValuesQueryBuilder.build());

            if (!getResultSync().isPresent()
                    || getResultSync().get().isInSync(noticeValue.getStamp())) {
                batch
                    .setUpResultValue(
                        noticeValue,
                        noticeValue.getStamp(),
                        this);
            }

            final DateTime before = nextNoticeValue
                .isPresent()? nextNoticeValue.getStamp(): DateTime.END_OF_TIME;
            final TimeInterval.Builder intervalBuilder = TimeInterval
                .newBuilder();

            intervalBuilder.setAfter(noticeValue.getStamp());
            intervalBuilder.setBefore(before);

            batchValuesQueryBuilder = BatchValuesQuery
                .newBuilder()
                .setPoint(Optional.of(getResultPoint()));
            batchValuesQueryBuilder.setInterval(intervalBuilder);

            for (final PointValue pointValue:
                    batch.getPointValues(batchValuesQueryBuilder.build())) {
                batch
                    .setUpResultValue(noticeValue, pointValue.getStamp(), this);
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean doValidate()
    {
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

        if (isSelectPreviousValue()) {
            getThisLogger()
                .error(
                    ProcessorMessages.BEHAVIOR_INCOMPATIBLE,
                    getName(),
                    SELECT_PREVIOUS_VALUE_PARAM);

            return false;
        }

        return validateNoLoop();
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
