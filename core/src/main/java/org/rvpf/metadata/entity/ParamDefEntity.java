/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ParamDefEntity.java 4076 2019-06-11 17:09:30Z SFB $
 */

package org.rvpf.metadata.entity;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.config.entity.ValidatorDefEntity;
import org.rvpf.document.loader.MetadataElementLoader;

/**
 * ParamDef Entity.
 */
public final class ParamDefEntity
    extends ValidatorDefEntity
{
    /**
     * Constructs an instance.
     *
     * @param name The optional entity name.
     * @param uuid The optional entity UUID.
     * @param hidden True if hidden.
     * @param multiple True if multiple.
     * @param holder The holder of this ParamDef.
     */
    ParamDefEntity(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid,
            final boolean hidden,
            final boolean multiple,
            @Nonnull final String holder)
    {
        super(name, uuid, hidden, multiple);

        _holder = Require.notNull(holder);
    }

    /**
     * Creates a key to identify a ParamDef.
     *
     * @param holder The ParamDef holder.
     * @param name The ParamDef name.
     *
     * @return The created key.
     */
    @Nonnull
    @CheckReturnValue
    public static String createKey(
            @Nonnull final String holder,
            @Nonnull final String name)
    {
        return holder + "." + name;
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

    /** {@inheritDoc}
     */
    @Override
    public int compareTo(final Entity other)
    {
        int comparison = super.compareTo(other);

        if (comparison == 0) {
            comparison = getHolder()
                .compareTo(((ParamDefEntity) other).getHolder());
        }

        return comparison;
    }

    /** {@inheritDoc}
     */
    @Override
    public ParamDefEntity copy()
    {
        return newBuilder().copyFrom(this).build();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if (super.equals(other)) {
            return getHolder().equals(((ParamDefEntity) other).getHolder());
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Gets the holder of this ParamDef.
     *
     * @return The holder.
     */
    @Nonnull
    @CheckReturnValue
    public String getHolder()
    {
        return _holder;
    }

    /**
     * Gets the key identifying this ParamDef.
     *
     * @return The key.
     */
    @Nonnull
    @CheckReturnValue
    public String getKey()
    {
        return createKey(_holder, getName().get());
    }

    /** {@inheritDoc}
     */
    @Override
    public String getPrefix()
    {
        return ENTITY_PREFIX + _holder;
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
    public String getTarget()
    {
        return MetadataElementLoader.PARAM_ELEMENT;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /** ParamDef element name. */
    public static final String ELEMENT_NAME = "ParamDef";

    /** ParamDef entity prefix. */
    public static final String ENTITY_PREFIX = "M";

    private final String _holder;

    /**
     * Property def builder.
     */
    public static final class Builder
        extends ValidatorDefEntity.Builder
    {
        /**
         * Constructs an instance.
         */
        Builder() {}

        /** {@inheritDoc}
         */
        @Override
        public ParamDefEntity build()
        {
            return new ParamDefEntity(
                getName(),
                getUUID(),
                isHidden(),
                isMultiple(),
                _holder);
        }

        /**
         * Copies the values from an other.
         *
         * @param propertyDef The other.
         *
         * @return This.
         */
        @Nonnull
        public final Builder copyFrom(@Nonnull final ParamDefEntity propertyDef)
        {
            super.copyFrom(propertyDef);

            return this;
        }

        /**
         * Sets the holder of this ParamDef.
         *
         * <p>The ParamDef holder is used to avoid name collisions with unrelated
         * ParamDef instances. It will be included in the class prefix to avoid such
         * collisions.</p>
         *
         * @param holder The holder.
         *
         * @return This.
         */
        @Nonnull
        public Builder setHolder(@Nonnull final String holder)
        {
            _holder = Require.notNull(holder);

            return this;
        }

        private String _holder;
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
