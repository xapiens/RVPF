/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LongContent.java 3936 2019-04-28 19:25:00Z SFB $
 */

package org.rvpf.content;

import java.io.Serializable;

import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;

/**
 * Long content converter.
 *
 * <p>This content is used to hold whole numbers between -9223372036854775808l
 * and 9223372036854775807l. It will accept any number (may involve rounding or
 * truncation) and will try to decode a string (in decimal, hexadecimal or octal
 * representation, according to Java conventions).</p>
 */
public final class LongContent
    extends NumberContent
{
    /** {@inheritDoc}
     */
    @Override
    public Long decode(final PointValue pointValue)
    {
        return _getLong(pointValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public Long denormalize(final NormalizedValue normalizedValue)
    {
        return _getLong(normalizedValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public Long normalize(final PointValue pointValue)
    {
        return _getLong(pointValue);
    }

    private Long _getLong(final PointValue pointValue)
    {
        final Serializable value = pointValue.getValue();

        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof Double) {
            return Long.valueOf(Math.round(((Double) value).doubleValue()));
        }

        if (value instanceof Float) {
            return Long.valueOf(Math.round(((Float) value).doubleValue()));
        }

        if (value instanceof Number) {
            return Long.valueOf(((Number) value).longValue());
        }

        if (value instanceof String) {
            try {
                return Long.decode((String) value);
            } catch (final NumberFormatException exception) {
                // Ignore (will warn).
            }
        } else if (value instanceof Boolean) {
            return Long.valueOf(((Boolean) value).booleanValue()? 1: 0);
        }

        if (value != null) {
            warnBadValue(pointValue);
        }

        return null;
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
