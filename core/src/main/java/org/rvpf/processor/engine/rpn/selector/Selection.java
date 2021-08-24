/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.processor.engine.rpn.selector;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;

/**
 * Selection.
 */
@NotThreadSafe
public abstract class Selection
    extends ResultValue
{
    /**
     * Constructs an instance.
     *
     * @param point The point definition.
     * @param stamp The optional time stamp of the value.
     */
    public Selection(
            @Nonnull final Point point,
            @Nonnull final Optional<DateTime> stamp)
    {
        super(point, stamp);
    }

    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    protected Selection(@Nonnull final Selection other)
    {
        super(other);

        _startValue = other._startValue;
        _stopValue = other._stopValue;
    }

    /**
     * Gets the interval start stamp.
     *
     * @return The optional interval start stamp.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<DateTime> getStartStamp()
    {
        return (_startValue != null)? Optional
            .of(_startValue.getStamp()): Optional.empty();
    }

    /**
     * Gets the point value starting the interval.
     *
     * @return The optional point value.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<PointValue> getStartValue()
    {
        return Optional.ofNullable(_startValue);
    }

    /**
     * Gets the interval stop stamp.
     *
     * @return The optional interval stop stamp.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<DateTime> getStopStamp()
    {
        return (_stopValue != null)? Optional
            .of(_stopValue.getStamp()): Optional.empty();
    }

    /**
     * Gets the point value stopping the interval.
     *
     * @return The optional point value.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<PointValue> getStopValue()
    {
        return Optional.ofNullable(_stopValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isReplaceable()
    {
        return false;
    }

    /**
     * Sets the point value starting the interval.
     *
     * @param startValue The point value.
     */
    public void setStartValue(@Nonnull final PointValue startValue)
    {
        _startValue = startValue;
    }

    /**
     * Sets the point value stopping the interval.
     *
     * @param stopValue The point value.
     */
    public void setStopValue(@Nonnull final PointValue stopValue)
    {
        _stopValue = Require.notNull(stopValue);
    }

    private static final long serialVersionUID = 1L;

    private transient PointValue _startValue;
    private transient PointValue _stopValue;
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
