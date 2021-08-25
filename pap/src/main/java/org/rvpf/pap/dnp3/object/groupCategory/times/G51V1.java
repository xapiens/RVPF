/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: G51V1.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.groupCategory.times;

import java.nio.ByteBuffer;

import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.pap.dnp3.object.ObjectInstance;
import org.rvpf.pap.dnp3.object.ObjectVariation;

/**
 * Time and Date Common Time-of-Occurrence Absolute time synchronized.
 */
public class G51V1
    extends ObjectInstance.Abstract
    implements ObjectInstance.WithTime
{
    /** {@inheritDoc}
     */
    @Override
    public void dumpToBuffer(final ByteBuffer buffer)
    {
        putTimeToBuffer(_time, buffer);
    }

    /** {@inheritDoc}
     */
    @Override
    public int getObjectLength()
    {
        return TIME_BYTES;
    }

    /** {@inheritDoc}
     */
    @Override
    public ObjectVariation getObjectVariation()
    {
        return TimeDateCommonTooVariation.ABSOLUTE_TIME_SYNCHRONIZED;
    }

    /** {@inheritDoc}
     */
    @Override
    public final DateTime getTime()
    {
        return DateTime.fromMillis(_time);
    }

    /** {@inheritDoc}
     */
    @Override
    public void loadFromBuffer(final ByteBuffer buffer)
    {
        _time = getTimeFromBuffer(buffer);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void setTime(@Nonnull final DateTime time)
    {
        _time = time.toMillis();
    }

    private long _time;
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
