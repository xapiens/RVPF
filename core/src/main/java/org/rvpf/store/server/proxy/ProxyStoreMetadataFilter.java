/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProxyStoreMetadataFilter.java 3951 2019-05-04 19:07:03Z SFB $
 */

package org.rvpf.store.server.proxy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.rvpf.base.exception.ValidationException;
import org.rvpf.base.util.container.IdentityHashSet;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.store.server.StoreMetadataFilter;

/**
 * Proxy Store metadata filter.
 */
public final class ProxyStoreMetadataFilter
    extends StoreMetadataFilter
{
    /**
     * Constructs an instance.
     *
     * @param storeNames The store names.
     */
    public ProxyStoreMetadataFilter(
            @Nonnull final Collection<String> storeNames)
    {
        super(Optional.empty(), Optional.empty());

        for (final String name: storeNames) {
            _storeNames.put(name.toUpperCase(Locale.ROOT), name);
        }

        _storeEntities = new IdentityHashSet<>();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<String> getClientIdent()
    {
        return Optional.of(_PROXY_STORE_IDENT);
    }

    /**
     * Gets the store names.
     *
     * @return The store names.
     */
    public Collection<String> getStoreNames()
    {
        return _storeNames.values();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isPointNeeded(
            final PointEntity pointEntity)
        throws ValidationException
    {
        if (_storeEntities.isEmpty()
                || _storeEntities.contains(
                    pointEntity.getStoreEntity().orElse(null))) {
            return true;
        }

        return super.isPointNeeded(pointEntity);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isStoreNeeded(final StoreEntity storeEntity)
    {
        final boolean needed;

        if (_storeNames.isEmpty()) {
            needed = true;
        } else {
            needed = _storeNames
                .containsKey(
                    storeEntity.getName().get().toUpperCase(Locale.ROOT));
        }

        if (needed) {
            _storeEntities.add(storeEntity);
        }

        return needed;
    }

    /** {@inheritDoc}
     */
    @Override
    public void reset()
    {
        _storeEntities = new IdentityHashSet<>();

        super.reset();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void includeStoresXML(final XMLElement root)
    {
        final XMLElement storesElement = root.addChild(STORES_ELEMENT);

        for (final String name: getStoreNames()) {
            storesElement
                .addChild(STORE_ELEMENT)
                .setAttribute(STORE_ATTRIBUTE, name);
        }
    }

    private static final String _PROXY_STORE_IDENT = "ProxyStore";

    private Set<StoreEntity> _storeEntities;
    private final Map<String, String> _storeNames = new HashMap<>();
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
