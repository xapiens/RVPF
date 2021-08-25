/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LongRegister.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.modbus.register;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.value.PointValue;

/**
 * Long register.
 */
public class LongRegister
    extends QuadWordRegister
{
    /**
     * Constructs an instance.
     *
     * @param address The optional register address.
     * @param point The optional point associated with the register.
     * @param readOnly True when read-only.
     * @param middleEndian True if the node is middle-endian.
     */
    public LongRegister(
            @Nonnull final Optional<Integer> address,
            @Nonnull final Optional<Point> point,
            final boolean readOnly,
            final boolean middleEndian)
    {
        super(address, point, readOnly, middleEndian);
    }

    /** {@inheritDoc}
     */
    @Override
    public PointValue[] getPointValues()
    {
        final PointValue[] pointValues;

        if (getPoints() != NO_POINTS) {
            pointValues = new PointValue[] {new PointValue(
                getPoints()[0],
                Optional.empty(),
                null,
                Long.valueOf(getLong())), };
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
        setLong(
            (pointValue.getValue() instanceof Number)? ((Number) pointValue
                .getValue())
                .longValue(): 0);
    }

    /**
     * Gets the long value from the word registers.
     *
     * @return The long value.
     */
    @CheckReturnValue
    protected long getLong()
    {
        final int limit = size() - 1;
        long longValue = 0;

        if (isMiddleEndian()) {    // Assumes little-endian words order.
            for (int i = limit; i >= 0; --i) {
                longValue <<= 16;
                longValue |= getWordRegister(i).getContent() & 0xFFFF;
            }
        } else {    // Assumes big-endian words order.
            for (int i = 0; i <= limit; ++i) {
                longValue <<= 16;
                longValue |= getWordRegister(i).getContent() & 0xFFFF;
            }
        }

        return longValue;
    }

    /**
     * Sets the long value to the word registers.
     *
     * @param longValue The long value.
     */
    protected void setLong(long longValue)
    {
        final int limit = size() - 1;

        if (isMiddleEndian()) {    // Assumes little-endian words order.
            for (int i = 0; i <= limit; ++i) {
                getWordRegister(i).setContent((short) longValue);
                longValue >>= 16;
            }
        } else {    // Assumes big-endian words order.
            for (int i = limit; i >= 0; --i) {
                getWordRegister(i).setContent((short) longValue);
                longValue >>= 16;
            }
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
