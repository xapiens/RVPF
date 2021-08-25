/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: G2V3.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.groupCategory.binaryInputs;

import java.nio.ByteBuffer;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;
import org.rvpf.pap.dnp3.object.ObjectVariation;

/**
 * Binary Input Event With relative time.
 */
public final class G2V3
    extends G2V1
{
    /** {@inheritDoc}
     */
    @Override
    public void dumpToBuffer(final ByteBuffer buffer)
    {
        super.dumpToBuffer(buffer);

        buffer.putShort(_relativeTime);
    }

    /** {@inheritDoc}
     */
    @Override
    public int getObjectLength()
    {
        return super.getObjectLength() + Short.BYTES;
    }

    /** {@inheritDoc}
     */
    @Override
    public ObjectVariation getObjectVariation()
    {
        return BinaryInputEventVariation.WITH_RELATIVE_TIME;
    }

    /**
     * Gets the relative time.
     *
     * @return The relative time.
     */
    @Nonnull
    @CheckReturnValue
    public ElapsedTime getRelativeTime()
    {
        return ElapsedTime.fromMillis(_relativeTime & 0xFFFF);
    }

    /** {@inheritDoc}
     */
    @Override
    public void loadFromBuffer(final ByteBuffer buffer)
    {
        super.loadFromBuffer(buffer);

        _relativeTime = buffer.getShort();
    }

    /**
     * Sets the relative time.
     *
     * @param relativeTime The relative time.
     */
    public void setRelativeTime(@Nonnull final ElapsedTime relativeTime)
    {
        _relativeTime = (short) relativeTime.toMillis();
    }

    private short _relativeTime;
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
