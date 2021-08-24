/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AttributeDefEntity.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.metadata.entity;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.UUID;
import org.rvpf.config.entity.ValidatorDefEntity;
import org.rvpf.document.loader.MetadataElementLoader;

/**
 * Attribute definition.
 */
public final class AttributeDefEntity
    extends ValidatorDefEntity
{
    /**
     * Constructs an instance.
     *
     * @param name The optional entity name.
     * @param uuid The optional entity UUID.
     * @param hidden True if hidden.
     * @param multiple True if multiple.
     */
    AttributeDefEntity(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid,
            final boolean hidden,
            final boolean multiple)
    {
        super(name, uuid, hidden, multiple);
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
    public AttributeDefEntity copy()
    {
        return newBuilder().copyFrom(this).build();
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
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getTarget()
    {
        return MetadataElementLoader.ATTRIBUTE_ELEMENT;
    }

    /** AttributeDef element name */
    public static final String ELEMENT_NAME = "AttributeDef";

    /** AttributeDef entity prefix. */
    public static final String ENTITY_PREFIX = "B";

    /**
     * PropertyDef builder.
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
        public AttributeDefEntity build()
        {
            return new AttributeDefEntity(
                getName(),
                getUUID(),
                isHidden(),
                isMultiple());
        }

        /**
         * Copies the values from an other.
         *
         * @param attributeDef The other.
         *
         * @return This.
         */
        @Nonnull
        public final Builder copyFrom(
                @Nonnull final AttributeDefEntity attributeDef)
        {
            super.copyFrom(attributeDef);

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
