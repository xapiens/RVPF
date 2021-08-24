/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreEntity.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.metadata.entity;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Params;
import org.rvpf.base.UUID;
import org.rvpf.base.store.Store;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.IdentityHashSet;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.entity.ClassDefEntity;
import org.rvpf.metadata.Permissions;
import org.rvpf.metadata.Proxied;
import org.rvpf.metadata.Text;
import org.rvpf.store.client.RMIStore;

/**
 * Store entity.
 */
public final class StoreEntity
    extends ProxyEntity
{
    /**
     * Constructs an instance.
     *
     * @param name The optional entity name.
     * @param uuid The optional entity UUID.
     * @param attributes The optional attributes.
     * @param texts The optional texts.
     * @param params The params.
     * @param classDef The optional class definition.
     * @param instance The optional proxied instance.
     * @param permissions The permissions.
     * @param pointReferences The point references.
     */
    StoreEntity(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid,
            @Nonnull final Optional<KeyedGroups> attributes,
            @Nonnull final Optional<Map<String, Text>> texts,
            @Nonnull final Optional<Params> params,
            @Nonnull final Optional<ClassDefEntity> classDef,
            @Nonnull final Optional<Proxied> instance,
            @Nonnull final Optional<? extends Permissions> permissions,
            @Nonnull final Set<PointEntity> pointReferences)
    {
        super(name, uuid, attributes, texts, params, classDef, instance);

        _permissions = Require.notNull(permissions);
        _pointReferences = Require.notNull(pointReferences);
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
     * Adds a reference to a point entity.
     *
     * @param pointEntity The point entity.
     *
     * @return True if added.
     */
    @CheckReturnValue
    public boolean addPointReference(@Nonnull final PointEntity pointEntity)
    {
        return _pointReferences.add(pointEntity);
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreEntity copy()
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
            final StoreEntity otherStore = (StoreEntity) other;

            return Objects.equals(_permissions, otherStore._permissions);
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
     * Gets the permissions.
     *
     * @return The optional permissions.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<? extends Permissions> getPermissions()
    {
        return _permissions;
    }

    /**
     * Gets the permissions entity.
     *
     * @return The optional permissions entity.
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    @CheckReturnValue
    public Optional<PermissionsEntity> getPermissionsEntity()
    {
        return (Optional<PermissionsEntity>) _permissions;
    }

    /**
     * Gets the point references.
     *
     * @return The point references.
     */
    @Nonnull
    @CheckReturnValue
    public Set<PointEntity> getPointReferences()
    {
        return _pointReferences;
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

    /**
     * Gets the store instance.
     *
     * @return The optional store instance.
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    @CheckReturnValue
    public Optional<? extends Store> getStore()
    {
        return (Optional<? extends Store>) getInstance();
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /** {@inheritDoc}
     */
    @Override
    protected Proxied createDefaultInstance()
    {
        return new RMIStore();
    }

    /** Store element name. */
    public static final String ELEMENT_NAME = "Store";

    /** Store entity prefix. */
    public static final String ENTITY_PREFIX = "S";

    /** Store entity reference name. */
    public static final String ENTITY_REFERENCE_NAME = "store";

    private final Optional<? extends Permissions> _permissions;
    private final Set<PointEntity> _pointReferences;

    /**
     * Builder.
     */
    public static class Builder
        extends ProxyEntity.Builder
    {
        /** {@inheritDoc}
         */
        @Override
        public StoreEntity build()
        {
            final StoreEntity storeEntity = new StoreEntity(
                getName(),
                getUUID(),
                getAttributes(),
                getTexts(),
                getParams(),
                getClassDef(),
                getInstance(),
                _permissions,
                (_pointReferences != null)
                ? _pointReferences: new IdentityHashSet<>());

            _pointReferences = null;

            return storeEntity;
        }

        /**
         * Sets the permissions.
         *
         * @param permissions The optional permissions.
         *
         * @return This.
         */
        public Builder setPermissions(
                @Nonnull final Optional<Permissions> permissions)
        {
            _permissions = permissions;

            return this;
        }

        /**
         * Copies the values from an other StoreEntity.
         *
         * @param storeEntity The other StoreEntity.
         *
         * @return This.
         */
        @Nonnull
        protected Builder copyFrom(@Nonnull final StoreEntity storeEntity)
        {
            super.copyFrom(storeEntity);

            _permissions = storeEntity.getPermissions();

            _pointReferences = new IdentityHashSet<>(
                storeEntity.getPointReferences());

            return this;
        }

        private Optional<? extends Permissions> _permissions = Optional.empty();
        private Set<PointEntity> _pointReferences;
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
