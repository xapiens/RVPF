/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MaskedRegister.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.modbus.register;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.modbus.message.MaskWriteRegister;
import org.rvpf.pap.modbus.message.WriteTransaction;

/**
 * Masked register.
 */
public class MaskedRegister
    extends WordRegister
{
    /**
     * Constructs an instance.
     *
     * @param address The optional register address.
     * @param point The optional point associated with the register.
     * @param mask The mask.
     */
    public MaskedRegister(
            @Nonnull final Optional<Integer> address,
            @Nonnull final Optional<Point> point,
            final int mask)
    {
        super(address, point, false);

        _mask = mask;
    }

    /** {@inheritDoc}
     */
    @Override
    public WriteTransaction.Request createWriteRequest()
    {
        return new MaskWriteRegister.Request(
            getAddress().get().intValue(),
            ~_mask,
            getContent());
    }

    /** {@inheritDoc}
     */
    @Override
    public PointValue[] getPointValues()
    {
        final PointValue[] pointValues = super.getPointValues();

        if (pointValues.length == 1) {
            final PointValue pointValue = pointValues[0];
            final Short value = (Short) pointValue.getValue();

            pointValue
                .setValue(Short.valueOf((short) (value.intValue() & _mask)));
        }

        return pointValues;
    }

    private final int _mask;
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
