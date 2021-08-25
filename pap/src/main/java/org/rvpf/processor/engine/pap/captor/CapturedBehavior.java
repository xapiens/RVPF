/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.processor.engine.pap.captor;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointInput;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.BatchValuesQuery;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.selector.SelectedBehavior;

/**
 * Captured behavior.
 */
public final class CapturedBehavior
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

        final CapturedBehavior otherBehavior = (CapturedBehavior) other;

        return (_excludeNullSteps == otherBehavior._excludeNullSteps)
               && (_captureLimit == otherBehavior._captureLimit)
               && Objects.equals(_captureTime, otherBehavior._captureTime);
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
    public Capture newResultValue(final Optional<DateTime> stamp)
    {
        throw Require.failure();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean prepareSelect(
            final ResultValue resultValue,
            final Batch batch)
    {
        if (!(resultValue instanceof Capture)) {
            return true;
        }

        switch (batch.getLookUpPass()) {
            case 1: {
                final Capture capture = (Capture) resultValue;

                if (!capture.getStartValue().isPresent()) {
                    return true;
                }

                if (!capture.getStopValue().isPresent()) {
                    _prepareSelectNotAfter(capture, batch);
                } else {
                    _prepareSelectAfter(capture, batch);
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
    public boolean select(final ResultValue resultValue, final Batch batch)
    {
        if (!(resultValue instanceof Capture)) {
            getThisLogger()
                .warn(ProcessorMessages.UNEXPECTED_RESULT_SET_UP, resultValue);

            return false;
        }

        final Capture capture = (Capture) resultValue;

        if (!capture.getStartStamp().isPresent()) {
            return true;
        }

        if (!capture.getStopValue().isPresent()) {
            return _selectNotAfter(capture, batch);
        }

        return _selectAfter(capture, batch);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        final Optional<PointRelation> capturedRelation = getRelation();

        if (capturedRelation.isPresent()) {
            final Params relationParams = capturedRelation.get().getParams();

            _captureLimit = relationParams.getInt(CAPTURE_LIMIT_PARAM, 0);
            _captureTime = relationParams
                .getElapsed(
                    CAPTURE_TIME_PARAM,
                    Optional.empty(),
                    Optional.empty());
            _excludeNullSteps = relationParams
                .getBoolean(EXCLUDE_NULLS_PARAM, false);

            final Optional<PointInput> capturesRelation = getInput(
                CapturesBehavior.class,
                false);

            if (!capturesRelation.isPresent()) {
                return false;
            }

            ((CapturesBehavior) capturesRelation
                .get()
                .getPrimaryBehavior()
                .get())
                .setCapturedBehavior(this);
        }

        return true;
    }

    /**
     * Gets the capture limit.
     *
     * @return The capture limit.
     */
    @CheckReturnValue
    int getCaptureLimit()
    {
        return _captureLimit;
    }

    /**
     * Gets the capture time.
     *
     * @return The optional capture time.
     */
    @Nonnull
    @CheckReturnValue
    Optional<ElapsedTime> getCaptureTime()
    {
        return Require.notNull(_captureTime);
    }

    private void _prepareSelectAfter(final Capture capture, final Batch batch)
    {
        final StoreValuesQuery.Builder storeValuesQueryBuilder =
            StoreValuesQuery
                .newBuilder()
                .setPoint(getInputPoint());
        final Optional<DateTime> startStamp = capture.getStartStamp();
        final Optional<DateTime> stopStamp = capture.getStopStamp();
        final int limitAfter = capture.getLimitAfter();

        storeValuesQueryBuilder.setAfter(startStamp.get());
        storeValuesQueryBuilder.setNotNull(_excludeNullSteps);

        if (stopStamp.isPresent()) {
            storeValuesQueryBuilder.setNotAfter(stopStamp.get());
        }

        if (limitAfter > 0) {
            storeValuesQueryBuilder.setRows(limitAfter);
        }

        batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
    }

    private void _prepareSelectNotAfter(
            final Capture capture,
            final Batch batch)
    {
        final DateTime startStamp = capture.getStartStamp().get();
        final int limitBefore = capture.getLimitBefore();
        final Optional<ElapsedTime> timeBefore = capture.getTimeBefore();
        final StoreValuesQuery.Builder storeValuesQueryBuilder =
            StoreValuesQuery
                .newBuilder()
                .setPoint(getInputPoint());

        if (timeBefore.isPresent()) {
            final DateTime stampBefore = startStamp.before(timeBefore.get());

            storeValuesQueryBuilder.setBefore(stampBefore);
            storeValuesQueryBuilder.setNotNull(_excludeNullSteps);
            batch.addStoreValuesQuery(storeValuesQueryBuilder.build());

            storeValuesQueryBuilder.clear();
            storeValuesQueryBuilder.setNotAfter(startStamp);
            storeValuesQueryBuilder.setNotBefore(stampBefore);
            storeValuesQueryBuilder.setNotNull(_excludeNullSteps);
            storeValuesQueryBuilder.setReverse(true);

            if (limitBefore > 0) {
                storeValuesQueryBuilder.setRows(limitBefore);
            }

            batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
        } else {
            storeValuesQueryBuilder.setNotAfter(startStamp);
            storeValuesQueryBuilder.setNotNull(_excludeNullSteps);
            storeValuesQueryBuilder.setRows(Math.max(limitBefore, 1));
            batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
        }
    }

    private boolean _selectAfter(final Capture capture, final Batch batch)
    {
        final BatchValuesQuery.Builder batchValuesQueryBuilder =
            BatchValuesQuery
                .newBuilder()
                .setPoint(Optional.of(getInputPoint()));

        batchValuesQueryBuilder.setAfter(capture.getStartStamp().get());
        batchValuesQueryBuilder.setNotAfter(capture.getStopStamp().get());
        batchValuesQueryBuilder.setNotNull(_excludeNullSteps);

        final Collection<PointValue> inputValues = batch
            .getPointValues(batchValuesQueryBuilder.build());
        final int limitAfter = capture.getLimitAfter();
        int count = 0;

        for (final PointValue inputValue: inputValues) {
            if (!addInputToResult(inputValue, capture)) {
                return false;
            }

            if ((limitAfter > 0) && (++count >= limitAfter)) {
                break;
            }
        }

        return true;
    }

    private boolean _selectNotAfter(final Capture capture, final Batch batch)
    {
        final DateTime startStamp = capture.getStartStamp().get();
        final int limitBefore = capture.getLimitBefore();
        final Optional<ElapsedTime> timeBefore = capture.getTimeBefore();
        BatchValuesQuery.Builder batchValuesQueryBuilder;

        batchValuesQueryBuilder = BatchValuesQuery
            .newBuilder()
            .setPoint(Optional.of(getInputPoint()));
        batchValuesQueryBuilder.setNotAfter(startStamp);
        batchValuesQueryBuilder.setNotNull(_excludeNullSteps);

        if (timeBefore.isPresent()) {
            batchValuesQueryBuilder
                .setNotBefore(startStamp.before(timeBefore.get()));
        } else if (limitBefore <= 0) {
            batchValuesQueryBuilder.setNotBefore(startStamp);
        }

        final Collection<PointValue> inputValues = batch
            .getPointValues(batchValuesQueryBuilder.build());

        if (inputValues.isEmpty()) {
            batchValuesQueryBuilder = BatchValuesQuery
                .newBuilder()
                .setPoint(Optional.of(getInputPoint()));
            batchValuesQueryBuilder
                .setBefore(
                    timeBefore.isPresent()? startStamp
                        .before(timeBefore.get()): startStamp);
            batchValuesQueryBuilder.setNotNull(_excludeNullSteps);

            if (!addInputToResult(
                    batch.getPointValue(batchValuesQueryBuilder.build()),
                    capture)) {
                return false;
            }
        } else {
            int skip = (limitBefore > 0)? inputValues.size() - limitBefore: 0;

            for (final PointValue inputValue: inputValues) {
                if (skip <= 0) {
                    if (!addInputToResult(inputValue, capture)) {
                        return false;
                    }
                } else {
                    --skip;
                }
            }
        }

        return true;
    }

    /** The capture limit. */
    public static final String CAPTURE_LIMIT_PARAM = Point.CAPTURE_LIMIT_PARAM;

    /** The capture time. */
    public static final String CAPTURE_TIME_PARAM = Point.CAPTURE_TIME_PARAM;

    /** Null values should be excluded. The default is to include them. */
    public static final String EXCLUDE_NULLS_PARAM = "ExcludeNulls";

    private int _captureLimit;
    private Optional<ElapsedTime> _captureTime;
    private boolean _excludeNullSteps;
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
