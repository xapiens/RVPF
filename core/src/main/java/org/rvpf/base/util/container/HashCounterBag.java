/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: HashCounterBag.java 3892 2019-02-13 13:24:20Z SFB $
 */

package org.rvpf.base.util.container;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.tool.Require;

/**
 * Hash counter bag.
 *
 * @param <E> The element type.
 */
@NotThreadSafe
public class HashCounterBag<E>
    extends AbstractCollection<E>
    implements Bag<E>
{
    /** {@inheritDoc}
     */
    @Override
    public boolean add(final E element)
    {
        final Integer count = _map.get(Require.notNull(element));

        _map
            .put(
                element,
                Integer.valueOf((count != null)? (count.intValue() + 1): 1));

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void clear()
    {
        _map.clear();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean contains(final Object element)
    {
        return _map.containsKey(element);
    }

    /** {@inheritDoc}
     */
    @Override
    public int count(final E element)
    {
        final Integer count = _map.get(element);

        return (count != null)? count.intValue(): 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public Iterator<E> iterator()
    {
        return _map.keySet().iterator();
    }

    /** {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(final Object element)
    {
        final Integer count = _map.get(element);

        if (count == null) {
            return false;
        }

        if (count.intValue() > 1) {
            _map.put((E) element, Integer.valueOf(count.intValue() - 1));
        } else {
            _map.remove(element);
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public int size()
    {
        return _map.size();
    }

    private final Map<E, Integer> _map = new LinkedHashMap<E, Integer>();
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
