/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: G32V7.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.groupCategory.analogInputs;

import java.nio.ByteBuffer;

import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.pap.dnp3.object.ObjectInstance;
import org.rvpf.pap.dnp3.object.ObjectVariation;

/**
 * Analog Input Event Single-precision with time.
 */
public final class G32V7
    extends G32V5
    implements ObjectInstance.WithTime
{
    /** {@inheritDoc}
     */
    @Override
    public void dumpToBuffer(final ByteBuffer buffer)
    {
        super.dumpToBuffer(buffer);

        putTimeToBuffer(_time, buffer);
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
        return AnalogInputEventVariation.FLOAT_WITH_TIME;
    }

    /** {@inheritDoc}
     */
    @Override
    public DateTime getTime()
    {
        return DateTime.fromMillis(_time);
    }

    /** {@inheritDoc}
     */
    @Override
    public void loadFromBuffer(final ByteBuffer buffer)
    {
        super.loadFromBuffer(buffer);

        _time = getTimeFromBuffer(buffer);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setTime(@Nonnull final DateTime time)
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
