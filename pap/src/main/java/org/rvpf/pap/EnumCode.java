/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: EnumCode.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Enum code.
 */
public interface EnumCode
{
    /**
     * Gets the code.
     *
     * @return The code.
     */
    @CheckReturnValue
    int getCode();

    /**
     * Returns the name.
     *
     * @return The name.
     */
    @Nonnull
    @CheckReturnValue
    String name();

    /**
     * Map factory.
     */
    final class MapFactory
    {
        private MapFactory() {}

        /**
         * Returns a code map.
         *
         * @param <T> The enum type.
         * @param values The enum values.
         *
         * @return A code map.
         */
        @Nonnull
        @CheckReturnValue
        public static <T extends Enum<T>> Map<Integer, T> codeMap(
                final T[] values)
        {
            final Map<Integer, T> codeMap = new HashMap<>();

            for (final T value: values) {
                codeMap.put(
                    Integer.valueOf(((EnumCode) value).getCode()),
                    value);
            }

            return codeMap;
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
