/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: G1V1.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.groupCategory.binaryInputs;

import java.nio.ByteBuffer;

import org.rvpf.pap.dnp3.object.ObjectInstance;
import org.rvpf.pap.dnp3.object.ObjectVariation;

/**
 * Binary Input Packed format.
 */
public final class G1V1
    extends ObjectInstance.Abstract
    implements ObjectInstance.Packed
{
    /** {@inheritDoc}
     */
    @Override
    public void dumpToBuffer(final ByteBuffer buffer)
    {
        buffer.put(_states);
    }

    /** {@inheritDoc}
     */
    @Override
    public int get(final int position)
    {
        return getBits(_states, position, 1, 0x01);
    }

    /** {@inheritDoc}
     */
    @Override
    public int getObjectLength()
    {
        return _states.length;
    }

    /** {@inheritDoc}
     */
    @Override
    public ObjectVariation getObjectVariation()
    {
        return BinaryInputVariation.PACKED_FORMAT;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getValueCount()
    {
        return _valueCount;
    }

    /** {@inheritDoc}
     */
    @Override
    public void loadFromBuffer(final ByteBuffer buffer)
    {
        _states = new byte[byteCount(1)];
        buffer.get(_states);
    }

    /** {@inheritDoc}
     */
    @Override
    public void put(final int position, final int value)
    {
        _states = putBits(value, _states, position, 1, 0x01);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setValueCount(final int count)
    {
        _valueCount = count;
    }

    private byte[] _states;
    private int _valueCount;
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
