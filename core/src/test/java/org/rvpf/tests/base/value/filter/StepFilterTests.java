/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StepFilterTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base.value.filter;

import java.util.Optional;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.filter.StepFilter;

import org.testng.annotations.Test;

/**
 * Step filter tests.
 */
public final class StepFilterTests
    extends ValueFilterTests
{
    /**
     * Should filter inside ceiling gap and within time limit.
     */
    @Test
    public static void shouldFilterInsideCeilingGap()
    {
        final StepFilter sut;
        final PointValue[] filtered;

        // Given a step filter with a ceiling gap,
        sut = new StepFilter(
            Optional.of(ElapsedTime.fromRaw(TIME_LIMIT)),
            Optional.empty(),
            _ZERO_DEAD_BAND_GAP,
            NO_DEADBAND_RATIO,
            _STEP_SIZE,
            _CEILING_GAP,
            _FLOOR_GAP);

        // when a new value is inside the floor gap
        // and within the time limit,
        Require.notNull(filter(sut, YESTERDAY, PREVIOUS_VALUE + _STEP_SIZE));
        filtered = filter(
            sut,
            YESTERDAY.after(TIME_LIMIT),
            higher(PREVIOUS_VALUE + _STEP_SIZE - _CEILING_GAP));

        // then it should filter.
        Require.success(filtered.length == 0, "filtered");
    }

    /**
     * Should filter inside floor gap and within time limit.
     */
    @Test
    public static void shouldFilterInsideFloorGap()
    {
        final StepFilter sut;
        final PointValue[] filtered;

        // Given a step filter with a floor gap,
        sut = new StepFilter(
            Optional.of(ElapsedTime.fromRaw(TIME_LIMIT)),
            Optional.empty(),
            _ZERO_DEAD_BAND_GAP,
            NO_DEADBAND_RATIO,
            _STEP_SIZE,
            _CEILING_GAP,
            _FLOOR_GAP);

        // when a new value is inside the floor gap
        // and within the time limit,
        Require.notNull(filter(sut, YESTERDAY, PREVIOUS_VALUE));
        filtered = filter(
            sut,
            YESTERDAY.after(TIME_LIMIT),
            lower(PREVIOUS_VALUE + _FLOOR_GAP));

        // then it should filter.
        Require.success(filtered.length == 0, "filtered");
    }

    /**
     * Should not filter outside ceiling gap.
     */
    @Test
    public static void shouldNotFilterOutsideCeilingGap()
    {
        final StepFilter sut;
        final PointValue[] filtered;

        // Given a step filter with a ceiling gap,
        sut = new StepFilter(
            Optional.of(ElapsedTime.fromRaw(TIME_LIMIT)),
            Optional.empty(),
            _ZERO_DEAD_BAND_GAP,
            NO_DEADBAND_RATIO,
            _STEP_SIZE,
            _CEILING_GAP,
            _FLOOR_GAP);

        // when a new value is outside the ceiling gap,
        Require.notNull(filter(sut, YESTERDAY, PREVIOUS_VALUE + _STEP_SIZE));
        filtered = filter(
            sut,
            YESTERDAY.after(TIME_LIMIT),
            lower(PREVIOUS_VALUE + _STEP_SIZE - _CEILING_GAP));

        // then it should not filter.
        Require.success(filtered.length == 1, "filtered");
    }

    /**
     * Should not filter outside floor gap.
     */
    @Test
    public static void shouldNotFilterOutsideFloorGap()
    {
        final StepFilter sut;
        final PointValue[] filtered;

        // Given a step filter with a floor gap,
        sut = new StepFilter(
            Optional.of(ElapsedTime.fromRaw(TIME_LIMIT)),
            Optional.empty(),
            _ZERO_DEAD_BAND_GAP,
            NO_DEADBAND_RATIO,
            _STEP_SIZE,
            _CEILING_GAP,
            _FLOOR_GAP);

        // when a new value is outside the floor gap,
        Require.notNull(filter(sut, YESTERDAY, PREVIOUS_VALUE));
        filtered = filter(
            sut,
            YESTERDAY.after(TIME_LIMIT),
            higher(PREVIOUS_VALUE + _FLOOR_GAP));

        // then it should not filter.
        Require.success(filtered.length == 1, "filtered");
    }

    /**
     * Should not filter outside the time limit.
     */
    @Test
    public static void shouldNotFilterOutsideTimeLimit()
    {
        final StepFilter sut;
        final PointValue[] filtered;

        // Given a step filter,
        sut = new StepFilter(
            Optional.of(ElapsedTime.fromRaw(TIME_LIMIT)),
            Optional.empty(),
            DEAD_BAND_GAP,
            NO_DEADBAND_RATIO,
            _STEP_SIZE,
            _CEILING_GAP,
            _FLOOR_GAP);

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
     * Should not filter when there are no previous value.
     */
    @Test
    public static void shouldNotFilterWhenNoPrevious()
    {
        final StepFilter sut;
        final PointValue[] filtered;

        // Given a step filter,
        sut = new StepFilter(
            Optional.of(ElapsedTime.fromRaw(TIME_LIMIT)),
            Optional.empty(),
            DEAD_BAND_GAP,
            NO_DEADBAND_RATIO,
            _STEP_SIZE,
            _CEILING_GAP,
            _FLOOR_GAP);

        // and no previous value,

        // when provided with a new value,
        filtered = filter(sut, YESTERDAY, 0.0);

        // then it should not filter.
        Require.success(filtered.length == 1, "filtered");
    }

    /**
     * Should snap down within the floor gap.
     */
    @Test
    public static void shouldSnapDownWithinFloorGap()
    {
        final StepFilter sut;
        final PointValue[] snapped;

        // Given a step filter with a floor gap,
        sut = new StepFilter(
            Optional.of(ElapsedTime.fromRaw(TIME_LIMIT)),
            Optional.empty(),
            _NO_DEAD_BAND_GAP,
            NO_DEADBAND_RATIO,
            _STEP_SIZE,
            _CEILING_GAP,
            _FLOOR_GAP);

        // when a new value is within the floor gap,
        snapped = filter(sut, YESTERDAY, lower(_SNAPPED_FLOOR + _FLOOR_GAP));
        Require.success(snapped.length == 1, "snapped");

        // then it should snap down to the floor.
        Require.equal(snapped[0].toDouble(), Double.valueOf(_SNAPPED_FLOOR));
    }

    /**
     * Should snap up within the ceiling gap.
     */
    @Test
    public static void shouldSnapUpWithinCeilingGap()
    {
        final StepFilter sut;
        final PointValue[] snapped;

        // Given a step filter with a floor gap,
        sut = new StepFilter(
            Optional.of(ElapsedTime.fromRaw(TIME_LIMIT)),
            Optional.empty(),
            _NO_DEAD_BAND_GAP,
            NO_DEADBAND_RATIO,
            _STEP_SIZE,
            _CEILING_GAP,
            _FLOOR_GAP);

        // when a new value is within the ceiling gap,
        snapped = filter(
            sut,
            YESTERDAY,
            higher(_SNAPPED_FLOOR + _STEP_SIZE - _CEILING_GAP));
        Require.success(snapped.length == 1, "snapped");

        // then it should snap up to the ceiling.
        Require
            .equal(
                snapped[0].toDouble(),
                Double.valueOf(_SNAPPED_FLOOR + _STEP_SIZE));
    }

    private static final double _CEILING_GAP = 0.3;
    private static final double _FLOOR_GAP = 0.3;
    private static final double _NO_DEAD_BAND_GAP = -1.0;
    private static final double _SNAPPED_FLOOR = 10.0;
    private static final double _STEP_SIZE = 1.0;
    private static final double _ZERO_DEAD_BAND_GAP = 0.0;
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
