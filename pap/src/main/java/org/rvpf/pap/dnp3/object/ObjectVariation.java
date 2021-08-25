/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ObjectVariation.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.pap.EnumCode;
import org.rvpf.pap.dnp3.object.content.DataType;

/**
 * Object variation.
 */
public interface ObjectVariation
    extends EnumCode
{
    /**
     * Gets the data type.
     *
     * @return The optional data type.
     */
    @Nonnull
    @CheckReturnValue
    Optional<DataType> getDataType();

    /**
     * Gets the object class.
     *
     * @return The object class.
     */
    @Nonnull
    @CheckReturnValue
    Class<? extends ObjectInstance> getObjectClass();

    /**
     * Gets the object group.
     *
     * @return The object group.
     */
    @Nonnull
    @CheckReturnValue
    ObjectGroup getObjectGroup();

    /**
     * Gets the point type.
     *
     * @return The optional point type.
     */
    @Nonnull
    @CheckReturnValue
    default Optional<PointType> getPointType()
    {
        return getObjectGroup().getPointType();
    }

    /**
     * Gets the variation title.
     *
     * @return The variation title.
     */
    @Nonnull
    @CheckReturnValue
    String getTitle();

    /**
     * Asks if this represents any variation.
     *
     * @return True if this represents any variation.
     */
    @CheckReturnValue
    default boolean isAny()
    {
        return getCode() == 0;
    }

    /**
     * Asks if this variation is in packed format.
     *
     * @return True if this variation is in packed format.
     */
    @CheckReturnValue
    default boolean isPacked()
    {
        return false;
    }

    /**
     * Returns a new object instance.
     *
     * @return The new object instance.
     */
    @Nonnull
    @CheckReturnValue
    default ObjectInstance newObjectInstance()
    {
        try {
            return getObjectClass().getConstructor().newInstance();
        } catch (Exception exception) {
            throw new InternalError(exception);
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
