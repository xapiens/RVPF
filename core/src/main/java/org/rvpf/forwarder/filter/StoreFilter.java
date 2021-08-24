/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreFilter.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.forwarder.filter;

import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.forwarder.ForwarderModule;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.service.ServiceMessages;

/**
 * Store filter.
 */
public final class StoreFilter
    extends ForwarderFilter.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public Serializable[] filter(Serializable message)
    {
        message = knownPointValue(message).orElse(null);

        return (message != null)? new Serializable[] {message, }: NO_MESSAGES;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean needsMetadata()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final ForwarderModule forwarderModule,
            final KeyedGroups filterProperties)
    {
        if (!super.setUp(forwarderModule, filterProperties)) {
            return false;
        }

        _storeName = filterProperties.getString(STORE_PROPERTY).orElse(null);

        if (_storeName == null) {
            getThisLogger()
                .error(BaseMessages.MISSING_PROPERTY, STORE_PROPERTY);

            return false;
        }

        getThisLogger().debug(ServiceMessages.STORE_NAME, _storeName);

        return loadMetadata(new _MetadataFilter());
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean onNewMetadata(final Metadata metadata)
    {
        if (!metadata.getStoreEntity(Optional.of(_storeName)).isPresent()) {
            getThisLogger().error(ServiceMessages.STORE_NOT_FOUND, _storeName);

            return false;
        }

        return super.onNewMetadata(metadata);
    }

    /**
     * Gets the store name.
     *
     * @return The store name.
     */
    String _getStoreName()
    {
        return Require.notNull(_storeName);
    }

    /** Store property. */
    public static final String STORE_PROPERTY = "store";

    private String _storeName;

    private final class _MetadataFilter
        extends MetadataFilter
    {
        /**
         * Constructs an instance.
         */
        _MetadataFilter()
        {
            super(false);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isPointNeeded(final PointEntity pointEntity)
        {
            return pointEntity.getStoreEntity().orElse(null) == _storeEntity;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isStoreNeeded(final StoreEntity storeEntity)
        {
            final boolean needed;

            needed = (_storeEntity == null)
                     && _getStoreName().equalsIgnoreCase(
                             storeEntity.getName().orElse(null));

            if (needed) {
                _storeEntity = storeEntity;
            }

            return needed;
        }

        /** {@inheritDoc}
         */
        @Override
        public void reset()
        {
            _storeEntity = null;

            super.reset();
        }

        /** {@inheritDoc}
         */
        @Override
        protected void includeStoresXML(final XMLElement root)
        {
            root
                .addChild(STORES_ELEMENT)
                .setAttribute(STORE_ATTRIBUTE, _getStoreName());
        }

        private StoreEntity _storeEntity;
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
