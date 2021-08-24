/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Summary.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.processor.engine.rpn.selector.summarizer;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.processor.engine.rpn.selector.Selection;

/**
 * Summary.
 */
@NotThreadSafe
final class Summary
    extends Selection
{
    /**
     * Constructs an instance.
     *
     * @param point The point definition.
     * @param stamp The optional time stamp of the value.
     * @param summarizesBehavior The summarizes behavior.
     */
    Summary(
            @Nonnull final Point point,
            @Nonnull final Optional<DateTime> stamp,
            final SummarizesBehavior summarizesBehavior)
    {
        super(point, stamp);

        _summarizesBehavior = summarizesBehavior;
    }

    private Summary(@Nonnull final Summary other)
    {
        super(other);

        _summarizesBehavior = other._summarizesBehavior;
    }

    /** {@inheritDoc}
     */
    @Override
    public Summary copy()
    {
        return new Summary(this);
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
     * Gets the result position.
     *
     * @return The result position.
     */
    String getResultPosition()
    {
        return _summarizesBehavior.getResultPosition();
    }

    /**
     * Asks if the interval is running.
     *
     * @return A true value if it is running.
     */
    @CheckReturnValue
    boolean isRunningInterval()
    {
        return _summarizesBehavior.isRunningInterval();
    }

    private final SummarizesBehavior _summarizesBehavior;
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
