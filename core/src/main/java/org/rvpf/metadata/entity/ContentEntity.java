/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ContentEntity.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.metadata.entity;

import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Content;
import org.rvpf.base.Params;
import org.rvpf.base.UUID;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.entity.ClassDefEntity;
import org.rvpf.metadata.Proxied;
import org.rvpf.metadata.Text;

/**
 * Content entity.
 */
public final class ContentEntity
    extends ProxyEntity
{
    /**
     * Constructs an instance.
     *
     * @param name The optional entity name.
     * @param uuid The optional entity UUID.
     * @param attributes The optional attributes.
     * @param texts The optional texts.
     * @param params The optional params.
     * @param classDef The optional class definition.
     * @param instance The optional proxied instance.
     */
    protected ContentEntity(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid,
            @Nonnull final Optional<KeyedGroups> attributes,
            @Nonnull final Optional<Map<String, Text>> texts,
            @Nonnull final Optional<Params> params,
            @Nonnull final Optional<ClassDefEntity> classDef,
            @Nonnull final Optional<Proxied> instance)
    {
        super(name, uuid, attributes, texts, params, classDef, instance);
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
    public ContentEntity copy()
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

    /**
     * Gets the Content instance.
     *
     * @return The Content instance.
     */
    @Nonnull
    @CheckReturnValue
    public Content getContent()
    {
        return (Content) getInstance().get();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getElementName()
    {
        return ELEMENT_NAME;
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

    /** Content element name. */
    public static final String ELEMENT_NAME = "Content";

    /** Content entity prefix. */
    public static final String ENTITY_PREFIX = "N";

    /** Content entity reference name. */
    public static final String ENTITY_REFERENCE_NAME = "content";

    /**
     * Builder.
     */
    public static final class Builder
        extends ProxyEntity.Builder
    {
        /**
         * Constructs an instance.
         */
        Builder() {}

        /** {@inheritDoc}
         */
        @Override
        public ContentEntity build()
        {
            return new ContentEntity(
                getName(),
                getUUID(),
                getAttributes(),
                getTexts(),
                getParams(),
                getClassDef(),
                getInstance());
        }

        /**
         * Copies the values from an entity.
         *
         * @param entity The entity.
         *
         * @return This.
         */
        @Nonnull
        protected Builder copyFrom(@Nonnull final ContentEntity entity)
        {
            super.copyFrom(entity);

            return this;
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
