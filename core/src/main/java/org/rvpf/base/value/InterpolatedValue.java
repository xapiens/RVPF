/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: InterpolatedValue.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.value;

import java.io.Serializable;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;

/**
 * Interpolated point value.
 */
@NotThreadSafe
public final class InterpolatedValue
    extends SynthesizedValue
{
    /**
     * Constructs an empty instance.
     *
     * <p>This is needed for an Externalizable implementation.</p>
     */
    public InterpolatedValue() {}

    /**
     * Constructs an instance.
     *
     * @param point The point definition.
     * @param stamp The optional time stamp of the value.
     * @param state The state.
     * @param value The value.
     */
    public InterpolatedValue(
            @Nonnull final Point point,
            @Nonnull final Optional<DateTime> stamp,
            @Nullable final Serializable state,
            @Nullable final Serializable value)
    {
        super(point, stamp, state, value);
    }

    private InterpolatedValue(@Nonnull final InterpolatedValue other)
    {
        super(other);
    }

    /** {@inheritDoc}
     */
    @Override
    public InterpolatedValue copy()
    {
        return new InterpolatedValue(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInterpolated()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getSynthesizedString()
    {
        return "interpolated";
    }

    private static final long serialVersionUID = 1L;
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
