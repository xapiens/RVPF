/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Tuple.java 3983 2019-05-14 11:11:45Z SFB $
 */

package org.rvpf.base.value;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.annotation.Nonnull;

/**
 * Tuple.
 */
public final class Tuple
    implements Container, List<Serializable>
{
    /**
     * Constructs an instance.
     */
    public Tuple()
    {
        this(0);
    }

    /**
     * Constructs an instance.
     *
     * @param contents The initial contents.
     */
    public Tuple(@Nonnull final Collection<? extends Serializable> contents)
    {
        _items = new ArrayList<>(contents);
    }

    /**
     * Constructs an instance.
     *
     * @param capacity Initial capacity.
     */
    public Tuple(final int capacity)
    {
        _items = new ArrayList<>(capacity);
    }

    /**
     * Constructs an instance.
     *
     * @param contents The initial contents.
     */
    public Tuple(@Nonnull final Serializable[] contents)
    {
        this(contents.length);

        for (final Serializable content: contents) {
            add(content);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean add(final Serializable item)
    {
        return _getItems().add(item);
    }

    /** {@inheritDoc}
     */
    @Override
    public void add(final int index, final Serializable item)
    {
        _getItems().add(index, item);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean addAll(final Collection<? extends Serializable> collection)
    {
        return _getItems().addAll(collection);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean addAll(
            final int index,
            final Collection<? extends Serializable> collection)
    {
        return _getItems().addAll(index, collection);
    }

    /** {@inheritDoc}
     */
    @Override
    public void clear()
    {
        _getItems().clear();
    }

    /** {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Tuple clone()
    {
        final Tuple clone;

        try {
            clone = (Tuple) super.clone();
        } catch (final CloneNotSupportedException exception) {
            throw new InternalError(exception);
        }

        clone._items = (ArrayList<Serializable>) _items.clone();
        clone._frozen = null;

        return clone;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean contains(final Object object)
    {
        return _items.contains(object);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean containsAll(final Collection<?> collection)
    {
        return _items.containsAll(collection);
    }

    /**
     * Ensures a minimum capacity.
     *
     * @param capacity The minimum capacity.
     */
    public void ensureCapacity(final int capacity)
    {
        if (isFrozen()) {
            throw new UnsupportedOperationException();
        }

        _items.ensureCapacity(capacity);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object)
    {
        if (object == this) {
            return true;
        }

        if (!(object instanceof Tuple)) {
            return false;
        }

        return _items.equals(((Tuple) object)._items);
    }

    /** {@inheritDoc}
     */
    @Override
    public void freeze()
    {
        if (_frozen == null) {
            _frozen = Collections.unmodifiableList(_items);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable get(final int index)
    {
        return _items.get(index);
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return _items.hashCode();
    }

    /** {@inheritDoc}
     */
    @Override
    public int indexOf(final Object object)
    {
        return _items.indexOf(object);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        return _items.isEmpty();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isFrozen()
    {
        return _frozen != null;
    }

    /** {@inheritDoc}
     */
    @Override
    public Iterator<Serializable> iterator()
    {
        return _getItems().iterator();
    }

    /** {@inheritDoc}
     */
    @Override
    public int lastIndexOf(final Object object)
    {
        return _items.lastIndexOf(object);
    }

    /** {@inheritDoc}
     */
    @Override
    public ListIterator<Serializable> listIterator()
    {
        return _getItems().listIterator();
    }

    /** {@inheritDoc}
     */
    @Override
    public ListIterator<Serializable> listIterator(final int index)
    {
        return _getItems().listIterator(index);
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable remove(final int index)
    {
        return _getItems().remove(index);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean remove(final Object object)
    {
        return _getItems().remove(object);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean removeAll(final Collection<?> collection)
    {
        return _getItems().removeAll(collection);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean retainAll(final Collection<?> collection)
    {
        return _getItems().retainAll(collection);
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable set(final int index, final Serializable item)
    {
        return _getItems().set(index, item);
    }

    /** {@inheritDoc}
     */
    @Override
    public int size()
    {
        return _items.size();
    }

    /** {@inheritDoc}
     */
    @Override
    public List<Serializable> subList(final int fromIndex, final int toIndex)
    {
        return _getItems().subList(fromIndex, toIndex);
    }

    /** {@inheritDoc}
     */
    @Override
    public Object[] toArray()
    {
        return _items.toArray();
    }

    /** {@inheritDoc}
     */
    @Override
    public <T> T[] toArray(final T[] array)
    {
        return _items.toArray(array);
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return _items.toString();
    }

    private List<Serializable> _getItems()
    {
        final List<Serializable> frozen = _frozen;

        return (frozen != null)? frozen: _items;
    }

    private Object readResolve()
    {
        freeze();

        return this;
    }

    private static final long serialVersionUID = 1L;

    private transient List<Serializable> _frozen;
    private ArrayList<Serializable> _items;
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
