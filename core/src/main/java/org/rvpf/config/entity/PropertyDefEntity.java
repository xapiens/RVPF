/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PropertyDefEntity.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.config.entity;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.UUID;
import org.rvpf.document.loader.ConfigElementLoader;

/**
 * PropertyDef entity.
 */
public final class PropertyDefEntity
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
    PropertyDefEntity(
            final Optional<String> name,
            final Optional<UUID> uuid,
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
    public PropertyDefEntity copy()
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
        return ConfigElementLoader.PROPERTY_ELEMENT;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /** PropertyDef element name. */
    public static final String ELEMENT_NAME = "PropertyDef";

    /** PropertyDef entity prefix. */
    public static final String ENTITY_PREFIX = "Y";

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
        public PropertyDefEntity build()
        {
            return new PropertyDefEntity(
                getName(),
                getUUID(),
                isHidden(),
                isMultiple());
        }

        /**
         * Copies the values from an other PropertyDefEntity.
         *
         * @param propertyDef The other PropertyDefEntity.
         *
         * @return This.
         */
        public final Builder copyFrom(
                @Nonnull final PropertyDefEntity propertyDef)
        {
            super.copyFrom(propertyDef);

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
