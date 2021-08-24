/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ConcurrentIdentityHashSet.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.util.container;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import org.rvpf.base.tool.Require;

/**
 * Concurrent identity hash set.
 *
 * <p>Similar in role to the ConcurrentHashSet, implements the Set interface
 * using reference equality instead of object equality.</p>
 *
 * @param <E> The element type.
 */
@ThreadSafe
public class ConcurrentIdentityHashSet<E>
    extends AbstractSet<E>
{
    /** {@inheritDoc}
     */
    @Override
    public boolean add(final E object)
    {
        return _map.put(new _Key(object), object) == null;
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
        return _map.containsKey(new _Key(object));
    }

    /** {@inheritDoc}
     */
    @Override
    public Iterator<E> iterator()
    {
        return _map.values().iterator();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean remove(final Object object)
    {
        return _map.remove(new _Key(object)) != null;
    }

    /** {@inheritDoc}
     */
    @Override
    public int size()
    {
        return _map.size();
    }

    private final Map<_Key, E> _map = new ConcurrentHashMap<>();

    /**
     * Key.
     */
    @Immutable
    private static final class _Key
    {
        /**
         * Constructs an instance.
         *
         * @param object The object represented by the key.
         */
        _Key(@Nonnull final Object object)
        {
            _object = Require.notNull(object);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean equals(final Object object)
        {
            if (object instanceof _Key) {
                return ((_Key) object)._object == _object;
            }

            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            return System.identityHashCode(_object);
        }

        private final Object _object;
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
