/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ReplicatorTransform.java 4039 2019-05-31 17:53:15Z SFB $
 */

package org.rvpf.processor.engine.replicator;

import java.util.List;
import java.util.Optional;

import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.entity.PointInput;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.Behavior;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.AbstractTransform;

/**
 * Replicator Transform.
 */
class ReplicatorTransform
    extends AbstractTransform
{
    /** {@inheritDoc}
     */
    @Override
    public Optional<PointValue> applyTo(
            final ResultValue resultValue,
            final Batch batch)
    {
        final PointValue inputValue = resultValue.getInputValues().get(0);
        final boolean same;
        PointValue outputValue;

        outputValue = (PointValue) resultValue.getValue();

        if (outputValue != null) {
            same = !inputValue.isDeleted()
                   && (inputValue.sameValueAs(
                       outputValue) || outputValue.sameValueAs(inputValue));
        } else {
            final Point inputPoint = inputValue.getPoint().get();

            same = inputValue.isDeleted()
                   || ((inputValue.getValue() == null)
                       && inputPoint.isNullRemoves(
                               false));
        }

        if (same) {
            outputValue = null;
        } else {
            outputValue = inputValue
                .morph(resultValue.getPoint(), Optional.empty());
        }

        return Optional.ofNullable(outputValue);
    }

    /** {@inheritDoc}
     *
     * <p>Only one input is allowed and it must have the Replicates
     * behavior.</p>
     */
    @Override
    public Optional<? extends Transform> getInstance(final Point point)
    {
        final List<? extends PointRelation> inputs = point.getInputs();
        final Behavior behavior;

        if (inputs.size() != 1) {
            getThisLogger()
                .error(
                    ProcessorMessages.TRANSFORM_SINGLE_INPUT,
                    getName(),
                    point);

            return Optional.empty();
        }

        behavior = ((PointInput) inputs.get(0))
            .getPrimaryBehavior()
            .orElse(null);

        if (!ReplicatedBehavior.class.isInstance(behavior)) {
            getThisLogger()
                .error(
                    ProcessorMessages.TRANSFORM_BEHAVIOR_1,
                    getName(),
                    ReplicatedBehavior.class.getName(),
                    point);

            return Optional.empty();
        }

        if (isNullRemoves(point)) {
            getThisLogger()
                .error(
                    ProcessorMessages.TRANSFORM_INCOMPATIBLE,
                    getName(),
                    NULL_REMOVES_PARAM);

            return Optional.empty();
        }

        return Optional.of(this);
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
