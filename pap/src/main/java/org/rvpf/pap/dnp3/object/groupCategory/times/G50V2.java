/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: G50V2.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.groupCategory.times;

import java.nio.ByteBuffer;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;
import org.rvpf.pap.dnp3.object.ObjectVariation;

/**
 * Time and Date Absolute time and interval.
 */
public final class G50V2
    extends G50V1
{
    /** {@inheritDoc}
     */
    @Override
    public void dumpToBuffer(final ByteBuffer buffer)
    {
        super.dumpToBuffer(buffer);

        buffer.putInt(_interval);
    }

    /**
     * Gets the interval.
     *
     * @return The interval.
     */
    @Nonnull
    @CheckReturnValue
    public ElapsedTime getInterval()
    {
        return ElapsedTime.fromMillis((long) _interval & 0xFFFFFFFF);
    }

    /** {@inheritDoc}
     */
    @Override
    public int getObjectLength()
    {
        return Integer.BYTES;
    }

    /** {@inheritDoc}
     */
    @Override
    public ObjectVariation getObjectVariation()
    {
        return TimeDateVariation.ABSOLUTE_TIME_INTERVAL;
    }

    /** {@inheritDoc}
     */
    @Override
    public void loadFromBuffer(final ByteBuffer buffer)
    {
        super.loadFromBuffer(buffer);

        _interval = buffer.getInt();
    }

    /**
     * Sets the interval.
     *
     * @param relativeTime The interval.
     */
    public void setRelativeTime(@Nonnull final ElapsedTime relativeTime)
    {
        _interval = (int) relativeTime.toMillis();
    }

    private int _interval;
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
