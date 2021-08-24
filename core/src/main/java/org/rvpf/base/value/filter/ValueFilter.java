/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ValueFilter.java 4041 2019-06-01 17:49:03Z SFB $
 */

package org.rvpf.base.value.filter;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.value.PointValue;

/**
 * Value filter.
 */
public interface ValueFilter
{
    /**
     * Filters a point value.
     *
     * <p>When a value filter needs to return a modified point value, if the
     * supplied one is frozen, it will return a frozen but modified clone;
     * otherwise, it will return the original with the needed modifications but
     * frozen.</p>
     *
     * @param pointValue The optional point value.
     *
     * @return Filtered point values.
     */
    @Nonnull
    @CheckReturnValue
    PointValue[] filter(@Nonnull Optional<PointValue> pointValue);

    /**
     * Gets the disabled indicator.
     *
     * @return The disabled indicator.
     */
    @CheckReturnValue
    boolean isDisabled();

    /**
     * Resets.
     */
    void reset();

    public abstract class Abstract
        implements ValueFilter
    {
        /**
         * Constructs an instance.
         *
         * @param timeLimit The optional time limit.
         * @param stampTrimUnit The optional stamp trim unit.
         */
        protected Abstract(
                @Nonnull final Optional<ElapsedTime> timeLimit,
                @Nonnull final Optional<ElapsedTime> stampTrimUnit)
        {
            _timeLimit = timeLimit;
            _stampTrimUnit = stampTrimUnit;
        }

        /** {@inheritDoc}
         */
        @Override
        public final PointValue[] filter(
                final Optional<PointValue> optionalPointValue)
        {
            if (isDisabled()) {
                return pointValues(optionalPointValue.orElse(null));
            }

            if (!optionalPointValue.isPresent()) {
                final Optional<PointValue> heldPointValue = getHeldPointValue();

                reset();

                return pointValues(heldPointValue.orElse(null));
            }

            PointValue pointValue = optionalPointValue.get();

            if (!pointValue.hasStamp()) {
                pointValue = pointValue.thawed();
                pointValue.setStamp(DateTime.now());
            }

            if (_stampTrimUnit.isPresent()) {
                final DateTime stamp = pointValue.getStamp();
                final DateTime trimmedStamp = stamp
                    .floored(_stampTrimUnit.get());

                if (!trimmedStamp.equals(stamp)) {
                    pointValue = pointValue.thawed();
                    pointValue.clearStamp();
                    pointValue.setStamp(trimmedStamp);
                }
            }

            if (pointValue.getValue() == null) {
                final Optional<PointValue> heldPointValue = getHeldPointValue();

                reset();

                return heldPointValue
                    .isPresent()? pointValues(
                        heldPointValue.get(),
                        pointValue): pointValues(pointValue);
            }

            pointValue = snap(pointValue);

            final Optional<PointValue> previousPointValue =
                getPreviousPointValue();

            if (!previousPointValue.isPresent()) {
                setPreviousPointValue(pointValue);

                return pointValues(pointValue);
            }

            final Optional<ElapsedTime> timeLimit = getTimeLimit();

            if (timeLimit.isPresent()) {
                if (timeLimit
                    .get()
                    .compareTo(
                        pointValue
                            .getStamp()
                            .sub(previousPointValue.get().getStamp())) < 0) {
                    final Optional<PointValue> heldPointValue =
                        getHeldPointValue();

                    reset();
                    setPreviousPointValue(pointValue);

                    return heldPointValue
                        .isPresent()? pointValues(
                            heldPointValue.get(),
                            pointValue): pointValues(pointValue);
                }
            }

            if (pointValue != null) {
                pointValue = doFilter(pointValue).orElse(null);

                if (pointValue != null) {
                    pointValue.freeze();
                }
            }

            return pointValues(pointValue);
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean isDisabled()
        {
            return _disabled;
        }

        /** {@inheritDoc}
         */
        @Override
        public void reset()
        {
            _previousPointValue = Optional.empty();
            _heldPointValue = Optional.empty();
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return getClass()
                .getSimpleName() + "@" + Integer.toHexString(
                    System.identityHashCode(this));
        }

        /**
         * Says if a value should be filtered.
         *
         * @param pointValue The point value.
         *
         * @return Empty if it should be filtered.
         */
        @Nonnull
        @CheckReturnValue
        protected abstract Optional<PointValue> doFilter(
                @Nonnull final PointValue pointValue);

        /**
         * Gets the held point value.
         *
         * @return The optional held point value.
         */
        @Nonnull
        @CheckReturnValue
        protected final Optional<PointValue> getHeldPointValue()
        {
            return _heldPointValue;
        }

        /**
         * Gets the previous point value.
         *
         * @return The optional previous point value.
         */
        @Nonnull
        @CheckReturnValue
        protected final Optional<PointValue> getPreviousPointValue()
        {
            return _previousPointValue;
        }

        /**
         * Gets the time limit.
         *
         * @return The optional time limit.
         */
        @Nonnull
        @CheckReturnValue
        protected final Optional<ElapsedTime> getTimeLimit()
        {
            return _timeLimit;
        }

        /**
         * Returns the point values into an array.
         *
         * @param pointValues The point values.
         *
         * @return The point value array.
         */
        @Nonnull
        @CheckReturnValue
        protected PointValue[] pointValues(final PointValue... pointValues)
        {
            return ((pointValues.length == 1)
                    && (pointValues[0] == null))? NO_POINT_VALUES: pointValues;
        }

        /**
         * Sets the disabled indicator.
         *
         * @param disabled The disabled indicator.
         */
        protected final void setDisabled(final boolean disabled)
        {
            _disabled = disabled;
        }

        /**
         * Sets the held point value.
         *
         * @param heldPointValue The held point value.
         */
        protected final void setHeldPointValue(
                @Nonnull final PointValue heldPointValue)
        {
            _heldPointValue = Optional.of(heldPointValue.frozen());
        }

        /**
         * Sets the previous point value.
         *
         * @param previousValue The previous value.
         */
        protected final void setPreviousPointValue(
                @Nonnull final PointValue previousValue)
        {
            _previousPointValue = Optional.of(previousValue.frozen());
        }

        /**
         * Snaps the point value.
         *
         * @param pointValue The point value.
         *
         * @return The snapped point value.
         */
        @Nonnull
        @CheckReturnValue
        protected PointValue snap(@Nonnull final PointValue pointValue)
        {
            return pointValue;
        }

        /** No point values. */
        protected static final PointValue[] NO_POINT_VALUES = new PointValue[0];

        private boolean _disabled;
        private Optional<PointValue> _heldPointValue = Optional.empty();
        private Optional<PointValue> _previousPointValue = Optional.empty();
        private final Optional<ElapsedTime> _stampTrimUnit;
        private final Optional<ElapsedTime> _timeLimit;
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
