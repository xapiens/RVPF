/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: NormalizedValue.java 4036 2019-05-31 11:19:42Z SFB $
 */

package org.rvpf.base.value;

import java.io.Serializable;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.Content;
import org.rvpf.base.DateTime;
import org.rvpf.base.Point;

/**
 * Normalized point value.
 *
 * <p>Instances of this class hold normalized point values. It can produce a
 * denormalized version of itself.</p>
 */
@NotThreadSafe
public class NormalizedValue
    extends PointValue
{
    /**
     * Constructs an instance.
     *
     * <p>This is needed for an Externalizable implementation.</p>
     */
    public NormalizedValue() {}

    /**
     * Constructs an instance.
     *
     * @param pointValue The source point value.
     */
    public NormalizedValue(@Nonnull final PointValue pointValue)
    {
        super(pointValue);
    }

    /**
     * Constructs an instance.
     *
     * @param point The Point definition.
     * @param stamp The optional time stamp of the value.
     * @param state The state.
     * @param value The value.
     */
    public NormalizedValue(
            @Nonnull final Point point,
            @Nonnull final Optional<DateTime> stamp,
            @Nullable final Serializable state,
            @Nullable final Serializable value)
    {
        super(point, stamp, state, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public NormalizedValue copy()
    {
        return new NormalizedValue(this);
    }

    /** {@inheritDoc}
     *
     * <p>The point's content instance is called to perform the actual
     * denormalization.</p>
     */
    @Override
    public PointValue denormalized()
    {
        final PointValue denormalized = new PointValue(this);

        if (getValue() != null) {
            final Optional<Content> content = getPoint().get().getContent();

            if (!content.isPresent()) {
                return new PointValue(this);
            }

            denormalized.setValue(content.get().denormalize(this));
        }

        return denormalized;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isNormalized()
    {
        return true;
    }

    /** {@inheritDoc}
     *
     * <p>This is already normalized.</p>
     */
    @Override
    public final NormalizedValue normalized()
    {
        return this;
    }

    private static final long serialVersionUID = 1L;

    /**
     * Null.
     */
    static class Null
        extends NormalizedValue
    {
        /**
         * Constructs an instance.
         *
         * @param pointValue The source point value.
         */
        public Null(@Nonnull final PointValue pointValue)
        {
            super(pointValue);

            setFrozen();
        }

        /** {@inheritDoc}
         */
        @Override
        public PointValue denormalized()
        {
            return new PointValue.Null(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean equals(final Object object)
        {
            if (this == object) {
                return true;
            }

            if ((object == null) || (getClass() != object.getClass())) {
                return false;
            }

            return Objects
                .equals(getPointUUID(), ((Null) object).getPointUUID());
        }

        /** {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            return Objects.hashCode(getPointUUID());
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean sameValueAs(final PointValue pointValue)
        {
            return false;
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
