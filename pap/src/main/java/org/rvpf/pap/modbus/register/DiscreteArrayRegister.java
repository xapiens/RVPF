/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DiscreteArrayRegister.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.modbus.register;

import java.io.Serializable;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.pap.modbus.message.ReadCoils;
import org.rvpf.pap.modbus.message.ReadDiscreteInputs;
import org.rvpf.pap.modbus.message.ReadTransaction;
import org.rvpf.pap.modbus.message.WriteMultipleCoils;
import org.rvpf.pap.modbus.message.WriteTransaction;

/**
 * Discrete array register.
 */
public final class DiscreteArrayRegister
    extends ArrayRegister
{
    /**
     * Constructs an instance.
     *
     * @param address The optional register address.
     * @param size The array size.
     * @param point The optional point associated with the register.
     * @param readOnly True when read-only.
     */
    public DiscreteArrayRegister(
            @Nonnull final Optional<Integer> address,
            final int size,
            @Nonnull final Optional<Point> point,
            final boolean readOnly)
    {
        super(address, size, point, readOnly);
    }

    /** {@inheritDoc}
     */
    @Override
    public ReadTransaction.Request createReadRequest()
    {
        final int startingAddress = getAddress().get().intValue();

        return isReadOnly()? new ReadDiscreteInputs.Request(
            startingAddress,
            size()): new ReadCoils.Request(startingAddress, size());
    }

    /** {@inheritDoc}
     */
    @Override
    public WriteTransaction.Request createWriteRequest()
    {
        return new WriteMultipleCoils.Request(
            getAddress().get().intValue(),
            getContents());
    }

    /** {@inheritDoc}
     */
    @Override
    public PointValue[] getPointValues()
    {
        final PointValue[] pointValues;

        if (getPoints() != NO_POINTS) {
            final short[] contents = getContents();
            final Tuple tuple = new Tuple(contents.length);

            for (final short content: contents) {
                tuple.add(Boolean.valueOf(content != 0));
            }

            pointValues = new PointValue[] {new PointValue(
                getPoints()[0],
                Optional.empty(),
                null,
                tuple), };
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
        final Serializable values = pointValue.getValue();

        if (!(values instanceof Tuple)) {
            throw new IllegalArgumentException();
        }

        final Tuple tuple = (Tuple) values;

        if (tuple.size() != size()) {
            throw new IllegalArgumentException();
        }

        final short[] contents = getContents();

        for (int i = 0; i < contents.length; ++i) {
            final Serializable value = tuple.get(i);
            final boolean valueIsOn;

            if (value instanceof Boolean) {
                valueIsOn = ((Boolean) value).booleanValue();
            } else if (value instanceof Number) {
                valueIsOn = ((Number) value).intValue() != 0;
            } else {
                valueIsOn = false;
            }

            contents[i] = (short) (valueIsOn? 1: 0);
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
