/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LinearFilterTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base.value.filter;

import java.util.Optional;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.filter.LinearFilter;

import org.testng.annotations.Test;

/**
 * Linear filter tests.
 */
public class LinearFilterTests
    extends ValueFilterTests
{
    /**
     * Should filter extrapolated within the dead band and the time limit.
     */
    @Test
    public static void shouldFilterWithinDeadband()
    {
        final LinearFilter sut;
        final PointValue[] filtered;
        final PointValue[] flushed;

        // Given a linear filter,
        sut = new LinearFilter(
            Optional.of(ElapsedTime.fromRaw(TIME_LIMIT)),
            Optional.empty(),
            DEAD_BAND_GAP,
            NO_DEADBAND_RATIO);

        // when a new value is within the dead band
        // and within the time limit,
        Require
            .success(
                filter(
                    sut,
                    YESTERDAY.before(TIME_LIMIT / 2),
                    _FIRST_VALUE).length == 1);
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
        Require.equal(flushed[0].getValue(), Double.valueOf(NEXT_VALUE));
    }

    /**
     * Should not filter a false turning point.
     */
    @Test
    public static void shouldNotFilterFalseTurningPoint()
    {
        final LinearFilter sut;
        final PointValue[] filtered;
        final PointValue[] flushed;

        // Given a linear filter,
        sut = new LinearFilter(
            Optional.of(ElapsedTime.fromRaw(TIME_LIMIT)),
            Optional.empty(),
            DEAD_BAND_GAP,
            NO_DEADBAND_RATIO);

        // when a held value represents a turning point,
        Require
            .success(
                filter(sut, YESTERDAY.before(TIME_LIMIT / 2), 0.0).length == 1);
        Require.success(filter(sut, YESTERDAY, PREVIOUS_VALUE).length == 1);
        Require
            .success(
                filter(
                    sut,
                    YESTERDAY.after(TIME_LIMIT / 2),
                    lower(MIDDLE_VALUE)).length == 0);
        filtered = filter(sut, YESTERDAY.after(TIME_LIMIT), NEXT_VALUE);
        flushed = sut.filter(Optional.empty());

        // then it should not filter.
        Require.success(filtered.length == 1, "filtered");
        Require
            .equal(filtered[0].getValue(), Double.valueOf(lower(MIDDLE_VALUE)));
        Require.success(flushed.length == 1);
        Require.equal(flushed[0].getValue(), Double.valueOf(NEXT_VALUE));
    }

    protected static final double _FIRST_VALUE = 100.0;
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
