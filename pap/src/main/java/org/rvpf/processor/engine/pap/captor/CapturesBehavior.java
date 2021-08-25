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
import org.rvpf.content.BooleanContent;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.BatchValuesQuery;
import org.rvpf.pap.PAPMessages;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.selector.SelectsBehavior;

/**
 * Captures behavior.
 */
public final class CapturesBehavior
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

        final CapturesBehavior otherBehavior = (CapturesBehavior) other;

        return (_captureLimit == otherBehavior._captureLimit)
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
        return new Capture(getResultPoint(), stamp, this);
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
                if (isStartStop()) {
                    final Boolean startStopValue = _BOOLEAN_CONTENT
                        .decode(noticeValue);

                    if (startStopValue == null) {
                        return true;
                    }

                    if (!startStopValue.booleanValue()) {
                        final StoreValuesQuery.Builder storeValuesQueryBuilder =
                            StoreValuesQuery
                                .newBuilder()
                                .setPoint(getInputPoint());

                        storeValuesQueryBuilder
                            .setBefore(noticeValue.getStamp());
                        batch
                            .addStoreValuesQuery(
                                storeValuesQueryBuilder.build());
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
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        final Optional<PointRelation> capturesRelation = getRelation();

        if (capturesRelation.isPresent()) {
            final Params relationParams = capturesRelation.get().getParams();

            _captureLimit = relationParams.getInt(CAPTURE_LIMIT_PARAM, 0);

            if (!isStartStop() && (_captureLimit > 0)) {
                getThisLogger()
                    .error(PAPMessages.START_STOP_NEEDED, CAPTURE_LIMIT_PARAM);

                return false;
            }

            _captureTime = relationParams
                .getElapsed(
                    CAPTURE_TIME_PARAM,
                    Optional.empty(),
                    Optional.empty());

            if (!isStartStop() && _captureTime.isPresent()) {
                getThisLogger()
                    .error(PAPMessages.START_STOP_NEEDED, CAPTURE_TIME_PARAM);

                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void trigger(final PointValue noticeValue, final Batch batch)
    {
        final Capture capture = newResultValue(
            Optional.of(noticeValue.getStamp()));

        if (isStartStop()) {
            final Boolean startStopValue = _BOOLEAN_CONTENT.decode(noticeValue);

            if (startStopValue == null) {
                return;
            }

            final BatchValuesQuery.Builder batchValuesQueryBuilder =
                BatchValuesQuery
                    .newBuilder()
                    .setPoint(Optional.of(getInputPoint()));

            if (startStopValue.booleanValue()) {
                capture.setStartValue(noticeValue);

                if (_captureTime.isPresent()) {
                    batch
                        .scheduleUpdate(
                            new PointValue(
                                noticeValue.getPoint().get(),
                                Optional.of(
                                        noticeValue.getStamp().after(
                                                _captureTime.orElse(null))),
                                null,
                                Boolean.FALSE));
                }
            } else {
                capture.setStopValue(noticeValue);
                batchValuesQueryBuilder.setBefore(noticeValue.getStamp());

                final PointValue startPointValue = batch
                    .getPointValue(batchValuesQueryBuilder.build());
                final Boolean startValue = _BOOLEAN_CONTENT
                    .decode(startPointValue);

                if ((startValue == null) || !startValue.booleanValue()) {
                    return;
                }

                capture.setStartValue(startPointValue);
            }
        } else {
            capture.setStartValue(noticeValue);
        }

        batch.setUpResultValue(capture, this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean validate()
    {
        if (!super.validate()) {
            return false;
        }

        if (_capturedBehavior == null) {
            getThisLogger()
                .warn(
                    ProcessorMessages.INPUT_BEHAVIOR_NOT_FOUND,
                    CapturedBehavior.class.getSimpleName());

            return false;
        }

        return true;
    }

    /**
     * Gets the limit after.
     *
     * @return The limit after.
     */
    @CheckReturnValue
    int getLimitAfter()
    {
        return _captureLimit;
    }

    /**
     * Gets the limit before.
     *
     * @return The limit before.
     */
    @CheckReturnValue
    int getLimitBefore()
    {
        return _capturedBehavior.getCaptureLimit();
    }

    /**
     * Gets the time after.
     *
     * @return The optional time after.
     */
    @Nonnull
    @CheckReturnValue
    Optional<ElapsedTime> getTimeAfter()
    {
        return Require.notNull(_captureTime);
    }

    /**
     * Gets the time before.
     *
     * @return The optional time before.
     */
    @Nonnull
    @CheckReturnValue
    Optional<ElapsedTime> getTimeBefore()
    {
        return _capturedBehavior.getCaptureTime();
    }

    /**
     * Sets the captured behavior.
     *
     * @param capturedBehavior The captured behavior.
     */
    void setCapturedBehavior(@Nonnull final CapturedBehavior capturedBehavior)
    {
        _capturedBehavior = capturedBehavior;
    }

    /** The capture limit. */
    public static final String CAPTURE_LIMIT_PARAM = Point.CAPTURE_LIMIT_PARAM;

    /** The capture time. */
    public static final String CAPTURE_TIME_PARAM = Point.CAPTURE_TIME_PARAM;

    /**  */

    private static final BooleanContent _BOOLEAN_CONTENT = new BooleanContent();

    private int _captureLimit;
    private Optional<ElapsedTime> _captureTime;
    private CapturedBehavior _capturedBehavior;
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
