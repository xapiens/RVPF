/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: InterpolatorFilter.java 4003 2019-05-18 12:38:46Z SFB $
 */

package org.rvpf.base.value.filter;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.value.PointValue;

/**
 * Interpolator filter.
 */
public class InterpolatorFilter
    extends DeadbandFilter
{
    /**
     * Constructs an instance.
     *
     * @param timeLimit The optional time limit.
     * @param stampTrimUnit The optional stamp trim unit.
     * @param deadbangGap The deadband gap.
     * @param deadbandRatio The deadband ratio.
     */
    public InterpolatorFilter(
            @Nonnull final Optional<ElapsedTime> timeLimit,
            @Nonnull final Optional<ElapsedTime> stampTrimUnit,
            final double deadbangGap,
            final double deadbandRatio)
    {
        super(timeLimit, stampTrimUnit, deadbangGap, deadbandRatio);
    }

    /**
     * Polates.
     *
     * @param stamp The abscissa of the requested ordinate.
     * @param pointValue1 The first point value.
     * @param pointValue2 The second point value.
     *
     * @return The requested ordinate.
     */
    @Nonnull
    @CheckReturnValue
    protected static Double polate(
            @Nonnull final DateTime stamp,
            @Nonnull final PointValue pointValue1,
            @Nonnull final PointValue pointValue2)
    {
        final long x1 = pointValue1.getStamp().toRaw();
        final long x2 = pointValue2.getStamp().toRaw();
        final long x = stamp.toRaw();
        final double y1 = pointValue1.toDouble().doubleValue();
        final double y2 = pointValue2.toDouble().doubleValue();
        final double y = y1 + ((y2 - y1) * (x - x1) / (x2 - x1));

        return Double.valueOf(y);
    }

    /** {@inheritDoc}
     */
    @Override
    protected Optional<PointValue> doFilter(final PointValue pointValue)
    {
        final Optional<PointValue> previousPointValue = getPreviousPointValue();
        final Optional<PointValue> heldPointValue = getHeldPointValue();

        if (!heldPointValue.isPresent()) {
            setHeldPointValue(pointValue);

            return Optional.empty();
        }

        final Double interpolated = polate(
            heldPointValue.get().getStamp(),
            previousPointValue.get(),
            pointValue);
        final Optional<PointValue> filtered;

        if (shouldFilter(heldPointValue.get().toDouble(), interpolated)) {
            filtered = Optional.empty();
        } else {
            setPreviousPointValue(heldPointValue.get());
            filtered = heldPointValue;
        }

        setHeldPointValue(pointValue);

        return filtered;
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
