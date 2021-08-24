/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DeadbandFilter.java 4003 2019-05-18 12:38:46Z SFB $
 */

package org.rvpf.base.value.filter;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.value.PointValue;

/**
 * Deadband filter.
 */
public class DeadbandFilter
    extends ValueFilter.Abstract
{
    /**
     * Constructs an instance.
     *
     * @param timeLimit The optional time limit.
     * @param stampTrimUnit The optional stamp trim unit.
     * @param deadbangGap The deadband gap.
     * @param deadbandRatio The deadband ratio.
     */
    public DeadbandFilter(
            @Nonnull final Optional<ElapsedTime> timeLimit,
            @Nonnull final Optional<ElapsedTime> stampTrimUnit,
            final double deadbangGap,
            final double deadbandRatio)
    {
        super(timeLimit, stampTrimUnit);

        _deadbandGap = deadbangGap;
        _deadbandRatio = deadbandRatio;

        setDisabled((_deadbandGap < 0.0) && (_deadbandRatio < 0.0));
    }

    /** {@inheritDoc}
     */
    @Override
    protected Optional<PointValue> doFilter(final PointValue pointValue)
    {
        final Optional<PointValue> previousPointValue = getPreviousPointValue();

        final PointValue filteredValue = shouldFilter(
            pointValue.toDouble(),
            previousPointValue.get().toDouble())? null: pointValue;

        if (filteredValue != null) {
            setPreviousPointValue(filteredValue);
        }

        return Optional.ofNullable(filteredValue);
    }

    /**
     * Asks if a value should be filtered, considering a reference value.
     *
     * @param value The value.
     * @param referenceValue The reference value.
     *
     * @return True if the value should be filtered.
     */
    @CheckReturnValue
    protected boolean shouldFilter(
            @Nonnull final Double value,
            @Nonnull final Double referenceValue)
    {
        double deadbandSize = _deadbandGap;

        if (deadbandSize < 0.0) {
            if (_deadbandRatio < 0.0) {
                return false;
            }

            deadbandSize = Math
                .abs(referenceValue.doubleValue()) * _deadbandRatio;
        }

        return Math
            .abs(value.doubleValue()
                 - referenceValue.doubleValue()) <= deadbandSize;
    }

    private final double _deadbandGap;
    private final double _deadbandRatio;
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
