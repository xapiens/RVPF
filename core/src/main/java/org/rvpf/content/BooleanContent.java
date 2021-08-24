/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BooleanContent.java 3936 2019-04-28 19:25:00Z SFB $
 */

package org.rvpf.content;

import java.io.Serializable;

import org.rvpf.base.tool.ValueConverter;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;

/**
 * Logical content converter.
 *
 * <p>This Content is used to hold boolean values. It will accept any Number as
 * false if equal to 0, true otherwise. It will accept the String values "true",
 * "on", "yes" or "1" as true and "false", "off", "no" or "0" as false.</p>
 */
public class BooleanContent
    extends AbstractContent
{
    /** {@inheritDoc}
     */
    @Override
    public final Boolean decode(final PointValue pointValue)
    {
        return _getBoolean(pointValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public final Boolean denormalize(final NormalizedValue normalizedValue)
    {
        return _getBoolean(normalizedValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public final Boolean normalize(final PointValue pointValue)
    {
        return _getBoolean(pointValue);
    }

    private Boolean _getBoolean(final PointValue pointValue)
    {
        final Serializable value = pointValue.getValue();

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof Number) {
            return Boolean.valueOf(((Number) value).longValue() != 0);
        }

        if (value instanceof String) {
            final String string = (String) value;

            if (ValueConverter.isTrue(string)) {
                return Boolean.TRUE;
            }

            if (ValueConverter.isFalse(string)) {
                return Boolean.FALSE;
            }
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
