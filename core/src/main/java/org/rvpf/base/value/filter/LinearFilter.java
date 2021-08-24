/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LinearFilter.java 4003 2019-05-18 12:38:46Z SFB $
 */

package org.rvpf.base.value.filter;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.value.PointValue;

/**
 * Linear filter.
 */
public class LinearFilter
    extends InterpolatorFilter
{
    /**
     * Constructs an instance.
     *
     * @param timeLimit The optional time limit.
     * @param stampTrimUnit The optional stamp trim unit.
     * @param deadbangGap The deadband gap.
     * @param deadbandRatio The deadband ratio.
     */
    public LinearFilter(
            @Nonnull final Optional<ElapsedTime> timeLimit,
            @Nonnull final Optional<ElapsedTime> stampTrimUnit,
            final double deadbangGap,
            final double deadbandRatio)
    {
        super(timeLimit, stampTrimUnit, deadbangGap, deadbandRatio);
    }

    /** {@inheritDoc}
     */
    @Override
    public void reset()
    {
        _firstPointValue = null;

        super.reset();
    }

    /** {@inheritDoc}
     */
    @Override
    protected Optional<PointValue> doFilter(PointValue pointValue)
    {
        if (_firstPointValue == null) {
            _firstPointValue = getPreviousPointValue();
            setPreviousPointValue(pointValue);

            return Optional.of(pointValue);
        }

        final Optional<PointValue> previousPointValue = getPreviousPointValue();
        final Optional<PointValue> heldPointValue = getHeldPointValue();

        if (!super.doFilter(pointValue).isPresent()) {
            if (!heldPointValue.isPresent()) {
                return Optional.empty();
            }

            final Double extrapolated = polate(
                pointValue.getStamp(),
                _firstPointValue.get(),
                getPreviousPointValue().get());

            if (shouldFilter(pointValue.toDouble(), extrapolated)) {
                pointValue = null;
            } else {
                _firstPointValue = getPreviousPointValue();
                setPreviousPointValue(heldPointValue.get());
                pointValue = heldPointValue.get();
            }
        } else {
            _firstPointValue = previousPointValue;
        }

        return Optional.ofNullable(pointValue);
    }

    private Optional<PointValue> _firstPointValue;
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
