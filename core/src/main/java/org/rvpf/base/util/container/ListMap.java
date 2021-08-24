/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ListMap.java 3933 2019-04-25 20:23:38Z SFB $
 */

package org.rvpf.base.util.container;

import java.io.Serializable;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.tool.Require;

/**
 * List map.
 *
 * <p>Extends a Map to allow multiple non null values for a single key.</p>
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public interface ListMap<K, V>
    extends Map<K, List<V>>
{
    /**
     * Adds a value to a key.
     *
     * @param key The key.
     * @param value The value.
     */
    void add(@Nonnull K key, @Nonnull V value);

    /**
     * Gets all values for a key.
     *
     * @param key The key.
     *
     * @return A list of all values for the key.
     */
    @Nonnull
    @CheckReturnValue
    List<V> getAll(@Nonnull final Object key);

    /**
     * Gets the first value for a key.
     *
     * @param key The key.
     *
     * @return The optional first value for the key.
     */
    @Nonnull
    @CheckReturnValue
    Optional<V> getFirst(@Nonnull K key);

    /**
     * Gets the last value for a key.
     *
     * @param key The key.
     *
     * @return The optional last value for the key.
     */
    @Nonnull
    @CheckReturnValue
    Optional<V> getLast(@Nonnull K key);

    /**
     * Puts an entry with an empty values list.
     *
     * @param key The key.
     */
    void put(@Nonnull K key);

    /**
     * Removes the first value for a key.
     *
     * <p>When the values list becomes empty, the entry is removed.</p>
     *
     * @param key The key.
     *
     * @return The optional removed value.
     */
    @Nonnull
    Optional<V> removeFirst(@Nonnull K key);

    /**
     * Removes the last value for a key.
     *
     * <p>When the values list becomes empty, the entry is removed.</p>
     *
     * @param key The key.
     *
     * @return The optional removed value.
     */
    @Nonnull
    Optional<V> removeLast(@Nonnull K key);

    /**
     * Abstract list map.
     *
     * @param <K> The key type.
     * @param <V> The value type.
     */
    @NotThreadSafe
    abstract class Abstract<K, V>
        extends AbstractMap<K, List<V>>
        implements ListMap<K, V>, Cloneable, Serializable
    {
        /**
         * Constructs an instance.
         *
         * @param map The map implementation.
         */
        protected Abstract(@Nonnull final Map<K, List<V>> map)
        {
            _map = Require.notNull(map);
        }

        /** {@inheritDoc}
         */
        @Override
        public void add(final K key, final V value)
        {
            List<V> values = _map.get(key);

            if (values == null) {
                values = new LinkedList<V>();
                _map.put(key, values);
            }

            values.add(value);
        }

        /** {@inheritDoc}
         */
        @Override
        public void clear()
        {
            super.clear();
        }

        /** {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public ListMap<K, V> clone()
        {
            final Abstract<K, V> clone;

            try {
                clone = (Abstract<K, V>) super.clone();
            } catch (final CloneNotSupportedException exception) {
                throw new InternalError(exception);
            }

            try {
                clone._map = (Map<K, List<V>>) clone._map
                    .getClass()
                    .getMethod("clone")
                    .invoke(clone._map);
            } catch (final RuntimeException exception) {
                throw exception;
            } catch (final Exception exception) {
                throw new RuntimeException(exception);
            }

            for (final Map.Entry<K, List<V>> entry: clone.entrySet()) {
                entry.setValue(new LinkedList<V>(entry.getValue()));
            }

            clone.mapReplaced(clone._map);

            return clone;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean containsKey(final Object key)
        {
            return _map.containsKey(key);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean containsValue(final Object value)
        {
            for (final List<V> list: _map.values()) {
                if (list.contains(value)) {
                    return true;
                }
            }

            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public Set<Entry<K, List<V>>> entrySet()
        {
            return _map.entrySet();
        }

        /** {@inheritDoc}
         */
        @Override
        public List<V> get(final Object key)
        {
            return getAll(key);
        }

        /** {@inheritDoc}
         */
        @Override
        public List<V> getAll(final Object key)
        {
            final List<V> all = _map.get(key);

            return (all != null)? all: Collections.emptyList();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<V> getFirst(final K key)
        {
            final List<V> values = _map.get(key);

            return (values != null)? Optional
                .of(values.get(0)): Optional.empty();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<V> getLast(final K key)
        {
            final List<V> values = _map.get(key);

            return (values != null)? Optional
                .of(values.get(values.size() - 1)): Optional.empty();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isEmpty()
        {
            return _map.isEmpty();
        }

        /** {@inheritDoc}
         */
        @Override
        public Set<K> keySet()
        {
            return super.keySet();
        }

        /** {@inheritDoc}
         */
        @Override
        public void put(final K key)
        {
            put(key, new LinkedList<V>());
        }

        /** {@inheritDoc}
         */
        @Override
        public List<V> put(final K key, final List<V> value)
        {
            return _map.put(key, value);
        }

        /** {@inheritDoc}
         */
        @Override
        public List<V> remove(final Object key)
        {
            return _map.remove(key);
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<V> removeFirst(final K key)
        {
            final List<V> values = _map.get(key);

            if (values == null) {
                return Optional.empty();
            }

            final V value = values.isEmpty()? null: values.remove(0);

            if (values.isEmpty()) {
                _map.remove(key);
            }

            return Optional.ofNullable(value);
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<V> removeLast(final K key)
        {
            final List<V> values = _map.get(key);

            if (values == null) {
                return Optional.empty();
            }

            final V value = values
                .isEmpty()? null: values.remove(values.size() - 1);

            if (values.isEmpty()) {
                _map.remove(key);
            }

            return Optional.ofNullable(value);
        }

        /** {@inheritDoc}
         */
        @Override
        public int size()
        {
            return _map.size();
        }

        /** {@inheritDoc}
         */
        @Override
        public Collection<List<V>> values()
        {
            return _map.values();
        }

        /**
         * Called when the Map is replaced.
         *
         * <p>This may be overridden by subclass as needed.</p>
         *
         * @param map The Map.
         */
        protected void mapReplaced(@Nonnull final Map<K, List<V>> map) {}

        private Object readResolve()
        {
            mapReplaced(_map);

            return this;
        }

        private static final long serialVersionUID = 1L;

        private Map<K, List<V>> _map;
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
