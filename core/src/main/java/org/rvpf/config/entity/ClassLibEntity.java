/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClassLibEntity.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.config.entity;

import java.net.URI;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.config.TopologicalErrorException;

/**
 * Class library entity.
 */
public class ClassLibEntity
    extends AbstractEntity
{
    /**
     * Constructs an instance.
     *
     * @param name The optional instance name.
     * @param uuid The optional instance UUID.
     * @param classLibs The referred entities.
     * @param locations The library locations.
     * @param cached True if cached (optional).
     * @param added True if added.
     * @param level The current level.
     */
    ClassLibEntity(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid,
            @Nonnull final List<ClassLibEntity> classLibs,
            @Nonnull final List<URI> locations,
            @Nonnull final Optional<Boolean> cached,
            final boolean added,
            final int level)
    {
        super(name, uuid);

        _classLibs = Require.notNull(classLibs);
        _locations = Require.notNull(locations);
        _cached = Require.notNull(cached);
        _added = added;
        _level = level;
    }

    /**
     * Returns a new builder.
     *
     * @return The new builder.
     */
    @Nonnull
    @CheckReturnValue
    public static Builder newBuilder()
    {
        return new Builder();
    }

    /**
     * Adjust the level of this class library dependencies.
     *
     * @throws TopologicalErrorException When a recursive reference is found.
     */
    public final void adjustLevel()
        throws TopologicalErrorException
    {
        adjustLevel(0);
    }

    /** {@inheritDoc}
     */
    @Override
    public final int compareTo(final Entity other)
    {
        int comparison;

        comparison = _level - ((ClassLibEntity) other)._level;

        if (comparison == 0) {
            final Optional<String> name = getName();

            if (name.isPresent()) {
                final Optional<String> otherName = other.getName();

                comparison = (otherName
                    .isPresent())? name.get().compareTo(otherName.get()): -1;
            } else if (other.getName().isPresent()) {
                comparison = 1;
            }
        }

        return comparison;
    }

    /** {@inheritDoc}
     */
    @Override
    public ClassLibEntity copy()
    {
        return newBuilder().copyFrom(this).build();
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if (super.equals(other)) {
            final ClassLibEntity otherEntity = (ClassLibEntity) other;

            if (_cached.equals(otherEntity._cached)) {
                return getLocations().equals(otherEntity.getLocations());
            }
        }

        return false;
    }

    /**
     * Gets the cached indicator.
     *
     * @return The optional cached indicator.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<Boolean> getCached()
    {
        return _cached;
    }

    /**
     * Gets the included class libraries.
     *
     * @return The included class libraries.
     */
    @Nonnull
    @CheckReturnValue
    public final List<ClassLibEntity> getClassLibs()
    {
        return _classLibs;
    }

    /** {@inheritDoc}
     */
    @Override
    public final String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Gets this class library level.
     *
     * <p>The class library level help control the declaration order to avoid
     * forward references.</p>
     *
     * @return An int where 0 means that there is no 'classLib' reference to
     *         this class library. Lower (negative) values represent a higher
     *         stack of references.
     */
    @CheckReturnValue
    public final int getLevel()
    {
        return _level;
    }

    /**
     * Gets a list of the locations from which this class library may be
     * fetched.
     *
     * @return The list as URIs.
     */
    @Nonnull
    @CheckReturnValue
    public final List<URI> getLocations()
    {
        return _locations;
    }

    /** {@inheritDoc}
     */
    @Override
    public final String getPrefix()
    {
        return ENTITY_PREFIX;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getReferenceName()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public final int hashCode()
    {
        return super.hashCode();
    }

    /**
     * Asks if this class library has been added to the CLASSPATH.
     *
     * @return True if added.
     */
    @CheckReturnValue
    public final boolean isAdded()
    {
        return _added;
    }

    /**
     * Asks if this class library is cached.
     *
     * @param defaultValue The default value.
     *
     * @return True if cached.
     */
    @CheckReturnValue
    public final boolean isCached(final boolean defaultValue)
    {
        return _cached.isPresent()? _cached.get().booleanValue(): defaultValue;
    }

    /**
     * Asks if this class library is defined.
     *
     * @return True if defined.
     */
    @CheckReturnValue
    public boolean isDefined()
    {
        return true;
    }

    /**
     * Sets the added indicator.
     *
     * @param added The added indicator.
     */
    public final void setAdded(final boolean added)
    {
        _added = added;
    }

    private void adjustLevel(final int level)
        throws TopologicalErrorException
    {
        if (_busy) {
            throw new TopologicalErrorException(this);
        }

        if (level < _level) {
            _level = level;
            _busy = true;

            for (final ClassLibEntity classLib: _classLibs) {
                classLib.adjustLevel(_level - 1);
            }

            _busy = false;
        }
    }

    /** ClassLib element name. */
    public static final String ELEMENT_NAME = "ClassLib";

    /** ClassLib entity prefix. */
    public static final String ENTITY_PREFIX = "L";

    private boolean _added;
    private boolean _busy;
    private final Optional<Boolean> _cached;
    private final List<ClassLibEntity> _classLibs;
    private int _level = 1;
    private final List<URI> _locations;

    /**
     * Builder.
     */
    public static class Builder
        extends AbstractEntity.Builder
    {
        /**
         * Constructs an instance.
         */
        Builder() {}

        /**
         * Adds a class library.
         *
         * @param classLib The class library.
         *
         * @return This.
         */
        public final Builder addClassLib(@Nonnull final ClassLibEntity classLib)
        {
            _classLibs.add(classLib);

            return this;
        }

        /**
         * Adds a location from which this class library may be fetched.
         *
         * @param location The location.
         *
         * @return This.
         */
        public final Builder addLocation(@Nonnull final URI location)
        {
            _locations.add(location);

            return this;
        }

        /** {@inheritDoc}
         */
        @Override
        public ClassLibEntity build()
        {
            return new ClassLibEntity(
                getName(),
                getUUID(),
                _classLibs,
                _locations,
                _cached,
                _added,
                _level);
        }

        /**
         * Copies the values from an other ClassLibEntity.
         *
         * @param classLib The other ClassLibEntity.
         *
         * @return This.
         */
        public final Builder copyFrom(@Nonnull final ClassLibEntity classLib)
        {
            super.copyFrom(classLib);

            _classLibs.clear();
            _classLibs.addAll(classLib.getClassLibs());

            _locations.clear();
            _locations.addAll(classLib.getLocations());

            _cached = classLib.getCached();
            _added = classLib.isAdded();
            _level = classLib.getLevel();

            return this;
        }

        /**
         * Asks if the class library is cached.
         *
         * @param defaultValue The default value.
         *
         * @return True if cached.
         */
        @CheckReturnValue
        public final boolean isCached(final boolean defaultValue)
        {
            return _cached
                .isPresent()? _cached.get().booleanValue(): defaultValue;
        }

        /**
         * Sets the added indicator.
         *
         * @param added The added indicator.
         *
         * @return This.
         */
        public final Builder setAdded(final boolean added)
        {
            _added = added;

            return this;
        }

        /**
         * Sets the cached indicator.
         *
         * @param cached The cached indicator (tri-state).
         *
         * @return This.
         */
        public final Builder setCached(@Nonnull final Optional<Boolean> cached)
        {
            _cached = cached;

            return this;
        }

        private boolean _added;
        private Optional<Boolean> _cached = Optional.empty();
        private final List<ClassLibEntity> _classLibs = new LinkedList<>();
        private int _level = 1;
        private final List<URI> _locations = new LinkedList<>();
    }


    /**
     * Undefined.
     */
    public static final class Undefined
        extends ClassLibEntity
    {
        /**
         * Constructs an instance.
         *
         * @param name The entity name.
         */
        Undefined(@Nonnull final String name)
        {
            super(
                Optional.of(name),
                Optional.empty(),
                Collections.emptyList(),
                Collections.emptyList(),
                Optional.empty(),
                false,
                0);
        }

        /**
         * Returns a new builder.
         *
         * @return The new builder.
         */
        public static Builder newBuilder()
        {
            return new Builder();
        }

        /** {@inheritDoc}
         */
        @Override
        public Undefined copy()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isDefined()
        {
            return false;
        }

        /**
         * Builder.
         */
        public static final class Builder
            extends ClassLibEntity.Builder
        {
            /**
             * Constructs an instance.
             */
            Builder() {}

            /** {@inheritDoc}
             */
            @Override
            public Undefined build()
            {
                return new Undefined(Require.notNull(_key));
            }

            /**
             * Sets the key.
             *
             * @param key The key.
             *
             * @return This.
             */
            @Nonnull
            public Builder setKey(@Nonnull final String key)
            {
                _key = key;

                return this;
            }

            private String _key;
        }
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
