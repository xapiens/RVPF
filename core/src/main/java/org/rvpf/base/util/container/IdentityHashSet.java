/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: IdentityHashSet.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.util.container;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.rvpf.base.tool.Require;

/**
 * Identity hash set.
 *
 * <p>Similar in role to the IdentityHashMap, implements the Set interface using
 * reference equality instead of object equality.</p>
 *
 * @param <E> The element type.
 */
@NotThreadSafe
public class IdentityHashSet<E>
    extends AbstractSet<E>
{
    /**
     * Constructs an instance.
     */
    public IdentityHashSet() {}

    /**
     * Constructs an instance.
     *
     * @param collection Initial content.
     */
    public IdentityHashSet(@Nonnull final Collection<E> collection)
    {
        addAll(collection);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean add(final E object)
    {
        return _map.put(Require.notNull(object), Boolean.TRUE) == null;
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
    public boolean contains(final Object object)
    {
        return _map.containsKey(object);
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
    @Override
    public boolean remove(final Object object)
    {
        return _map.remove(object) != null;
    }

    /** {@inheritDoc}
     */
    @Override
    public int size()
    {
        return _map.size();
    }

    private final Map<E, Boolean> _map = new IdentityHashMap<>();
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
