/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AttributesDefLoader.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.document.loader;

import java.util.Optional;

import org.rvpf.base.exception.ValidationException;
import org.rvpf.metadata.entity.AttributeDefEntity;
import org.rvpf.metadata.entity.AttributesDefEntity;

/**
 * Attributes definition loader.
 */
final class AttributesDefLoader
    extends MetadataElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
        throws ValidationException
    {
        final AttributesDefEntity.Builder attributesDefBuilder =
            AttributesDefEntity
                .newBuilder();
        final DocumentElement attributesDefElement = getElement();
        final Optional<String> extendsAttribute = attributesDefElement
            .getAttributeValue(EXTENDS_ATTRIBUTE, Optional.empty());
        final AttributesDefEntity extended = extendsAttribute
            .isPresent()? (AttributesDefEntity) getEntity(
                extendsAttribute.get(),
                AttributesDefEntity.ENTITY_PREFIX,
                true)
                .orElse(null): null;

        attributesDefBuilder
            .setUsage(attributesDefElement.getAttributeValue(USAGE_ATTRIBUTE));

        if (extended != null) {
            for (final AttributeDefEntity attributeDef:
                    extended.getAttributeDefs()) {
                attributesDefBuilder.addAttributeDef(attributeDef);
            }
        }

        attributesDefBuilder
            .setHidden(
                attributesDefElement
                    .getAttributeValue(
                            HIDDEN_ATTRIBUTE,
                                    attributesDefBuilder.isHidden()));
        attributesDefBuilder
            .setMultiple(
                attributesDefElement
                    .getAttributeValue(
                            MULTIPLE_ATTRIBUTE,
                                    attributesDefBuilder.isMultiple()));

        final AttributeDefEntity.Builder attributeDefBuilder =
            AttributeDefEntity
                .newBuilder();

        for (final DocumentElement attributeDefElement:
                attributesDefElement.getChildren(ATTRIBUTE_DEF_ELEMENT)) {
            if (!attributeDefElement.isEnabled()) {
                continue;
            }

            attributeDefBuilder
                .setName(
                    attributeDefElement
                        .getAttributeValue(DocumentElement.NAME_ATTRIBUTE));

            attributeDefBuilder
                .setHidden(
                    attributeDefElement
                        .getAttributeValue(HIDDEN_ATTRIBUTE, false))
                .setMultiple(
                    attributeDefElement
                        .getAttributeValue(MULTIPLE_ATTRIBUTE, false));
            attributesDefBuilder.addAttributeDef(attributeDefBuilder.build());
        }

        final AttributesDefEntity attributesDef = attributesDefBuilder.build();

        putEntity(attributesDef.getUsage(), attributesDef);
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
