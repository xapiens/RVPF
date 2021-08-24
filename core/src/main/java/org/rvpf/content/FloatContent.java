/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: FloatContent.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.content;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;

/**
 * Float content converter.
 */
public class FloatContent
    extends FloatingPointContent
{
    /** {@inheritDoc}
     */
    @Override
    public final Float decode(final PointValue pointValue)
    {
        return getFloat(pointValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public Float denormalize(final NormalizedValue normalizedValue)
    {
        return getFloat(normalizedValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public Float normalize(final PointValue pointValue)
    {
        return getFloat(pointValue);
    }

    /**
     * Gets a Float object from the value.
     *
     * @param pointValue The point value holding the value.
     *
     * @return The Float value or null.
     */
    @Nullable
    @CheckReturnValue
    protected final Float getFloat(@Nonnull final PointValue pointValue)
    {
        final Serializable value = pointValue.getValue();

        if (value instanceof Float) {
            return (Float) value;
        }

        if (value instanceof Number) {
            return Float.valueOf(((Number) value).floatValue());
        }

        if (value instanceof String) {
            try {
                return Float.valueOf((String) value);
            } catch (final NumberFormatException exception) {
                // Ignore (will warn).
            }
        } else if (value instanceof Boolean) {
            return Float.valueOf(((Boolean) value).booleanValue()? 1.0f: 0.0f);
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
