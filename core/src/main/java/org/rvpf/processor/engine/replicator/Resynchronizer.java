/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Resynchronizer.java 4043 2019-06-02 15:05:37Z SFB $
 */

package org.rvpf.processor.engine.replicator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointInput;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.Behavior;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.behavior.Synchronized;

/**
 * Resynchronizer.
 */
final class Resynchronizer
    extends ReplicatorTransform
{
    /** {@inheritDoc}
     */
    @Override
    public Optional<PointValue> applyTo(
            final ResultValue resultValue,
            final Batch batch)
    {
        final PointValue inputValue = resultValue.getInputValues().get(0);
        final Point resultPoint = resultValue.getPoint().get();
        final Sync resultSync = resultPoint.getSync().get();
        PointValue outputValue;

        if (resultSync.isInSync(resultValue.getStamp())) {
            if (inputValue != null) {
                outputValue = super.applyTo(resultValue, batch).orElse(null);

                if (outputValue != null) {
                    outputValue = outputValue
                        .morph(
                            Optional.empty(),
                            Optional.of(resultValue.getStamp()));
                }
            } else {
                outputValue = new PointValue(resultValue);
            }

            if (outputValue != null) {
                getThisLogger()
                    .debug(ProcessorMessages.RESYNCHRONIZED_VALUE, outputValue);
            }
        } else {    // Deletes the out of sync value.
            outputValue = new VersionedValue.Deleted(resultValue);
            getThisLogger()
                .debug(ProcessorMessages.DELETING_OUT_OF_SYNC, resultValue);
        }

        return Optional.ofNullable(outputValue);
    }

    /**
     * Gets the ceiling interval.
     *
     * @return The ceiling interval.
     */
    public ElapsedTime getCeilingInterval()
    {
        return _ceilingInterval;
    }

    /**
     * Gets the ceiling ratio.
     *
     * @return The ceiling ratio.
     */
    public double getCeilingRatio()
    {
        return _ceilingRatio;
    }

    /**
     * Gets the floor interval.
     *
     * @return The floor interval.
     */
    public ElapsedTime getFloorInterval()
    {
        return _floorInterval;
    }

    /**
     * Gets the floor ratio.
     *
     * @return The floor ratio.
     */
    public double getFloorRatio()
    {
        return _floorRatio;
    }

    /** {@inheritDoc}
     *
     * <p>The result must have a Sync.</p>
     *
     * <p>The first input must have the Resynchronized Behavior.</p>
     *
     * <p>A second input is allowed, but its Behavior must be Synchronized and
     * it must be the Sync Point.</p>
     */
    @Override
    public Optional<? extends Transform> getInstance(final Point point)
    {
        final List<? extends PointRelation> inputs = point.getInputs();
        PointInput relation;
        Behavior behavior;

        if (!point.isSynced()) {
            getThisLogger().error(ProcessorMessages.POINT_SYNC, point);

            return Optional.empty();
        }

        if (inputs.isEmpty()) {
            getThisLogger().error(ProcessorMessages.POINT_GE_1_INPUT, point);

            return Optional.empty();
        }

        relation = (PointInput) inputs.get(0);
        behavior = relation.getPrimaryBehavior().orElse(null);

        if (!ResynchronizedBehavior.class.isInstance(behavior)) {
            getThisLogger()
                .error(
                    ProcessorMessages.TRANSFORM_BEHAVIOR_1,
                    getName(),
                    ResynchronizedBehavior.class.getName(),
                    point);

            return Optional.empty();
        }

        if (inputs.size() > 2) {
            getThisLogger().error(ProcessorMessages.POINT_LE_2_INPUTS, point);

            return Optional.empty();
        }

        if (inputs.size() > 1) {
            relation = (PointInput) inputs.get(1);
            behavior = relation.getPrimaryBehavior().orElse(null);

            if (!Synchronized.class.isInstance(behavior)) {
                getThisLogger()
                    .error(
                        ProcessorMessages.TRANSFORM_BEHAVIOR_2,
                        getName(),
                        Synchronized.class.getName(),
                        point);

                return Optional.empty();
            }

            if (!Objects
                .equals(relation.getInputPoint().getSync(), point.getSync())) {
                getThisLogger()
                    .error(
                        ProcessorMessages.POINT_INPUT_2_SYNC,
                        getName(),
                        point);

                return Optional.empty();
            }
        } else if (_ceilingInterval != null) {
            getThisLogger()
                .error(
                    ProcessorMessages.POINT_INPUT_2_PRESENT,
                    point,
                    CEILING_INTERVAL_PARAM);

            return Optional.empty();
        } else if (_ceilingRatio > 0.0) {
            getThisLogger()
                .error(
                    ProcessorMessages.POINT_INPUT_2_NOT_0,
                    point,
                    CEILING_RATIO_PARAM);

            return Optional.empty();
        }

        return Optional.of(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        _floorInterval = getParams()
            .getElapsed(
                FLOOR_INTERVAL_PARAM,
                Optional.empty(),
                Optional.empty())
            .orElse(null);

        if (_floorInterval == null) {
            _floorRatio = getParams().getDouble(FLOOR_RATIO_PARAM, 0.0);
        }

        _ceilingInterval = getParams()
            .getElapsed(
                CEILING_INTERVAL_PARAM,
                Optional.empty(),
                Optional.empty())
            .orElse(null);

        if (_ceilingInterval == null) {
            _ceilingRatio = getParams().getDouble(CEILING_RATIO_PARAM, 0.0);
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected Logger getThisLogger()
    {
        return _LOGGER;
    }

    private static final Logger _LOGGER = Logger
        .getInstance(Resynchronizer.class);

    private ElapsedTime _ceilingInterval;
    private double _ceilingRatio;
    private ElapsedTime _floorInterval;
    private double _floorRatio;
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
