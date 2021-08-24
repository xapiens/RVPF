/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Content.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.base;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;

/**
 * Content.
 *
 * <p>Used for the conversion of point values.</p>
 *
 * <p>The {@link #decode}/{@link #encode} methods deal with the physical
 * representation of the values, taking into account persistence constraints.
 * The conversion is not always symetric as the goal is to be lenient on input,
 * but consistent on output.</p>
 *
 * <p>The {@link #normalize}/{@link #denormalize} methods deal with the logical
 * representation of the values (units conversion).</p>
 */
public interface Content
{
    /**
     * Decodes the value of a point value.
     *
     * <p>Converts from an external representation to an internal
     * representation.</p>
     *
     * @param pointValue The point value associated with the value to decode.
     *
     * @return The decoded value.
     */
    @Nullable
    @CheckReturnValue
    Serializable decode(@Nonnull PointValue pointValue);

    /**
     * Denormalizes the value of a point value.
     *
     * <p>Converts from an internal unit to an external unit.</p>
     *
     * @param normalizedValue The point value associated with the value to be
     *                        denormalized.
     *
     * @return The denormalized value.
     */
    @Nullable
    @CheckReturnValue
    Serializable denormalize(@Nonnull NormalizedValue normalizedValue);

    /**
     * Encodes the value of a point value.
     *
     * <p>Converts from an internal representation to an external
     * representation.</p>
     *
     * @param pointValue The point value associated with the value to encode.
     *
     * @return The encoded value.
     */
    @Nullable
    @CheckReturnValue
    Serializable encode(@Nonnull PointValue pointValue);

    /**
     * Gets an appropriate instance of this content for the point.
     *
     * @param point The point.
     *
     * @return A content instance (null on failure).
     */
    @Nullable
    @CheckReturnValue
    Content getInstance(@Nonnull Point point);

    /**
     * Normalizes the value of a point value.
     *
     * <p>Converts from an external unit to an internal unit.</p>
     *
     * @param pointValue The point value associated with the value to be
     *                   normalized.
     *
     * @return A normalized value.
     */
    @Nullable
    @CheckReturnValue
    Serializable normalize(@Nonnull PointValue pointValue);
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
