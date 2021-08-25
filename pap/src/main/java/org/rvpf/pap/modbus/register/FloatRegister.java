/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: FloatRegister.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.modbus.register;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.value.PointValue;

/**
 * Float register.
 */
public final class FloatRegister
    extends IntegerRegister
{
    /**
     * Constructs an instance.
     *
     * @param address The optional register address.
     * @param point The optional point associated with the register.
     * @param readOnly True when read-only.
     * @param middleEndian True if the node is middle-endian.
     */
    public FloatRegister(
            @Nonnull final Optional<Integer> address,
            @Nonnull final Optional<Point> point,
            final boolean readOnly,
            final boolean middleEndian)
    {
        super(address, point, true, readOnly, middleEndian);
    }

    /** {@inheritDoc}
     */
    @Override
    public PointValue[] getPointValues()
    {
        final Point[] points = getPoints();
        final PointValue[] pointValues;

        if (points != NO_POINTS) {
            pointValues = new PointValue[] {new PointValue(
                points[0],
                Optional.empty(),
                null,
                Float.valueOf(Float.intBitsToFloat(getInt()))), };
        } else {
            pointValues = NO_POINT_VALUES;
        }

        return pointValues;
    }

    /** {@inheritDoc}
     */
    @Override
    public void putPointValue(final PointValue pointValue)
    {
        setInt(
            Float
                .floatToIntBits(
                    (pointValue.getValue() instanceof Number)
                    ? ((Number) pointValue
                            .getValue())
                            .floatValue(): Float.NaN));
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
