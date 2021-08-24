/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Dict.java 3983 2019-05-14 11:11:45Z SFB $
 */

package org.rvpf.base.value;

import java.io.Serializable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dict.
 */
public final class Dict
    implements Container, Map<String, Serializable>
{
    /**
     * Constructs an instance.
     */
    public Dict()
    {
        _entries = new LinkedHashMap<String, Serializable>();
    }

    /**
     * Constructs an instance.
     *
     * @param capacity Initial capacity.
     */
    public Dict(final int capacity)
    {
        _entries = new LinkedHashMap<String, Serializable>(capacity);
    }

    /** {@inheritDoc}
     */
    @Override
    public void clear()
    {
        _getEntries().clear();
    }

    /** {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Dict clone()
    {
        final Dict clone;

        try {
            clone = (Dict) super.clone();
        } catch (final CloneNotSupportedException exception) {
            throw new InternalError(exception);
        }

        clone._entries = (LinkedHashMap<String, Serializable>) _entries.clone();
        clone._frozen = null;

        return clone;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean containsKey(final Object key)
    {
        return _entries.containsKey(key);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean containsValue(final Object value)
    {
        return _entries.containsValue(value);
    }

    /** {@inheritDoc}
     */
    @Override
    public Set<Entry<String, Serializable>> entrySet()
    {
        return _getEntries().entrySet();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object)
    {
        if (object == this) {
            return true;
        }

        if (!(object instanceof Dict)) {
            return false;
        }

        return _entries.equals(((Dict) object)._entries);
    }

    /** {@inheritDoc}
     */
    @Override
    public void freeze()
    {
        if (_frozen == null) {
            _frozen = Collections.unmodifiableMap(_entries);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable get(final Object key)
    {
        return _entries.get(key);
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return _entries.hashCode();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        return _entries.isEmpty();
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
    public Set<String> keySet()
    {
        return _getEntries().keySet();
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable put(final String key, final Serializable value)
    {
        return _getEntries().put(key, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void putAll(final Map<? extends String, ? extends Serializable> map)
    {
        _getEntries().putAll(map);
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable remove(final Object key)
    {
        return _getEntries().remove(key);
    }

    /** {@inheritDoc}
     */
    @Override
    public int size()
    {
        return _entries.size();
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return _entries.toString();
    }

    /** {@inheritDoc}
     */
    @Override
    public Collection<Serializable> values()
    {
        return _getEntries().values();
    }

    private Map<String, Serializable> _getEntries()
    {
        final Map<String, Serializable> frozen = _frozen;

        return (frozen != null)? frozen: _entries;
    }

    private Object readResolve()
    {
        freeze();

        return this;
    }

    private static final long serialVersionUID = 1L;

    private LinkedHashMap<String, Serializable> _entries;
    private transient Map<String, Serializable> _frozen;
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
