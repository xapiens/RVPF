/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ParamsEntity.java 4076 2019-06-11 17:09:30Z SFB $
 */

package org.rvpf.metadata.entity;

import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Params;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.metadata.Text;

/**
 * Params Entity.
 */
public abstract class ParamsEntity
    extends MetadataEntity
{
    /**
     * Constructs an instance.
     */
    protected ParamsEntity()
    {
        _params = Optional.empty();
    }

    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    protected ParamsEntity(@Nonnull final ParamsEntity other)
    {
        super(other);

        if (other._params.isPresent()) {
            _params = Optional.of(other._params.get().copy());
        } else {
            _params = Optional.empty();
        }
    }

    /**
     * Constructs an instance.
     *
     * @param name The optional entity name.
     * @param uuid The optional entity UUID.
     * @param attributes The optional attributes.
     * @param texts The optional texts.
     * @param params The params.
     */
    protected ParamsEntity(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid,
            @Nonnull final Optional<KeyedGroups> attributes,
            @Nonnull final Optional<Map<String, Text>> texts,
            @Nonnull final Optional<Params> params)
    {
        super(name, uuid, attributes, texts);

        _params = Require.notNull(params);

    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (super.equals(other)) {
            return getParams().equals(((ParamsEntity) other).getParams());
        }

        return false;
    }

    /**
     * Gets the Params of this Entity.
     *
     * @return The Params.
     */
    @Nonnull
    @CheckReturnValue
    public final Params getParams()
    {
        return _params.isPresent()? _params.get(): Params.EMPTY_PARAMS;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /**
     * Sets the params of this entity.
     *
     * @param params The optional params.
     */
    public final void setParams(@Nonnull final Optional<Params> params)
    {
        _params = Require.notNull(params);
    }

    /**
     * Gets the params of this entity.
     *
     * @return The optional params.
     */
    @Nonnull
    @CheckReturnValue
    final Optional<Params> _getParams()
    {
        return _params;
    }

    private Optional<Params> _params;

    /**
     * Builder.
     */
    public abstract static class Builder
        extends MetadataEntity.Builder
    {
        /**
         * Constructs an instance.
         */
        protected Builder() {}

        /**
         * Gets the params.
         *
         * @return The optional params.
         */
        public final Optional<Params> getParams()
        {
            return _params;
        }

        /**
         * Sets the Params of this Entity.
         *
         * @param params The optional Params.
         *
         * @return This.
         */
        @Nonnull
        public final Builder setParams(@Nonnull final Optional<Params> params)
        {
            _params = Require.notNull(params);

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
        protected Builder copyFrom(@Nonnull final ParamsEntity entity)
        {
            super.copyFrom(entity);

            final Optional<Params> params = entity._getParams();

            _params = params
                .isPresent()? Optional
                    .of(params.get().copy()): Optional.empty();

            return this;
        }

        private Optional<Params> _params = Optional.empty();
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
