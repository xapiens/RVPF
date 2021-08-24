/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreLoader.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.document.loader;

import java.util.Optional;

import org.rvpf.base.exception.ValidationException;
import org.rvpf.metadata.entity.PermissionsEntity;
import org.rvpf.metadata.entity.StoreEntity;

/**
 * Store entry.
 */
final class StoreLoader
    extends MetadataElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
        throws ValidationException
    {
        final MetadataFilter filter = getMetadataFilter();

        if (!filter.areStoresNeeded()) {
            return;
        }

        final StoreEntity.Builder storeEntityBuilder = StoreEntity.newBuilder();

        setUpEntityBuilder(storeEntityBuilder);

        final DocumentElement storeElement = getElement();

        {
            final Optional<String> referenceAttribute = storeElement
                .getAttributeValue(CLASS_DEF_REFERENCE, Optional.empty());

            storeEntityBuilder
                .setClassDef(
                    referenceAttribute.isPresent()? Optional
                        .of(
                                getClassDefEntity(
                                        referenceAttribute.get())): Optional
                                                .empty());
        }

        if (filter.arePermissionsNeeded()) {
            final Optional<String> referenceAttribute = storeElement
                .getAttributeValue(PERMISSIONS_REFERENCE, Optional.empty());
            final PermissionsEntity permissionsEntity = referenceAttribute
                .isPresent()? (PermissionsEntity) getEntity(
                    referenceAttribute.get(),
                    PermissionsEntity.ENTITY_PREFIX): null;

            storeEntityBuilder
                .setPermissions(Optional.ofNullable(permissionsEntity));
        }

        storeEntityBuilder
            .setParams(getParams(storeElement, STORE_ENTITY))
            .setAttributes(getAttributes(storeElement));

        final StoreEntity store = storeEntityBuilder.build();

        if (filter.isStoreNeeded(store)) {
            addGroupMember(store);
            getMetadata().addStoreEntity(Optional.of(store));
            anchorEntity(store);
            putEntity(store);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    String getEntityPrefix()
    {
        return StoreEntity.ENTITY_PREFIX;
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
