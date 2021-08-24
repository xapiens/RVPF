/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Listeners.java 3983 2019-05-14 11:11:45Z SFB $
 */

package org.rvpf.base.util.container;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Listeners.
 *
 * <p>This container is specialized for objects acting as listeners. They are
 * kept in their order of entry. Duplicate additions, recognized by their object
 * identity, will be rejected.</p>
 *
 * <p>It is assumed that the number of entries will be small and that
 * addition/removal will be infrequent.</p>
 *
 * @param <L> The listener type.
 */
@ThreadSafe
public final class Listeners<L>
    implements Iterable<L>
{
    /**
     * Adds a listener if not already contained.
     *
     * @param listener The listener.
     *
     * @return True if added.
     */
    public synchronized boolean add(@Nonnull final L listener)
    {
        final IdentityDeputy<L> deputy = new IdentityDeputy<>(listener);

        if (_listeners.contains(deputy)) {
            return false;
        }

        _listeners.add(deputy);

        return true;
    }

    /**
     * Removes all listeners.
     */
    public void clear()
    {
        _listeners.clear();
    }

    /**
     * Asks if empty.
     *
     * @return True if empty.
     */
    @CheckReturnValue
    public boolean isEmpty()
    {
        return _listeners.isEmpty();
    }

    /** {@inheritDoc}
     */
    @Override
    public Iterator<L> iterator()
    {
        return IdentityDeputy.iterator(_listeners.iterator());
    }

    /**
     * Removes a listener.
     *
     * @param listener The listener.
     *
     * @return True if removed.
     */
    public boolean remove(@Nonnull final L listener)
    {
        return _listeners.remove(new IdentityDeputy<>(listener));
    }

    private final Collection<IdentityDeputy<L>> _listeners =
        new ConcurrentLinkedQueue<>();
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
