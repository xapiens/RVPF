/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AbstractListNavigableMap.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.base.util.container;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedMap;
import javax.annotation.Nonnull;

/** Abstract list sorted map.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public abstract class AbstractListNavigableMap<K, V>
    extends ListMap.Abstract<K, V>
    implements ListNavigableMap<K, V>
{
    /** Constructs an instance.
     *
     * @param map The map implementation.
     */
    protected AbstractListNavigableMap(
            @Nonnull final NavigableMap<K, List<V>> map)
    {
        super(map);

        _map = map;
    }

    /** {@inheritDoc}
     */
    @Override
    public Entry<K, List<V>> ceilingEntry(final K key)
    {
        return _map.ceilingEntry(key);
    }

    /** {@inheritDoc}
     */
    @Override
    public K ceilingKey(final K key)
    {
        return _map.ceilingKey(key);
    }

    /** {@inheritDoc}
     */
    @Override
    public Comparator<? super K> comparator()
    {
        return _map.comparator();
    }

    /** {@inheritDoc}
     */
    @Override
    public NavigableSet<K> descendingKeySet()
    {
        return _map.descendingKeySet();
    }

    /** {@inheritDoc}
     */
    @Override
    public NavigableMap<K, List<V>> descendingMap()
    {
        return _map.descendingMap();
    }

    /** {@inheritDoc}
     */
    @Override
    public Entry<K, List<V>> firstEntry()
    {
        return _map.firstEntry();
    }

    /** {@inheritDoc}
     */
    @Override
    public K firstKey()
    {
        return _map.firstKey();
    }

    /** {@inheritDoc}
     */
    @Override
    public Entry<K, List<V>> floorEntry(final K key)
    {
        return _map.floorEntry(key);
    }

    /** {@inheritDoc}
     */
    @Override
    public K floorKey(final K key)
    {
        return _map.floorKey(key);
    }

    /** {@inheritDoc}
     */
    @Override
    public SortedMap<K, List<V>> headMap(final K toKey)
    {
        return _map.headMap(toKey);
    }

    /** {@inheritDoc}
     */
    @Override
    public NavigableMap<K, List<V>> headMap(
            final K toKey,
            final boolean inclusive)
    {
        return _map.headMap(toKey, inclusive);
    }

    /** {@inheritDoc}
     */
    @Override
    public Entry<K, List<V>> higherEntry(final K key)
    {
        return _map.higherEntry(key);
    }

    /** {@inheritDoc}
     */
    @Override
    public K higherKey(final K key)
    {
        return _map.higherKey(key);
    }

    /** {@inheritDoc}
     */
    @Override
    public Entry<K, List<V>> lastEntry()
    {
        return _map.lastEntry();
    }

    /** {@inheritDoc}
     */
    @Override
    public K lastKey()
    {
        return _map.lastKey();
    }

    /** {@inheritDoc}
     */
    @Override
    public Entry<K, List<V>> lowerEntry(final K key)
    {
        return _map.lowerEntry(key);
    }

    /** {@inheritDoc}
     */
    @Override
    public K lowerKey(final K key)
    {
        return _map.lowerKey(key);
    }

    /** {@inheritDoc}
     */
    @Override
    public NavigableSet<K> navigableKeySet()
    {
        return _map.navigableKeySet();
    }

    /** {@inheritDoc}
     */
    @Override
    public Entry<K, List<V>> pollFirstEntry()
    {
        return _map.pollFirstEntry();
    }

    /** {@inheritDoc}
     */
    @Override
    public Entry<K, List<V>> pollLastEntry()
    {
        return _map.pollLastEntry();
    }

    /** {@inheritDoc}
     */
    @Override
    public SortedMap<K, List<V>> subMap(final K fromKey, final K toKey)
    {
        return _map.subMap(fromKey, toKey);
    }

    /** {@inheritDoc}
     */
    @Override
    public NavigableMap<K, List<V>> subMap(
            final K fromKey,
            final boolean fromInclusive,
            final K toKey,
            final boolean toInclusive)
    {
        return _map.subMap(fromKey, fromInclusive, toKey, toInclusive);
    }

    /** {@inheritDoc}
     */
    @Override
    public SortedMap<K, List<V>> tailMap(final K fromKey)
    {
        return _map.tailMap(fromKey);
    }

    /** {@inheritDoc}
     */
    @Override
    public NavigableMap<K, List<V>> tailMap(
            final K fromKey,
            final boolean inclusive)
    {
        return _map.tailMap(fromKey, inclusive);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void mapReplaced(final Map<K, List<V>> map)
    {
        super.mapReplaced(map);

        _map = (NavigableMap<K, List<V>>) map;
    }

    private static final long serialVersionUID = 1L;

    private transient NavigableMap<K, List<V>> _map;
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
