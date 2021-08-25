/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: WordRegister.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.modbus.register;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.modbus.message.ReadHoldingRegisters;
import org.rvpf.pap.modbus.message.ReadInputRegisters;
import org.rvpf.pap.modbus.message.ReadTransaction;
import org.rvpf.pap.modbus.message.WriteSingleRegister;
import org.rvpf.pap.modbus.message.WriteTransaction;

/**
 * Word register.
 */
public class WordRegister
    extends Register
{
    /**
     * Constructs an instance.
     *
     * @param address The optional register address.
     * @param point The optional point associated with the register.
     * @param readOnly True when read-only.
     */
    public WordRegister(
            @Nonnull final Optional<Integer> address,
            @Nonnull final Optional<Point> point,
            final boolean readOnly)
    {
        super(address, readOnly);

        _points = point.isPresent()? new Point[] {point.get()}: NO_POINTS;
    }

    /** {@inheritDoc}
     */
    @Override
    public ReadTransaction.Request createReadRequest()
    {
        final int startingAddress = getAddress().get().intValue();

        return isReadOnly()? new ReadInputRegisters.Request(
            startingAddress,
            size()): new ReadHoldingRegisters.Request(startingAddress, size());
    }

    /** {@inheritDoc}
     */
    @Override
    public WriteTransaction.Request createWriteRequest()
    {
        return new WriteSingleRegister.Request(
            getAddress().get().intValue(),
            getContent());
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
        final PointValue[] pointValues;

        if (_points != NO_POINTS) {
            pointValues = new PointValue[] {new PointValue(
                getPoints()[0],
                Optional.empty(),
                null,
                Short.valueOf(getContent())), };
        } else {
            pointValues = NO_POINT_VALUES;
        }

        return pointValues;
    }

    /** {@inheritDoc}
     */
    @Override
    public final Point[] getPoints()
    {
        return _points;
    }

    /** {@inheritDoc}
     */
    @Override
    public void putPointValue(final PointValue pointValue)
    {
        setContent(
            (pointValue.getValue() instanceof Number)? ((Number) pointValue
                .getValue())
                .shortValue(): 0);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setContent(final short content)
    {
        _content = content;
    }

    /** {@inheritDoc}
     */
    @Override
    public int size()
    {
        return 1;
    }

    private short _content;
    private final Point[] _points;
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
