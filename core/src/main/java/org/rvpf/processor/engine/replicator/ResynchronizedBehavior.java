/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ResynchronizedBehavior.java 4036 2019-05-31 11:19:42Z SFB $
 */

package org.rvpf.processor.engine.replicator;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.BatchValuesQuery;
import org.rvpf.processor.ProcessorMessages;

/**
 * Resynchronized behavior.
 */
public final class ResynchronizedBehavior
    extends ReplicatedBehavior
{
    /** {@inheritDoc}
     */
    @Override
    public boolean prepareSelect(
            final ResultValue resultValue,
            final Batch batch)
    {
        final boolean done = super.prepareSelect(resultValue, batch);

        switch (batch.getLookUpPass()) {
            case 1: {
                if (resultValue.getInputValues().isEmpty()) {
                    final StoreValuesQuery.Builder storeValuesQueryBuilder =
                        StoreValuesQuery
                            .newBuilder()
                            .setPoint(getInputPoint());

                    storeValuesQueryBuilder.setBefore(resultValue.getStamp());
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
     * <p>When the input Point is not the result Point, a later input may
     * resolve to the same result timestamp.</p>
     */
    @Override
    public boolean prepareTrigger(
            final PointValue noticeValue,
            final Batch batch)
    {
        switch (batch.getLookUpPass()) {
            case 1: {
                if ((getInputPoint() != getResultPoint())
                        && (_resultStamp(noticeValue.getStamp()) != null)) {
                    final StoreValuesQuery.Builder storeValuesQueryBuilder =
                        StoreValuesQuery
                            .newBuilder()
                            .setPoint(getInputPoint());

                    storeValuesQueryBuilder.setAfter(noticeValue.getStamp());
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
     */
    @Override
    public boolean select(final ResultValue resultValue, final Batch batch)
    {
        if (resultValue.getInputValues().isEmpty()) {
            final BatchValuesQuery.Builder batchValuesQueryBuilder =
                BatchValuesQuery
                    .newBuilder()
                    .setPoint(Optional.of(getInputPoint()));

            batchValuesQueryBuilder.setBefore(resultValue.getStamp());

            PointValue inputValue = batch
                .getPointValue(batchValuesQueryBuilder.build());

            if ((inputValue.isPresent())
                    && !resultValue.getStamp().equals(
                        _resultStamp(inputValue.getStamp()))) {
                final Point inputPoint = inputValue.getPoint().get();

                inputValue = new PointValue(
                    inputPoint,
                    Optional.empty(),
                    null,
                    null);
            }

            if (!addInputToResult(inputValue, resultValue)) {
                return false;
            }
        }

        return super.select(resultValue, batch);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        if ((getRelation().isPresent()) && !getResultSync().isPresent()) {
            getThisLogger()
                .error(ProcessorMessages.BEHAVIOR_NEEDS_SYNC, getName());

            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void trigger(final PointValue noticeValue, final Batch batch)
    {
        final DateTime resultStamp = _resultStamp(noticeValue.getStamp());
        ResultValue resultValue;

        // Ignores notices outside resynchronization limits.

        if (resultStamp == null) {
            getThisLogger()
                .warn(ProcessorMessages.OUT_RESYNC_RANGE, noticeValue);

            return;
        }

        // Ignores prior events resolving to the same timestamp.

        if (getInputPoint() != getResultPoint()) {
            final BatchValuesQuery.Builder batchValuesQueryBuilder =
                BatchValuesQuery
                    .newBuilder()
                    .setPoint(noticeValue.getPoint());
            final PointValue nextValue;

            batchValuesQueryBuilder.setAfter(noticeValue.getStamp());
            nextValue = batch.getPointValue(batchValuesQueryBuilder.build());

            if (nextValue.isPresent()
                    && resultStamp.equals(_resultStamp(nextValue.getStamp()))) {
                return;
            }
        }

        // Sets up the synchronized result.

        resultValue = batch
            .getResultValue(
                new PointValue(
                    getResultPoint(),
                    Optional.ofNullable(resultStamp),
                    null,
                    null))
            .orElse(null);

        if (resultValue != null) {    // Resolves multiple updates.
            if (!resultValue.getInputValues().isEmpty()) {
                if (resultValue
                    .getInputValues()
                    .get(0)
                    .getStamp()
                    .isBefore(noticeValue.getStamp())) {
                    resultValue.getInputValues().clear();
                }
            }
        } else if (!resultStamp.equals(noticeValue.getStamp())
                   || (getInputPoint() != getResultPoint())) {
            resultValue = batch.setUpResultValue(resultStamp, this);
        }

        if (resultValue != null) {
            resultValue.addInputValue(noticeValue);
        } else {    // Replicates.
            super.trigger(noticeValue, batch);
        }

        // Cleans up autosynchronized points.

        if (!resultStamp.equals(noticeValue.getStamp())
                && (getInputPoint() == getResultPoint())) {
            resultValue = batch.setUpResultValue(noticeValue.getStamp(), this);
            resultValue.addInputValue(noticeValue);
        }
    }

    /** {@inheritDoc}
     *
     * <p>If the input resynchronizes itself, it is not allowed to have other
     * dependents on the same Processor.</p>
     */
    @Override
    protected boolean doValidate()
    {
        if (!validateTransform(Resynchronizer.class)) {
            return false;
        }

        if ((getInputPoint() == getResultPoint()) && !validateNoResults()) {
            return false;
        }

        return validateNotSynchronized();
    }

    private DateTime _resultStamp(final DateTime noticeStamp)
    {
        DateTime resultStamp;

        if (getResultSync().get().isInSync(noticeStamp)) {
            resultStamp = noticeStamp;
        } else {
            final Resynchronizer resynchronizer =
                (Resynchronizer) getTransform();

            resultStamp = null;

            if (resynchronizer.getFloorInterval() != null) {
                resultStamp = getResultSync()
                    .get()
                    .getPreviousStamp(noticeStamp)
                    .orElse(null);

                if ((resultStamp != null)
                        && resultStamp.after(
                            resynchronizer.getFloorInterval()).isNotAfter(
                                    noticeStamp)) {
                    resultStamp = null;
                }
            }

            if ((resultStamp == null)
                    && (resynchronizer.getFloorRatio() > 0.0)) {
                resultStamp = getResultSync()
                    .get()
                    .getPreviousStamp(noticeStamp)
                    .orElse(null);

                if (resultStamp != null) {
                    final DateTime nextStamp = getResultSync()
                        .get()
                        .getNextStamp(resultStamp)
                        .orElse(null);

                    if (nextStamp != null) {
                        final ElapsedTime interval = nextStamp.sub(resultStamp);

                        if (noticeStamp
                            .sub(resultStamp)
                            .ratio(interval) > resynchronizer.getFloorRatio()) {
                            resultStamp = null;
                        }
                    } else {
                        resultStamp = null;
                    }
                }
            }

            if ((resultStamp == null)
                    && (resynchronizer.getCeilingInterval() != null)) {
                resultStamp = getResultSync()
                    .get()
                    .getNextStamp(noticeStamp)
                    .orElse(null);

                if ((resultStamp != null)
                        && resultStamp.before(
                            resynchronizer.getCeilingInterval()).isNotBefore(
                                    noticeStamp)) {
                    resultStamp = null;
                }
            }

            if ((resultStamp == null)
                    && (resynchronizer.getCeilingRatio() > 0.0)) {
                resultStamp = getResultSync()
                    .get()
                    .getNextStamp(noticeStamp)
                    .orElse(null);

                if (resultStamp != null) {
                    final DateTime previousStamp = getResultSync()
                        .get()
                        .getPreviousStamp(resultStamp)
                        .orElse(null);

                    if (previousStamp != null) {
                        final ElapsedTime interval = resultStamp
                            .sub(previousStamp);

                        if (resultStamp
                            .sub(noticeStamp)
                            .ratio(interval) > resynchronizer.getCeilingRatio()) {
                            resultStamp = null;
                        }
                    } else {
                        resultStamp = null;
                    }
                }
            }
        }

        return resultStamp;
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
