/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: G20V5.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.groupCategory.counters;

import java.io.Serializable;

import java.nio.ByteBuffer;

import org.rvpf.pap.dnp3.object.ObjectInstance;
import org.rvpf.pap.dnp3.object.ObjectVariation;

/**
 * Counter 32-bit without flag.
 */
public class G20V5
    extends ObjectInstance.Abstract
    implements ObjectInstance.WithValue
{
    /** {@inheritDoc}
     */
    @Override
    public void dumpToBuffer(final ByteBuffer buffer)
    {
        buffer.putInt(_count);
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
        return CounterVariation.UNSIGNED_WITHOUT_FLAG;
    }

    /** {@inheritDoc}
     */
    @Override
    public final Serializable getValue()
    {
        return Long.valueOf(_count & 0xFFFFFFFFL);
    }

    /** {@inheritDoc}
     */
    @Override
    public void loadFromBuffer(final ByteBuffer buffer)
    {
        _count = buffer.getInt();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void setValue(final Serializable value)
    {
        _count = WithValue.intValue(value);
    }

    private int _count;
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
