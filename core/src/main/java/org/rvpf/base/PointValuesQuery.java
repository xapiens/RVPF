/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PointValuesQuery.java 3895 2019-02-15 21:39:11Z SFB $
 */

package org.rvpf.base;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.sync.Sync;

/**
 * Point values query.
 */
public interface PointValuesQuery
{
    /**
     * Gets the time interval instance.
     *
     * @return The time interval instance.
     */
    @Nonnull
    @CheckReturnValue
    TimeInterval getInterval();

    /**
     * Gets the point.
     *
     * @return The optional point.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Point> getPoint();

    /**
     * Gets the sync object.
     *
     * @return The optional sync object.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Sync> getSync();

    /**
     * Gets the extrapolated indicator.
     *
     * @return The extrapolated indicator.
     */
    @CheckReturnValue
    boolean isExtrapolated();

    /**
     * Gets the interpolated indicator.
     *
     * @return The interpolated indicator.
     */
    @CheckReturnValue
    boolean isInterpolated();

    /**
     * Gets the not null indicator.
     *
     * @return The not null indicator.
     */
    @CheckReturnValue
    boolean isNotNull();

    /**
     * Asks if this is for inter/extra-polated values.
     *
     * @return True if it is.
     */
    @CheckReturnValue
    boolean isPolated();

    /**
     * Abstract point values query.
     */
    @Immutable
    abstract class Abstract
        implements PointValuesQuery
    {
        /**
         * Constructs an instance.
         *
         * @param point An optional point.
         * @param interval An interval.
         * @param sync An optional sync instance.
         */
        protected Abstract(
                @Nonnull final Optional<Point> point,
                @Nonnull final TimeInterval interval,
                @Nonnull final Optional<Sync> sync)
        {
            _point = point;
            _interval = interval;
            _sync = sync;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean equals(final Object object)
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public final TimeInterval getInterval()
        {
            return _interval;
        }

        /** {@inheritDoc}
         */
        @Override
        public final Optional<Point> getPoint()
        {
            return _point;
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<Sync> getSync()
        {
            return _sync;
        }

        /** {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            throw new UnsupportedOperationException();
        }

        private final TimeInterval _interval;
        private final Optional<Point> _point;
        private final Optional<Sync> _sync;
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
