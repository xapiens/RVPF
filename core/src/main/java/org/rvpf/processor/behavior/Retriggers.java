/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Retriggers.java 4036 2019-05-31 11:19:42Z SFB $
 */

package org.rvpf.processor.behavior;

import java.util.Collection;
import java.util.Objects;
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
 * Retriggers behavior.
 *
 * <p>This behavior is used to trigger the computation of dependent values only
 * when they already exist. It will not create new values.</p>
 *
 * <p>This behavior supports self-triggering, where the update of a value will
 * trigger the computation of the next existing value for the same Point. This
 * is also supported by the value selection in super which will select the
 * previous value on self-reference.</p>
 */
public final class Retriggers
    extends PrimaryBehavior
{
    /** {@inheritDoc}
     */
    @Override
    public boolean isResultFetched(
            final PointValue noticeValue,
            final ResultValue resultValue)
    {
        return true;
    }

    /** {@inheritDoc}
     *
     * <p>On the first pass, asks for the next notice. Asks for an other pass if
     * the result Point is not the notice Point.</p>
     *
     * <p>On the second pass, if there is a next notice, asks for all the
     * results between this notice and the next. Otherwise, asks for all the
     * results at or after the notice.</p>
     *
     * <p>All of this is prefixed by a preliminary pass to get the next notice
     * when using 'SelectPreviousValue'.</p>
     */
    @Override
    public boolean prepareTrigger(PointValue noticeValue, final Batch batch)
    {
        if (isSynchronized()) {
            return super.prepareTrigger(noticeValue, batch);
        }

        boolean done = true;
        int lookUpPass = batch.getLookUpPass();

        if (isSelectPreviousValue()) {
            if (--lookUpPass == 0) {
                final StoreValuesQuery.Builder storeValuesQueryBuilder =
                    StoreValuesQuery
                        .newBuilder()
                        .setPoint(noticeValue.getPoint().get());

                storeValuesQueryBuilder.setAfter(noticeValue.getStamp());
                batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
                done = false;
            } else {
                final BatchValuesQuery.Builder batchValuesQueryBuilder =
                    BatchValuesQuery
                        .newBuilder()
                        .setPoint(noticeValue.getPoint());

                batchValuesQueryBuilder.setAfter(noticeValue.getStamp());
                noticeValue = batch
                    .getPointValue(batchValuesQueryBuilder.build());

                if (noticeValue.isAbsent()) {
                    lookUpPass = 0;
                }
            }
        }

        final PointValue nextNoticeValue;

        switch (lookUpPass) {
            case 1: {
                final StoreValuesQuery.Builder storeValuesQueryBuilder =
                    StoreValuesQuery
                        .newBuilder()
                        .setPoint(noticeValue.getPoint().get());

                storeValuesQueryBuilder.setAfter(noticeValue.getStamp());
                storeValuesQueryBuilder
                    .setBefore(
                        _getTriggerLimit(
                            noticeValue.getStamp(),
                            DateTime.END_OF_TIME));
                batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
                done &= Objects
                    .equals(noticeValue.getPoint().get(), getResultPoint());

                break;
            }
            case 2: {
                if (!Objects
                    .equals(noticeValue.getPoint().get(), getResultPoint())) {
                    final StoreValuesQuery.Builder storeValuesQueryBuilder =
                        StoreValuesQuery
                            .newBuilder()
                            .setPoint(getResultPoint());

                    storeValuesQueryBuilder
                        .setNotBefore(noticeValue.getStamp());

                    {
                        final BatchValuesQuery.Builder batchValuesQueryBuilder =
                            BatchValuesQuery
                                .newBuilder()
                                .setPoint(noticeValue.getPoint());

                        batchValuesQueryBuilder
                            .setAfter(noticeValue.getStamp());
                        nextNoticeValue = batch
                            .getPointValue(batchValuesQueryBuilder.build());
                    }

                    storeValuesQueryBuilder
                        .setBefore(
                            _getTriggerLimit(
                                noticeValue.getStamp(),
                                nextNoticeValue.isPresent()? nextNoticeValue
                                        .getStamp(): DateTime.END_OF_TIME));
                    batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
                }

                break;
            }
            default: {
                break;
            }
        }

        return done;
    }

    /** {@inheritDoc}
     *
     * <p>If this is a self-trigger, retriggers the computation of the next
     * value. Otherwiser retriggers the computation of all the results between
     * this notice and the next, defaulting to the end of time.</p>
     */
    @Override
    public void trigger(PointValue noticeValue, final Batch batch)
    {
        BatchValuesQuery.Builder batchValuesQueryBuilder;

        if (isSynchronized()) {
            final DateTime resultStamp = getSynchronizedResultStamp(
                noticeValue,
                batch)
                .get();

            batchValuesQueryBuilder = BatchValuesQuery
                .newBuilder()
                .setPoint(Optional.of(getResultPoint()));
            batchValuesQueryBuilder.setAt(resultStamp);

            if (batch
                .getPointValue(batchValuesQueryBuilder.build())
                .isPresent()) {
                batch.setUpResultValue(noticeValue, resultStamp, this);
            }

            return;
        }

        if (isSelectPreviousValue()) {
            batchValuesQueryBuilder = BatchValuesQuery
                .newBuilder()
                .setPoint(noticeValue.getPoint());
            batchValuesQueryBuilder.setAfter(noticeValue.getStamp());
            noticeValue = batch.getPointValue(batchValuesQueryBuilder.build());

            if (noticeValue.isAbsent()) {
                return;
            }
        }

        if (Objects.equals(noticeValue.getPoint().get(), getResultPoint())) {
            batchValuesQueryBuilder = BatchValuesQuery
                .newBuilder()
                .setPoint(Optional.of(getResultPoint()));
            batchValuesQueryBuilder.setAfter(noticeValue.getStamp());

            final PointValue nextResultValue = batch
                .getPointValue(batchValuesQueryBuilder.build());

            if (nextResultValue.isPresent()) {
                batch
                    .setUpResultValue(
                        noticeValue,
                        nextResultValue.getStamp(),
                        this);
            }
        } else {
            final PointValue nextNoticeValue;

            batchValuesQueryBuilder = BatchValuesQuery
                .newBuilder()
                .setPoint(noticeValue.getPoint());
            batchValuesQueryBuilder.setAfter(noticeValue.getStamp());
            nextNoticeValue = batch
                .getPointValue(batchValuesQueryBuilder.build());

            final Collection<PointValue> values;
            final TimeInterval.Builder intervalBuilder = TimeInterval
                .newBuilder();
            final DateTime nextNoticeStamp = nextNoticeValue
                .isPresent()? nextNoticeValue.getStamp(): DateTime.END_OF_TIME;

            intervalBuilder.setNotBefore(noticeValue.getStamp());
            intervalBuilder
                .setBefore(
                    _getTriggerLimit(noticeValue.getStamp(), nextNoticeStamp));

            batchValuesQueryBuilder = BatchValuesQuery
                .newBuilder()
                .setPoint(Optional.of(getResultPoint()));
            batchValuesQueryBuilder.setInterval(intervalBuilder);
            values = batch.getPointValues(batchValuesQueryBuilder.build());

            for (final PointValue pointValue: values) {
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

        return true;
    }

    private DateTime _getTriggerLimit(
            final DateTime noticeStamp,
            final DateTime nextNoticeStamp)
    {
        DateTime beforeStamp = nextNoticeStamp;
        final DateTime notAfterStamp;

        if (hasSinceSyncPosition()) {
            notAfterStamp = getSyncStamp(
                getSinceSyncPosition() - 1,
                noticeStamp,
                true)
                .get()
                .before();
        } else if (hasSinceResultSyncPosition()) {
            notAfterStamp = getResultSyncStamp(
                getSinceResultSyncPosition(),
                noticeStamp,
                true)
                .get();
        } else if (hasAfterResultSyncPosition()) {
            notAfterStamp = getResultSyncStamp(
                getAfterResultSyncPosition(),
                noticeStamp,
                true)
                .get()
                .after();
        } else {
            notAfterStamp = null;
        }

        if ((notAfterStamp != null) && notAfterStamp.isBefore(beforeStamp)) {
            beforeStamp = notAfterStamp.after();
        }

        return beforeStamp;
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
