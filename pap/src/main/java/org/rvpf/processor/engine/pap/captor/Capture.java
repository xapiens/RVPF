/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.processor.engine.pap.captor;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.processor.engine.rpn.selector.Selection;

/**
 * Capture.
 */
@NotThreadSafe
final class Capture
    extends Selection
{
    /**
     * Constructs an instance.
     *
     * @param point The point definition.
     * @param stamp The optional time stamp of the value.
     * @param capturesBehavior The captures behavior.
     */
    Capture(
            @Nonnull final Point point,
            @Nonnull final Optional<DateTime> stamp,
            @Nonnull final CapturesBehavior capturesBehavior)
    {
        super(point, stamp);

        _capturesBehavior = capturesBehavior;
    }

    private Capture(final Capture other)
    {
        super(other);

        _capturesBehavior = other._capturesBehavior;
    }

    /** {@inheritDoc}
     */
    @Override
    public Capture copy()
    {
        return new Capture(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object)
    {
        return super.equals(object);    // Quells FindBugs.
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();    // Quells FindBugs.
    }

    /**
     * Gets the limit after.
     *
     * @return The limit after.
     */
    @CheckReturnValue
    int getLimitAfter()
    {
        return _capturesBehavior.getLimitAfter();
    }

    /**
     * Gets the limit before.
     *
     * @return The limit before.
     */
    @CheckReturnValue
    int getLimitBefore()
    {
        return _capturesBehavior.getLimitBefore();
    }

    /**
     * Gets the time after.
     *
     * @return The optional time after.
     */
    @Nonnull
    @CheckReturnValue
    Optional<ElapsedTime> getTimeAfter()
    {
        return _capturesBehavior.getTimeAfter();
    }

    /**
     * Gets the time before.
     *
     * @return The optional time before.
     */
    @Nonnull
    @CheckReturnValue
    Optional<ElapsedTime> getTimeBefore()
    {
        return _capturesBehavior.getTimeBefore();
    }

    private final CapturesBehavior _capturesBehavior;
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
