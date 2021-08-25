/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TraceBuffer.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap;

import javax.annotation.CheckReturnValue;

/**
 * Trace buffer.
 */
public final class TraceBuffer
{
    /**
     * Constructs an instance.
     *
     * @param enabled True if enabled.
     */
    public TraceBuffer(final boolean enabled)
    {
        _enabled = enabled;
    }

    /**
     * Appends a byte.
     *
     * @param byteValue The byte.
     */
    public void append(final byte byteValue)
    {
        if (_enabled) {
            if (_used < (_buffer.length - 1)) {
                _buffer[_used++] = byteValue;
            } else {
                _buffer[_buffer.length - 1] = (byte) 0xFF;
            }
        }
    }

    /**
     * Asks if this buffer is empty.
     *
     * @return True if empty.
     */
    @CheckReturnValue
    public boolean isEmpty()
    {
        return _used == 0;
    }

    /**
     * Resets.
     */
    public void reset()
    {
        _used = 0;
        _buffer[_buffer.length - 1] = 0x00;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < _used; ++i) {
            if (i > 0) {
                stringBuilder.append('-');
            }

            final byte byteValue = _buffer[i];

            stringBuilder
                .append((char) _halfByteToHexDigit((byte) (byteValue >> 4)));
            stringBuilder.append((char) _halfByteToHexDigit(byteValue));
        }

        return stringBuilder.toString();
    }

    private static byte _halfByteToHexDigit(byte halfByte)
    {
        final byte hexDigit;

        halfByte &= 0xF;

        if (halfByte < 0xA) {
            hexDigit = (byte) ('0' + halfByte);
        } else {
            hexDigit = (byte) ('A' + halfByte - 0xA);
        }

        return hexDigit;
    }

    private byte[] _buffer = new byte[514];
    private final boolean _enabled;
    private int _used;
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
