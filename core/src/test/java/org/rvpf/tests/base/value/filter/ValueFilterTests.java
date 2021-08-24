/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ValueFilterTests.java 4003 2019-05-18 12:38:46Z SFB $
 */

package org.rvpf.tests.base.value.filter;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.filter.ValueFilter;
import org.rvpf.tests.Tests;

/**
 * Value filter tests.
 */
public abstract class ValueFilterTests
    extends Tests
{
    /**
     * Filters.
     *
     * @param sut The system under test (value filter).
     * @param dateTime A date and time.
     * @param value A value.
     *
     * @return The resulting point values (may be null).
     */
    @Nonnull
    @CheckReturnValue
    protected static PointValue[] filter(
            @Nonnull final ValueFilter sut,
            final DateTime dateTime,
            final double value)
    {
        final PointValue pointValue = new PointValue(
            POINT_NAME,
            Optional.of(dateTime),
            null,
            Double.valueOf(value));

        return sut.filter(Optional.of(pointValue));
    }

    /**
     * Returns the double value higher by an ulp.
     *
     * @param value The double value.
     *
     * @return The higher value.
     */
    @CheckReturnValue
    protected static double higher(final double value)
    {
        return value + Math.ulp(value);
    }

    /**
     * Returns the double value lower by an ulp.
     *
     * @param value The double value.
     *
     * @return The lower value.
     */
    @CheckReturnValue
    protected static double lower(final double value)
    {
        return value - Math.ulp(value);
    }

    protected static final double DEAD_BAND_GAP = 1.0;
    protected static final double MIDDLE_VALUE = 300.0;
    protected static final double NEXT_VALUE = 400.0;
    protected static final double NO_DEADBAND_RATIO = -1.0;
    protected static final String POINT_NAME = ".";
    protected static final double PREVIOUS_VALUE = 200.0;
    protected static final long STAMP_TRIM_UNIT = ElapsedTime.SECOND.toRaw();
    protected static final long TIME_LIMIT = ElapsedTime.HOUR.toRaw();
    protected static final DateTime YESTERDAY = DateTime.now().previousDay();
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
