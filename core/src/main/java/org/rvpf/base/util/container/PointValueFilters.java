/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PointValueFilters.java 4036 2019-05-31 11:19:42Z SFB $
 */

package org.rvpf.base.util.container;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.filter.ValueFilter;

/**
 * Point value filters.
 */
public final class PointValueFilters
{
    /**
     * Clears the filters.
     */
    public void clear()
    {
        _valueFilters.clear();
    }

    /**
     * Filters a point value.
     *
     * @param pointValue The point value.
     *
     * @return Filtered point values.
     */
    @Nonnull
    @CheckReturnValue
    public PointValue[] filter(@Nonnull final PointValue pointValue)
    {
        ValueFilter valueFilter = _valueFilters.get(pointValue.getPointUUID());

        if (valueFilter == null) {
            final Optional<Point> point = pointValue.getPoint();

            if (point.isPresent()) {
                valueFilter = point.get().filter();
                _valueFilters.put(point.get().getUUID().get(), valueFilter);
            }
        }

        return (valueFilter != null)? valueFilter
            .filter(Optional.of(pointValue)): new PointValue[] {pointValue, };
    }

    /**
     * Uses the specified points to get filters.
     *
     * @param points The points.
     */
    public void filterPoints(@Nonnull final Collection<Point> points)
    {
        for (final Point point: points) {
            final ValueFilter valueFilter = point.filter();

            if (!valueFilter.isDisabled()) {
                _valueFilters.put(point.getUUID().get(), valueFilter);
            }
        }
    }

    private final Map<UUID, ValueFilter> _valueFilters = new HashMap<>();
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
