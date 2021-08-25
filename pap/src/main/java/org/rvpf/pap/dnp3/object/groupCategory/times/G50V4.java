/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: G50V4.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.groupCategory.times;

import java.nio.ByteBuffer;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.pap.dnp3.object.ObjectVariation;
import org.rvpf.pap.dnp3.object.content.IntervalUnit;

/**
 * Time and Date Indexed absolute time and long interval.
 */
public final class G50V4
    extends G50V1
{
    /** {@inheritDoc}
     */
    @Override
    public void dumpToBuffer(final ByteBuffer buffer)
    {
        super.dumpToBuffer(buffer);

        buffer.putInt(_count);
        buffer.put(_unit);
    }

    /**
     * Gets the count.
     *
     * @return The count.
     */
    @CheckReturnValue
    public long getCount()
    {
        return (long) _count & 0xFFFFFFFF;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getObjectLength()
    {
        return super.getObjectLength() + Integer.BYTES + Byte.BYTES;
    }

    /** {@inheritDoc}
     */
    @Override
    public ObjectVariation getObjectVariation()
    {
        return TimeDateVariation.INDEXED_TIME_INTERVAL;
    }

    /**
     * Gets the unit.
     *
     * @return The unit (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<IntervalUnit> getUnit()
    {
        return IntervalUnit.instance(_unit & 0xFF);
    }

    /** {@inheritDoc}
     */
    @Override
    public void loadFromBuffer(final ByteBuffer buffer)
    {
        super.loadFromBuffer(buffer);

        _count = buffer.getInt();
        _unit = buffer.get();
    }

    /**
     * Sets the count.
     *
     * @param count The count.
     */
    public void setCount(final long count)
    {
        _count = (int) count;
    }

    /**
     * Sets the unit.
     *
     * @param unit The unit.
     */
    public void setUnit(@Nonnull final IntervalUnit unit)
    {
        _unit = (byte) unit.getCode();
    }

    private int _count;
    private byte _unit;
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
