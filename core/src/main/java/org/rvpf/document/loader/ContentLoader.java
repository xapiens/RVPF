/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ContentLoader.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.document.loader;

import java.util.Optional;

import org.rvpf.base.exception.ValidationException;
import org.rvpf.metadata.entity.ContentEntity;

/**
 * Content loader.
 */
final class ContentLoader
    extends MetadataElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
        throws ValidationException
    {
        if (!getMetadataFilter().areContentsNeeded()) {
            return;
        }

        final ContentEntity.Builder contentBuilder = ContentEntity.newBuilder();

        setUpEntityBuilder(contentBuilder);

        final DocumentElement contentElement = getElement();

        contentBuilder
            .setClassDef(
                Optional
                    .of(
                            getClassDefEntity(
                                    contentElement
                                            .getAttributeValue(
                                                    CLASS_DEF_REFERENCE))))
            .setParams(getParams(contentElement, CONTENT_ENTITY))
            .setAttributes(getAttributes(contentElement));

        final ContentEntity content = contentBuilder.build();

        addGroupMember(content);
        getMetadata().addContentEntity(Optional.of(content));
        anchorEntity(content);
        putEntity(content);
    }

    /** {@inheritDoc}
     */
    @Override
    String getEntityPrefix()
    {
        return ContentEntity.ENTITY_PREFIX;
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
