/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ComplexContent.java 3936 2019-04-28 19:25:00Z SFB $
 */

package org.rvpf.content;

import java.io.Serializable;

import org.rvpf.base.value.Complex;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;

/**
 * Complex content converter.
 */
public final class ComplexContent
    extends AbstractContent
{
    /** {@inheritDoc}
     */
    @Override
    public Serializable decode(final PointValue pointValue)
    {
        return _getComplex(pointValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable denormalize(final NormalizedValue normalizedValue)
    {
        return _getComplex(normalizedValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable normalize(final PointValue pointValue)
    {
        return _getComplex(pointValue);
    }

    private Complex _getComplex(final PointValue pointValue)
    {
        final Serializable value = pointValue.getValue();

        if (value instanceof Complex) {
            return (Complex) value;
        }

        if (value instanceof Number) {
            return Complex.cartesian(((Double) value).doubleValue(), 0.0);
        }

        if (value instanceof String) {
            try {
                return Complex.valueOf((String) value);
            } catch (final NumberFormatException exception) {
                // Ignore (will warn).
            }
        } else if (value instanceof Boolean) {
            return Complex
                .cartesian(((Boolean) value).booleanValue()? 1.0: 0.0, 0.0);
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
