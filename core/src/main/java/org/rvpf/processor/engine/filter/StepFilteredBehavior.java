/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StepFilteredBehavior.java 4036 2019-05-31 11:19:42Z SFB $
 */

package org.rvpf.processor.engine.filter;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.base.value.filter.StepFilter;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.BatchValuesQuery;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.behavior.PrimaryBehavior;

/**
 * Step filtered behavior.
 */
public final class StepFilteredBehavior
    extends PrimaryBehavior
{
    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (!super.equals(other)) {
            return false;
        }

        final StepFilteredBehavior otherBehavior = (StepFilteredBehavior) other;

        if (!_stepFilter.equals(otherBehavior._stepFilter)) {
            return false;
        }

        return _polated == otherBehavior._polated;
    }

    /**
     * Asks if a point value notice should be filtered out.
     *
     * @param noticeValue The point value notice.
     * @param previousValue The optional previous point value.
     * @param synthesizedValue The optional synthesized value.
     *
     * @return Filtered point values.
     */
    public PointValue[] filter(
            @Nonnull final PointValue noticeValue,
            @Nonnull final Optional<PointValue> previousValue,
            @Nonnull final Optional<PointValue> synthesizedValue)
    {
        if (previousValue.isPresent()) {
            _stepFilter.reset();
            _stepFilter.filter(Optional.of(previousValue.get()));
        }

        if (synthesizedValue.isPresent()) {
            if (!previousValue.isPresent()) {
                _stepFilter.reset();
            }

            _stepFilter.filter(Optional.of(synthesizedValue.get()));
        }

        return _stepFilter.filter(Optional.of(noticeValue));
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
    public boolean isResultFetched(
            final PointValue noticeValue,
            final ResultValue resultValue)
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
        return true;
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
                {
                    final StoreValuesQuery.Builder storeValuesQueryBuilder =
                        StoreValuesQuery
                            .newBuilder()
                            .setPoint(getResultPoint());

                    storeValuesQueryBuilder
                        .setNotBefore(_getSince(noticeValue.getStamp()));
                    storeValuesQueryBuilder.setBefore(noticeValue.getStamp());
                    storeValuesQueryBuilder.setRows(1);
                    storeValuesQueryBuilder.setReverse(true);
                    batch.addStoreValuesQuery(storeValuesQueryBuilder.build());
                }

                if (_polated) {
                    {
                        final StoreValuesQuery.Builder storeValuesQueryBuilder =
                            StoreValuesQuery
                                .newBuilder()
                                .setPoint(getResultPoint());

                        storeValuesQueryBuilder.setAt(noticeValue.getStamp());
                        storeValuesQueryBuilder
                            .setInterpolated(isInputInterpolated());
                        storeValuesQueryBuilder
                            .setExtrapolated(isInputExtrapolated());
                        batch
                            .addStoreValuesQuery(
                                storeValuesQueryBuilder.build());
                    }

                    {
                        final StoreValuesQuery.Builder storeValuesQueryBuilder =
                            StoreValuesQuery
                                .newBuilder()
                                .setPoint(noticeValue.getPoint().get());

                        storeValuesQueryBuilder
                            .setNotBefore(_getSince(noticeValue.getStamp()));
                        storeValuesQueryBuilder
                            .setBefore(noticeValue.getStamp());
                        storeValuesQueryBuilder.setRows(1);
                        storeValuesQueryBuilder.setReverse(true);
                        batch
                            .addStoreValuesQuery(
                                storeValuesQueryBuilder.build());
                    }
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

        if (getRelation().isPresent()) {
            final ElapsedTime filterTimeLimit = getElapsedInputValue(
                FILTER_TIME_LIMIT_PARAM,
                Optional.empty(),
                Optional.empty())
                .orElse(null);
            final ElapsedTime filterStampTrimUnit = getElapsedInputValue(
                FILTER_STAMP_TRIM_UNIT_PARAM,
                Optional.empty(),
                Optional.empty())
                .orElse(null);
            final double stepSize = getDoubleInputValue(STEP_SIZE_PARAM, 0.0);
            final double deadbandGap = getDoubleInputValue(
                DEADBAND_GAP_PARAM,
                -1.0);
            final double deadbandRatio = getDoubleInputValue(
                DEADBAND_RATIO_PARAM,
                -1.0);
            double ceilingGap;
            double floorGap;
            double ratio;

            ceilingGap = getDoubleInputValue(CEILING_GAP_PARAM, -1.0);
            ratio = getDoubleInputValue(CEILING_RATIO_PARAM, 0.5);

            if ((ratio > 0.0) && (ceilingGap < 0.0)) {
                ceilingGap = stepSize * ratio;
            }

            floorGap = getDoubleInputValue(FLOOR_GAP_PARAM, -1.0);
            ratio = getDoubleInputValue(FLOOR_RATIO_PARAM, 0.5);

            if ((ratio > 0.0) && (floorGap < 0.0)) {
                floorGap = stepSize * ratio;
            }

            _stepFilter = new StepFilter(
                Optional.ofNullable(filterTimeLimit),
                Optional.ofNullable(filterStampTrimUnit),
                deadbandGap,
                deadbandRatio,
                stepSize,
                ceilingGap,
                floorGap);

            if (_stepFilter.isDisabled()) {
                getThisLogger()
                    .warn(
                        ProcessorMessages.STEP_FILTER_DISABLED,
                        getInputPoint());
            }

            _polated = isInputInterpolated() || isInputExtrapolated();
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void trigger(final PointValue noticeValue, final Batch batch)
    {
        final PointValue previousValue;
        final PointValue synthesizedValue;

        {
            final BatchValuesQuery.Builder batchValuesQueryBuilder =
                BatchValuesQuery
                    .newBuilder()
                    .setPoint(Optional.of(getResultPoint()));

            batchValuesQueryBuilder.setBefore(noticeValue.getStamp());
            previousValue = batch
                .getPointValue(batchValuesQueryBuilder.build());
        }

        if (_polated) {
            final BatchValuesQuery.Builder batchValuesQueryBuilder =
                BatchValuesQuery
                    .newBuilder()
                    .setPoint(Optional.of(getResultPoint()));

            batchValuesQueryBuilder.setAt(noticeValue.getStamp());
            batchValuesQueryBuilder.setInterpolated(isInputInterpolated());
            batchValuesQueryBuilder.setExtrapolated(isInputExtrapolated());
            synthesizedValue = batch
                .getPointValue(batchValuesQueryBuilder.build());
        } else {
            synthesizedValue = null;
        }

        final PointValue[] filteredValues = filter(
            noticeValue.copy(),
            Optional.of(previousValue),
            Optional.of(synthesizedValue));

        if (filteredValues.length == 0) {
            batch.forgetInputValue(noticeValue);
        } else {
            for (final PointValue filteredValue: filteredValues) {
                final ResultValue result = batch
                    .setUpResultValue(filteredValue.getStamp(), this);

                result.addInputValue(filteredValue);
            }
        }
    }

    /** {@inheritDoc}
     *
     * <p>If the input filters itself, it is not allowed to have other
     * dependents on the same Processor.</p>
     */
    @Override
    protected boolean doValidate()
    {
        if (!validateTransform(StepFilterTransform.class)) {
            return false;
        }

        if ((getInputPoint() == getResultPoint()) && !validateNoResults()) {
            return false;
        }

        return validateNotSynchronized();
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean supportsExtrapolated()
    {
        return getInputPoint() != getResultPoint();
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean supportsInterpolated()
    {
        return getInputPoint() != getResultPoint();
    }

    private DateTime _getSince(final DateTime stamp)
    {
        final DateTime since;

        if (hasSinceSyncPosition()) {
            since = getSyncStamp(getSinceSyncPosition(), stamp, false)
                .orElse(null);
        } else if (hasSinceResultSyncPosition()) {
            since = getResultSyncStamp(
                getSinceResultSyncPosition(),
                stamp,
                false)
                .get();
        } else if (hasAfterResultSyncPosition()) {
            since = getResultSyncStamp(
                getAfterResultSyncPosition(),
                stamp,
                false)
                .get()
                .after();
        } else {
            since = DateTime.BEGINNING_OF_TIME;
        }

        return since;
    }

    /**
     * Specifies the size of the offset relative to the next step which can be
     * filtered.
     */
    public static final String CEILING_GAP_PARAM = Point.CEILING_GAP_PARAM;

    /**
     * Specifies the ratio of the offset relative to the next Step which can be
     * filtered.
     */
    public static final String CEILING_RATIO_PARAM = Point.CEILING_RATIO_PARAM;

    /**
     * Specifies the ratio of the offset relative to the previous value which
     * will be filtered.
     */
    public static final String DEADBAND_RATIO_PARAM =
        Point.DEADBAND_RATIO_PARAM;

    /**
     * Specifies the size of the deadband around the previous value which will
     * be filtered.
     */
    public static final String DEADBAND_GAP_PARAM = Point.DEADBAND_GAP_PARAM;

    /**
     * Specifies the size of the offset relative to the previous Step which can
     * be filtered.
     */
    public static final String FLOOR_GAP_PARAM = Point.FLOOR_GAP_PARAM;

    /**
     * Specifies the ratio of the offset relative to the previous Step which
     * can be filtered.
     */
    public static final String FLOOR_RATIO_PARAM = Point.FLOOR_RATIO_PARAM;

    /** Specifies the value step size. */
    public static final String STEP_SIZE_PARAM = Point.STEP_SIZE_PARAM;

    private boolean _polated;
    private StepFilter _stepFilter;
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
