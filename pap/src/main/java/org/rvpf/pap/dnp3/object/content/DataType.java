/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DataType.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.content;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Data type.
 */
public enum DataType
{
    ANY(Object.class),
    BSTR1(Boolean.class),
    BSTR2(DoubleBitState.class),
    INT16(Short.class),
    INT32(Integer.class),
    UINT16(Integer.class),
    UINT32(Long.class),
    FLOAT16(Float.class),
    FLOAT32(Double.class);

    /**
     * Constructs an instance.
     *
     * @param dataClass The data class.
     */
    DataType(@Nonnull final Class<?> dataClass)
    {
        _dataClass = dataClass;
    }

    /**
     * Gets the data class.
     *
     * @return The data class.
     */
    @Nonnull
    @CheckReturnValue
    public Class<?> getDataClass()
    {
        return _dataClass;
    }

    private final Class<?> _dataClass;
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
