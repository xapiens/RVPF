/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BatchValuesQuery.java 4078 2019-06-11 20:55:00Z SFB $
 */

package org.rvpf.metadata.processor;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.PointValuesQuery;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.sync.Sync;

/**
 * Batch values query.
 *
 * <p>An instance of this class holds informations and state for a single query
 * to a batch object. It should not be reused except by the batch object itself.
 * </p>
 */
@Immutable
public final class BatchValuesQuery
    extends PointValuesQuery.Abstract
{
    /**
     * Constructs an instance.
     *
     * @param point An optional point.
     * @param interval An interval.
     * @param sync An optional sync instance.
     * @param notNull True avoids null values.
     * @param interpolated True for interpolated values.
     * @param extrapolated True for extrapolated values.
     */
    BatchValuesQuery(
            @Nonnull final Optional<Point> point,
            @Nonnull final TimeInterval interval,
            @Nonnull final Optional<Sync> sync,
            final boolean notNull,
            final boolean interpolated,
            final boolean extrapolated)
    {
        super(point, interval, sync);

        _notNull = notNull;
        _interpolated = interpolated;
        _extrapolated = extrapolated;
    }

    /**
     * Returns a new builder.
     *
     * @return The new builder.
     */
    @Nonnull
    @CheckReturnValue
    public static Builder newBuilder()
    {
        return new Builder();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object)
    {
        return this == object;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return System.identityHashCode(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isExtrapolated()
    {
        return _extrapolated;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInterpolated()
    {
        return _interpolated;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isNotNull()
    {
        return _notNull;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isPolated()
    {
        return _interpolated || _extrapolated;
    }

    private final boolean _extrapolated;
    private final boolean _interpolated;
    private final boolean _notNull;

    /**
     * Builder.
     */
    @NotThreadSafe
    public static final class Builder
    {
        /**
         * Constructs an instance.
         */
        Builder() {}

        /**
         * Builds a batch values query.
         *
         * @return The batch values query.
         */
        @Nonnull
        @CheckReturnValue
        public BatchValuesQuery build()
        {
            return new BatchValuesQuery(
                Optional.ofNullable(_point),
                _intervalBuilder.build(),
                Optional.ofNullable(_sync),
                _notNull,
                _interpolated,
                _extrapolated);
        }

        /**
         * Copies the values from a batch values query.
         *
         * @param batchValueQuery The batch values query.
         *
         * @return This.
         */
        @Nonnull
        public Builder copyFrom(@Nonnull final BatchValuesQuery batchValueQuery)
        {
            _point = batchValueQuery.getPoint().orElse(null);
            _intervalBuilder.copyFrom(batchValueQuery.getInterval());
            _sync = batchValueQuery.getSync().orElse(null);
            _notNull = batchValueQuery.isNotNull();
            _interpolated = batchValueQuery.isInterpolated();
            _extrapolated = batchValueQuery.isExtrapolated();

            return this;
        }

        /**
         * Sets the after time.
         *
         * @param after The after time.
         *
         * @return This.
         */
        @Nonnull
        public Builder setAfter(@Nonnull final DateTime after)
        {
            _intervalBuilder.setAfter(after);

            return this;
        }

        /**
         * Sets the time for this query.
         *
         * @param at The time.
         *
         * @return This.
         */
        @Nonnull
        public Builder setAt(@Nonnull final DateTime at)
        {
            _intervalBuilder.setAt(at);

            return this;
        }

        /**
         * Sets the before time.
         *
         * @param before The before time.
         *
         * @return This.
         */
        @Nonnull
        public Builder setBefore(@Nonnull final DateTime before)
        {
            _intervalBuilder.setBefore(before);

            return this;
        }

        /**
         * Sets the extrapolated indicator.
         *
         * @param extrapolated The extrapolated indicator.
         *
         * @return This.
         */
        @Nonnull
        public Builder setExtrapolated(final boolean extrapolated)
        {
            _extrapolated = extrapolated;

            return this;
        }

        /**
         * Sets the interpolated indicator.
         *
         * @param interpolated The interpolated indicator.
         *
         * @return This.
         */
        @Nonnull
        public Builder setInterpolated(final boolean interpolated)
        {
            _interpolated = interpolated;

            return this;
        }

        /**
         * Sets the time interval.
         *
         * @param interval The time interval.
         *
         * @return This.
         */
        @Nonnull
        public Builder setInterval(@Nonnull final TimeInterval interval)
        {
            _intervalBuilder.setAfter(interval.getAfter().orElse(null));
            _intervalBuilder.setBefore(interval.getBefore().orElse(null));

            return this;
        }

        /**
         * Sets the time interval.
         *
         * @param intervalBuilder A time interval builder.
         *
         * @return This.
         */
        @Nonnull
        public Builder setInterval(
                @Nonnull final TimeInterval.Builder intervalBuilder)
        {
            return setInterval(intervalBuilder.build());
        }

        /**
         * Sets the not after time.
         *
         * @param notAfter The not after time.
         *
         * @return This.
         */
        @Nonnull
        public Builder setNotAfter(@Nonnull final DateTime notAfter)
        {
            _intervalBuilder.setNotAfter(notAfter);

            return this;
        }

        /**
         * Sets the not before time.
         *
         * @param notBefore The not before time.
         *
         * @return This.
         */
        @Nonnull
        public Builder setNotBefore(@Nonnull final DateTime notBefore)
        {
            _intervalBuilder.setNotBefore(notBefore);

            return this;
        }

        /**
         * Sets the not null indicator.
         *
         * @param notNull The not null indicator.
         *
         * @return This.
         */
        @Nonnull
        public final Builder setNotNull(final boolean notNull)
        {
            _notNull = notNull;

            return this;
        }

        /**
         * Sets the point.
         *
         * @param point An optional point.
         *
         * @return This.
         */
        @Nonnull
        public Builder setPoint(@Nonnull final Optional<Point> point)
        {
            _point = point.orElse(null);

            return this;
        }

        /**
         * Sets the sync object.
         *
         * @param sync The optional sync object.
         *
         * @return This.
         */
        @Nonnull
        public Builder setSync(@Nonnull final Optional<Sync> sync)
        {
            _sync = sync.orElse(null);

            return this;
        }

        private boolean _extrapolated;
        private boolean _interpolated;
        private final TimeInterval.Builder _intervalBuilder = TimeInterval
            .newBuilder();
        private boolean _notNull;
        private Point _point;
        private Sync _sync;
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
