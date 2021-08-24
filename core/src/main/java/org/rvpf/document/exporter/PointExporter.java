/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PointExporter.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.document.exporter;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.config.TopologicalErrorException;
import org.rvpf.document.loader.DocumentElement;
import org.rvpf.document.loader.MetadataElementLoader;
import org.rvpf.metadata.entity.BehaviorEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.PointInput;
import org.rvpf.metadata.processor.Behavior;

/**
 * Point exporter.
 */
final class PointExporter
    extends ParamsEntityExporter
{
    /**
     * Creates an instance.
     *
     * @param owner The exporter owning this.
     */
    PointExporter(@Nonnull final MetadataExporter owner)
    {
        super(owner, PointEntity.Definition.class);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void export(final Entity entity, final XMLElement element)
    {
        super.export(entity, element);

        final PointEntity pointEntity = (PointEntity) entity;

        for (final String alias: getMetadata().getPointAliases(pointEntity)) {
            final XMLElement aliasElement = createElement(
                MetadataElementLoader.ALIAS_ELEMENT);

            aliasElement.setAttribute(DocumentElement.NAME_ATTRIBUTE, alias);
            element.addChild(aliasElement);
        }

        element
            .setAttribute(
                MetadataElementLoader.ORIGIN_REFERENCE,
                reference(pointEntity.getOriginEntity()));
        element
            .setAttribute(
                MetadataElementLoader.STORE_REFERENCE,
                reference(pointEntity.getStoreEntity()));
        element
            .setAttribute(
                MetadataElementLoader.CONTENT_REFERENCE,
                reference(pointEntity.getContentEntity()));
        element
            .setAttribute(
                MetadataElementLoader.SYNC_REFERENCE,
                reference(pointEntity.getSyncEntity()));
        element
            .setAttribute(
                MetadataElementLoader.TRANSFORM_REFERENCE,
                reference(pointEntity.getTransformEntity()));
        element
            .setAttribute(
                MetadataElementLoader.PERMISSIONS_REFERENCE,
                reference(pointEntity.getPermissionsEntity()));

        for (final PointRelation relation: pointEntity.getInputs()) {
            final PointInput pointInput = (PointInput) relation;
            final XMLElement inputElement = createElement(
                MetadataElementLoader.INPUT_ELEMENT);
            final PointEntity inputPoint = (PointEntity) pointInput
                .getInputPoint();
            boolean firstBehavior = true;

            export(pointInput.getParams(), inputElement);

            inputElement
                .setAttribute(
                    PointEntity.ENTITY_REFERENCE_NAME,
                    (inputPoint == pointEntity)? Optional
                        .of("."): reference(Optional.of(inputPoint)));

            for (Optional<Behavior> behavior = pointInput.getPrimaryBehavior();
                    behavior.isPresent(); behavior = behavior.get().getNext()) {
                final BehaviorEntity behaviorEntity = behavior
                    .get()
                    .getEntity();

                if (!(behaviorEntity.isGenerated()
                        || behavior.get().isInherited())) {
                    if (firstBehavior) {
                        inputElement
                            .setAttribute(
                                MetadataElementLoader.BEHAVIOR_REFERENCE,
                                reference(Optional.of(behaviorEntity)));
                        firstBehavior = false;
                    } else {
                        final XMLElement behaviorElement = createElement(
                            MetadataElementLoader.BEHAVIOR_REFERENCE);

                        behaviorElement
                            .setAttribute(
                                MetadataElementLoader.BEHAVIOR_REFERENCE,
                                reference(Optional.of(behaviorEntity)));

                        inputElement.addChild(behaviorElement);
                    }
                }
            }

            inputElement
                .setAttribute(
                    MetadataElementLoader.SYNC_REFERENCE,
                    reference(pointInput.getSyncEntity()));

            element.addChild(inputElement);
        }

        for (final Point.Replicate replicate: pointEntity.getReplicates()) {
            final XMLElement replicateElement = createElement(
                MetadataElementLoader.REPLICATE_ELEMENT);

            replicateElement
                .setAttribute(
                    MetadataElementLoader.POINT_REFERENCE,
                    reference(Optional.of(replicate.getPoint())));

            if (replicate.getConvert().isPresent()) {
                final Boolean convert = replicate.getConvert().get();

                replicateElement
                    .setAttribute(
                        MetadataElementLoader.CONVERT_ATTRIBUTE,
                        convert.booleanValue());
            }

            element.addChild(replicateElement);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getElementName()
    {
        return MetadataElementLoader.POINT_ENTITY;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void registerReferences(final Entity entity)
    {
        super.registerReferences(entity);

        final PointEntity pointEntity = (PointEntity) entity;

        registerReference(pointEntity.getOriginEntity());
        registerReference(pointEntity.getStoreEntity());
        registerReference(pointEntity.getContentEntity());
        registerReference(pointEntity.getSyncEntity());
        registerReference(pointEntity.getTransformEntity());
        registerReference(pointEntity.getPermissionsEntity());

        for (final PointRelation relation: pointEntity.getInputs()) {
            final PointInput input = (PointInput) relation;
            final PointEntity inputPoint = (PointEntity) input.getInputPoint();

            if (inputPoint != pointEntity) {
                registerReference(Optional.of(inputPoint));
            }

            for (Optional<Behavior> behavior = input.getPrimaryBehavior();
                    behavior.isPresent(); behavior = behavior.get().getNext()) {
                if (!behavior.get().getEntity().isGenerated()) {
                    registerReference(Optional.of(behavior.get().getEntity()));
                }
            }

            registerReference(input.getSyncEntity());
            registerReferences(
                input.getParams(),
                MetadataElementLoader.INPUT_ELEMENT);
        }

        for (final Point.Replicate replicate: pointEntity.getReplicates()) {
            registerReference(Optional.of(replicate.getPoint()));
        }

        try {
            pointEntity.adjustLevel();
        } catch (final TopologicalErrorException exception) {
            throw new RuntimeException(exception);
        }
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
