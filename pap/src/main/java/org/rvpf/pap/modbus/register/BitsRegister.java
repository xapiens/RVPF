/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BitsRegister.java 4036 2019-05-31 11:19:42Z SFB $
 */

package org.rvpf.pap.modbus.register;

import java.io.Serializable;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.pap.modbus.message.ReadTransaction;
import org.rvpf.pap.modbus.message.WriteTransaction;

/**
 * Bits register.
 */
public final class BitsRegister
    extends Register
{
    /**
     * Constructs an instance.
     *
     * @param address The optional register address.
     * @param readOnly True when read-only.
     */
    public BitsRegister(
            @Nonnull final Optional<Integer> address,
            final boolean readOnly)
    {
        super(address, readOnly);
    }

    /** {@inheritDoc}
     */
    @Override
    public ReadTransaction.Request createReadRequest()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public WriteTransaction.Request createWriteRequest()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public short getContent()
    {
        return _content;
    }

    /** {@inheritDoc}
     */
    @Override
    public PointValue[] getPointValues()
    {
        final PointValue[] pointValues = new PointValue[_points.length];

        for (int i = 0; i < _points.length; ++i) {
            if (_points[i] != null) {
                pointValues[i] = new PointValue(
                    _points[i],
                    Optional.empty(),
                    null,
                    (((getContent() >> i) & 1) != 0)
                    ? Boolean.TRUE: Boolean.FALSE);
            }
        }

        return pointValues;
    }

    /** {@inheritDoc}
     */
    @Override
    public Point[] getPoints()
    {
        return _points;
    }

    /** {@inheritDoc}
     */
    @Override
    public void putPointValue(final PointValue pointValue)
    {
        final Point point = pointValue.getPoint().get();

        for (int i = 0; i < _points.length; ++i) {
            if (point == _points[i]) {
                final Serializable value = pointValue.getValue();
                final short mask = (short) (1 << i);
                final boolean valueIsOn;

                if (value instanceof Boolean) {
                    valueIsOn = ((Boolean) value).booleanValue();
                } else if (value instanceof Number) {
                    valueIsOn = ((Number) value).intValue() != 0;
                } else {
                    valueIsOn = false;
                }

                if (valueIsOn) {
                    setContent((short) (getContent() | mask));
                } else {
                    setContent((short) (getContent() & ~mask));
                }

                break;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void setContent(final short content)
    {
        _content = content;
    }

    /**
     * Sets the point for a bit.
     *
     * @param point The optional point.
     * @param bit The bit.
     *
     * @return True on success.
     */
    public boolean setPoint(@Nonnull final Optional<Point> point, final int bit)
    {
        if (_points[bit] != null) {
            getThisLogger()
                .warn(
                    ModbusMessages.OVERLOADED_BIT,
                    point,
                    _points[bit],
                    Integer.valueOf(bit));

            return false;
        }

        _points[bit] = point.orElse(null);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public int size()
    {
        throw new UnsupportedOperationException();
    }

    private short _content;
    private final Point[] _points = new Point[16];
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
