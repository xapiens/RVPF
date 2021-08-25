/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: QuadWordRegister.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.modbus.register;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;

/**
 * Quad word register.
 */
public abstract class QuadWordRegister
    extends DoubleWordRegister
{
    /**
     * Constructs an instance.
     *
     * @param address The optional register address.
     * @param point The optional point associated with the register.
     * @param readOnly True when read-only.
     * @param middleEndian True if the node is middle-endian.
     */
    public QuadWordRegister(
            @Nonnull final Optional<Integer> address,
            @Nonnull final Optional<Point> point,
            final boolean readOnly,
            final boolean middleEndian)
    {
        super(address, point, readOnly, middleEndian);

        _nextRegister = new DoubleWordRegister(
            Optional.of(Integer.valueOf(address.get().intValue() + 2)),
            Optional.empty(),
            readOnly,
            middleEndian);
    }

    /**
     * Gets a word register.
     *
     * @param index The word register index (0..3).
     *
     * @return A new word register for the next value.
     */
    @Nonnull
    @CheckReturnValue
    public WordRegister getWordRegister(final int index)
    {
        switch (index) {
            case 0: {
                return this;
            }
            case 1: {
                return getNextRegister();
            }
            case 2: {
                return _nextRegister;
            }
            case 3: {
                return _nextRegister.getNextRegister();
            }
            default: {
                throw new InternalError();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public int size()
    {
        return 4;
    }

    private final DoubleWordRegister _nextRegister;
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
