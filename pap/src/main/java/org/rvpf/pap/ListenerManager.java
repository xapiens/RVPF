/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.pap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.util.container.Listeners;

/**
 * Listener manager.
 *
 * @param <L> The listener type.
 */
public abstract class ListenerManager<L>
{
    /**
     * Adds a listener.
     *
     * @param listener The listener.
     *
     * @return True if added, false if already added.
     */
    @CheckReturnValue
    public boolean addListener(final L listener)
    {
        return _listeners.add(listener);
    }

    /**
     * Clears the listeners container.
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

    /**
     * Removes a listener.
     *
     * @param listener The listener.
     *
     * @return True if removed, false if already removed.
     */
    @CheckReturnValue
    public boolean removeListener(final L listener)
    {
        return _listeners.remove(listener);
    }

    /**
     * Gets the listeners.
     *
     * @return The listeners.
     */
    @Nonnull
    @CheckReturnValue
    protected Listeners<L> getListeners()
    {
        return _listeners;
    }

    private final Listeners<L> _listeners = new Listeners<>();
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
