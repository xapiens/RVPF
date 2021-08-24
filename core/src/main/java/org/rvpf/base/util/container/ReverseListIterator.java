/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ReverseListIterator.java 3892 2019-02-13 13:24:20Z SFB $
 */

package org.rvpf.base.util.container;

import java.util.ListIterator;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Reverse list iterator.
 *
 * @param <E> The elements type.
 */
@NotThreadSafe
public class ReverseListIterator<E>
    implements ListIterator<E>
{
    /**
     * Constructs an instance.
     *
     * @param iterator The reversed iterator.
     */
    public ReverseListIterator(@Nonnull final ListIterator<E> iterator)
    {
        _iterator = iterator;
    }

    /** {@inheritDoc}
     */
    @Override
    public void add(final E element)
    {
        _iterator.add(element);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean hasNext()
    {
        return _iterator.hasPrevious();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean hasPrevious()
    {
        return _iterator.hasNext();
    }

    /** {@inheritDoc}
     */
    @Override
    public E next()
    {
        return _iterator.previous();
    }

    /** {@inheritDoc}
     */
    @Override
    public int nextIndex()
    {
        return _iterator.previousIndex();
    }

    /** {@inheritDoc}
     */
    @Override
    public E previous()
    {
        return _iterator.next();
    }

    /** {@inheritDoc}
     */
    @Override
    public int previousIndex()
    {
        return _iterator.nextIndex();
    }

    /** {@inheritDoc}
     */
    @Override
    public void remove()
    {
        _iterator.remove();
    }

    /** {@inheritDoc}
     */
    @Override
    public void set(final E element)
    {
        _iterator.set(element);
    }

    private final ListIterator<E> _iterator;
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
