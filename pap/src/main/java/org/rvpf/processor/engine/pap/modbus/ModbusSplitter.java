/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ModbusSplitter.java 4076 2019-06-11 17:09:30Z SFB $
 */

package org.rvpf.processor.engine.pap.modbus;

import java.io.Serializable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.Content;
import org.rvpf.base.Origin;
import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.content.BooleanContent;
import org.rvpf.content.TupleContent;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.modbus.Modbus;
import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.processor.engine.pap.PAPSplitter;

/**
 * Modbus splitter.
 */
public final class ModbusSplitter
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
        final Content pointContent = point.getContent().orElse(null);
        final boolean multipleValues = pointContent instanceof TupleContent;
        final boolean isBits = multipleValues? ((TupleContent) pointContent)
            .getContent() instanceof BooleanContent: false;
        boolean middleEndian = false;

        if (origin.isPresent()) {
            final Attributes attributes = origin
                .get()
                .getAttributes(Modbus.ATTRIBUTES_USAGE)
                .orElse(null);

            if (attributes != null) {
                if (multipleValues && !isBits) {
                    final boolean littleEndian = attributes
                        .getBoolean(Modbus.LITTLE_ENDIAN_ATTRIBUTE);

                    middleEndian = attributes
                        .getBoolean(Modbus.MIDDLE_ENDIAN_ATTRIBUTE);

                    if (littleEndian) {    // Inverts the interpretation.
                        middleEndian = !middleEndian;
                    }
                }
            } else {
                getThisLogger()
                    .debug(
                        PAPMessages.MISSING_ATTRIBUTES,
                        Modbus.ATTRIBUTES_USAGE,
                        origin);
            }
        } else {
            getThisLogger().debug(PAPMessages.MISSING_ORIGIN, point);
        }

        if (multipleValues && !isBits) {
            getThisLogger()
                .debug(
                    ModbusMessages.POINT_SPLITS_MIDDLE_ENDIAN,
                    point,
                    String.valueOf(middleEndian));
        }

        final List<? extends PointRelation> resultRelations = point
            .getResults();
        final _Detail[] details = new _Detail[resultRelations.size()];
        int detail = 0;
        boolean success = true;

        for (final PointRelation resultRelation: resultRelations) {
            final Point resultPoint = resultRelation.getResultPoint();

            if (resultPoint.getInputs().size() > 1) {
                getThisLogger().warn(PAPMessages.POINT_EQ_1_INPUT, resultPoint);
                success = false;

                continue;
            }

            if (multipleValues) {
                final int offset = getResultPosition(resultRelation);

                if (offset < 0) {
                    success = false;

                    continue;
                }

                final Params resultRelationParams = resultRelation.getParams();
                final int size;
                final int bit;
                final boolean isSigned;
                final boolean isFloat;

                if (isBits) {
                    bit = -1;
                    isFloat = false;
                    size = 0;
                    isSigned = false;
                } else {
                    bit = resultRelationParams.getInt(Modbus.BIT_PARAM, -1);
                    isFloat = resultRelationParams
                        .getBoolean(Modbus.FLOAT_PARAM, false);
                    size = resultRelationParams
                        .getInt(
                            Modbus.SIZE_PARAM,
                            isFloat? 2: 1);
                    isSigned = resultRelationParams
                        .getBoolean(Modbus.SIGNED_PARAM, false);

                    if ((1 > size)
                            || (size > 4)
                            || (isFloat && !((size == 2) || (size == 4)))) {
                        getThisLogger()
                            .warn(
                                PAPMessages.BAD_PARAMETER_VALUE,
                                Modbus.SIZE_PARAM,
                                String.valueOf(size));
                        success = false;

                        continue;
                    }
                }

                details[detail++] = new _Detail(
                    resultPoint,
                    offset,
                    size,
                    bit,
                    isSigned,
                    isFloat);
            } else {
                details[detail++] = new _Detail(
                    resultPoint,
                    0,
                    0,
                    0,
                    false,
                    false);
            }
        }

        if (details.length > 0) {
            _plans.put(point, new _Plan(details, middleEndian, multipleValues));
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
        if (!plan.isMultipleValues()) {
            return Optional.ofNullable(tuple.get(0));
        }

        final int size = detail.getSize();
        int offset = detail.getPosition();

        if (size == 0) {    // Is bits.
            if (tuple.size() < (offset + 1)) {
                return Optional.empty();
            }

            return Optional.ofNullable(tuple.get(offset));
        }

        int bit = detail.getBit();

        if (bit >= size * Short.SIZE) {
            offset += bit / (size * Short.SIZE);
            bit %= Short.SIZE;
        }

        if (tuple.size() < (offset + size)) {
            return Optional.empty();
        }

        long holder = 0;

        for (int i = 0; i < size; ++i) {
            final Serializable item = tuple
                .get(offset + (plan.isMiddleEndian()? ((size - i) - 1): i));

            if (item instanceof Number) {
                holder <<= Short.SIZE;
                holder |= ((Number) item).shortValue() & 0xFFFF;
            } else {
                return Optional.empty();
            }
        }

        if (bit >= 0) {
            holder &= 1 << bit;
        } else if (detail.isSigned()) {
            final int shift = ((Long.SIZE / Short.SIZE) - size) * Short.SIZE;

            holder <<= shift;
            holder >>= shift;
        }

        final Serializable value;

        if (bit >= 0) {
            value = Boolean.valueOf(holder != 0);
        } else if (detail.isFloat()) {
            if (size == 2) {
                value = Float.valueOf(Float.intBitsToFloat((int) holder));
            } else {
                value = Double.valueOf(Double.longBitsToDouble(holder));
            }
        } else {
            value = Long.valueOf(holder);
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
         * @param size The number of tuple items.
         * @param bit The bit in the tuple value (negative if none).
         * @param signed True if the tuple value is signed.
         * @param isFloat True if the tuple value is a float.
         */
        _Detail(
                @Nonnull final Point point,
                final int position,
                final int size,
                final int bit,
                final boolean signed,
                final boolean isFloat)
        {
            super(point, position, bit);

            _size = size;
            _signed = ((bit < 0) && !isFloat)? signed: false;
            _float = isFloat;
        }

        /**
         * Gets the size.
         *
         * @return The size (0 for bit).
         */
        @CheckReturnValue
        int getSize()
        {
            return _size;
        }

        /**
         * Gets the float indicator.
         *
         * @return The float indicator.
         */
        @CheckReturnValue
        boolean isFloat()
        {
            return _float;
        }

        /**
         * Gets the signed indicator.
         *
         * @return The signed indicator.
         */
        @CheckReturnValue
        boolean isSigned()
        {
            return _signed;
        }

        private final boolean _float;
        private final boolean _signed;
        private final int _size;
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
         * @param middleEndian The middle endian indicator.
         * @param multipleValues The multiple values indicator.
         */
        _Plan(
                @Nonnull final _Detail[] details,
                final boolean middleEndian,
                final boolean multipleValues)
        {
            super(details);

            _middleEndian = middleEndian;
            _multipleValues = multipleValues;
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

        /**
         * Gets the middle endian indicator.
         *
         * @return The middle endian indicator.
         */
        @CheckReturnValue
        boolean isMiddleEndian()
        {
            return _middleEndian;
        }

        /**
         * Gets the multiple values indicator.
         *
         * @return The multiple values indicator.
         */
        @CheckReturnValue
        boolean isMultipleValues()
        {
            return _multipleValues;
        }

        private final boolean _middleEndian;
        private final boolean _multipleValues;
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
