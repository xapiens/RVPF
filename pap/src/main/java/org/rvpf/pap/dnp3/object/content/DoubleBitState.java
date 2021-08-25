/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DoubleBitState.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.pap.dnp3.object.content;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.EnumCode;

/**
 * Double-bit state.
 */
public enum DoubleBitState
    implements EnumCode
{
    INTERMEDIATE(0b00),
    DETERMINED_OFF(0b01),
    DETERMINED_ON(0b10),
    INDETERMINATE(0b11);

    /**
     * Constructs an instance.
     *
     * @param code The code.
     */
    DoubleBitState(final int code)
    {
        Require.success(code == ordinal());
    }

    /**
     * Gets the instance for an ordinal.
     *
     * @param ordinal The ordinal.
     *
     * @return The instance.
     */
    @Nonnull
    @CheckReturnValue
    public static DoubleBitState instance(final int ordinal)
    {
        return _ENUM_CONSTANTS[ordinal];
    }

    @Override
    public int getCode()
    {
        return ordinal();
    }

    private static final DoubleBitState[] _ENUM_CONSTANTS = DoubleBitState.class
        .getEnumConstants();
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
