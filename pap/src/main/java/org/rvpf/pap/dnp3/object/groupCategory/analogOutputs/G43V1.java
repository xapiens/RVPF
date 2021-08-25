/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: G43V1.java 3976 2019-05-11 17:26:10Z SFB $
 */

package org.rvpf.pap.dnp3.object.groupCategory.analogOutputs;

import java.io.Serializable;

import java.nio.ByteBuffer;

import java.util.Optional;

import org.rvpf.pap.dnp3.object.ObjectInstance;
import org.rvpf.pap.dnp3.object.ObjectVariation;
import org.rvpf.pap.dnp3.object.content.ControlStatusCode;

/**
 * Analog Output Command Event 32-bit without time.
 */
public class G43V1
    extends ObjectInstance.Abstract
    implements ObjectInstance.WithValue, ObjectInstance.WithStatus
{
    /** {@inheritDoc}
     */
    @Override
    public void dumpToBuffer(final ByteBuffer buffer)
    {
        buffer.put(_status);
        buffer.putInt(_value);
    }

    /** {@inheritDoc}
     */
    @Override
    public int getObjectLength()
    {
        return Byte.BYTES + Integer.BYTES;
    }

    /** {@inheritDoc}
     */
    @Override
    public ObjectVariation getObjectVariation()
    {
        return AnalogOutputCommandEventVariation.INTEGER_WITHOUT_TIME;
    }

    /** {@inheritDoc}
     */
    @Override
    public final ControlStatusCode getStatus()
    {
        return ControlStatusCode.instance(_status & 0xFF);
    }

    /** {@inheritDoc}
     */
    @Override
    public final Serializable getValue()
    {
        return Integer.valueOf(_value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void loadFromBuffer(final ByteBuffer buffer)
    {
        _status = buffer.get();
        _value = buffer.getInt();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setStatus(Optional<ControlStatusCode> status)
    {
        if (!status.isPresent()) {
            status = Optional.of(ControlStatusCode.SUCCESS);
        }

        _status = (byte) status.get().getCode();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void setValue(final Serializable value)
    {
        _value = WithValue.intValue(value);
    }

    private byte _status;
    private int _value;
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
