/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Mappable.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.base.util;

import java.io.Serializable;
import java.util.Map;
import javax.annotation.Nonnull;

/** Mappable.
 *
 * <p>Implemented by objects using a {@link Map} to hold their non transient
 * informations.</p>
 */
public interface Mappable
{
    /** Reads a map of the fields.
     *
     * @param map The source map.
     */
    void readMap(@Nonnull Map<String, Serializable> map);

    /** Writes a map of the fields.
     *
     * @param map The destination map.
     */
    void writeMap(@Nonnull Map<String, Serializable> map);

    /** Serializable mode: used last in {@link #writeMap} */
    String SERIALIZABLE_MODE = null;

    /** Simple String mode: used first in {@link #writeMap}. */
    String SIMPLE_STRING_MODE = "";
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
