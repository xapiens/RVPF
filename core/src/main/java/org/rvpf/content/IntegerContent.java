/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: IntegerContent.java 3936 2019-04-28 19:25:00Z SFB $
 */

package org.rvpf.content;

import java.io.Serializable;

import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;

/**
 * Integer content converter.
 *
 * <p>This content is used to hold whole numbers between -2147483648 and
 * 2147483647. It will accept any number (may involve rounding or truncation)
 * and will try to decode a string (in decimal, hexadecimal or octal
 * representation, according to Java conventions).</p>
 */
public final class IntegerContent
    extends NumberContent
{
    /** {@inheritDoc}
     */
    @Override
    public Integer decode(final PointValue pointValue)
    {
        return _getInteger(pointValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public Integer denormalize(final NormalizedValue normalizedValue)
    {
        return _getInteger(normalizedValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public Integer normalize(final PointValue pointValue)
    {
        return _getInteger(pointValue);
    }

    private Integer _getInteger(final PointValue pointValue)
    {
        final Serializable value = pointValue.getValue();

        if (value instanceof Integer) {
            return (Integer) value;
        }

        if (value instanceof Double) {
            return Integer
                .valueOf((int) Math.round(((Double) value).doubleValue()));
        }

        if (value instanceof Float) {
            return Integer
                .valueOf((int) Math.round(((Float) value).doubleValue()));
        }

        if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue());
        }

        if (value instanceof String) {
            try {
                return Integer.decode((String) value);
            } catch (final NumberFormatException exception) {
                // Ignore (will warn).
            }
        } else if (value instanceof Boolean) {
            return Integer.valueOf(((Boolean) value).booleanValue()? 1: 0);
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
