/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ValueFilterFactory.java 4043 2019-06-02 15:05:37Z SFB $
 */

package org.rvpf.metadata.entity.filter;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.value.filter.DisabledFilter;
import org.rvpf.base.value.filter.ValueFilter;

/**
 * Value filter factory.
 */
public interface ValueFilterFactory
{
    /**
     * Returns a new value filter.
     *
     * @param params The filter parameters.
     *
     * @return The new filter.
     */
    @Nonnull
    @CheckReturnValue
    ValueFilter newFilter(@Nonnull Params params);

    /**
     * Abstract.
     */
    abstract class Abstract
        implements ValueFilterFactory
    {
        /**
         * Filters disabled filters.
         *
         * @param filter A filter.
         *
         * @return Then filter unless disabled, then a disabled filter.
         */
        protected static ValueFilter filter(final ValueFilter filter)
        {
            return filter.isDisabled()? new DisabledFilter(): filter;
        }

        /**
         * Gets the stamp trim unit.
         *
         * @param params The filter parameters.
         *
         * @return The optional stamp trim unit.
         */
        @Nonnull
        @CheckReturnValue
        protected static final Optional<ElapsedTime> getStampTrimUnit(
                @Nonnull final Params params)
        {
            return params
                .getElapsed(
                    Point.FILTER_STAMP_TRIM_UNIT_PARAM,
                    Optional.empty(),
                    Optional.empty());
        }

        /**
         * Gets the time limit.
         *
         * @param params The filter parameters.
         *
         * @return The optional time limit.
         */
        @Nonnull
        @CheckReturnValue
        protected static final Optional<ElapsedTime> getTimeLimit(
                @Nonnull final Params params)
        {
            return params
                .getElapsed(
                    Point.FILTER_TIME_LIMIT_PARAM,
                    Optional.empty(),
                    Optional.empty());
        }
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
