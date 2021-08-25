/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DoubleWordRegister.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.pap.modbus.register;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.pap.modbus.message.WriteMultipleRegisters;
import org.rvpf.pap.modbus.message.WriteTransaction;

/**
 * Double word register.
 */
public class DoubleWordRegister
    extends WordRegister
{
    /**
     * Constructs an instance.
     *
     * @param address The optional register address.
     * @param point The optional point associated with the register.
     * @param readOnly True when read-only.
     * @param middleEndian True if the node is middle-endian.
     */
    public DoubleWordRegister(
            @Nonnull final Optional<Integer> address,
            @Nonnull final Optional<Point> point,
            final boolean readOnly,
            final boolean middleEndian)
    {
        super(address, point, readOnly);

        _middleEndian = middleEndian;
        _nextRegister = new WordRegister(
            Optional.of(Integer.valueOf(address.get().intValue() + 1)),
            Optional.empty(),
            readOnly);
    }

    /** {@inheritDoc}
     */
    @Override
    public WriteTransaction.Request createWriteRequest()
    {
        final short[] contents = new short[] {getContent(),
                getNextRegister().getContent(), };

        return new WriteMultipleRegisters.Request(
            getAddress().get().intValue(),
            contents);
    }

    /**
     * Gets a register for the next value.
     *
     * @return A new word register for the next value.
     */
    @Nonnull
    @CheckReturnValue
    public final WordRegister getNextRegister()
    {
        return _nextRegister;
    }

    /** {@inheritDoc}
     */
    @Override
    public void setContents(@Nonnull final short[] contents)
    {
        Require.success(contents.length == size());

        setContent(contents[0]);
        getNextRegister().setContent(contents[1]);
    }

    /** {@inheritDoc}
     */
    @Override
    public int size()
    {
        return 2;
    }

    /**
     * Gets the middle endian indicator.
     *
     * @return The middle endian indicator.
     */
    protected boolean isMiddleEndian()
    {
        return _middleEndian;
    }

    private final boolean _middleEndian;
    private final WordRegister _nextRegister;
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
