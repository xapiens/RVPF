/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TransformLoader.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.document.loader;

import java.util.Optional;

import org.rvpf.base.exception.ValidationException;
import org.rvpf.metadata.entity.BehaviorEntity;
import org.rvpf.metadata.entity.ContentEntity;
import org.rvpf.metadata.entity.EngineEntity;
import org.rvpf.metadata.entity.SyncEntity;
import org.rvpf.metadata.entity.TransformEntity;

/**
 * Transform loader.
 */
final class TransformLoader
    extends MetadataElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
        throws ValidationException
    {
        if (!getMetadataFilter().areTransformsNeeded()) {
            return;
        }

        final TransformEntity.Builder transformBuilder = TransformEntity
            .newBuilder();
        final DocumentElement transformElement = getElement();

        setUpEntityBuilder(transformBuilder);

        final EngineEntity engineEntity = (EngineEntity) getEntity(
            transformElement.getAttributeValue(ENGINE_REFERENCE),
            EngineEntity.ENTITY_PREFIX,
            getMetadataFilter().areEnginesFiltered())
            .orElse(null);

        transformBuilder
            .setEngineEntity(Optional.ofNullable(engineEntity))
            .setParams(getParams(transformElement, TRANSFORM_ENTITY))
            .setAttributes(getAttributes(transformElement));

        for (final DocumentElement argElement:
                transformElement.getChildren(ARG_ELEMENT)) {
            if (!argElement.isEnabled()) {
                continue;
            }

            final TransformEntity.Arg.Builder argBuilder = TransformEntity.Arg
                .newBuilder();
            final String behavior = argElement
                .getAttributeValue(BEHAVIOR_REFERENCE, Optional.empty())
                .orElse(null);
            final String content = argElement
                .getAttributeValue(CONTENT_REFERENCE, Optional.empty())
                .orElse(null);
            final String sync = argElement
                .getAttributeValue(SYNC_REFERENCE, Optional.empty())
                .orElse(null);

            if (getMetadataFilter().areBehaviorsNeeded()) {
                if (behavior != null) {
                    argBuilder
                        .addBehaviorEntity(
                            (BehaviorEntity) getEntity(
                                behavior,
                                BehaviorEntity.ENTITY_PREFIX));
                }

                for (final DocumentElement behaviorElement:
                        argElement.getChildren(BEHAVIOR_REFERENCE)) {
                    argBuilder
                        .addBehaviorEntity(
                            (BehaviorEntity) getEntity(
                                behaviorElement
                                        .getAttributeValue(BEHAVIOR_REFERENCE),
                                BehaviorEntity.ENTITY_PREFIX));
                }
            }

            if (getMetadataFilter().areContentsNeeded()) {
                if (content != null) {
                    argBuilder
                        .setContentEntity(
                            (ContentEntity) getEntity(
                                content,
                                ContentEntity.ENTITY_PREFIX));
                }
            }

            if (sync != null) {
                argBuilder
                    .setSyncEntity(
                        (SyncEntity) getEntity(sync, SyncEntity.ENTITY_PREFIX));
            }

            argBuilder
                .setMultiple(
                    argElement.getAttributeValue(MULTIPLE_ATTRIBUTE, false))
                .setParams(getParams(argElement, ARG_ELEMENT));

            transformBuilder.addArg(argBuilder.build());
        }

        final TransformEntity transform = transformBuilder.build();

        addGroupMember(transform);
        getMetadata().addTransformEntity(Optional.of(transform));
        putEntity(transform);
    }

    /** {@inheritDoc}
     */
    @Override
    String getEntityPrefix()
    {
        return TransformEntity.ENTITY_PREFIX;
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
