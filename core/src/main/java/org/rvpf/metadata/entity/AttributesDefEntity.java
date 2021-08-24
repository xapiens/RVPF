/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AttributesDefEntity.java 4076 2019-06-11 17:09:30Z SFB $
 */

package org.rvpf.metadata.entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.config.entity.ValidatorDefEntity;
import org.rvpf.document.loader.MetadataElementLoader;
import org.rvpf.service.ServiceMessages;

/**
 * Attributes definition entity.
 */
public final class AttributesDefEntity
    extends ValidatorDefEntity
{
    /**
     * Constructs an instance.
     *
     * @param name The optional entity name.
     * @param uuid The optional entity UUID.
     * @param hidden True if hidden.
     * @param multiple True if multiple.
     * @param usage The attributes usage.
     * @param attributeDefs The attribute definitions.
     */
    AttributesDefEntity(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid,
            final boolean hidden,
            final boolean multiple,
            @Nonnull final String usage,
            @Nonnull final Map<String, AttributeDefEntity> attributeDefs)
    {
        super(name, uuid, hidden, multiple);

        _usage = Require.notNull(usage);
        _attributeDefs = Require.notNull(attributeDefs);
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
    public AttributesDefEntity copy()
    {
        return newBuilder().copyFrom(this).build();
    }

    /**
     * Gets an attribute definition.
     *
     * @param name The attribute name.
     *
     * @return The attribute definition.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<AttributeDefEntity> getAttributeDef(
            @Nonnull final String name)
    {
        return Optional
            .ofNullable(_attributeDefs.get(name.toUpperCase(Locale.ROOT)));
    }

    /**
     * Gets the attribute definitions.
     *
     * @return The attribute definitions.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<AttributeDefEntity> getAttributeDefs()
    {
        return _attributeDefs.values();
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
        return MetadataElementLoader.ATTRIBUTES_ELEMENT;
    }

    /**
     * Gets the usage.
     *
     * @return The usage.
     */
    @Nonnull
    @CheckReturnValue
    public String getUsage()
    {
        return _usage;
    }

    /**
     * Gets the attribute definitions.
     *
     * @return The attribute definitions.
     */
    @Nonnull
    @CheckReturnValue
    Map<String, AttributeDefEntity> _getAttributeDefs()
    {
        return _attributeDefs;
    }

    /** AttributesDef element name. */
    public static final String ELEMENT_NAME = "AttributesDef";

    /** AttributesDef entity prefix. */
    public static final String ENTITY_PREFIX = "A";

    private final Map<String, AttributeDefEntity> _attributeDefs;
    private final String _usage;

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

        /**
         * Adds an attribute definition.
         *
         * @param attributeDef The attribute definition.
         *
         * @return This.
         */
        @Nonnull
        public Builder addAttributeDef(
                @Nonnull final AttributeDefEntity attributeDef)
        {
            final String attributeName = attributeDef.getName().get();

            if (_attributeDefs == null) {
                _attributeDefs = new HashMap<>();
            }

            if (_attributeDefs
                .put(
                    attributeName.toUpperCase(Locale.ROOT),
                    attributeDef) != null) {
                getThisLogger()
                    .warn(
                        ServiceMessages.ATTRIBUTE_DEF_MULTIPLE,
                        attributeName);
            }

            return this;
        }

        /** {@inheritDoc}
         */
        @Override
        public AttributesDefEntity build()
        {
            final AttributesDefEntity attributesDef = new AttributesDefEntity(
                getName(),
                getUUID(),
                isHidden(),
                isMultiple(),
                Require.notNull(_usage),
                (_attributeDefs != null)? _attributeDefs: new HashMap<>());

            _attributeDefs = null;

            return attributesDef;
        }

        /**
         * Copies the values from an other AttributesDefEntity.
         *
         * @param attributesDef The other AttributesDefEntity.
         *
         * @return This.
         */
        @Nonnull
        public final Builder copyFrom(
                @Nonnull final AttributesDefEntity attributesDef)
        {
            super.copyFrom(attributesDef);

            _usage = attributesDef.getUsage();

            _attributeDefs = new HashMap<>(attributesDef._getAttributeDefs());

            return this;
        }

        /**
         * Sets the usage.
         *
         * @param usage The usage (required).
         *
         * @return This.
         */
        @Nonnull
        public Builder setUsage(@Nonnull final String usage)
        {
            _usage = Require.notNull(usage);

            return this;
        }

        private Map<String, AttributeDefEntity> _attributeDefs;
        private String _usage;
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
