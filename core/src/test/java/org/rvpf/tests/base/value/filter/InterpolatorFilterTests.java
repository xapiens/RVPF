/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: InterpolatorFilterTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base.value.filter;

import java.util.Optional;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.filter.InterpolatorFilter;

import org.testng.annotations.Test;

/**
 * Interpolator filter tests.
 */
public class InterpolatorFilterTests
    extends ValueFilterTests
{
    /**
     * Should break on null value.
     */
    @Test
    public static void shouldBreakOnNullValue()
    {
        final InterpolatorFilter sut;
        final PointValue[] filtered;
        final PointValue[] flushed;

        // Given an interpolator filter,
        sut = new InterpolatorFilter(
            Optional.of(ElapsedTime.fromRaw(TIME_LIMIT)),
            Optional.empty(),
            DEAD_BAND_GAP,
            NO_DEADBAND_RATIO);

        // when an interpolation sequence is active
        // and a point with a null value is supplied,
        Require.success(filter(sut, YESTERDAY, PREVIOUS_VALUE).length == 1);
        Require
            .success(
                filter(
                    sut,
                    YESTERDAY.after(TIME_LIMIT / 2),
                    lower(MIDDLE_VALUE)).length == 0);

        final PointValue pointValue = new PointValue(
            POINT_NAME,
            Optional.of(YESTERDAY.after(TIME_LIMIT)),
            null,
            null);

        filtered = sut.filter(Optional.of(pointValue));
        flushed = sut.filter(Optional.empty());

        // then is should break the sequence.
        Require.success(filtered.length == 2);
        Require
            .equal(filtered[0].getValue(), Double.valueOf(lower(MIDDLE_VALUE)));
        Require.success(filtered[1].getValue() == null);
        Require.success(flushed.length == 0);
    }

    /**
     * Should filter within the dead band and the time limit.
     */
    @Test
    public static void shouldFilterWithinDeadband()
    {
        final InterpolatorFilter sut;
        final PointValue[] filtered;
        final PointValue[] flushed;

        // Given an interpolator filter,
        sut = new InterpolatorFilter(
            Optional.of(ElapsedTime.fromRaw(TIME_LIMIT)),
            Optional.empty(),
            DEAD_BAND_GAP,
            NO_DEADBAND_RATIO);

        // when a new value is within the dead band
        // and within the time limit,
        Require.success(filter(sut, YESTERDAY, PREVIOUS_VALUE).length == 1);
        Require
            .success(
                filter(
                    sut,
                    YESTERDAY.after(TIME_LIMIT / 2),
                    lower(MIDDLE_VALUE)).length == 0);
        filtered = filter(sut, YESTERDAY.after(TIME_LIMIT), NEXT_VALUE);
        flushed = sut.filter(Optional.empty());

        // then it should filter.
        Require.success(filtered.length == 0, "filtered");
        Require.success(flushed.length == 1);
        Require.equal(Double.valueOf(NEXT_VALUE), flushed[0].getValue());
    }

    /**
     * Should not filter outside the time limit.
     */
    @Test
    public static void shouldNotFilterOutsideTimeLimit()
    {
        final InterpolatorFilter sut;
        final PointValue[] filtered;
        final PointValue[] flushed;

        // Given an interpolator filter,
        sut = new InterpolatorFilter(
            Optional.of(ElapsedTime.fromRaw(TIME_LIMIT)),
            Optional.empty(),
            DEAD_BAND_GAP,
            NO_DEADBAND_RATIO);

        // when a new value is within the dead band
        // and within the time limit,
        Require.success(filter(sut, YESTERDAY, PREVIOUS_VALUE).length == 1);
        Require
            .success(
                filter(
                    sut,
                    YESTERDAY.after(TIME_LIMIT / 2),
                    lower(MIDDLE_VALUE)).length == 0);
        filtered = filter(sut, YESTERDAY.after(TIME_LIMIT).after(), NEXT_VALUE);
        Require.success(sut.filter(Optional.empty()).length == 0);
        flushed = sut.filter(Optional.empty());

        // then it should not filter.
        Require.success(filtered.length == 2, "filtered");
        Require.success(flushed.length == 0);
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
