/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CIPSplitter.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.processor.engine.pap.cip;

import java.io.Serializable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.content.TupleContent;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.cip.CIP;
import org.rvpf.processor.engine.pap.PAPSplitter;

/**
 * CIP splitter.
 */
public final class CIPSplitter
    extends PAPSplitter
{
    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Point point)
    {
        if (_plans.containsKey(point)) {
            return true;
        }

        final Optional<? extends Origin> origin = point.getOrigin();

        if (origin.isPresent()) {
            final Optional<Attributes> attributes = origin
                .get()
                .getAttributes(CIP.ATTRIBUTES_USAGE);

            if (!attributes.isPresent()) {
                getThisLogger()
                    .debug(
                        PAPMessages.MISSING_ATTRIBUTES,
                        CIP.ATTRIBUTES_USAGE,
                        origin.get());
            }
        } else {
            getThisLogger().debug(PAPMessages.MISSING_ORIGIN, point);
        }

        final boolean multipleValues = point
            .getContent()
            .orElse(null) instanceof TupleContent;
        final List<? extends PointRelation> resultRelations = point
            .getResults();
        final _Detail[] details = new _Detail[resultRelations.size()];
        int detail = 0;
        boolean success = true;

        for (final PointRelation resultRelation: resultRelations) {
            final int index = multipleValues? getResultPosition(
                resultRelation): 0;

            if (index >= 0) {
                final int bit = resultRelation
                    .getParams()
                    .getInt(CIP.BIT_PARAM, -1);

                if (bit < 64) {
                    details[detail++] = new _Detail(
                        resultRelation.getResultPoint(),
                        index,
                        bit);
                } else {
                    getThisLogger()
                        .warn(
                            PAPMessages.BAD_PARAMETER_VALUE,
                            CIP.BIT_PARAM,
                            String.valueOf(bit));
                    success = false;
                }
            } else {
                success = false;
            }
        }

        if (details.length > 0) {
            _plans.put(point, new _Plan(details));
        }

        return success;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<Splitted> split(final PointValue pointValue)
    {
        final Serializable value = pointValue.getValue();

        if (value instanceof Splitted) {
            return Optional.of((Splitted) value);
        }

        final Tuple tuple;

        if (value instanceof Tuple) {
            tuple = (Tuple) value;
        } else {
            tuple = new Tuple(1);
            tuple.add(value);
        }

        final _Plan plan = _plans.get(pointValue.getPoint().get());

        if (plan == null) {
            return Optional.empty();
        }

        final Splitted splitted = new Splitted();

        for (final _Detail detail: plan.getDetails()) {
            splitted.put(detail.getPoint(), _getValue(plan, tuple, detail));
        }

        return Optional.of(splitted);
    }

    private static Optional<Serializable> _getValue(
            final _Plan plan,
            final Tuple tuple,
            final _Detail detail)
    {
        int index = detail.getPosition();

        if (index >= tuple.size()) {
            return Optional.empty();
        }

        final Serializable value;
        Serializable serializable = tuple.get(index);

        if (serializable instanceof Number) {
            int bit = detail.getBit();

            if (bit >= 0) {
                final int size;

                if (serializable instanceof Integer) {
                    size = Integer.SIZE;
                } else if (serializable instanceof Short) {
                    size = Short.SIZE;
                } else if (serializable instanceof Byte) {
                    size = Byte.SIZE;
                } else if (serializable instanceof Long) {
                    size = Long.SIZE;
                } else {
                    return Optional.empty();
                }

                if (bit >= size) {
                    index += bit / size;
                    bit %= size;

                    if (index >= tuple.size()) {
                        return Optional.empty();
                    }

                    serializable = tuple.get(index);
                }

                value = Boolean
                    .valueOf(
                        (((Number) serializable).longValue() & (1 << bit))
                        != 0);
            } else {
                value = serializable;
            }
        } else {
            value = serializable;
        }

        return Optional.ofNullable(value);
    }

    private final Map<Point, _Plan> _plans = new IdentityHashMap<>();

    /**
     * Detail.
     */
    private static final class _Detail
        extends Detail
    {
        /**
         * Constructs an instance.
         *
         * @param point The point.
         * @param position The position in the tuple.
         * @param bit The bit in the tuple value (negative if none).
         */
        _Detail(@Nonnull final Point point, final int position, final int bit)
        {
            super(point, position, bit);
        }
    }


    /**
     * _Plan.
     */
    private static final class _Plan
        extends Plan
    {
        /**
         * Constructs an instance.
         *
         * @param details Plan details.
         */
        _Plan(@Nonnull final _Detail[] details)
        {
            super(details);
        }

        /**
         * Gets the details.
         *
         * @return The details.
         */
        @Override
        protected _Detail[] getDetails()
        {
            return (_Detail[]) super.getDetails();
        }
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
