/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: G31V6.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.groupCategory.analogInputs;

import java.io.Serializable;

import java.nio.ByteBuffer;

import org.rvpf.pap.dnp3.object.ObjectInstance;
import org.rvpf.pap.dnp3.object.ObjectVariation;

/**
 * Frozen Analog Input 16-bit without flag.
 */
public class G31V6
    extends ObjectInstance.Abstract
    implements ObjectInstance.WithValue
{
    /** {@inheritDoc}
     */
    @Override
    public void dumpToBuffer(final ByteBuffer buffer)
    {
        buffer.putShort(_value);
    }

    /** {@inheritDoc}
     */
    @Override
    public int getObjectLength()
    {
        return Short.BYTES;
    }

    /** {@inheritDoc}
     */
    @Override
    public ObjectVariation getObjectVariation()
    {
        return FrozenAnalogInputVariation.SHORT_WITHOUT_FLAG;
    }

    /** {@inheritDoc}
     */
    @Override
    public final Serializable getValue()
    {
        return Short.valueOf(_value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void loadFromBuffer(final ByteBuffer buffer)
    {
        _value = buffer.getShort();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void setValue(final Serializable value)
    {
        _value = WithValue.shortValue(value);
    }

    private short _value;
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
