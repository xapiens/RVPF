/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: EngineLoader.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.document.loader;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.rvpf.base.Params;
import org.rvpf.base.exception.ValidationException;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.entity.ClassDefEntity;
import org.rvpf.metadata.entity.EngineEntity;
import org.rvpf.metadata.entity.ProxyEntity;

/**
 * Engine loader.
 */
final class EngineLoader
    extends MetadataElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
        throws ValidationException
    {
        if (!getMetadataFilter().areEnginesNeeded()) {
            return;
        }

        final DocumentElement engineElement = getElement();
        final String clone = engineElement
            .getAttributeValue(CLONE_ATTRIBUTE, Optional.empty())
            .orElse(null);
        final EngineEntity.Builder engineBuilder = EngineEntity.newBuilder();

        if (clone == null) {
            setUpEntityBuilder(engineBuilder);
            engineBuilder
                .setClassDef(
                    Optional
                        .of(
                                getClassDefEntity(
                                        engineElement
                                                .getAttributeValue(
                                                        CLASS_DEF_REFERENCE))))
                .setParams(getParams(engineElement, ENGINE_ENTITY))
                .setAttributes(getAttributes(engineElement));
        } else {
            final EngineEntity clonedEntity =
                (EngineEntity) ((ProxyEntity) getEntity(
                    clone,
                    EngineEntity.ENTITY_PREFIX));
            final Optional<String> referenceAttribute = engineElement
                .getAttributeValue(CLASS_DEF_REFERENCE, Optional.empty());
            final ClassDefEntity classDef = referenceAttribute
                .isPresent()? getClassDefEntity(referenceAttribute.get()): null;

            engineBuilder.copyFrom(clonedEntity);
            setUpEntityBuilder(engineBuilder);

            if (classDef != null) {
                engineBuilder.setClassDef(Optional.ofNullable(classDef));
            }

            final Params clonedParams = clonedEntity.getParams().copy();
            final Optional<Params> newParams = getParams(
                engineElement,
                ENGINE_ENTITY);

            if (newParams.isPresent()) {
                for (final Map.Entry<String, List<Object>> entry:
                        newParams.get().getValuesEntries()) {
                    final String name = entry.getKey();

                    for (final Object value: entry.getValue()) {
                        clonedParams.add(name, value);
                    }
                }
            }

            engineBuilder.setParams(Optional.of(clonedParams));

            final Optional<KeyedGroups> elementAttributes = getAttributes(
                engineElement);

            if (elementAttributes.isPresent()) {
                final KeyedGroups attributesMap = clonedEntity
                    .getAttributes()
                    .get()
                    .copy();

                attributesMap.addAll(elementAttributes.get());
                attributesMap.freeze();
                engineBuilder
                    .setAttributes(
                        attributesMap.isEmpty()? Optional.empty(): Optional
                            .of(attributesMap));
            }
        }

        final EngineEntity engineEntity = engineBuilder.build();

        if (getMetadataFilter().isEngineNeeded(engineEntity)) {
            addGroupMember(engineEntity);
            getMetadata().addEngineEntity(Optional.of(engineEntity));
            putEntity(engineEntity);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    String getEntityPrefix()
    {
        return EngineEntity.ENTITY_PREFIX;
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
