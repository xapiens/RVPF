/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClockContent.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.clock;

import java.io.Serializable;

import org.rvpf.base.tool.Require;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;
import org.rvpf.content.AbstractContent;
import org.rvpf.service.ServiceMessages;

/**
 * Clock content converter.
 *
 * <p>This class helps the manipulation of point values representing a clock
 * event.</p>
 */
public class ClockContent
    extends AbstractContent
{
    /** {@inheritDoc}
     */
    @Override
    public final Short decode(final PointValue pointValue)
    {
        Short shortValue = _getShort(pointValue);
        final short millis = (shortValue != null)? shortValue
            .shortValue(): (short) 0;

        if ((millis < (short) 0) || (999 < millis)) {
            getThisLogger()
                .warn(ServiceMessages.UNEXPECTED_CLOCK_VALUE, pointValue);
            shortValue = Short.valueOf((short) 0);
        }

        return shortValue;
    }

    /** {@inheritDoc}
     */
    @Override
    public final Short denormalize(final NormalizedValue normalizedValue)
    {
        final Serializable value = normalizedValue.getValue();
        final Short shortValue;

        if (value != null) {
            final long millis = ((Long) value).longValue();

            Require.success(millis == normalizedValue.getStamp().toMillis());
            shortValue = Short.valueOf((short) (millis % 1000));
        } else {
            shortValue = null;
        }

        return shortValue;
    }

    /** {@inheritDoc}
     */
    @Override
    public final Long normalize(final PointValue pointValue)
    {
        final Short shortValue = _getShort(pointValue);
        final Long millis;

        if (shortValue != null) {
            final long stampSeconds = pointValue.getStamp().toMillis() / 1000;

            millis = Long
                .valueOf((stampSeconds * 1000) + shortValue.shortValue());
        } else {
            millis = null;
        }

        return millis;
    }

    private Short _getShort(final PointValue pointValue)
    {
        final Object value = pointValue.getValue();

        if (value instanceof Short) {
            return (Short) value;
        }

        if (value instanceof Double) {
            return Short
                .valueOf((short) Math.round(((Double) value).doubleValue()));
        }

        if (value instanceof Float) {
            return Short
                .valueOf((short) Math.round(((Float) value).floatValue()));
        }

        if (value instanceof Number) {
            return Short.valueOf(((Number) value).shortValue());
        }

        if (value instanceof String) {
            try {
                return Short.decode((String) value);
            } catch (final NumberFormatException exception) {
                // Ignore (will warn).
            }
        } else if (value instanceof Boolean) {
            return Short
                .valueOf((short) (((Boolean) value).booleanValue()? 1: 0));
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
