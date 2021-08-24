/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Values.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.store.server.c;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/** Values.
 */
final class Values
{
    /** Adds a value.
     *
     * @param handle The handle for the point.
     * @param valueTime The time stamp for the value.
     * @param valueDeleted True if deleted.
     * @param valueQuality The quality of the value.
     * @param valueBytes The value.
     */
    void add(
            final int handle,
            final long valueTime,
            final boolean valueDeleted,
            final int valueQuality,
            final byte[] valueBytes)
    {
        _values.add(
            new Value(
                handle,
                valueTime,
                valueDeleted,
                valueQuality,
                valueBytes));
    }

    /** Clears the values.
     */
    void clear()
    {
        _iterator = null;
        _values.clear();
    }

    /** Gets the quality.
     *
     * @return The quality.
     */
    int getQuality()
    {
        return _quality;
    }

    /** Gets the time.
     *
     * @return The time.
     */
    long getTime()
    {
        return _time;
    }

    /** Gets the value.
     *
     * @return The value.
     */
    byte[] getValue()
    {
        return _value;
    }

    /** Asks if there is a next value.
     *
     * @return True if there is a next value.
     */
    boolean hasNext()
    {
        if (_iterator == null) {
            _iterator = _values.iterator();
        }

        return _iterator.hasNext();
    }

    /** Gets the deleted indicator.
     *
     * @return The deleted indicator.
     */
    boolean isDeleted()
    {
        return _deleted;
    }

    /** Positions to the next value.
     *
     * @return A client handle.
     */
    int next()
    {
        if (_iterator == null) {
            _iterator = _values.iterator();
        }

        final Value next = _iterator.next();

        _time = next.getTime();
        _deleted = next.isDeleted();
        _quality = next.getQuality();
        _value = next.getValue();

        return next.getHandle();
    }

    /** Sets the status.
     *
     * @param status The new status.
     */
    void setStatus(final Status status)
    {
        _status = status;
    }

    /** Returns the size of this.
     *
     * @return The size of this.
     */
    int size()
    {
        return _values.size();
    }

    /** Returns the status code.
     *
     * @return The status code.
     */
    int statusCode()
    {
        return _status.code();
    }

    private boolean _deleted;

    private Iterator<Value> _iterator;
    private int _quality;
    private Status _status = Status.SUCCESS;
    private long _time;
    private byte[] _value;
    private final List<Value> _values = new LinkedList<>();

    /** Value.
     */
    private static final class Value
    {
        /** Constructs an instance.
         *
         * @param valueHandle The client handle for the point.
         * @param valueTime The time stamp for the value.
         * @param valueDeleted True if deleted.
         * @param valueQuality The quality of the value.
         * @param valueBytes The value.
         */
        Value(
                final int valueHandle,
                final long valueTime,
                final boolean valueDeleted,
                final int valueQuality,
                final byte[] valueBytes)
        {
            _handle = valueHandle;
            _time = valueTime;
            _deleted = valueDeleted;
            _quality = valueQuality;
            _value = valueBytes;
        }

        int getHandle()
        {
            return _handle;
        }

        int getQuality()
        {
            return _quality;
        }

        long getTime()
        {
            return _time;
        }

        byte[] getValue()
        {
            return _value;
        }

        boolean isDeleted()
        {
            return _deleted;
        }

        private final boolean _deleted;
        private final int _handle;
        private final int _quality;
        private final long _time;
        private final byte[] _value;
    }
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
