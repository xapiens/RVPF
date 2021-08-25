/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: G11V2.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.groupCategory.binaryOutputs;

import java.nio.ByteBuffer;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.pap.dnp3.object.ObjectVariation;

/**
 * Binary Output Event With time.
 */
public final class G11V2
    extends G11V1
{
    /** {@inheritDoc}
     */
    @Override
    public void dumpToBuffer(final ByteBuffer buffer)
    {
        super.dumpToBuffer(buffer);

        putTimeToBuffer(_absoluteTime, buffer);
    }

    /**
     * Gets the absolute time.
     *
     * @return The absolute time.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime getAbsoluteTime()
    {
        return DateTime.fromMillis(_absoluteTime);
    }

    /** {@inheritDoc}
     */
    @Override
    public int getObjectLength()
    {
        return super.getObjectLength() + TIME_BYTES;
    }

    /** {@inheritDoc}
     */
    @Override
    public ObjectVariation getObjectVariation()
    {
        return BinaryOutputEventVariation.WITH_TIME;
    }

    /** {@inheritDoc}
     */
    @Override
    public void loadFromBuffer(final ByteBuffer buffer)
    {
        super.loadFromBuffer(buffer);

        _absoluteTime = getTimeFromBuffer(buffer);
    }

    /**
     * Sets the absolute time.
     *
     * @param absoluteTime The absolute time.
     */
    public void setAbsoluteTime(@Nonnull final DateTime absoluteTime)
    {
        _absoluteTime = absoluteTime.toMillis();
    }

    private long _absoluteTime;
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
