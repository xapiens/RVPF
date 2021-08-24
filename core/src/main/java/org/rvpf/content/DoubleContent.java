/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DoubleContent.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.content;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;

/**
 * Double content converter.
 */
public class DoubleContent
    extends FloatingPointContent
{
    /** {@inheritDoc}
     */
    @Override
    public final Double decode(final PointValue pointValue)
    {
        return getDouble(pointValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public Double denormalize(final NormalizedValue normalizedValue)
    {
        return getDouble(normalizedValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public Double normalize(final PointValue pointValue)
    {
        return getDouble(pointValue);
    }

    /**
     * Gets a Double object from the value.
     *
     * @param pointValue The point value holding the value.
     *
     * @return The Double value or null.
     */
    @Nullable
    @CheckReturnValue
    protected final Double getDouble(@Nonnull final PointValue pointValue)
    {
        final Serializable value = pointValue.getValue();

        if (value instanceof Double) {
            return (Double) value;
        }

        if (value instanceof Number) {
            return Double.valueOf(((Number) value).doubleValue());
        }

        if (value instanceof String) {
            try {
                return Double.valueOf((String) value);
            } catch (final NumberFormatException exception) {
                // Ignore (will warn).
            }
        } else if (value instanceof Boolean) {
            return Double.valueOf(((Boolean) value).booleanValue()? 1.0d: 0.0d);
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
