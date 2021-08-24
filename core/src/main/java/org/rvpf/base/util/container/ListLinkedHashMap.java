/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ListLinkedHashMap.java 3892 2019-02-13 13:24:20Z SFB $
 */

package org.rvpf.base.util.container;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * List linked hash map.
 *
 * <p>Uses a LinkedHashMap to allow multiple values for a single key.</p>
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
@NotThreadSafe
public class ListLinkedHashMap<K, V>
    extends ListMap.Abstract<K, V>
{
    /**
     * Constructs an instance.
     */
    public ListLinkedHashMap()
    {
        super(new LinkedHashMap<K, List<V>>());
    }

    /**
     * Constructs an instance.
     *
     * @param initialCapacity The initial capacity.
     */
    public ListLinkedHashMap(final int initialCapacity)
    {
        super(new LinkedHashMap<K, List<V>>(initialCapacity));
    }

    /**
     * Constructs an instance.
     *
     * @param map A map holding initial entries.
     */
    public ListLinkedHashMap(@Nonnull final Map<K, List<V>> map)
    {
        super(new LinkedHashMap<K, List<V>>(map));
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
