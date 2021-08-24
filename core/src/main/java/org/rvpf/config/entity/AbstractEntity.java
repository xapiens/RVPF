/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AbstractEntity.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.config.entity;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import org.rvpf.base.Attributes;
import org.rvpf.base.Entity;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;

/**
 * Abstract entity.
 */
public abstract class AbstractEntity
    implements Entity
{
    /**
     * Constructs an instance.
     */
    protected AbstractEntity() {}

    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    protected AbstractEntity(@Nonnull final AbstractEntity other)
    {
        setName(other.getName());
        setUUID(other.getUUID());
    }

    /**
     * Constructs an instance.
     *
     * @param name The optional instance name.
     * @param uuid The optional instance UUID.
     */
    protected AbstractEntity(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid)
    {
        _name = Require.notNull(name);
        _uuid = Require.notNull(uuid);
    }

    /**
     * Implements Comparable.
     *
     * @param other An other Entity.
     *
     * @return A negative integer, zero, or a positive integer.
     */
    @Override
    public int compareTo(final Entity other)
    {
        int comparison = 0;

        if (getClass() != other.getClass()) {
            throw new ClassCastException();
        }

        if (_name.isPresent()) {
            final Optional<String> otherName = other.getName();

            comparison = (otherName
                .isPresent())? _name.get().compareTo(otherName.get()): -1;
        } else if (other.getName().isPresent()) {
            comparison = 1;
        }

        if (comparison == 0) {
            if (_uuid.isPresent()) {
                final Optional<UUID> otherUUID = other.getUUID();

                comparison = (otherUUID
                    .isPresent())? _uuid.get().compareTo(otherUUID.get()): -1;
            } else if (other.getUUID().isPresent()) {
                comparison = 1;
            }
        }

        return comparison;
    }

    /** {@inheritDoc}
     *
     * <p>For two {@link Entity} to be equal, they must at least be instances of
     * the same class, have the same {@link UUID} and name.</p>
     */
    @Override
    public boolean equals(final Object other)
    {
        if ((other == null) || (getClass() != other.getClass())) {
            return false;
        }

        final AbstractEntity otherEntity = (AbstractEntity) other;

        return Objects.equals(_uuid, otherEntity._uuid)
               && Objects.equals(_name, otherEntity._name);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<Attributes> getAttributes(final String usage)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<String> getName()
    {
        return _name;
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<String> getNameInUpperCase()
    {
        return Optional
            .ofNullable(
                (_name.isPresent())? _name
                    .get()
                    .toUpperCase(Locale.ROOT): null);
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<UUID> getUUID()
    {
        return _uuid;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return _name.hashCode() ^ _uuid.hashCode();
    }

    /**
     * Sets the name of this Entity.
     *
     * @param name The optional name.
     */
    public final void setName(@Nonnull final Optional<String> name)
    {
        _name = name
            .isPresent()? Optional.of(name.get().trim()): Optional.empty();
    }

    /**
     * Sets the UUID of this Entity.
     *
     * @param uuid The optional UUID.
     */
    public final void setUUID(@Nonnull final Optional<UUID> uuid)
    {
        Require.failure(_uuid.isPresent(), "UUID already set");

        _uuid = uuid;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        if (_name.isPresent()) {
            return _name.get();
        }

        if (_uuid.isPresent()) {
            return _uuid.get().toString();
        }

        return super.toString();
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        return Logger.getInstance(getClass());
    }

    private Optional<String> _name = Optional.empty();
    private Optional<UUID> _uuid = Optional.empty();

    /**
     * Builder.
     */
    public abstract static class Builder
    {
        /**
         * Constructs an instance.
         */
        protected Builder() {}

        /**
         * Builds an abstract entity.
         *
         * @return The abstract entity.
         */
        @Nonnull
        @CheckReturnValue
        public abstract AbstractEntity build();

        /**
         * Sets the name.
         *
         * @param name The optional name.
         *
         * @return This.
         */
        @Nonnull
        public Builder setName(final Optional<String> name)
        {
            _name = name.orElse(null);

            return this;
        }

        /**
         * Sets the name.
         *
         * @param name The name.
         *
         * @return This.
         */
        @Nonnull
        public Builder setName(final String name)
        {
            _name = name;

            return this;
        }

        /**
         * Sets the UUID.
         *
         * @param uuid The UUID.
         *
         * @return This.
         */
        @Nonnull
        public Builder setUUID(final UUID uuid)
        {
            _uuid = uuid;

            return this;
        }

        /**
         * Copies the values from an entity.
         *
         * @param entity The entity.
         *
         * @return This.
         */
        @Nonnull
        @OverridingMethodsMustInvokeSuper
        protected Builder copyFrom(@Nonnull final AbstractEntity entity)
        {
            _name = entity.getName().orElse(null);
            _uuid = entity.getUUID().orElse(null);

            return this;
        }

        /**
         * Gets the name.
         *
         * @return The optional name.
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<String> getName()
        {
            return Optional.ofNullable(_name);
        }

        /**
         * Gets the logger.
         *
         * @return The logger.
         */
        @Nonnull
        @CheckReturnValue
        protected final Logger getThisLogger()
        {
            return Logger.getInstance(getClass());
        }

        /**
         * Gets the UUID.
         *
         * @return The optional UUID.
         */
        protected Optional<UUID> getUUID()
        {
            return Optional.ofNullable(_uuid);
        }

        private String _name;
        private UUID _uuid;
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
