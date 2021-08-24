/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PropertiesDefEntity.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.config.entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.document.loader.ConfigElementLoader;

/**
 * PropertiesDef entity.
 */
public final class PropertiesDefEntity
    extends ValidatorDefEntity
{
    /**
     * Constructs an instance.
     */
    public PropertiesDefEntity() {}

    private PropertiesDefEntity(@Nonnull final PropertiesDefEntity other)
    {
        super(other);

        _propertyDefs.putAll(other._propertyDefs);
        _propertiesDefs.putAll(other._propertiesDefs);
        _validated = other._validated;
    }

    /**
     * Adds a child PropertiesDef.
     *
     * @param propertiesDef The PropertiesDef.
     *
     * @return False when duplicate.
     */
    public boolean addPropertiesDef(
            @Nonnull final PropertiesDefEntity propertiesDef)
    {
        return _propertiesDefs
            .put(propertiesDef.getName().get(), propertiesDef) == null;
    }

    /**
     * Adds a child PropertyDef.
     *
     * @param propertyDef The PropertyDef.
     *
     * @return False when duplicate.
     */
    public boolean addPropertyDef(@Nonnull final PropertyDefEntity propertyDef)
    {
        return _propertyDefs
            .put(propertyDef.getName().get(), propertyDef) == null;
    }

    /** {@inheritDoc}
     */
    @Override
    public PropertiesDefEntity copy()
    {
        return new PropertiesDefEntity(this);
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

    /**
     * Gets a child PropertiesDef.
     *
     * @param name The name of the PropertiesDef.
     *
     * @return The optional PropertiesDef.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<PropertiesDefEntity> getPropertiesDef(
            @Nonnull final String name)
    {
        return Optional.ofNullable(_propertiesDefs.get(name));
    }

    /**
     * Gets the collection of child PropertiesDef.
     *
     * @return The collection of child PropertiesDef.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<PropertiesDefEntity> getPropertiesDefs()
    {
        return _propertiesDefs.values();
    }

    /**
     * Gets a child PropertyDef.
     *
     * @param name The name of the PropertyDef.
     *
     * @return The optional PropertyDef.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<PropertyDefEntity> getPropertyDef(
            @Nonnull final String name)
    {
        return Optional.ofNullable(_propertyDefs.get(name));
    }

    /**
     * Gets the collection of child PropertyDef.
     *
     * @return The collection of child PropertyDef.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<PropertyDefEntity> getPropertyDefs()
    {
        return _propertyDefs.values();
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
        return ConfigElementLoader.PROPERTIES_ELEMENT;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /**
     * Asks if the children are validated.
     *
     * @return True if the children are validated.
     */
    @CheckReturnValue
    public boolean isValidated()
    {
        return _validated;
    }

    /**
     * Sets the validation indicator.
     *
     * @param validated The validation indicator.
     */
    public void setValidated(final boolean validated)
    {
        _validated = validated;
    }

    /** PropertiesDef element name. */
    public static final String ELEMENT_NAME = "PropertiesDef";

    /** PropertiesDef entity prefix. */
    public static final String ENTITY_PREFIX = "R";

    private final Map<String, PropertiesDefEntity> _propertiesDefs =
        new HashMap<>();
    private final Map<String, PropertyDefEntity> _propertyDefs =
        new HashMap<>();
    private boolean _validated = true;
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
