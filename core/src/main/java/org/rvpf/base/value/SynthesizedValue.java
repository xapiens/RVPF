/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SynthesizedValue.java 3961 2019-05-06 20:14:59Z SFB $
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
 * Synthesized point value.
 */
@NotThreadSafe
public class SynthesizedValue
    extends PointValue
{
    /**
     * Constructs an instance.
     *
     * <p>This is needed for an externalizable implementation.</p>
     */
    public SynthesizedValue() {}

    /**
     * Constructs an instance.
     *
     * @param point The point definition.
     * @param stamp The optional time stamp of the value.
     * @param state The state.
     * @param value The value.
     */
    public SynthesizedValue(
            @Nonnull final Point point,
            @Nonnull final Optional<DateTime> stamp,
            @Nullable final Serializable state,
            @Nullable final Serializable value)
    {
        super(point, stamp, state, value);
    }

    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    protected SynthesizedValue(@Nonnull final SynthesizedValue other)
    {
        super(other);
    }

    /** {@inheritDoc}
     */
    @Override
    public SynthesizedValue copy()
    {
        return new SynthesizedValue(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isAbsent()
    {
        return super.isAbsent() || (getValue() == null);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isSynthesized()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getSynthesizedString()
    {
        return "synthesized";
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
