/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MessageType.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base.util.rlp;

import java.util.Arrays;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;

/**
 * Message type.
 *
 * <p>Represents the Resource Location Protocol message types.</p>
 */
enum MessageType
{
    WHO_PROVIDES,
    DO_YOU_PROVIDE,
    WHO_ANYWHERE_PROVIDES,
    DOES_ANYONE_PROVIDE,
    I_PROVIDE,
    THEY_PROVIDE;

    /**
     * Gets the instance for an ordinal.
     *
     * @param ordinal The ordinal.
     *
     * @return The instance.
     */
    @Nonnull
    @CheckReturnValue
    public static MessageType instance(final int ordinal)
    {
        Require.failure((ordinal < 0) || (_TYPE_ARRAY.length <= ordinal));

        return _TYPE_ARRAY[ordinal];
    }

    private static final MessageType[] _TYPE_ARRAY =
        new MessageType[values().length];

    static {
        Arrays
            .stream(values())
            .forEach(type -> _TYPE_ARRAY[type.ordinal()] = type);
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
