/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CacheValue.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.processor;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;

/**
 * CacheValue.
 */
final class CacheValue
    implements Comparable<CacheValue>
{
    /**
     * Constructs an instance.
     *
     * @param pointValue The cached point value.
     * @param after The optional 'after' stamp.
     * @param before The optional 'before' stamp.
     * @param nullIgnored True when null is ignored.
     */
    CacheValue(
            @Nonnull final PointValue pointValue,
            @Nonnull final Optional<DateTime> after,
            @Nonnull final Optional<DateTime> before,
            final boolean nullIgnored)
    {
        setPointValue(pointValue);
        setAfter(after);
        setBefore(before);
        setNullIgnored(nullIgnored);
        _hits = _boost;
    }

    /** {@inheritDoc}
     */
    @Override
    public int compareTo(final CacheValue other)
    {
        return getStamp().compareTo(other.getStamp());
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if (other instanceof CacheValue) {
            return getStamp().equals(((CacheValue) other).getStamp());
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return _pointValue.getStamp().hashCode();
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(getPointValue().toString());
        stringBuilder
            .append(
                TimeInterval.newBuilder().setAfter(_after).setBefore(_before));

        if (isNullIgnored()) {
            stringBuilder.append(", nulls ignored");
        }

        return stringBuilder.toString();
    }

    /**
     * Sets the boost value.
     *
     * @param boost The boost value.
     */
    static void setBoost(final int boost)
    {
        _boost = boost;
    }

    /**
     * Gets the 'after' stamp.
     *
     * @return Returns the optional 'after' stamp.
     */
    @Nonnull
    @CheckReturnValue
    Optional<DateTime> getAfter()
    {
        return Optional.ofNullable(_after);
    }

    /**
     * Gets the 'before' stamp.
     *
     * @return Returns the optional 'before' stamp.
     */
    @Nonnull
    @CheckReturnValue
    Optional<DateTime> getBefore()
    {
        return Optional.ofNullable(_before);
    }

    /**
     * Gets the cached point value.
     *
     * @return The cached point value.
     */
    @Nonnull
    @CheckReturnValue
    PointValue getPointValue()
    {
        return _pointValue;
    }

    /**
     * Gets the point value stamp.
     *
     * @return The point value stamp.
     */
    @Nonnull
    @CheckReturnValue
    DateTime getStamp()
    {
        return _pointValue.getStamp();
    }

    /**
     * Hits this cache value.
     */
    void hit()
    {
        ++_hits;
    }

    /**
     * Returns and clears the number of hits.
     *
     * @return The number of hits.
     */
    @CheckReturnValue
    int hits()
    {
        final int hits = _hits;

        if (_hits > 0) {
            --_hits;
        }

        return hits;
    }

    /**
     * Asks if the cached value is null.
     *
     * @return True if the cached value is null.
     */
    @CheckReturnValue
    boolean isNull()
    {
        return _pointValue.getValue() == null;
    }

    /**
     * Asks if a null is ignored.
     *
     * @return True if a null is ignored.
     */
    @CheckReturnValue
    boolean isNullIgnored()
    {
        return _nullIgnored;
    }

    /**
     * Sets the 'after' stamp.
     *
     * @param after The optional 'after' stamp.
     */
    void setAfter(@Nonnull final Optional<DateTime> after)
    {
        _after = after.orElse(null);

        if (_after == null) {
            _after = _pointValue.getStamp().before();
        } else {
            Require
                .success(
                    _after.isBefore(_pointValue.getStamp()),
                    _after + " not before " + _pointValue.getStamp());
        }
    }

    /**
     * Sets the 'before' stamp.
     *
     * @param before The optional 'before' stamp.
     */
    void setBefore(@Nonnull final Optional<DateTime> before)
    {
        _before = before.orElse(null);

        if (_before == null) {
            _before = _pointValue.getStamp().after();
        } else {
            Require
                .success(
                    _before.isAfter(_pointValue.getStamp()),
                    _before + " not after " + _pointValue.getStamp());
        }
    }

    /**
     * Sets the null ignored indicator.
     *
     * @param nullIgnored The null ignored indicator.
     */
    void setNullIgnored(final boolean nullIgnored)
    {
        _nullIgnored = nullIgnored;
    }

    /**
     * Sets the point value.
     *
     * @param pointValue The point value.
     */
    void setPointValue(@Nonnull final PointValue pointValue)
    {
        pointValue.freeze();
        _pointValue = pointValue;
    }

    /** Log shared with {@link CacheEntry}. */
    static final Logger LOGGER = Logger.getInstance(CacheValue.class);

    /**  */

    private static int _boost;

    private DateTime _after;
    private DateTime _before;
    private int _hits;
    private boolean _nullIgnored;
    private PointValue _pointValue;
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
