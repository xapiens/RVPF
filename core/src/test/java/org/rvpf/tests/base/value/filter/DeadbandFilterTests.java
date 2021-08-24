/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DeadbandFilterTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base.value.filter;

import java.util.Optional;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.filter.DeadbandFilter;

import org.testng.annotations.Test;

/**
 * Deadband filter tests.
 */
public final class DeadbandFilterTests
    extends ValueFilterTests
{
    /**
     * Should filter within the dead band and the time limit.
     */
    @Test
    public static void shouldFilterWithinDeadband()
    {
        final DeadbandFilter sut;
        final PointValue[] filtered;

        // Given a deadband filter,
        sut = new DeadbandFilter(
            Optional.of(ElapsedTime.fromRaw(TIME_LIMIT)),
            Optional.empty(),
            DEAD_BAND_GAP,
            NO_DEADBAND_RATIO);

        // when a new value is within the dead band
        // and within the time limit,
        Require.notNull(filter(sut, YESTERDAY, PREVIOUS_VALUE));
        filtered = filter(
            sut,
            YESTERDAY.after(TIME_LIMIT),
            lower(PREVIOUS_VALUE + DEAD_BAND_GAP));

        // then it should filter.
        Require.success(filtered.length == 0, "filtered");
    }

    /**
     * Should not filter outside the dead band.
     */
    @Test
    public static void shouldNotFilterOutsideDeadband()
    {
        final DeadbandFilter sut;
        final PointValue[] filtered;

        // Given a deadband filter,
        sut = new DeadbandFilter(
            Optional.of(ElapsedTime.fromRaw(TIME_LIMIT)),
            Optional.empty(),
            DEAD_BAND_GAP,
            NO_DEADBAND_RATIO);

        // when a new value is outside the dead band,
        Require.notNull(filter(sut, YESTERDAY, PREVIOUS_VALUE));
        filtered = filter(
            sut,
            YESTERDAY.after(),
            higher(PREVIOUS_VALUE + DEAD_BAND_GAP));

        // then it should not filter.
        Require.success(filtered.length == 1, "filtered");
    }

    /**
     * Should not filter outside the time limit.
     */
    @Test
    public static void shouldNotFilterOutsideTimeLimit()
    {
        final DeadbandFilter sut;
        final PointValue[] filtered;

        // Given a deadband filter,
        sut = new DeadbandFilter(
            Optional.of(ElapsedTime.fromRaw(TIME_LIMIT)),
            Optional.empty(),
            DEAD_BAND_GAP,
            NO_DEADBAND_RATIO);

        // when a new value is outside the time limit,
        Require.notNull(filter(sut, YESTERDAY, PREVIOUS_VALUE));
        filtered = filter(
            sut,
            YESTERDAY.after(TIME_LIMIT).after(),
            PREVIOUS_VALUE);

        // then it should not filter.
        Require.success(filtered.length == 1, "filtered");
    }

    /**
     * Should trim the stamp of point values according to the stamp trim unit.
     */
    @Test
    public static void shouldTrimStamp()
    {
        final DeadbandFilter sut;
        final PointValue[] filtered;

        // Given a deadband filter
        sut = new DeadbandFilter(
            Optional.empty(),
            Optional.of(ElapsedTime.fromRaw(STAMP_TRIM_UNIT)),
            DEAD_BAND_GAP,
            NO_DEADBAND_RATIO);

        // when a value stamp is between values of the stamp trim unit,
        filtered = filter(
            sut,
            YESTERDAY.after(ElapsedTime.fromRaw(STAMP_TRIM_UNIT / 2)),
            PREVIOUS_VALUE);

        // then the stamp should be trimmed.
        Require.equal(filtered[0].getStamp(), YESTERDAY, "filtered");
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
