/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SyncLoader.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.document.loader;

import java.util.Optional;

import org.rvpf.base.exception.ValidationException;
import org.rvpf.config.entity.ClassDefEntity;
import org.rvpf.metadata.entity.SyncEntity;

/**
 * Sync loader.
 */
final class SyncLoader
    extends MetadataElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
        throws ValidationException
    {
        if (!getMetadataFilter().areSyncsNeeded()) {
            return;
        }

        final SyncEntity.Builder syncEntityBuilder = SyncEntity.newBuilder();

        setUpEntityBuilder(syncEntityBuilder);

        final DocumentElement syncElement = getElement();
        final ClassDefEntity classDef = getClassDefEntity(
            syncElement.getAttributeValue(CLASS_DEF_REFERENCE));

        syncEntityBuilder
            .setClassDef(classDef)
            .setParams(getParams(syncElement, SYNC_ENTITY))
            .setAttributes(getAttributes(syncElement));

        final SyncEntity syncEntity = syncEntityBuilder.build();

        getMetadata().addSyncEntity(Optional.of(syncEntity));
        anchorEntity(syncEntity);
        putEntity(syncEntity);
    }

    /** {@inheritDoc}
     */
    @Override
    String getEntityPrefix()
    {
        return SyncEntity.ENTITY_PREFIX;
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
