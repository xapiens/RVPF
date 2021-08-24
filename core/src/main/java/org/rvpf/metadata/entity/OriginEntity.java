/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: OriginEntity.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.metadata.entity;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Origin;
import org.rvpf.base.Params;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.IdentityHashSet;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.metadata.Text;

/**
 * Origin entity.
 */
public final class OriginEntity
    extends ParamsEntity
    implements Origin
{
    /**
     * Constructs an instance.
     *
     * @param name The optional entity name.
     * @param uuid The optional entity UUID.
     * @param attributes The optional attributes.
     * @param texts The optional texts.
     * @param params The optional params.
     * @param pointEntities The point entities.
     */
    protected OriginEntity(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid,
            @Nonnull final Optional<KeyedGroups> attributes,
            @Nonnull final Optional<Map<String, Text>> texts,
            @Nonnull final Optional<Params> params,
            @Nonnull final Set<PointEntity> pointEntities)
    {
        super(name, uuid, attributes, texts, params);

        _pointEntities = Require.notNull(pointEntities);
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
     * Adds a a point entity.
     *
     * @param pointEntity The point entity.
     *
     * @return True if added.
     */
    @CheckReturnValue
    public boolean addPointEntity(@Nonnull final PointEntity pointEntity)
    {
        return _pointEntities.add(pointEntity);
    }

    /** {@inheritDoc}
     */
    @Override
    public OriginEntity copy()
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

        return super.equals(other);
    }

    /** {@inheritDoc}
     */
    @Override
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Gets the point entities.
     *
     * @return The point entities.
     */
    @Nonnull
    @CheckReturnValue
    public Set<PointEntity> getPointEntities()
    {
        return _pointEntities;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getPrefix()
    {
        return ENTITY_PREFIX;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getReferenceName()
    {
        return ENTITY_REFERENCE_NAME;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /** Origin element name. */
    public static final String ELEMENT_NAME = "Origin";

    /** Origin entity prefix. */
    public static final String ENTITY_PREFIX = "O";

    /** Origin entity reference name. */
    public static final String ENTITY_REFERENCE_NAME = "origin";

    private final Set<PointEntity> _pointEntities;

    /**
     * Builder.
     */
    public static final class Builder
        extends ParamsEntity.Builder
    {
        /**
         * Constructs an instance.
         */
        Builder() {}

        /** {@inheritDoc}
         */
        @Override
        public OriginEntity build()
        {
            final OriginEntity originEntity = new OriginEntity(
                getName(),
                getUUID(),
                getAttributes(),
                getTexts(),
                getParams(),
                (_pointEntities != null)
                ? _pointEntities: new IdentityHashSet<>());

            _pointEntities = null;

            return originEntity;
        }

        /**
         * Copies the values from an other origin entity.
         *
         * @param originEntity The other origin entity.
         *
         * @return This.
         */
        @Nonnull
        protected Builder copyFrom(@Nonnull final OriginEntity originEntity)
        {
            super.copyFrom(originEntity);

            _pointEntities = new IdentityHashSet<>(
                originEntity.getPointEntities());

            return this;
        }

        private Set<PointEntity> _pointEntities;
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
