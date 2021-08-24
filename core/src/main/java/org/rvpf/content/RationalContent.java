/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RationalContent.java 3936 2019-04-28 19:25:00Z SFB $
 */

package org.rvpf.content;

import java.io.Serializable;

import org.rvpf.base.value.BigRational;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Rational;

/**
 * Rational content converter.
 */
public final class RationalContent
    extends FloatingPointContent
{
    /** {@inheritDoc}
     */
    @Override
    public Serializable decode(final PointValue pointValue)
    {
        return _getRational(pointValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable denormalize(final NormalizedValue normalizedValue)
    {
        return _getRational(normalizedValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable normalize(final PointValue pointValue)
    {
        return _getRational(pointValue);
    }

    private Rational _getRational(final PointValue pointValue)
    {
        final Serializable value = pointValue.getValue();

        if (value instanceof Rational) {
            return (Rational) value;
        }

        if (value instanceof BigRational) {
            final BigRational bigRational = (BigRational) value;

            return Rational
                .valueOf(
                    bigRational.getNumerator().longValue(),
                    bigRational.getDenominator().longValue());
        }

        if (value instanceof Double) {
            return Rational
                .valueOf(Math.round(((Double) value).doubleValue()), 1);
        }

        if (value instanceof Float) {
            return Rational
                .valueOf(Math.round(((Float) value).floatValue()), 1);
        }

        if (value instanceof Number) {
            return Rational.valueOf(((Number) value).longValue(), 1);
        }

        if (value instanceof String) {
            try {
                return Rational.valueOf((String) value);
            } catch (final NumberFormatException exception) {
                // Ignore (will warn).
            }
        } else if (value instanceof Boolean) {
            return Rational.valueOf(((Boolean) value).booleanValue()? 1: 0, 1);
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
