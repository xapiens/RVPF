/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ReverseSync.java 3982 2019-05-13 16:23:23Z SFB $
 */

package org.rvpf.base.sync;

import java.util.ListIterator;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.TimeInterval;

/**
 * Reverse sync.
 */
public final class ReverseSync
    implements Sync
{
    /**
     * Constructs an instance.
     *
     * @param sync The actual sync.
     */
    public ReverseSync(@Nonnull final Sync sync)
    {
        _sync = sync;
    }

    private ReverseSync(final ReverseSync other)
    {
        _sync = other._sync.copy();
    }

    /** {@inheritDoc}
     */
    @Override
    public ReverseSync copy()
    {
        return new ReverseSync(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ReverseSync)) {
            return false;
        }

        return _sync.equals(((ReverseSync) other)._sync);
    }

    /** {@inheritDoc}
     */
    @Override
    public DateTime getCurrentStamp()
    {
        return _sync.getCurrentStamp();
    }

    /** {@inheritDoc}
     */
    @Override
    public TimeInterval getDefaultLimits()
    {
        return _sync.getDefaultLimits();
    }

    /** {@inheritDoc}
     */
    @Override
    public DateTime getFirstStamp()
    {
        return _sync.getLastStamp();
    }

    /** {@inheritDoc}
     */
    @Override
    public DateTime getLastStamp()
    {
        return _sync.getFirstStamp();
    }

    /** {@inheritDoc}
     */
    @Override
    public TimeInterval getLimits()
    {
        return _sync.getLimits();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getNextStamp()
    {
        return _sync.getPreviousStamp();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getNextStamp(final DateTime stamp)
    {
        return _sync.getPreviousStamp(stamp);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getNextStamp(final int intervals)
    {
        return _sync.getPreviousStamp(intervals);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getNextStamp(
            final DateTime stamp,
            final int intervals)
    {
        return _sync.getPreviousStamp(stamp, intervals);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getPreviousStamp()
    {
        return _sync.getNextStamp();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getPreviousStamp(final DateTime stamp)
    {
        return _sync.getNextStamp(stamp);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getPreviousStamp(final int intervals)
    {
        return _sync.getNextStamp(intervals);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getPreviousStamp(
            final DateTime stamp,
            final int intervals)
    {
        return _sync.getNextStamp(stamp, intervals);
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isBounded()
    {
        return _sync.isBounded();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInSync()
    {
        return _sync.isInSync();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInSync(final DateTime stamp)
    {
        return _sync.isInSync(stamp);
    }

    /** {@inheritDoc}
     */
    @Override
    public ListIterator<DateTime> iterator()
    {
        return _sync.reverseIterator();
    }

    /** {@inheritDoc}
     */
    @Override
    public ListIterator<DateTime> iterator(final DateTime stamp)
    {
        return _sync.reverseIterator(stamp);
    }

    /** {@inheritDoc}
     */
    @Override
    public ListIterator<DateTime> reverseIterator()
    {
        return _sync.iterator();
    }

    /** {@inheritDoc}
     */
    @Override
    public ListIterator<DateTime> reverseIterator(final DateTime stamp)
    {
        return _sync.iterator(stamp);
    }

    /** {@inheritDoc}
     */
    @Override
    public void seed(final DateTime stamp)
    {
        _sync.seed(stamp);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setLimits(final TimeInterval limits)
    {
        _sync.setLimits(limits);
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "(" + _sync.toString() + ")";
    }

    private static final long serialVersionUID = 1L;

    private final Sync _sync;
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
