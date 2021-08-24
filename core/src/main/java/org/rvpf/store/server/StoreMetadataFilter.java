/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreMetadataFilter.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.exception.ValidationException;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.StoreEntity;

/**
 * Store metadata filter.
 */
public class StoreMetadataFilter
    extends MetadataFilter
{
    /**
     * Constructs an instance.
     *
     * @param storeName The store name (empty for the proxy store).
     * @param partnerNames The name of the store partners (optional).
     */
    public StoreMetadataFilter(
            @Nonnull final Optional<String> storeName,
            @Nonnull final Optional<Collection<String>> partnerNames)
    {
        super(false);

        _storeName = storeName.orElse(null);

        if (partnerNames.isPresent()) {
            for (final String partnerName: partnerNames.get()) {
                _partnerNames.add(partnerName.toUpperCase(Locale.ROOT));
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areAttributesNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean areContentsNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean areGroupsNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areOriginsNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areOriginsRequired()
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean arePermissionsNeeded()
    {
        return _permissionsNeeded;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean arePointInputsFlagged()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean arePointReplicatesNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean areStoresFiltered()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean areStoresNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areStoresRequired()
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<String> getClientIdent()
    {
        return Optional.of(_STORE_IDENT);
    }

    /**
     * Gets the store entity.
     *
     * @return The optional store entity.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<StoreEntity> getStoreEntity()
    {
        return Optional.ofNullable(_storeEntity);
    }

    /**
     * Gets the store name.
     *
     * @return The optional store name.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<String> getStoreName()
    {
        return Optional.ofNullable(_storeName);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isPointNeeded(
            final PointEntity pointEntity)
        throws ValidationException
    {
        final Optional<StoreEntity> storeEntity = pointEntity.getStoreEntity();

        if (!storeEntity.isPresent()) {
            return false;
        }

        return (storeEntity.get() == _storeEntity)
               || _partnerEntities.contains(storeEntity.get());
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isStoreNeeded(final StoreEntity storeEntity)
    {
        final String storeName = storeEntity.getName().orElse(null);
        final boolean isTheStoreEntity;

        isTheStoreEntity = (_storeEntity == null)
                && _storeName.equalsIgnoreCase(storeName);

        if (isTheStoreEntity) {
            _storeEntity = storeEntity;
        } else if (getPartnerNames()
            .contains(storeName.toUpperCase(Locale.ROOT))) {
            _partnerEntities.add(storeEntity);
        } else {
            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void reset()
    {
        setPermissionsNeeded(callBack().arePermissionsNeeded());

        _storeEntity = null;

        super.reset();
    }

    /**
     * Gets the partner names.
     *
     * @return The partner names.
     */
    @Nonnull
    @CheckReturnValue
    protected final Set<String> getPartnerNames()
    {
        return _partnerNames;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void includeStoresXML(final XMLElement root)
    {
        if (_partnerEntities.isEmpty()) {
            root
                .addChild(STORES_ELEMENT)
                .setAttribute(STORE_ATTRIBUTE, getStoreName());
        } else {
            final XMLElement storesElement = root.addChild(STORES_ELEMENT);

            storesElement
                .addChild(STORE_ELEMENT)
                .setAttribute(STORE_ATTRIBUTE, _storeName);

            for (final StoreEntity storeEntity: _partnerEntities) {
                storesElement
                    .addChild(STORE_ELEMENT)
                    .setAttribute(STORE_ATTRIBUTE, storeEntity.getName());
            }
        }
    }

    /**
     * Sets the permissions needed indicator.
     *
     * @param permissionsNeeded The permissions needed indicator.
     */
    final void setPermissionsNeeded(final boolean permissionsNeeded)
    {
        _permissionsNeeded = permissionsNeeded;
    }

    private static final String _STORE_IDENT = "Store";

    private final Set<StoreEntity> _partnerEntities = new HashSet<>();
    private final Set<String> _partnerNames = new HashSet<>();
    private boolean _permissionsNeeded;
    private StoreEntity _storeEntity;
    private final String _storeName;
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
