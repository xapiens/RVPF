/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CutoffControl.java 4036 2019-05-31 11:19:42Z SFB $
 */

package org.rvpf.processor;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;

/**
 * Cutoff control.
 */
final class CutoffControl
{
    /**
     * Constructs a CutoffControl instance.
     *
     * @param point The Point controling the cutoff.
     */
    CutoffControl(@Nonnull final Point point)
    {
        _point = point;
    }

    /**
     * Gets the cutoff control Point.
     *
     * @return The cutoff control Point.
     */
    @Nonnull
    @CheckReturnValue
    Point getPoint()
    {
        return _point;
    }

    /**
     * Uses the supplied value to control the cutoff.
     *
     * @param pointValue The time value (Clock content).
     */
    void use(@Nonnull final Optional<NormalizedValue> pointValue)
    {
        if (!pointValue.isPresent()) {
            _setCutoff(null);
        } else if (_point.equals(pointValue.get().getPoint().get())) {
            try {
                final Long value = (Long) pointValue.get().getValue();

                if (value != null) {
                    _setCutoff(DateTime.fromMillis(value.longValue()));
                } else {
                    _setCutoff(null);
                }
            } catch (final RuntimeException exception) {
                _LOGGER
                    .warn(
                        ProcessorMessages.CUTOFF_VALUE_ERROR,
                        exception.getMessage());
            }
        } else {
            _LOGGER
                .warn(
                    ProcessorMessages.POINT_MISCONFIGURED,
                    pointValue.get().getPoint().orElse(null));
        }
    }

    /**
     * Verifies if a Point value passes the cutoff control.
     *
     * @param pointValue The Point value.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean verify(@Nonnull final PointValue pointValue)
    {
        return (_time == null) || pointValue.getStamp().isNotBefore(_time);
    }

    private void _setCutoff(final DateTime time)
    {
        if (time != null) {
            _LOGGER.info(ProcessorMessages.CUTOFF_TIME, time);
        } else if (_time != null) {
            _LOGGER.info(ProcessorMessages.CUTOFF_CANCELLED);
        }

        _time = time;
    }

    private static final Logger _LOGGER = Logger
        .getInstance(CutoffControl.class);

    private final Point _point;
    private DateTime _time;
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
