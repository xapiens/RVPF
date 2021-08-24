/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StepFilter.java 4001 2019-05-17 18:57:13Z SFB $
 */

package org.rvpf.base.value.filter;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.value.PointValue;

/**
 * Step filter.
 */
public final class StepFilter
    extends DeadbandFilter
{
    /**
     * Constructs an instance.
     *
     * @param timeLimit The optional time limit.
     * @param trimUnit The optional trim unit.
     * @param deadbangGap The deadband gap.
     * @param deadbandRatio The deadband ratio.
     * @param stepSize The step size.
     * @param ceilingGap The ceiling gap.
     * @param floorGap The floor gap.
     */
    public StepFilter(
            @Nonnull final Optional<ElapsedTime> timeLimit,
            @Nonnull final Optional<ElapsedTime> trimUnit,
            final double deadbangGap,
            final double deadbandRatio,
            final double stepSize,
            double ceilingGap,
            double floorGap)
    {
        super(timeLimit, trimUnit, deadbangGap, deadbandRatio);

        _stepSize = stepSize;

        setDisabled(isDisabled() && (_stepSize <= 0.0));

        if (!isDisabled()) {
            if (ceilingGap < 0.0) {
                ceilingGap = _stepSize / 2;
            }

            if (floorGap < 0.0) {
                floorGap = _stepSize / 2;
            }
        }

        _ceilingGap = ceilingGap;
        _floorGap = floorGap;
    }

    /** {@inheritDoc}
     */
    @Override
    protected PointValue snap(final PointValue pointValue)
    {
        if (_stepSize > 0.0) {
            final Double value = pointValue.toDouble();

            if (value == null) {
                return pointValue;
            }

            final double scaled = value.doubleValue() / _stepSize;
            double step;
            double gap;

            step = Math.floor(scaled);
            gap = scaled - step;

            if (gap <= _floorGap) {
                return _snap(pointValue, step);
            }

            step = Math.ceil(scaled);
            gap = step - scaled;

            if (gap <= _ceilingGap) {
                return _snap(pointValue, step);
            }
        }

        return pointValue;
    }

    private PointValue _snap(@Nonnull PointValue pointValue, final double step)
    {
        pointValue = pointValue.thawed();
        pointValue.setValue(Double.valueOf(step * _stepSize));

        return pointValue;
    }

    private final double _ceilingGap;
    private final double _floorGap;
    private final double _stepSize;
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
