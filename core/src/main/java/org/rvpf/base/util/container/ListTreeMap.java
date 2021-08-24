/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ListTreeMap.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.base.util.container;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nonnull;

/** List tree map.
 *
 * <p>Uses a TreeMap to allow multiple values for a single key.</p>
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class ListTreeMap<K, V>
    extends AbstractListNavigableMap<K, V>
{
    /** Constructs an instance.
     */
    public ListTreeMap()
    {
        super(new TreeMap<K, List<V>>());
    }

    /** Constructs an instance.
     *
     * @param map A map holding initial entries.
     */
    public ListTreeMap(@Nonnull final Map<K, List<V>> map)
    {
        super(new TreeMap<K, List<V>>(map));
    }

    /** Constructs an instance.
     *
     * @param comparator The comparator used to order this map.
     */
    public ListTreeMap(@Nonnull final Comparator<? super K> comparator)
    {
        super(new TreeMap<K, List<V>>(comparator));
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
