/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.pap.dnp3.object;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.pap.dnp3.object.ObjectHeader.RangeCode;

/**
 * Object range.
 */
public final class ObjectRange
{
    /**
     * Constructs an instance.
     *
     * @param count The count.
     */
    private ObjectRange(final int count)
    {
        _count = count;
        _startIndex = 0;
        _stopIndex = 0;
    }

    /**
     * Constructs an instance.
     *
     * @param startIndex The start index.
     * @param stopIndex The stop index.
     */
    private ObjectRange(final int startIndex, final int stopIndex)
    {
        _startIndex = startIndex;
        _stopIndex = stopIndex;
        _count = 0;
    }

    /**
     * Returns a new instance.
     *
     * @param count The count.
     *
     * @return The new instance.
     */
    @Nonnull
    @CheckReturnValue
    public static ObjectRange newCountInstance(final int count)
    {
        if (count <= 0) {
            throw new IllegalArgumentException("Negative or null count.");
        }

        return new ObjectRange(count);
    }

    /**
     * Returns a new instance.
     *
     * @param index The index.
     *
     * @return The new instance.
     */
    @Nonnull
    @CheckReturnValue
    public static ObjectRange newIndexInstance(final int index)
    {
        return newIndexInstance(index, index);
    }

    /**
     * Returns a new instance.
     *
     * @param startIndex The start index.
     * @param stopIndex The stop index.
     *
     * @return The new instance.
     */
    @Nonnull
    @CheckReturnValue
    public static ObjectRange newIndexInstance(
            final int startIndex,
            final int stopIndex)
    {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Negative start index.");
        }

        if (stopIndex < startIndex) {
            throw new IllegalArgumentException(
                "Stop index less than start index.");
        }

        return new ObjectRange(startIndex, stopIndex);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object)
    {
        if (object instanceof ObjectRange) {
            final ObjectRange other = (ObjectRange) object;

            return (_count == other._count)
                   && (_startIndex == other._startIndex)
                   && (_stopIndex == other._stopIndex);
        }

        return false;
    }

    /**
     * Gets the range object.
     *
     * @return The range object.
     */
    @Nonnull
    @CheckReturnValue
    public Object getRange()
    {
        if (this == NONE) {
            return _NOTHING;
        }

        if (_count > 0) {
            if (_count <= Byte.MAX_VALUE) {
                return new byte[] {(byte) _count, };
            } else if (_count <= Short.MAX_VALUE) {
                return new short[] {(short) _count, };
            } else {
                return new int[] {_count, };
            }
        } else if (_stopIndex <= Byte.MAX_VALUE) {
            return new byte[] {(byte) _startIndex, (byte) _stopIndex, };
        } else if (_stopIndex <= Short.MAX_VALUE) {
            return new short[] {(short) _startIndex, (short) _stopIndex, };
        } else {
            return new int[] {_startIndex, _stopIndex, };
        }

    }

    /**
     * Gets the range code.
     *
     * @return The range code.
     */
    @Nonnull
    @CheckReturnValue
    public RangeCode getRangeCode()
    {
        if (this == NONE) {
            return RangeCode.NONE;
        }

        if (_count > 0) {
            if (_count <= Byte.MAX_VALUE) {
                return RangeCode.COUNT_BYTE;
            } else if (_count <= Short.MAX_VALUE) {
                return RangeCode.COUNT_SHORT;
            } else {
                return RangeCode.COUNT_INT;
            }
        } else if (_stopIndex <= Byte.MAX_VALUE) {
            return RangeCode.START_STOP_INDEX_BYTE;
        } else if (_stopIndex <= Short.MAX_VALUE) {
            return RangeCode.START_STOP_INDEX_SHORT;
        } else {
            return RangeCode.START_STOP_INDEX_INT;
        }

    }

    /**
     * Gets the start index.
     *
     * @return The start index.
     */
    @CheckReturnValue
    public int getStartIndex()
    {
        return _startIndex;
    }

    /**
     * Gets the stop index.
     *
     * @return The stop index.
     */
    @CheckReturnValue
    public int getStopIndex()
    {
        return _stopIndex;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return Integer
            .hashCode(
                _count) ^ Integer.hashCode(
                    _startIndex) ^ Integer.hashCode(_stopIndex);
    }

    /**
     * Asks if the object contains multiple values.
     *
     * @return True if the object contains multiple values.
     */
    @CheckReturnValue
    public boolean isMultiple()
    {
        return (_stopIndex > _startIndex) || (_count > 1);
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "[" + ((_count > 0)? String
            .valueOf(_count): _startIndex + ":" + _stopIndex) + "]";
    }

    /** Object range 'NONE'. */
    public static final ObjectRange NONE = new ObjectRange(0);

    /**  */

    private static final Object[] _NOTHING = new Object[0];

    private final int _count;
    private final int _startIndex;
    private final int _stopIndex;
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
