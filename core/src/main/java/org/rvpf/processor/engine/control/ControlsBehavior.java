/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ControlsBehavior.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.control;

import java.util.Optional;

import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.BatchValuesQuery;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.behavior.PrimaryBehavior;

/**
 * Controls behavior.
 *
 * <p>Always triggers the computation of a result with the same timestamp as
 * the notice.</p>
 *
 * <p>It is always associated with the only input of a control transform.</p>
 */
public final class ControlsBehavior
    extends PrimaryBehavior
{
    /** {@inheritDoc}
     */
    @Override
    public boolean prepareSelect(
            final ResultValue resultValue,
            final Batch batch)
    {
        return true;
    }

    /** {@inheritDoc}
     *
     * <p>On the first pass, asks for the next notice.</p>
     */
    @Override
    public boolean prepareTrigger(
            final PointValue noticeValue,
            final Batch batch)
    {
        final boolean done;

        switch (batch.getLookUpPass()) {
            case 1: {
                final StoreValuesQuery.Builder storeValuesQueryBuilder =
                    StoreValuesQuery
                        .newBuilder()
                        .setPoint(noticeValue.getPoint().get());

                storeValuesQueryBuilder.setAfter(noticeValue.getStamp());
                batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
                done = !noticeValue.isDeleted();

                break;
            }
            case 2: {
                if (noticeValue.isDeleted()) {
                    final PointValue nextNoviceValue;

                    {
                        final BatchValuesQuery.Builder batchValuesQueryBuilder =
                            BatchValuesQuery
                                .newBuilder()
                                .setPoint(
                                    Optional
                                            .ofNullable(noticeValue.getPoint().get()));

                        batchValuesQueryBuilder
                            .setAfter(noticeValue.getStamp());
                        nextNoviceValue = batch
                            .getPointValue(batchValuesQueryBuilder.build());
                    }

                    if (nextNoviceValue.isAbsent()) {
                        final StoreValuesQuery.Builder storeValuesQueryBuilder =
                            StoreValuesQuery
                                .newBuilder()
                                .setPoint(noticeValue.getPoint().get());

                        storeValuesQueryBuilder
                            .setBefore(noticeValue.getStamp());
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
        final BatchValuesQuery.Builder batchValuesQueryBuilder =
            BatchValuesQuery
                .newBuilder()
                .setPoint(Optional.of(getInputPoint()));

        batchValuesQueryBuilder.setNotAfter(resultValue.getStamp());

        final PointValue inputValue = batch
            .getPointValue(batchValuesQueryBuilder.build());

        Require.success(inputValue.isPresent());
        resultValue.addInputValue(inputValue);

        return true;
    }

    /** {@inheritDoc}
     *
     * <p>If the notice is the last value of its point, sets up a result at the
     * notice's time.</p>
     */
    @Override
    public void trigger(PointValue noticeValue, final Batch batch)
    {
        final PointValue nextNoviceValue;

        {
            final BatchValuesQuery.Builder batchValuesQueryBuilder =
                BatchValuesQuery
                    .newBuilder()
                    .setPoint(Optional.ofNullable(noticeValue.getPoint().get()));

            batchValuesQueryBuilder.setAfter(noticeValue.getStamp());
            nextNoviceValue = batch
                .getPointValue(batchValuesQueryBuilder.build());
        }

        if (nextNoviceValue.isAbsent()) {
            while (noticeValue.isDeleted()) {
                final BatchValuesQuery.Builder batchQueryBuilder =
                    BatchValuesQuery
                        .newBuilder()
                        .setPoint(
                            Optional.ofNullable(noticeValue.getPoint().get()));

                batchQueryBuilder.setBefore(noticeValue.getStamp());
                noticeValue = batch.getPointValue(batchQueryBuilder.build());
            }

            if (noticeValue.isPresent()) {
                batch.setUpResultValue(noticeValue.getStamp(), this);
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean doValidate()
    {
        if (!validateTransform(ControlTransform.class)) {
            return false;
        }

        if (getResultPoint().getInputs().size() != 1) {
            getThisLogger()
                .error(ProcessorMessages.POINT_EQ_1_INPUT, getResultPoint());

            return false;
        }

        return validateNotSynchronized();
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
