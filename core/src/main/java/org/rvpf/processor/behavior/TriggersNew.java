/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TriggersNew.java 4036 2019-05-31 11:19:42Z SFB $
 */

package org.rvpf.processor.behavior;

import java.util.Optional;

import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.BatchValuesQuery;
import org.rvpf.processor.ProcessorMessages;

/**
 * Triggers new behavior.
 *
 * <p>This behavior only triggers the computation of a result with the same
 * timestamp as the notice if no result exist with a timestamp later or equal,
 * or at least not before the next notice.</p>
 */
public final class TriggersNew
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
     * <p>On the first pass, asks for a result at or after the notice.</p>
     *
     * <p>On the second pass, if the queried result is found, asks for the next
     * notice on this point.</p>
     */
    @Override
    public boolean prepareTrigger(
            final PointValue noticeValue,
            final Batch batch)
    {
        boolean done = true;

        switch (batch.getLookUpPass()) {
            case 1: {
                if (isSynchronized()) {
                    if (!getResultSync().isPresent()
                            || getResultSync().get().isInSync(
                                noticeValue.getStamp())) {
                        final StoreValuesQuery.Builder storeValuesQueryBuilder =
                            StoreValuesQuery
                                .newBuilder()
                                .setPoint(getResultPoint());

                        storeValuesQueryBuilder.setAt(noticeValue.getStamp());
                        batch
                            .addStoreValuesQuery(
                                storeValuesQueryBuilder.build());
                    }
                } else {
                    final BatchValuesQuery.Builder batchValuesQueryBuilder =
                        BatchValuesQuery
                            .newBuilder()
                            .setPoint(Optional.of(getResultPoint()));

                    batchValuesQueryBuilder
                        .setNotBefore(noticeValue.getStamp());

                    if (batch
                        .getPointValue(batchValuesQueryBuilder.build())
                        .isAbsent()) {
                        final StoreValuesQuery.Builder storeValuesQueryBuilder =
                            StoreValuesQuery
                                .newBuilder()
                                .setPoint(getResultPoint());

                        storeValuesQueryBuilder
                            .setNotBefore(noticeValue.getStamp());
                        batch
                            .addStoreValuesQuery(
                                storeValuesQueryBuilder.build());
                    }

                    done = false;
                }

                break;
            }
            case 2: {
                final BatchValuesQuery.Builder batchValuesQueryBuilder =
                    BatchValuesQuery
                        .newBuilder()
                        .setPoint(Optional.of(getResultPoint()));

                batchValuesQueryBuilder.setNotBefore(noticeValue.getStamp());

                if (!isSynchronized()
                        && (batch.getPointValue(
                            batchValuesQueryBuilder.build()).isPresent())) {
                    final StoreValuesQuery.Builder storeValuesQueryBuilder =
                        StoreValuesQuery
                            .newBuilder()
                            .setPoint(noticeValue.getPoint().get());

                    storeValuesQueryBuilder.setAfter(noticeValue.getStamp());
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
     * <p>If no result where found, or at least not before the next notice, then
     * sets up the new result.</p>
     */
    @Override
    public void trigger(final PointValue noticeValue, final Batch batch)
    {
        BatchValuesQuery.Builder batchValuesQueryBuilder;

        if (isSynchronized()) {
            if (!getResultSync().isPresent()
                    || getResultSync().get().isInSync(noticeValue.getStamp())) {
                batchValuesQueryBuilder = BatchValuesQuery
                    .newBuilder()
                    .setPoint(Optional.of(getResultPoint()));
                batchValuesQueryBuilder.setAt(noticeValue.getStamp());

                if (batch
                    .getPointValue(batchValuesQueryBuilder.build())
                    .isAbsent()) {
                    batch
                        .setUpResultValue(
                            noticeValue,
                            noticeValue.getStamp(),
                            this);
                }
            }

            return;
        }

        final PointValue resultValue;

        batchValuesQueryBuilder = BatchValuesQuery
            .newBuilder()
            .setPoint(Optional.of(getResultPoint()));
        batchValuesQueryBuilder.setNotBefore(noticeValue.getStamp());
        resultValue = batch.getPointValue(batchValuesQueryBuilder.build());

        if (resultValue.isPresent()) {
            batchValuesQueryBuilder = BatchValuesQuery
                .newBuilder()
                .setPoint(noticeValue.getPoint());
            batchValuesQueryBuilder.setAfter(noticeValue.getStamp());

            final PointValue nextInputValue = batch
                .getPointValue(batchValuesQueryBuilder.build());

            if ((nextInputValue.isPresent())
                    && resultValue.getStamp().isBefore(
                        nextInputValue.getStamp())) {
                return;
            }
        }

        batch.setUpResultValue(noticeValue, noticeValue.getStamp(), this);
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
