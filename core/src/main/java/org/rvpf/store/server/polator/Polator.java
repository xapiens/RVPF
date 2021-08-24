/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Polator.java 3970 2019-05-09 19:35:44Z SFB $
 */

package org.rvpf.store.server.polator;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.DateTime;
import org.rvpf.base.security.Identity;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.SynthesizedValue;
import org.rvpf.content.DoubleContent;
import org.rvpf.store.server.StoreCursor;

/**
 * Inter/extra-polator.
 */
public interface Polator
{
    /**
     * Inter/extra-polates values for a store query.
     *
     * @param polatedQuery A query asking for inter/extra-polation.
     * @param storeCursor The calling store cursor.
     * @param identity The requesting identity.
     *
     * @return A point value for each synchronization position.
     */
    @Nonnull
    @CheckReturnValue
    StoreValues polate(
            @Nonnull StoreValuesQuery polatedQuery,
            @Nonnull StoreCursor storeCursor,
            @Nonnull Identity identity);

    /**
     * Abstract.
     */
    abstract class Abstract
        implements Polator
    {
        /** {@inheritDoc}
         */
        @Override
        public final StoreValues polate(
                final StoreValuesQuery polatedQuery,
                final StoreCursor storeCursor,
                final Identity identity)
        {
            final ActualValues actualValues = new ActualValues(
                this,
                polatedQuery.getPoint().get(),
                storeCursor,
                identity,
                polatedQuery.getPolatorTimeLimit());
            final RequestedValues requestedValues = new RequestedValues(
                polatedQuery,
                actualValues);

            Optional<DateTime> requestedStamp = requestedValues
                .nextStamp(Optional.empty());

            while (requestedStamp.isPresent()) {
                PointValue requestedValue = null;

                if (polatedQuery.isInterpolated()) {
                    final Optional<PointValue[]> valuesAfter = actualValues
                        .getValuesAfter(
                            requestedStamp.get(),
                            interpolationNeedsAfter());

                    if (valuesAfter.isPresent()) {
                        final PointValue[] valuesBefore = actualValues
                            .getValuesBefore(
                                requestedStamp.get(),
                                interpolationNeedsBefore())
                            .orElse(null);

                        if (valuesBefore != null) {
                            requestedValue = interpolate(
                                polatedQuery,
                                requestedStamp.get(),
                                valuesBefore,
                                valuesAfter.get());
                        }
                    }
                }

                if ((requestedValue == null) && polatedQuery.isExtrapolated()) {
                    final Optional<PointValue[]> valuesBefore = actualValues
                        .getValuesBefore(
                            requestedStamp.get(),
                            extrapolationNeedsBefore());

                    if (valuesBefore.isPresent()) {
                        requestedValue = extrapolate(
                            polatedQuery,
                            requestedStamp.get(),
                            valuesBefore.get());
                    }
                }

                if (requestedValue == null) {
                    requestedValue = new SynthesizedValue(
                        polatedQuery.getPoint().get(),
                        requestedStamp,
                        null,
                        null);
                }

                requestedStamp = requestedValues
                    .nextStamp(Optional.of(requestedValue));
            }

            return requestedValues.getRequestedValues();
        }

        /**
         * Decodes the value from a point value.
         *
         * @param pointValue The point value.
         *
         * @return The numeric value (may be null).
         */
        @Nullable
        @CheckReturnValue
        protected static final Double decode(
                @Nonnull final PointValue pointValue)
        {
            return _DECODER.decode(pointValue);
        }

        /**
         * Extrapolates a value.
         *
         * @param polatedQuery The polated query.
         * @param stamp The stamp for the synthesized value.
         * @param pointValuesBefore The needed point values before.
         *
         * @return The point value.
         */
        @Nonnull
        @CheckReturnValue
        protected abstract PointValue extrapolate(
                @Nonnull StoreValuesQuery polatedQuery,
                @Nonnull DateTime stamp,
                PointValue[] pointValuesBefore);

        /**
         * Returns the number of values extrapolation needs before.
         *
         * @return The number of values extrapolation needs before.
         */
        protected abstract int extrapolationNeedsBefore();

        /**
         * Interpolates a value.
         *
         * @param polatedQuery The polated query.
         * @param stamp The stamp for the synthesized value.
         * @param pointValuesBefore The needed point values before.
         * @param pointValuesAfter The needed point values after.
         *
         * @return The point value.
         */
        @Nonnull
        @CheckReturnValue
        protected abstract PointValue interpolate(
                @Nonnull StoreValuesQuery polatedQuery,
                @Nonnull DateTime stamp,
                PointValue[] pointValuesBefore,
                PointValue[] pointValuesAfter);

        /**
         * Returns the number of values interpolation needs after.
         *
         * @return The number of values interpolation needs after.
         */
        protected abstract int interpolationNeedsAfter();

        /**
         * Returns the number of values interpolation needs before.
         *
         * @return The number of values interpolation needs before.
         */
        protected abstract int interpolationNeedsBefore();

        private static final DoubleContent _DECODER = new DoubleContent();
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
